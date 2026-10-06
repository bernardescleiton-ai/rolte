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
                "title" to "Gire e Ganhe!",
                "subtitle" to "Você tem uma chance de ganhar um desconto exclusivo.",
                "code_placeholder" to "Digite seu código",
                "liberate_button" to "LIBERAR ROLETA",
                "spin_button" to "GIRAR AGORA",
                "result_title" to "🎉 PARABÉNS! 🎉",
                "bg_color" to "#0F172A",
                "text_color" to "#FFFFFF",
                "btn_color" to "#10B981"
            )
            for ((k, v) in defaultSettings) {
                repository.saveSetting(SettingEntity(campaignId = campId, key = k, value = v))
            }
            loadCampaignBySlug(slug)
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
}

sealed class CodeStatus {
    object Idle : CodeStatus()
    object Success : CodeStatus()
    data class Error(val message: String) : CodeStatus()
}
