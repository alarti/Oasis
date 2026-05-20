package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        OasisScreen()
      }
    }
  }
}

@Composable
fun OasisScreen(modifier: Modifier = Modifier) {
  var isInspiring by remember { mutableStateOf(true) }
  var phaseText by remember { mutableStateOf("Inspira...") }

  val scale by animateFloatAsState(
    targetValue = if (isInspiring) 1.5f else 1f,
    animationSpec = tween(durationMillis = 4000, easing = LinearOutSlowInEasing),
    label = "breathePulse"
  )

  val alpha by animateFloatAsState(
    targetValue = if (isInspiring) 0.8f else 0.3f,
    animationSpec = tween(durationMillis = 4000, easing = LinearEasing),
    label = "breatheAlpha"
  )

  LaunchedEffect(Unit) {
    while (true) {
      isInspiring = true
      phaseText = "Inspira profundo..."
      delay(4000)
      isInspiring = false
      phaseText = "Exhala suavemente..."
      delay(4000)
    }
  }

  val quotes = listOf(
    "El mundo puede esperar un momento.",
    "Tu valor no se mide por lo que produces.",
    "Respira. Estás aquí y ahora.",
    "Desconectar también es avanzar.",
    "Haz una pausa. Tómate el tiempo de simplemente ser."
  )
  val currentQuote = remember { quotes.random() }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0F1113))
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xFF2D2F36), 
              Color(0xFF111318)
            )
          ),
          shape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)
        )
        .border(
           width = 1.dp,
           color = Color(0xFF44474F),
           shape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)
        ),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
          .windowInsetsPadding(WindowInsets.systemBars)
          .padding(32.dp)
      ) {
      // Title
      Text(
        text = "O A S I S",
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        style = MaterialTheme.typography.titleMedium,
        letterSpacing = 16.sp,
        fontWeight = FontWeight.Light,
        modifier = Modifier.padding(bottom = 64.dp)
      )

      // Canvas breathing circle
      Box(
        modifier = Modifier
          .size(200.dp)
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          drawCircle(
            color = Color(0xFFD0BCFF), 
            radius = (size.minDimension / 2) * scale,
            alpha = alpha
          )
          drawCircle(
            color = Color(0xFFD0BCFF).copy(alpha = alpha / 2),
            radius = (size.minDimension / 2) * (scale * 1.2f),
            alpha = alpha / 2
          )
        }
      }

      Spacer(modifier = Modifier.height(48.dp))

      // Instruction text
      Text(
        text = phaseText,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Medium
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Subtitle / Quote
      Text(
        text = currentQuote,
        color = Color(0xFF94979F),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        lineHeight = 28.sp,
        modifier = Modifier.padding(horizontal = 16.dp)
      )
    }
    }
  }
}
