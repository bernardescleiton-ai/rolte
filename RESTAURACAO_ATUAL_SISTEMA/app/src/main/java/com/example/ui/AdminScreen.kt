package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.example.data.*
import com.example.viewmodel.RoletaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: RoletaViewModel,
    onNavigatePublic: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Campanhas", "Prêmios", "Códigos", "Resultados", "Clientes", "Configurações")

    val campaigns by viewModel.campaigns.collectAsState()
    val currentCampaign by viewModel.currentCampaign.collectAsState()
    val prizes by viewModel.prizes.collectAsState()
    val accessCodes by viewModel.accessCodes.collectAsState()
    val allSpins by viewModel.allSpins.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showNewCampaignDialog by remember { mutableStateOf(false) }
    var showNewPrizeDialog by remember { mutableStateOf(false) }
    var showGenerateCodesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Painel Administrativo - Roleta da Sorte", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    currentCampaign?.let { camp ->
                        IconButton(onClick = { onNavigatePublic(camp.slug) }) {
                            Icon(Icons.Default.Visibility, contentDescription = "Ver Roleta Pública")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.Campaign, contentDescription = title)
                                1 -> Icon(Icons.Default.CardGiftcard, contentDescription = title)
                                2 -> Icon(Icons.Default.VpnKey, contentDescription = title)
                                3 -> Icon(Icons.Default.Assessment, contentDescription = title)
                                4 -> Icon(Icons.Default.People, contentDescription = title)
                                5 -> Icon(Icons.Default.Settings, contentDescription = title)
                                else -> Icon(Icons.Default.Star, contentDescription = title)
                            }
                        },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedTab) {
                0 -> CampaignsTab(
                    campaigns = campaigns,
                    currentCampaign = currentCampaign,
                    onSelect = { viewModel.loadCampaignById(it) },
                    onToggleActive = { viewModel.toggleCampaignActive(it) },
                    onNew = { showNewCampaignDialog = true },
                    onDelete = { viewModel.deleteCampaign(it) }
                )
                1 -> PrizesTab(
                    currentCampaign = currentCampaign,
                    prizes = prizes,
                    onSave = { viewModel.savePrize(it) },
                    onDelete = { viewModel.deletePrize(it) },
                    onNew = { showNewPrizeDialog = true }
                )
                2 -> CodesTab(
                    currentCampaign = currentCampaign,
                    codes = accessCodes,
                    onGenerate = { count -> currentCampaign?.let { viewModel.generateCodes(it.id, count) } },
                    onAddManual = { code -> currentCampaign?.let { viewModel.addManualCode(it.id, code) } },
                    onUpdateStatus = { code, status -> viewModel.updateCodeStatus(code, status) },
                    onDelete = { viewModel.deleteCode(it) },
                    onDeleteCodes = { viewModel.deleteCodes(it) },
                    onDeleteUsedCodes = { currentCampaign?.let { viewModel.deleteUsedCodes(it.id) } }
                )
                3 -> ResultsTab(
                    spins = allSpins,
                    currentCampaign = currentCampaign,
                    onUpdateSpin = { spin, clientName, obs ->
                        viewModel.updateSpinClientInfo(
                            spinId = spin.id,
                            campaignId = spin.campaignId,
                            accessCodeId = spin.accessCodeId,
                            prizeId = spin.prizeId,
                            clientName = clientName,
                            observation = obs,
                            prizeName = spin.prizeNameSnapshot,
                            discount = spin.discountSnapshot,
                            createdAt = spin.createdAt
                        )
                    },
                    onDeleteSpin = { viewModel.deleteSpin(it) },
                    onDeleteSpins = { viewModel.deleteSpins(it) },
                    onClearAllSpins = { viewModel.clearAllSpins() }
                )
                4 -> ClientsTab(
                    currentCampaign = currentCampaign,
                    clients = clients,
                    promoTemplate = settings["promo_message"] ?: "Olá {nome}! Seu plano vence em {vencimento}.\nVocê ganhou um giro exclusivo na nossa Roleta da Sorte!\nAcesse o link abaixo e use o seu código para liberar a roleta:\n{link}\nSeu código: {codigo}\nBoa sorte! 🎉",
                    onSavePromoTemplate = { tmpl ->
                        currentCampaign?.let { viewModel.saveSetting(it.id, "promo_message", tmpl) }
                    },
                    onAddBulkClients = { rawText ->
                        currentCampaign?.let { viewModel.addBulkClients(it.id, rawText) }
                    },
                    onToggleSent = { viewModel.toggleClientSent(it) },
                    onMarkSent = { viewModel.markClientSent(it) },
                    onDeleteClient = { viewModel.deleteClient(it) },
                    onDeleteClients = { viewModel.deleteClients(it) },
                    onClearClients = { currentCampaign?.let { viewModel.clearClientsForCampaign(it.id) } }
                )
                5 -> SettingsTab(
                    currentCampaign = currentCampaign,
                    settings = settings,
                    onSave = { key, value ->
                        currentCampaign?.let { viewModel.saveSetting(it.id, key, value) }
                    }
                )
            }
        }

        // New Campaign Dialog
        if (showNewCampaignDialog) {
            var name by remember { mutableStateOf("") }
            var slug by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewCampaignDialog = false },
                title = { Text("Nova Campanha") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; slug = it.lowercase().replace(Regex("[^a-z0-9]"), "-") },
                            label = { Text("Nome da Campanha") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = slug,
                            onValueChange = { slug = it },
                            label = { Text("Slug da URL (ex: outubro)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (name.isNotBlank() && slug.isNotBlank()) {
                            viewModel.createCampaign(name, slug)
                            showNewCampaignDialog = false
                        }
                    }) {
                        Text("Criar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewCampaignDialog = false }) { Text("Cancelar") }
                }
            )
        }

        // New Prize Dialog
        if (showNewPrizeDialog && currentCampaign != null) {
            var name by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
            var displayText by remember { mutableStateOf("") }
            var resultText by remember { mutableStateOf("") }
            var discount by remember { mutableStateOf("") }
            var weight by remember { mutableStateOf("10") }
            var quantity by remember { mutableStateOf("100") }
            var unlimited by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showNewPrizeDialog = false },
                title = { Text("Adicionar Prêmio") },
                text = {
                    LazyColumn(modifier = Modifier.height(350.dp)) {
                        item {
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do Prêmio") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = displayText, onValueChange = { displayText = it }, label = { Text("Texto na Roleta (ex: 20%)") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = resultText, onValueChange = { resultText = it }, label = { Text("Texto no Resultado (ex: 20% DE DESCONTO)") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Valor Desconto (ex: 20%)") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Peso (ex: 20)") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantidade Estoque") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val w = weight.toIntOrNull() ?: 10
                        val q = quantity.toIntOrNull() ?: 100
                        viewModel.savePrize(
                            PrizeEntity(
                                campaignId = currentCampaign!!.id,
                                name = name,
                                description = description,
                                displayText = displayText,
                                resultText = resultText,
                                discount = discount,
                                weight = w,
                                quantity = q,
                                unlimitedQuantity = unlimited,
                                active = true
                            )
                        )
                        showNewPrizeDialog = false
                    }) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewPrizeDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
