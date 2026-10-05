package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.RoletaViewModel
import com.example.viewmodel.CodeStatus
import com.example.R
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

    val currentCampaign by viewModel.currentCampaign.collectAsState()
    val prizes by viewModel.prizes.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val activeCode by viewModel.activeCode.collectAsState()
    val codeValidationStatus by viewModel.codeValidationStatus.collectAsState()
    val spinResult by viewModel.spinResult.collectAsState()

    var codeInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val rotationAnim = remember { Animatable(0f) }
    var targetRotation by remember { mutableStateOf(0f) }

    val activePrizes = prizes.filter { it.active && (it.unlimitedQuantity || it.quantity > 0) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
    ) {
        // Background poster matching the reference image exactly
        Image(
            painter = painterResource(id = R.drawable.roleta_poster),
            contentDescription = "Roleta da Sorte Poster",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark overlay gradient for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0B0F19).copy(alpha = 0.85f), Color(0xFF0B0F19))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(250.dp)) // Space for poster header

            // Gorgeous Casino Wheel with Golden Rim, Glowing Bulbs, and Spinning Slices
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFEF08A), Color(0xFFEAB308), Color(0xFF78350F))
                        ),
                        shape = CircleShape
                    )
                    .padding(14.dp)
                    .shadow(30.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Inner Wheel Container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF060913)),
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
                            android.graphics.Color.parseColor("#7C3AED"), // Purple
                            android.graphics.Color.parseColor("#2563EB"), // Blue
                            android.graphics.Color.parseColor("#059669"), // Green
                            android.graphics.Color.parseColor("#DB2777"), // Pink
                            android.graphics.Color.parseColor("#EA580C")  // Orange
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

                            // Draw slice border line
                            drawArc(
                                color = Color(0xFFFDE047),
                                startAngle = startAngle,
                                sweepAngle = 1f,
                                useCenter = true
                            )

                            drawContext.canvas.nativeCanvas.apply {
                                val angleRad = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                                val radius = size.width / 3.2f
                                val x = center.x + (radius * cos(angleRad)).toFloat()
                                val y = center.y + (radius * sin(angleRad)).toFloat()

                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 32f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                    isFakeBoldText = true
                                    setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
                                }
                                val displayText = if (activePrizes.isNotEmpty()) activePrizes[i].displayText else "Prêmio"
                                drawText(displayText, x, y + 10f, paint)
                            }
                        }
                    }
                }

                // Center Gold Cap with Clover
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFEF08A), Color(0xFFCA8A04))
                            )
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF0B0F19)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Centro",
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Top Casino Pointer
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-4).dp)
                        .size(32.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFFFDE047), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Interaction Area (Inputs / Buttons matching poster)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
            ) {
                if (activeCode == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A).copy(alpha = 0.95f), RoundedCornerShape(24.dp))
                            .padding(20.dp)
                    ) {
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it },
                            placeholder = { Text("Digite seu código (ex: RLT-7X92KP)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFACC15),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.validateCode(codeInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFEF08A), Color(0xFFFACC15), Color(0xFFCA8A04))
                                    ),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Text(
                                text = "LIBERAR ROLETA",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF1E1B4B)
                            )
                        }

                        if (codeValidationStatus is CodeStatus.Error) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = (codeValidationStatus as CodeStatus.Error).message,
                                color = Color(0xFFEF4444),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else if (spinResult == null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Código validado com sucesso!",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.spinWheel { prize, prizeIndex ->
                                    coroutineScope.launch {
                                        val sliceCount = if (activePrizes.isEmpty()) 1 else activePrizes.size
                                        val degreesPerSlice = 360f / sliceCount
                                        val extraSpins = 360f * 6
                                        val targetSliceAngle = prizeIndex * degreesPerSlice + (degreesPerSlice / 2)
                                        targetRotation = rotationAnim.value + extraSpins + (360f - (rotationAnim.value % 360f)) - targetSliceAngle

                                        rotationAnim.animateTo(
                                            targetValue = targetRotation,
                                            animationSpec = tween(
                                                durationMillis = 5000,
                                                easing = EaseOutCubic
                                            )
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFEF08A), Color(0xFFFACC15), Color(0xFFCA8A04))
                                    ),
                                    RoundedCornerShape(24.dp)
                                )
                                .shadow(10.dp, RoundedCornerShape(24.dp))
                        ) {
                            Text(
                                text = "CLIQUE AQUI E GIRE A ROLETA",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = Color(0xFF1E1B4B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val prize = spinResult!!
                    Card(
                        shape = RoundedCornerShape(20.dp),
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
                                text = "🎉 PARABÉNS! 🎉",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color(0xFFFBBF24),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "VOCÊ GANHOU",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = prize.resultText.ifEmpty { prize.name },
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = prize.description,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Seu prêmio foi registrado com sucesso no sistema.",
                                fontSize = 12.sp,
                                color = Color(0xFF10B981),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
