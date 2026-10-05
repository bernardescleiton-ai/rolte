package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PrizeEntity
import com.example.viewmodel.CodeStatus
import com.example.viewmodel.RoletaViewModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RoletaPublicScreen(
    viewModel: RoletaViewModel,
    slug: String,
    onNavigateAdmin: () -> Unit
) {
    LaunchedEffect(slug) {
        viewModel.loadCampaignBySlug(slug)
    }

    val campaign by viewModel.currentCampaign.collectAsState()
    val prizes by viewModel.prizes.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val codeStatus by viewModel.codeValidationStatus.collectAsState()
    val activeCode by viewModel.activeCode.collectAsState()
    val spinResult by viewModel.spinResult.collectAsState()
    val isSpinning by viewModel.isSpinning.collectAsState()

    var codeInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    var targetRotation by remember { mutableStateOf(0f) }
    val rotationAnim = remember { Animatable(0f) }

    val activePrizes = prizes.filter { it.unlimitedQuantity || it.quantity > 0 }

    val bgColorHex = settings["bg_color"] ?: "#0F172A"
    val textColorHex = settings["text_color"] ?: "#FFFFFF"
    val btnColorHex = settings["btn_color"] ?: "#10B981"
    val btnTextColorHex = settings["btn_text_color"] ?: "#FFFFFF"

    val bgColor = try { Color(android.graphics.Color.parseColor(bgColorHex)) } catch (e: Exception) { Color(0xFF0F172A) }
    val textColor = try { Color(android.graphics.Color.parseColor(textColorHex)) } catch (e: Exception) { Color.White }
    val btnColor = try { Color(android.graphics.Color.parseColor(btnColorHex)) } catch (e: Exception) { Color(0xFF10B981) }
    val btnTextColor = try { Color(android.graphics.Color.parseColor(btnTextColorHex)) } catch (e: Exception) { Color.White }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Admin button in top right
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(onClick = onNavigateAdmin) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Painel Administrativo",
                    tint = textColor.copy(alpha = 0.7f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = settings["title"] ?: "Gire e Ganhe!",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = settings["subtitle"] ?: "Teste sua sorte e ganhe prêmios exclusivos.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor.copy(alpha = 0.8f),
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Wheel
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationAnim.value)
                ) {
                    val sliceCount = if (activePrizes.isEmpty()) 1 else activePrizes.size
                    val sweepAngle = 360f / sliceCount
                    val wheelColors = listOf(
                        android.graphics.Color.parseColor("#3B82F6"),
                        android.graphics.Color.parseColor("#8B5CF6"),
                        android.graphics.Color.parseColor("#EC4899"),
                        android.graphics.Color.parseColor("#F59E0B"),
                        android.graphics.Color.parseColor("#10B981"),
                        android.graphics.Color.parseColor("#6366F1"),
                        android.graphics.Color.parseColor("#14B8A6")
                    )

                    for (i in 0 until sliceCount) {
                        val startAngle = i * sweepAngle
                        val colorInt = wheelColors[i % wheelColors.size]
                        
                        drawArc(
                            color = Color(colorInt),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true
                        )

                        drawContext.canvas.nativeCanvas.apply {
                            val angleRad = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                            val radius = size.width / 3.5f
                            val x = center.x + (radius * cos(angleRad)).toFloat()
                            val y = center.y + (radius * sin(angleRad)).toFloat()

                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 36f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                                isFakeBoldText = true
                            }
                            val displayText = if (activePrizes.isNotEmpty()) activePrizes[i].displayText else "Prêmio"
                            drawText(displayText, x, y + 12f, paint)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Ponteiro",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (activeCode == null) {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    placeholder = { Text(settings["code_placeholder"] ?: "Digite seu código") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = btnColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedPlaceholderColor = textColor.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = textColor.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.validateCode(codeInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = btnColor, contentColor = btnTextColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = settings["liberate_button"] ?: "LIBERAR ROLETA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                if (codeStatus is CodeStatus.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = (codeStatus as CodeStatus.Error).message,
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (spinResult == null) {
                Text(
                    text = settings["valid_code_msg"] ?: "Código validado! Você tem 1 giro.",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.spinWheel { prize, prizeIndex ->
                            coroutineScope.launch {
                                val sliceCount = if (activePrizes.isEmpty()) 1 else activePrizes.size
                                val degreesPerSlice = 360f / sliceCount
                                val extraSpins = 360f * 5
                                val targetSliceAngle = prizeIndex * degreesPerSlice + (degreesPerSlice / 2)
                                targetRotation = rotationAnim.value + extraSpins + (360f - (rotationAnim.value % 360f)) - targetSliceAngle

                                rotationAnim.animateTo(
                                    targetValue = targetRotation,
                                    animationSpec = tween(
                                        durationMillis = 5000,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        }
                    },
                    enabled = !isSpinning,
                    colors = ButtonDefaults.buttonColors(containerColor = btnColor, contentColor = btnTextColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (isSpinning) (settings["spinning_msg"] ?: "Girando...") else (settings["spin_button"] ?: "GIRAR AGORA"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = settings["result_title"] ?: "🎉 PARABÉNS! 🎉",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                fontSize = 24.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = settings["result_prize_prefix"] ?: "VOCÊ GANHOU",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = spinResult?.resultText ?: spinResult?.name ?: "Prêmio",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Yellow,
                                fontSize = 28.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = spinResult?.description ?: "",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = settings["result_success_msg"] ?: "Seu prêmio foi registrado com sucesso.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