fun CampaignsTab(
    campaigns: List<CampaignEntity>,
    currentCampaign: CampaignEntity?,
    onSelect: (Long) -> Unit,
    onToggleActive: (CampaignEntity) -> Unit,
    onNew: () -> Unit,
    onDelete: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Campanhas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Cada campanha possui seus prêmios, códigos e giros separados", fontSize = 12.sp, color = Color.Gray)
            }
            Button(onClick = onNew) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nova Campanha")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(campaigns) { camp ->
                val isSelected = currentCampaign?.id == camp.id
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(camp.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(if (camp.active) "Ativa" else "Inativa", color = if (camp.active) Color(0xFF10B981) else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Link URL: /roleta/${camp.slug}", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onSelect(camp.id) }) {
                                Text(if (isSelected) "Selecionada (Em Edição)" else "Selecionar")
                            }
                            OutlinedButton(onClick = { onToggleActive(camp) }) {
                                Text(if (camp.active) "Pausar" else "Ativar")
                            }
                            if (campaigns.size > 1) {
                                OutlinedButton(onClick = { onDelete(camp.id) }) {
                                    Text("Excluir", color = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrizesTab(
    currentCampaign: CampaignEntity?,
    prizes: List<PrizeEntity>,
    onSave: (PrizeEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onNew: () -> Unit
) {
    if (currentCampaign == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Selecione uma campanha primeiro.")
        }
        return
    }

    val totalWeight = prizes.filter { it.active }.sumOf { it.weight }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Prêmios (${currentCampaign.name})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = onNew) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adicionar Prêmio")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(prizes) { prize ->
                val probability = if (totalWeight > 0 && prize.active) (prize.weight.toFloat() / totalWeight) * 100f else 0f
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(prize.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(String.format("Prob: %.1f%%", probability), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Desconto: ${prize.discount} | Peso: ${prize.weight} | Estoque: ${if (prize.unlimitedQuantity) "Ilimitado" else prize.quantity.toString()}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onSave(prize.copy(active = !prize.active)) }) {
                                Text(if (prize.active) "Desativar" else "Ativar")
                            }
                            OutlinedButton(onClick = { onDelete(prize.id) }) {
                                Text("Excluir", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodesTab(
    currentCampaign: CampaignEntity?,
    codes: List<AccessCodeEntity>,
    onGenerate: (Int) -> Unit,
    onAddManual: (String) -> Unit,
    onUpdateStatus: (AccessCodeEntity, String) -> Unit,
    onDelete: (Long) -> Unit,
    onDeleteCodes: (List<Long>) -> Unit = {},
    onDeleteUsedCodes: () -> Unit = {}
) {
    if (currentCampaign == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Selecione uma campanha primeiro.")
        }
        return
    }

    var manualCodeInput by remember { mutableStateOf("") }
    var generateCountInput by remember { mutableStateOf("50") }
    val selectedCodeIds = remember { mutableStateListOf<Long>() }
    val usedCodes = remember(codes) { codes.filter { it.status == "USED" } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gerenciamento de Códigos (${currentCampaign.name})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = manualCodeInput,
                onValueChange = { manualCodeInput = it },
                label = { Text("Código Manual") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (manualCodeInput.isNotBlank()) {
                    onAddManual(manualCodeInput)
                    manualCodeInput = ""
                }
            }, modifier = Modifier.align(Alignment.CenterVertically)) {
                Text("Adicionar")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = generateCountInput,
                onValueChange = { generateCountInput = it },
                label = { Text("Quantidade para Gerar") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                val count = generateCountInput.toIntOrNull() ?: 10
                onGenerate(count)
            }, modifier = Modifier.align(Alignment.CenterVertically)) {
                Text("Gerar Lote")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions for used codes
        if (usedCodes.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Usados: ${usedCodes.size} | Selecionados: ${selectedCodeIds.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = {
                        if (selectedCodeIds.size == usedCodes.size) {
                            selectedCodeIds.clear()
                        } else {
                            selectedCodeIds.clear()
                            selectedCodeIds.addAll(usedCodes.map { it.id })
                        }
                    }) {
                        Text(if (selectedCodeIds.size == usedCodes.size) "Desmarcar" else "Selecionar Usados")
                    }

                    if (selectedCodeIds.isNotEmpty()) {
                        Button(
                            onClick = {
                                onDeleteCodes(selectedCodeIds.toList())
                                selectedCodeIds.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excluir (${selectedCodeIds.size})")
                        }
                    } else {
                        Button(
                            onClick = {
                                onDeleteUsedCodes()
                                selectedCodeIds.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                        ) {
                            Text("Excluir Todos Usados")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text("Lista de Códigos (${codes.size})", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
            items(codes) { code ->
                val isUsed = code.status == "USED"
                val isSelected = selectedCodeIds.contains(code.id)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isUsed) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedCodeIds.add(code.id) else selectedCodeIds.remove(code.id)
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Column {
                                Text(code.code, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Status: ${code.status}", color = when(code.status) {
                                    "AVAILABLE" -> Color(0xFF10B981)
                                    "USED" -> Color(0xFFEAB308)
                                    else -> Color.Gray
                                })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (code.status == "AVAILABLE") {
                                OutlinedButton(onClick = { onUpdateStatus(code, "INACTIVE") }) { Text("Inativar") }
                            } else if (code.status == "INACTIVE") {
                                OutlinedButton(onClick = { onUpdateStatus(code, "AVAILABLE") }) { Text("Ativar") }
                            }
                            IconButton(onClick = {
                                selectedCodeIds.remove(code.id)
                                onDelete(code.id)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultsTab(
    spins: List<SpinEntity>,
    currentCampaign: CampaignEntity?,
    onUpdateSpin: (SpinEntity, String?, String?) -> Unit,
    onDeleteSpin: (Long) -> Unit,
    onDeleteSpins: (List<Long>) -> Unit,
    onClearAllSpins: () -> Unit
) {
    val selectedIds = remember { mutableStateListOf<Long>() }
    val filteredSpins = remember(spins, currentCampaign) {
        if (currentCampaign != null) spins.filter { it.campaignId == currentCampaign.id } else spins
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (currentCampaign != null) "Resultados (${currentCampaign.name})" else "Resultados dos Giros",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text("${filteredSpins.size} registrado(s)", fontSize = 12.sp, color = Color.Gray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedIds.isNotEmpty()) {
                    Button(
                        onClick = {
                            onDeleteSpins(selectedIds.toList())
                            selectedIds.clear()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir Selecionados", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir (${selectedIds.size})")
                    }
                } else if (filteredSpins.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onClearAllSpins() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Excluir Todos")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredSpins.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val allSelected = selectedIds.size == filteredSpins.size
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = { checked ->
                        selectedIds.clear()
                        if (checked) {
                            selectedIds.addAll(filteredSpins.map { it.id })
                        }
                    }
                )
                Text(
                    if (allSelected) "Desmarcar Todos" else "Selecionar Todos (${filteredSpins.size})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredSpins) { spin ->
                var clientName by remember { mutableStateOf(spin.clientName ?: "") }
                var observation by remember { mutableStateOf(spin.observation ?: "") }
                var isEditing by remember { mutableStateOf(false) }
                val isSelected = selectedIds.contains(spin.id)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedIds.add(spin.id) else selectedIds.remove(spin.id)
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text("Prêmio: ${spin.prizeNameSnapshot}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("Desconto: ${spin.discountSnapshot}", fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(onClick = { onDeleteSpin(spin.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isEditing) {
                            OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Cliente") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(value = observation, onValueChange = { observation = it }, label = { Text("Observação") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                onUpdateSpin(spin, clientName.ifBlank { null }, observation.ifBlank { null })
                                isEditing = false
                            }) {
                                Text("Salvar")
                            }
                        } else {
                            Text("Cliente: ${spin.clientName ?: "Não informado"}")
                            Text("Observação: ${spin.observation ?: "Nenhuma"}")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { isEditing = true }) {
                                Text("Editar Dados do Cliente")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    currentCampaign: CampaignEntity?,
    settings: Map<String, String>,
    onSave: (String, String) -> Unit
) {
    if (currentCampaign == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Selecione uma campanha primeiro.")
        }
        return
    }

    var title by remember(currentCampaign.id) { mutableStateOf(settings["title"] ?: "") }
    var subtitle by remember(currentCampaign.id) { mutableStateOf(settings["subtitle"] ?: "") }
    var btnText by remember(currentCampaign.id) { mutableStateOf(settings["spin_button"] ?: "") }
    var bgColor by remember(currentCampaign.id) { mutableStateOf(settings["bg_color"] ?: "") }
    var bgImage by remember(currentCampaign.id) { mutableStateOf(settings["bg_image"] ?: "") }
    var btnColor by remember(currentCampaign.id) { mutableStateOf(settings["btn_color"] ?: "") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Configurações da Página (${currentCampaign.name})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título Principal") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtítulo") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = btnText, onValueChange = { btnText = it }, label = { Text("Texto Botão Girar") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = bgColor, onValueChange = { bgColor = it }, label = { Text("Cor de Fundo (HEX ex: #0F172A)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = bgImage, onValueChange = { bgImage = it }, label = { Text("URL da Imagem de Fundo (Opcional)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = btnColor, onValueChange = { btnColor = it }, label = { Text("Cor do Botão (HEX ex: #10B981)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    onSave("title", title)
                    onSave("subtitle", subtitle)
                    onSave("spin_button", btnText)
                    onSave("bg_color", bgColor)
                    onSave("bg_image", bgImage)
                    onSave("btn_color", btnColor)
                }) {
                    Text("Salvar Configurações")
                }
            }
        }
    }
}

@Composable
fun ClientsTab(
    currentCampaign: CampaignEntity?,
    clients: List<ClientEntity>,
    promoTemplate: String,
    onSavePromoTemplate: (String) -> Unit,
    onAddBulkClients: (String) -> Unit,
    onToggleSent: (ClientEntity) -> Unit,
    onMarkSent: (ClientEntity) -> Unit,
    onDeleteClient: (ClientEntity) -> Unit,
    onDeleteClients: (List<Long>) -> Unit,
    onClearClients: () -> Unit
) {
    if (currentCampaign == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Selecione ou crie uma campanha primeiro.")
        }
        return
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var bulkInput by remember { mutableStateOf("") }
    var promoMsg by remember(promoTemplate) { mutableStateOf(promoTemplate) }
    var filterStatus by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<Long>() }

    val totalCount = clients.size
    val pendingCount = clients.count { !it.sent }
    val sentCount = clients.count { it.sent }

    val filteredClients = clients.filter { client ->
        val matchesStatus = when (filterStatus) {
            "pending" -> !client.sent
            "sent" -> client.sent
            else -> true
        }
        val query = searchQuery.trim().lowercase()
        val matchesQuery = if (query.isEmpty()) true else {
            client.nome.lowercase().contains(query) ||
            client.whatsapp.contains(query) ||
            client.codigo.lowercase().contains(query)
        }
        matchesStatus && matchesQuery
    }

    fun openWhatsApp(client: ClientEntity) {
        val publicLink = "https://roleta.app/#roleta/${currentCampaign.slug}"
        val message = promoMsg
            .replace("{nome}", client.nome)
            .replace("{whatsapp}", client.whatsapp)
            .replace("{vencimento}", client.vencimento)
            .replace("{codigo}", client.codigo)
            .replace("{link}", publicLink)
            .replace("{campanha}", currentCampaign.name)

        val encodedMsg = Uri.encode(message)
        val waUrl = "https://api.whatsapp.com/send?phone=${client.cleanPhone}&text=$encodedMsg"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
            context.startActivity(intent)
            onMarkSent(client)
            Toast.makeText(context, "Abrindo WhatsApp para ${client.nome}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao abrir WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Clientes & Envio WhatsApp",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("Campanha: ${currentCampaign.name}") }
                    )
                }
                Text(
                    "Adicione clientes em massa, gere códigos válidos automaticamente e envie mensagens pelo WhatsApp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Card 1: Bulk Input
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "1. ADICIONAR CLIENTES EM MASSA",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    bulkInput = "0001joão\n48988364133\n17/10/2016\nRLT-7X92KP\n\n0002maria99\n11988887777\n25/11/2026\n\n0003carlos_silva\n21999998888\n30/12/2026"
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Exemplo", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { bulkInput = "" },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Limpar", fontSize = 12.sp)
                            }
                        }
                    }

                    Text(
                        "Cole os clientes abaixo. Para cada cliente informe Nome (letras e números permitidos, ex: 0001joão), WhatsApp e Data de Vencimento. O sistema organizará cada cliente e vinculará um código válido automaticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = bulkInput,
                        onValueChange = { bulkInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        maxLines = 10,
                        placeholder = {
                            Text("0001joão\n48988364133\n17/10/2016\nRLT-7X92KP\n\n0002maria99\n11988887777\n25/11/2026")
                        }
                    )

                    Button(
                        onClick = {
                            if (bulkInput.isNotBlank()) {
                                onAddBulkClients(bulkInput)
                                bulkInput = ""
                                Toast.makeText(context, "Clientes adicionados com códigos gerados!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Organizar e Adicionar Clientes")
                    }
                }
            }
        }

        // Card 2: Promo Message Template
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "2. TEXTO PROMOCIONAL PARA O WHATSAPP",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FilledTonalButton(
                            onClick = {
                                onSavePromoTemplate(promoMsg)
                                Toast.makeText(context, "Texto promocional salvo!", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Salvar Texto", fontSize = 12.sp)
                        }
                    }

                    Text(
                        "Personalize o modelo da mensagem. Toque nas tags para inserir variáveis dinâmicas:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("{nome}", "{whatsapp}", "{vencimento}", "{codigo}", "{link}").forEach { tag ->
                            SuggestionChip(
                                onClick = { promoMsg += " $tag" },
                                label = { Text(tag, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = promoMsg,
                        onValueChange = { promoMsg = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )
                }
            }
        }

        // Section 3: Clientes Organizados Header & Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Clientes Organizados (${filteredClients.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Total: $totalCount | Pendentes: $pendingCount | Enviados: $sentCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (pendingCount > 0) {
                        Button(
                            onClick = {
                                val nextPending = clients.firstOrNull { !it.sent }
                                if (nextPending != null) openWhatsApp(nextPending)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Próximo Pendente", fontSize = 12.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = filterStatus == "all",
                        onClick = { filterStatus = "all" },
                        label = { Text("Todos ($totalCount)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterStatus == "pending",
                        onClick = { filterStatus = "pending" },
                        label = { Text("Pendentes ($pendingCount)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterStatus == "sent",
                        onClick = { filterStatus = "sent" },
                        label = { Text("Enviados ($sentCount)", fontSize = 11.sp) }
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nome, whatsapp ou código...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                if (selectedIds.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selectedIds.size} selecionado(s)", style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = {
                                onDeleteClients(selectedIds.toList())
                                selectedIds.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excluir Selecionados", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Client List Items
        if (filteredClients.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Nenhum cliente cadastrado nesta campanha.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredClients, key = { it.id }) { client ->
                val isSelected = selectedIds.contains(client.id)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) selectedIds.add(client.id) else selectedIds.remove(client.id)
                            }
                        )

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(client.nome, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Surface(
                                    color = if (client.sent) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        if (client.sent) "Enviado" else "Pendente",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (client.sent) Color(0xFF047857) else Color(0xFFB45309)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(client.whatsapp, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                Text("Venc: ${client.vencimento}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "Código: ${client.codigo}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalButton(
                                onClick = { openWhatsApp(client) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                                    contentColor = Color(0xFF047857)
                                )
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Enviar", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 12.sp)
                            }

                            IconButton(
                                onClick = {
                                    val publicLink = "https://roleta.app/#roleta/${currentCampaign.slug}"
                                    val message = promoMsg
                                        .replace("{nome}", client.nome)
                                        .replace("{whatsapp}", client.whatsapp)
                                        .replace("{vencimento}", client.vencimento)
                                        .replace("{codigo}", client.codigo)
                                        .replace("{link}", publicLink)
                                        .replace("{campanha}", currentCampaign.name)
                                    clipboardManager.setText(AnnotatedString(message))
                                    Toast.makeText(context, "Mensagem copiada!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(18.dp))
                            }

                            IconButton(onClick = { onToggleSent(client) }) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Alternar status",
                                    tint = if (client.sent) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(onClick = { onDeleteClient(client) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Excluir",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
