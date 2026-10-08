package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class RoletaViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = Repository(db)

    val campaigns: StateFlow<List<CampaignEntity>> = repository.allCampaigns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSpins: StateFlow<List<SpinEntity>> = repository.allSpins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Public state for active campaign page
    private val _currentCampaign = MutableStateFlow<CampaignEntity?>(null)
    val currentCampaign: StateFlow<CampaignEntity?> = _currentCampaign.asStateFlow()

    private val _prizes = MutableStateFlow<List<PrizeEntity>>(emptyList())
    val prizes: StateFlow<List<PrizeEntity>> = _prizes.asStateFlow()

    private val _accessCodes = MutableStateFlow<List<AccessCodeEntity>>(emptyList())
    val accessCodes: StateFlow<List<AccessCodeEntity>> = _accessCodes.asStateFlow()

    private val _clients = MutableStateFlow<List<ClientEntity>>(emptyList())
    val clients: StateFlow<List<ClientEntity>> = _clients.asStateFlow()

    private val _settings = MutableStateFlow<Map<String, String>>(emptyMap())
    val settings: StateFlow<Map<String, String>> = _settings.asStateFlow()

    // Public UI Flow state
    private val _codeValidationStatus = MutableStateFlow<CodeStatus>(CodeStatus.Idle)
    val codeValidationStatus: StateFlow<CodeStatus> = _codeValidationStatus.asStateFlow()

    private val _activeCode = MutableStateFlow<AccessCodeEntity?>(null)
    val activeCode: StateFlow<AccessCodeEntity?> = _activeCode.asStateFlow()

    private val _spinResult = MutableStateFlow<PrizeEntity?>(null)
    val spinResult: StateFlow<PrizeEntity?> = _spinResult.asStateFlow()

    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    init {
        // Load default campaign on start
        viewModelScope.launch {
            campaigns.collect { list ->
                if (list.isNotEmpty() && _currentCampaign.value == null) {
                    loadCampaignBySlug(list.first().slug)
                }
            }
        }
    }

    private var observeJob: kotlinx.coroutines.Job? = null

    fun loadCampaignBySlug(slug: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val camp = repository.getCampaignBySlug(slug) ?: repository.campaignDao.getAllCampaigns().firstOrNull()?.firstOrNull()
            _currentCampaign.value = camp
            camp?.let { c ->
                launch {
                    repository.getPrizesForCampaign(c.id).collect { prizeList ->
                        _prizes.value = prizeList
                    }
                }
                launch {
                    repository.getCodesForCampaign(c.id).collect { codeList ->
                        _accessCodes.value = codeList
                    }
                }
                launch {
                    repository.getClientsForCampaign(c.id).collect { clientList ->
                        _clients.value = clientList
                    }
                }
                launch {
                    repository.getSettings(c.id).collect { settingsList ->
                        _settings.value = settingsList.associate { it.key to it.value }
                    }
                }
            }
        }
    }

    fun loadCampaignById(id: Long) {
        viewModelScope.launch {
            val camp = repository.getCampaignById(id)
            _currentCampaign.value = camp
            camp?.let { c ->
                loadCampaignBySlug(c.slug)
            }
        }
    }

    fun validateCode(codeStr: String) {
        val camp = _currentCampaign.value ?: return
        if (!camp.active) {
            _codeValidationStatus.value = CodeStatus.Error("Campanha encerrada.")
            return
        }

        viewModelScope.launch {
            val trimmed = codeStr.trim().uppercase()
            if (trimmed.isEmpty()) {
                _codeValidationStatus.value = CodeStatus.Error("Digite um código válido.")
                return@launch
            }

            val codeEntity = repository.getCodeByString(camp.id, trimmed)
            if (codeEntity == null) {
                _codeValidationStatus.value = CodeStatus.Error("Código inválido.")
                return@launch
            }

            when (codeEntity.status) {
                "USED" -> {
                    _codeValidationStatus.value = CodeStatus.Error("Este código já foi utilizado.")
                }
                "INACTIVE" -> {
                    _codeValidationStatus.value = CodeStatus.Error("Este código está inativo.")
                }
                "AVAILABLE" -> {
                    _activeCode.value = codeEntity
                    _codeValidationStatus.value = CodeStatus.Success
                }
                else -> {
                    _codeValidationStatus.value = CodeStatus.Error("Status de código desconhecido.")
                }
            }
        }
    }

    fun spinWheel(onResultReady: (PrizeEntity, Int) -> Unit) {
        val camp = _currentCampaign.value ?: return
        val code = _activeCode.value ?: return
        if (_isSpinning.value) return

        _isSpinning.value = true

        viewModelScope.launch {
            // Re-verify code atomically
            val freshCode = repository.getCodeByString(camp.id, code.code)
            if (freshCode == null || freshCode.status != "AVAILABLE") {
                _isSpinning.value = false
                _codeValidationStatus.value = CodeStatus.Error("Código inválido ou já utilizado.")
                return@launch
            }

            // Get active prizes with available stock
            val activePrizes = repository.prizeDao.getActivePrizesForCampaign(camp.id).filter {
                it.unlimitedQuantity || it.quantity > 0
            }

            if (activePrizes.isEmpty()) {
                _isSpinning.value = false
                _codeValidationStatus.value = CodeStatus.Error("Nenhum prêmio disponível no momento (esgotado).")
                return@launch
            }

            // Weighted random selection
            val totalWeight = activePrizes.sumOf { it.weight }
            if (totalWeight <= 0) {
                _isSpinning.value = false
                _codeValidationStatus.value = CodeStatus.Error("Configuração de pesos inválida.")
                return@launch
            }

            var randomWeight = Random.nextInt(totalWeight)
            var selectedPrize = activePrizes.first()
            for (prize in activePrizes) {
                if (randomWeight < prize.weight) {
                    selectedPrize = prize
                    break
                }
                randomWeight -= prize.weight
            }

            // Mark code as USED atomically
            val updatedCode = freshCode.copy(status = "USED", usedAt = System.currentTimeMillis())
            repository.updateCode(updatedCode)
            _activeCode.value = updatedCode

            // Decrement stock if not unlimited
            if (!selectedPrize.unlimitedQuantity && selectedPrize.quantity > 0) {
                val newQty = selectedPrize.quantity - 1
                val updatedPrize = selectedPrize.copy(
                    quantity = newQty,
                    active = if (newQty == 0) false else selectedPrize.active
                )
                repository.updatePrize(updatedPrize)
            }

            // Record spin
            val spin = SpinEntity(
                campaignId = camp.id,
                accessCodeId = updatedCode.id,
                prizeId = selectedPrize.id,
                prizeNameSnapshot = selectedPrize.name,
                discountSnapshot = selectedPrize.discount
            )
            repository.insertSpin(spin)

            _spinResult.value = selectedPrize

            // Determine index of prize in activePrizes for wheel target angle calculation
            val prizeIndex = activePrizes.indexOf(selectedPrize)
            
            onResultReady(selectedPrize, prizeIndex)
        }
    }

    fun resetPublicState() {
        _activeCode.value = null
        _spinResult.value = null
        _codeValidationStatus.value = CodeStatus.Idle
        _isSpinning.value = false
    }

    // Admin CRUD Operations
    fun createCampaign(name: String, slug: String) {
        viewModelScope.launch {
            val campId = repository.insertCampaign(CampaignEntity(name = name, slug = slug))
            // Insert default settings for new campaign
            val defaultSettings = mapOf(
                "title" to name.uppercase(),
                "subtitle" to "Gire a roleta e ganhe descontos e benefícios exclusivos!",
                "code_placeholder" to "Digite seu código",
                "liberate_button" to "LIBERAR ROLETA",
                "spin_button" to "GIRAR AGORA",
                "result_title" to "🎉 PARABÉNS! 🎉",
                "bg_color" to "#0F172A",
                "text_color" to "#FFFFFF",
                "btn_color" to "#FACC15"
            )
            for ((k, v) in defaultSettings) {
                repository.saveSetting(SettingEntity(campaignId = campId, key = k, value = v))
            }

            // Insert initial separated prizes for this campaign
            val starterPrizes = listOf(
                PrizeEntity(campaignId = campId, name = "R$ 5 de Desconto", description = "Desconto exclusivo", displayText = "R$ 5", resultText = "R$ 5 DE DESCONTO", discount = "R$ 5", weight = 80, quantity = 500, unlimitedQuantity = false, active = true),
                PrizeEntity(campaignId = campId, name = "R$ 10 de Desconto", description = "Desconto especial", displayText = "R$ 10", resultText = "R$ 10 DE DESCONTO", discount = "R$ 10", weight = 60, quantity = 300, unlimitedQuantity = false, active = true),
                PrizeEntity(campaignId = campId, name = "R$ 15 de Desconto", description = "Super desconto", displayText = "R$ 15", resultText = "R$ 15 DE DESCONTO", discount = "R$ 15", weight = 20, quantity = 150, unlimitedQuantity = false, active = true),
                PrizeEntity(campaignId = campId, name = "R$ 20 de Desconto", description = "Desconto máximo", displayText = "R$ 20", resultText = "R$ 20 DE DESCONTO", discount = "R$ 20", weight = 10, quantity = 50, unlimitedQuantity = false, active = true),
                PrizeEntity(campaignId = campId, name = "GIRO GRÁTIS", description = "Giro da sorte", displayText = "GIRO GRÁTIS", resultText = "VOCÊ GANHOU MAIS UM GIRO DA SORTE!", discount = "GIRO GRÁTIS", weight = 10, quantity = 20, unlimitedQuantity = false, active = true)
            )
            for (p in starterPrizes) {
                repository.insertPrize(p)
            }

            // Generate initial separated access codes for this campaign
            for (i in 1..8) {
                val rand = (100000..999999).random().toString(36).uppercase()
                repository.insertCode(AccessCodeEntity(campaignId = campId, code = "RLT-$rand", status = "AVAILABLE"))
            }

            loadCampaignById(campId)
        }
    }

    fun toggleCampaignActive(campaign: CampaignEntity) {
        viewModelScope.launch {
            val updated = campaign.copy(active = !campaign.active)
            repository.updateCampaign(updated)
            if (_currentCampaign.value?.id == campaign.id) {
                _currentCampaign.value = updated
            }
        }
    }

    fun updateCampaign(campaign: CampaignEntity) {
        viewModelScope.launch {
            repository.updateCampaign(campaign)
            loadCampaignBySlug(campaign.slug)
        }
    }

    fun deleteCampaign(id: Long) {
        viewModelScope.launch {
            repository.deleteCampaign(id)
            if (_currentCampaign.value?.id == id) {
                val remaining = repository.campaignDao.getAllCampaigns().firstOrNull()?.firstOrNull()
                if (remaining != null) {
                    loadCampaignById(remaining.id)
                } else {
                    _currentCampaign.value = null
                }
            }
        }
    }

    fun savePrize(prize: PrizeEntity) {
        viewModelScope.launch {
            if (prize.id == 0L) {
                repository.insertPrize(prize)
            } else {
                repository.updatePrize(prize)
            }
        }
    }

    fun deletePrize(id: Long) {
        viewModelScope.launch {
            repository.deletePrize(id)
        }
    }

    fun generateCodes(campaignId: Long, count: Int) {
        viewModelScope.launch {
            val newCodes = mutableListOf<AccessCodeEntity>()
            val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            for (i in 0 until count) {
                val sb = StringBuilder("RLT-")
                for (j in 0 until 6) {
                    sb.append(charset[Random.nextInt(charset.length)])
                }
                newCodes.add(
                    AccessCodeEntity(
                        campaignId = campaignId,
                        code = sb.toString(),
                        status = "AVAILABLE"
                    )
                )
            }
            repository.insertCodes(newCodes)
        }
    }

    fun addManualCode(campaignId: Long, codeStr: String) {
        viewModelScope.launch {
            val trimmed = codeStr.trim().uppercase()
            if (trimmed.isNotEmpty()) {
                repository.insertCode(
                    AccessCodeEntity(
                        campaignId = campaignId,
                        code = trimmed,
                        status = "AVAILABLE"
                    )
                )
            }
        }
    }

    fun updateCodeStatus(code: AccessCodeEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updateCode(code.copy(status = newStatus))
        }
    }

    fun deleteCode(id: Long) {
        viewModelScope.launch {
            repository.deleteCode(id)
        }
    }

    fun deleteCodes(ids: List<Long>) {
        viewModelScope.launch {
            repository.deleteCodes(ids)
        }
    }

    fun deleteUsedCodes(campaignId: Long) {
        viewModelScope.launch {
            repository.deleteUsedCodes(campaignId)
        }
    }

    fun updateSpinClientInfo(spinId: Long, campaignId: Long, accessCodeId: Long, prizeId: Long, clientName: String?, observation: String?, prizeName: String, discount: String, createdAt: Long) {
        viewModelScope.launch {
            val spin = SpinEntity(
                id = spinId,
                campaignId = campaignId,
                accessCodeId = accessCodeId,
                prizeId = prizeId,
                clientName = clientName,
                observation = observation,
                prizeNameSnapshot = prizeName,
                discountSnapshot = discount,
                createdAt = createdAt
            )
            repository.updateSpin(spin)
        }
    }

    fun deleteSpin(id: Long) {
        viewModelScope.launch {
            repository.deleteSpin(id)
        }
    }

    fun deleteSpins(ids: List<Long>) {
        viewModelScope.launch {
            repository.deleteSpins(ids)
        }
    }

    fun clearAllSpins() {
        viewModelScope.launch {
            repository.clearAllSpins()
        }
    }

    fun saveSetting(campaignId: Long, key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(SettingEntity(campaignId = campaignId, key = key, value = value))
        }
    }

    // Bulk Clients & WhatsApp Management
    fun addBulkClients(campaignId: Long, rawText: String, onFinished: (Int) -> Unit = {}) {
        viewModelScope.launch {
            if (rawText.isBlank()) return@launch
            val parsedList = parseBulkClientsInput(rawText)
            if (parsedList.isEmpty()) return@launch

            val clientsToInsert = mutableListOf<ClientEntity>()
            val codesToInsert = mutableListOf<AccessCodeEntity>()

            for (item in parsedList) {
                val rawPhone = item.whatsapp.trim()
                val cleanDigits = rawPhone.filter { it.isDigit() }
                var formattedPhone = rawPhone
                if (cleanDigits.length == 11) {
                    formattedPhone = "(${cleanDigits.substring(0, 2)}) ${cleanDigits.substring(2, 7)}-${cleanDigits.substring(7)}"
                } else if (cleanDigits.length == 10) {
                    formattedPhone = "(${cleanDigits.substring(0, 2)}) ${cleanDigits.substring(2, 6)}-${cleanDigits.substring(6)}"
                }

                val phoneForWa = if (!cleanDigits.startsWith("55") && (cleanDigits.length == 10 || cleanDigits.length == 11)) {
                    "55$cleanDigits"
                } else {
                    cleanDigits
                }

                val codeToUse = if (!item.codigo.isNullOrBlank()) {
                    item.codigo.trim().uppercase()
                } else {
                    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
                    val rand = (1..6).map { chars.random() }.joinToString("")
                    "RLT-$rand"
                }

                codesToInsert.add(
                    AccessCodeEntity(
                        campaignId = campaignId,
                        code = codeToUse,
                        status = "AVAILABLE"
                    )
                )

                clientsToInsert.add(
                    ClientEntity(
                        campaignId = campaignId,
                        nome = item.nome.trim(),
                        whatsapp = formattedPhone,
                        cleanPhone = phoneForWa,
                        vencimento = item.vencimento.trim(),
                        codigo = codeToUse,
                        sent = false,
                        sentAt = null
                    )
                )
            }

            repository.insertCodes(codesToInsert)
            repository.insertClients(clientsToInsert)
            onFinished(clientsToInsert.size)
        }
    }

    fun toggleClientSent(client: ClientEntity) {
        viewModelScope.launch {
            repository.updateClient(
                client.copy(
                    sent = !client.sent,
                    sentAt = if (!client.sent) System.currentTimeMillis() else null
                )
            )
        }
    }

    fun markClientSent(client: ClientEntity) {
        viewModelScope.launch {
            repository.updateClient(
                client.copy(
                    sent = true,
                    sentAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.deleteClient(client)
        }
    }

    fun deleteClients(ids: List<Long>) {
        viewModelScope.launch {
            repository.deleteClients(ids)
        }
    }

    fun clearClientsForCampaign(campaignId: Long) {
        viewModelScope.launch {
            repository.clearClientsForCampaign(campaignId)
        }
    }

    private data class ParsedClientInput(
        val nome: String,
        val whatsapp: String,
        val vencimento: String,
        val codigo: String?
    )

    private data class PhoneAndVenc(val whatsapp: String, val vencimento: String)

    private fun extractPhoneAndVencimento(val1: String, val2: String): PhoneAndVenc {
        val s1 = val1.trim()
        val s2 = val2.trim()

        val isDate1 = s1.contains(Regex("\\d{1,2}[/\\-\\.]\\d{1,2}[/\\-\\.]\\d{2,4}")) || s1.contains(Regex("^\\d{4}[/\\-\\.]\\d{1,2}"))
        val isDate2 = s2.contains(Regex("\\d{1,2}[/\\-\\.]\\d{1,2}[/\\-\\.]\\d{2,4}")) || s2.contains(Regex("^\\d{4}[/\\-\\.]\\d{1,2}"))

        val digits1 = s1.filter { it.isDigit() }
        val digits2 = s2.filter { it.isDigit() }

        if (isDate1 && !isDate2) {
            return PhoneAndVenc(whatsapp = s2, vencimento = s1)
        }
        if (isDate2 && !isDate1) {
            return PhoneAndVenc(whatsapp = s1, vencimento = s2)
        }
        if (digits1.length >= 10 && !s1.contains('/') && s2.contains('/')) {
            return PhoneAndVenc(whatsapp = s1, vencimento = s2)
        }
        if (digits2.length >= 10 && !s2.contains('/') && s1.contains('/')) {
            return PhoneAndVenc(whatsapp = s2, vencimento = s1)
        }
        return PhoneAndVenc(whatsapp = s1, vencimento = s2)
    }

    private fun parseBulkClientsInput(rawText: String): List<ParsedClientInput> {
        val result = mutableListOf<ParsedClientInput>()
        val blocks = rawText.split(Regex("\\n\\s*\\n+")).map { it.trim() }.filter { it.isNotBlank() }

        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.size == 3) {
                val pair = extractPhoneAndVencimento(lines[1], lines[2])
                result.add(ParsedClientInput(lines[0], pair.whatsapp, pair.vencimento, null))
            } else if (lines.size == 4) {
                val pair = extractPhoneAndVencimento(lines[1], lines[2])
                val l3 = lines[3]
                if (l3.startsWith("RLT-", ignoreCase = true) || l3.matches(Regex("^[A-Z0-9-]{5,15}$", RegexOption.IGNORE_CASE))) {
                    result.add(ParsedClientInput(lines[0], pair.whatsapp, pair.vencimento, l3.uppercase()))
                } else {
                    result.add(ParsedClientInput(lines[0], pair.whatsapp, pair.vencimento, l3))
                }
            } else if (lines.size > 4) {
                var i = 0
                while (i < lines.size) {
                    val l0 = lines.getOrNull(i)
                    val l1 = lines.getOrNull(i + 1)
                    val l2 = lines.getOrNull(i + 2)
                    val l3 = lines.getOrNull(i + 3)
                    if (l0 != null && l1 != null && l2 != null) {
                        val pair = extractPhoneAndVencimento(l1, l2)
                        if (l3 != null && (l3.startsWith("RLT-", ignoreCase = true) || l3.matches(Regex("^[A-Z0-9-]{5,15}$", RegexOption.IGNORE_CASE)))) {
                            result.add(ParsedClientInput(l0, pair.whatsapp, pair.vencimento, l3.uppercase()))
                            i += 4
                        } else {
                            result.add(ParsedClientInput(l0, pair.whatsapp, pair.vencimento, null))
                            i += 3
                        }
                    } else {
                        i++
                    }
                }
            } else if (lines.size == 1 && (lines[0].contains(';') || lines[0].contains(',') || lines[0].contains('\t'))) {
                val parts = lines[0].split(Regex("[;,\\t|]+")).map { it.trim() }.filter { it.isNotBlank() }
                if (parts.size >= 3) {
                    val pair = extractPhoneAndVencimento(parts[1], parts[2])
                    result.add(
                        ParsedClientInput(
                            nome = parts[0],
                            whatsapp = pair.whatsapp,
                            vencimento = pair.vencimento,
                            codigo = parts.getOrNull(3)?.uppercase()
                        )
                    )
                }
            }
        }
        return result
    }
}

sealed class CodeStatus {
    object Idle : CodeStatus()
    object Success : CodeStatus()
    data class Error(val message: String) : CodeStatus()
}
