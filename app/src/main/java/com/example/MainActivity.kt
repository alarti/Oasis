package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.sound.ZenSoundEngine
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

enum class Screen {
    INICIO, ESCAPE, PAZ, AJUSTES
}

data class LocalZenScenario(
    val title: String,
    val metaphor: String,
    val prompt: String,
    val color1: Color,
    val color2: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OasisScreen() {
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(Screen.INICIO) }

    // SharedPreferences for name personalization
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("oasis_pref", Context.MODE_PRIVATE) }
    var userName by remember { mutableStateOf(sharedPreferences.getString("user_name", "Viajero") ?: "Viajero") }

    // ViewModel implementation for centralized robust state management (M3 mandate)
    val viewModel: OasisViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    // Zen Sound state
    var isZenMusicPlaying by remember { mutableStateOf(ZenSoundEngine.isPlaying()) }

    DisposableEffect(Unit) {
        onDispose {
            ZenSoundEngine.stop()
        }
    }

    // IA / Demo Mode State
    var isDemoMode by remember { mutableStateOf(true) }

    // --- State: Escape (AI Image Gen) ---
    var stressInput by remember { mutableStateOf("") }
    var searchTitle by remember { mutableStateOf("") }
    var isGeneratingImage by remember { mutableStateOf(false) }
    var generatedImage by remember { mutableStateOf<Bitmap?>(null) }
    var imageMessage by remember { mutableStateOf("Personaliza tu oasis de serenidad redactando tu desasosiego debajo o seleccionando un templo de calma.") }
    var translatedPromptUsed by remember { mutableStateOf("") }
    var activeScenarioIndex by remember { mutableStateOf(0) }
    var isScenarioCustomLoaded by remember { mutableStateOf(false) }

    // Scenarios loaded from local + DB
    val savedScenarios by viewModel.savedScenarios.collectAsState()

    val baseScenarios = remember {
        listOf(
            LocalZenScenario(
                title = "Río Calmo",
                metaphor = "Tu mente es un río de aguas templadas. Cada aflicción es una hoja dorada que cae de los sauces, flotando lejos sin tocar el fondo.",
                prompt = "Serene watercolor of willow branch leaves floating on crystal clear misty river, magical golden light, calm ripples.",
                color1 = Color(0xFF80DEEA),
                color2 = Color(0xFF006064)
            ),
            LocalZenScenario(
                title = "Nebulosa Paz",
                metaphor = "Los plazos urgentes se diluyen en una nebulosa violeta cósmica. Flotas sin peso, custodiado por amables estrellas tímidas.",
                prompt = "Enchanting cosmic cloud nebula, deep purple and violet background with glowing soft spheres, minimalist abstract vector.",
                color1 = Color(0xFFD0BCFF),
                color2 = Color(0xFF4A148C)
            ),
            LocalZenScenario(
                title = "Dunas de Luz",
                metaphor = "La fatiga se absorbe en dunas de arena fina que el viento nocturno peina en absoluto silencio. Granos dorados que brillan.",
                prompt = "Minimalist smooth sand dunes under soft moonlight dark sky, pink pastel highlights, calming atmosphere, highly cinematic.",
                color1 = Color(0xFFFFB74D),
                color2 = Color(0xFFE65100)
            ),
            LocalZenScenario(
                title = "Templo Lluvia",
                metaphor = "Las conversaciones se silencian bajo el compás de la lluvia sobre techos de bambú. Estás abrigado dentro del templo respirando paz.",
                prompt = "Traditional Japanese architecture, wet stones reflection, gentle raindrops, warm soft paper lantern light, dreamy painting.",
                color1 = Color(0xFFF06292),
                color2 = Color(0xFF880E4F)
            )
        )
    }

    // Currently Selected Scenario Details
    val currentScenarioColors = remember(activeScenarioIndex, isScenarioCustomLoaded, savedScenarios) {
        if (isScenarioCustomLoaded && activeScenarioIndex in savedScenarios.indices) {
            val sc = savedScenarios[activeScenarioIndex]
            val c1 = try { Color(android.graphics.Color.parseColor(sc.color1Hex)) } catch (_: Exception) { Color(0xFF80DEEA) }
            val c2 = try { Color(android.graphics.Color.parseColor(sc.color2Hex)) } catch (_: Exception) { Color(0xFF006064) }
            Pair(c1, c2)
        } else {
            val idx = activeScenarioIndex.coerceIn(0, baseScenarios.lastIndex)
            val sc = baseScenarios[idx]
            Pair(sc.color1, sc.color2)
        }
    }

    val currentScenarioMetaphor = remember(activeScenarioIndex, isScenarioCustomLoaded, savedScenarios) {
        if (isScenarioCustomLoaded && activeScenarioIndex in savedScenarios.indices) {
            savedScenarios[activeScenarioIndex].metaphor
        } else {
            val idx = activeScenarioIndex.coerceIn(0, baseScenarios.lastIndex)
            baseScenarios[idx].metaphor
        }
    }

    // --- State: Paz (Zen Mantra Wis) ---
    var generatedMantra by remember { mutableStateOf("El trabajo es solo una porción del paisaje, tú eres el cielo entero que lo envuelve. Respira suave, estás seguro ahora de pausar.") }
    var isGeneratingMantra by remember { mutableStateOf(false) }
    var myCustomMantraInput by remember { mutableStateOf("") }

    // Observe DB mantras
    val savedMantras by viewModel.savedMantras.collectAsState()

    // --- State: Breathing (Metronome) ---
    val isBreatheRunning by viewModel.isBreatheRunning.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val currentPhase by viewModel.currentPhase.collectAsState()
    val phaseSecondsRemaining by viewModel.phaseSecondsRemaining.collectAsState()
    val sessionProgressSec by viewModel.sessionProgressSec.collectAsState()
    val targetSessionMinutes by viewModel.targetSessionMinutes.collectAsState()
    val isAudioAlertsEnabled by viewModel.isAudioAlertsEnabled.collectAsState()

    // Breathing Stats Collect
    val currentStreak by viewModel.currentStreak.collectAsState()
    val totalBreathingMinutes by viewModel.totalBreathingMinutes.collectAsState()
    val totalSessionsCompleted by viewModel.totalSessionsCompleted.collectAsState()

    // Timer trigger feedback overlay
    var showCompletionDialog by remember { mutableStateOf(false) }
    var lastPracticedMode by remember { mutableStateOf("") }
    var lastPracticedMinutes by remember { mutableStateOf(1) }

    LaunchedEffect(isBreatheRunning) {
        if (!isBreatheRunning && sessionProgressSec > 0) {
            val targetSec = targetSessionMinutes * 60
            if (sessionProgressSec >= targetSec) {
                lastPracticedMode = selectedMode.displayName
                lastPracticedMinutes = targetSessionMinutes
                showCompletionDialog = true
            }
        }
    }

    // Dynamic color matching of the breathing rings to the current phase
    val breathingPhaseColor = when (currentPhase) {
        BreathingPhase.INHALE -> Color(0xFF80DEEA)   // Luminous Cyan
        BreathingPhase.HOLD_IN -> Color(0xFFFFD54F)  // Warm Amber
        BreathingPhase.EXHALE -> Color(0xFFE1BEE7)   // Soft Orchid
        BreathingPhase.HOLD_OUT -> Color(0xFF9FA8DA)  // Twilight Slate
    }

    // Breathing scale animation synchronized with phase directions
    val scaleBreatheTarget = when {
        !isBreatheRunning -> 1.0f
        currentPhase == BreathingPhase.INHALE -> 1.55f
        currentPhase == BreathingPhase.HOLD_IN -> 1.55f
        currentPhase == BreathingPhase.EXHALE -> 1.0f
        currentPhase == BreathingPhase.HOLD_OUT -> 1.0f
        else -> 1.0f
    }

    val phaseScaleDuration = when (currentPhase) {
        BreathingPhase.INHALE -> selectedMode.inhaleSec * 1000
        BreathingPhase.HOLD_IN -> selectedMode.holdInSec * 1000
        BreathingPhase.EXHALE -> selectedMode.exhaleSec * 1000
        BreathingPhase.HOLD_OUT -> selectedMode.holdOutSec * 1000
    }

    val animateBreatheScale by animateFloatAsState(
        targetValue = scaleBreatheTarget,
        animationSpec = tween(
            durationMillis = if (isBreatheRunning) phaseScaleDuration else 4000,
            easing = if (currentPhase == BreathingPhase.INHALE) LinearOutSlowInEasing else FastOutLinearInEasing
        ),
        label = "breatheAnimScale"
    )

    // Main Scaffold layout
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                Divider(color = Color(0xFF2C2F36), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F1113))
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(82.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(
                        icon = Icons.Default.Home,
                        label = "Inicio",
                        isSelected = currentScreen == Screen.INICIO,
                        onClick = { currentScreen = Screen.INICIO }
                    )
                    NavItem(
                        icon = Icons.Default.Search,
                        label = "Escape",
                        isSelected = currentScreen == Screen.ESCAPE,
                        onClick = { currentScreen = Screen.ESCAPE }
                    )
                    NavItem(
                        icon = Icons.Default.Favorite,
                        label = "Paz",
                        isSelected = currentScreen == Screen.PAZ,
                        onClick = { currentScreen = Screen.PAZ }
                    )
                    NavItem(
                        icon = Icons.Default.Settings,
                        label = "Ajustes",
                        isSelected = currentScreen == Screen.AJUSTES,
                        onClick = { currentScreen = Screen.AJUSTES }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0C0E))
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Elegant Outer Border Wrap (Sophisticated Dark Minimalist Aesthetic)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E2125),
                                Color(0xFF0E0F11)
                            )
                        ),
                        shape = RoundedCornerShape(36.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF2C2F36),
                        shape = RoundedCornerShape(36.dp)
                    )
            ) {
                Crossfade(targetState = currentScreen, label = "mainFadeTransitions") { screen ->
                    when (screen) {
                        Screen.INICIO -> {
                            LazyColumn(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top,
                                contentPadding = PaddingValues(24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Greet personal user
                                item {
                                    Text(
                                        text = "Hola, $userName",
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraLight,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                        textAlign = TextAlign.Start
                                    )
                                    Text(
                                        text = "Tu Santuario personal de relajación y pausa",
                                        color = Color(0xFF94979F),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                                        textAlign = TextAlign.Start
                                    )
                                }

                                // Interactive stats row
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        StatCard(
                                            title = "Racha",
                                            value = if (currentStreak == 0) "Comenzar" else "$currentStreak días",
                                            iconText = "🔥",
                                            colorBg = Color(0xFFE65100).copy(alpha = 0.12f),
                                            colorBorder = Color(0xFFFFB74D),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatCard(
                                            title = "Práctica",
                                            value = "$totalBreathingMinutes min",
                                            iconText = "⏱️",
                                            colorBg = Color(0xFF006064).copy(alpha = 0.12f),
                                            colorBorder = Color(0xFF80DEEA),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatCard(
                                            title = "Sesiones",
                                            value = "$totalSessionsCompleted pausas",
                                            iconText = "🌌",
                                            colorBg = Color(0xFF4A148C).copy(alpha = 0.12f),
                                            colorBorder = Color(0xFFD0BCFF),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Core metronome wave circle
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(242.dp)
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Draw smooth ambient glows inside canvas
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val radius = (size.minDimension / 2f)
                                            // Phase glow layers
                                            drawCircle(
                                                color = breathingPhaseColor,
                                                radius = radius * animateBreatheScale * 0.72f,
                                                alpha = 0.18f
                                            )
                                            drawCircle(
                                                color = breathingPhaseColor.copy(alpha = 0.4f),
                                                radius = radius * animateBreatheScale * 0.95f,
                                                alpha = 0.08f
                                            )

                                            // Rotating outline arc representing phase progress
                                            if (isBreatheRunning) {
                                                val totalPhaseSec = when (currentPhase) {
                                                    BreathingPhase.INHALE -> selectedMode.inhaleSec
                                                    BreathingPhase.HOLD_IN -> selectedMode.holdInSec
                                                    BreathingPhase.EXHALE -> selectedMode.exhaleSec
                                                    BreathingPhase.HOLD_OUT -> selectedMode.holdOutSec
                                                }
                                                val remainingFraction = if (totalPhaseSec > 0) {
                                                    phaseSecondsRemaining.toFloat() / totalPhaseSec.toFloat()
                                                } else 1.0f

                                                drawArc(
                                                    color = breathingPhaseColor,
                                                    startAngle = -90f,
                                                    sweepAngle = remainingFraction * 360f,
                                                    useCenter = false,
                                                    style = Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                                                    size = Size(radius * 1.55f, radius * 1.55f),
                                                    topLeft = Offset(radius - radius * 0.775f, radius - radius * 0.775f)
                                                )
                                            }
                                        }

                                        // Center status textual prompts
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(24.dp)
                                        ) {
                                            if (isBreatheRunning) {
                                                Text(
                                                    text = "$phaseSecondsRemaining",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.displayMedium,
                                                    fontWeight = FontWeight.Light
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = currentPhase.label,
                                                    color = breathingPhaseColor,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else {
                                                Text(
                                                    text = "🧘",
                                                    fontSize = 44.sp,
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )
                                                Text(
                                                    text = "Respira",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }

                                // Interactive practices actions
                                item {
                                    Spacer(modifier = Modifier.height(28.dp))

                                    if (isBreatheRunning) {
                                        // Dynamic practicing stats indicators
                                        val sessionTargetSec = targetSessionMinutes * 60
                                        val progressFraction = (sessionProgressSec.toFloat() / sessionTargetSec.toFloat()).coerceIn(0f, 1f)
                                        
                                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Pausa activa: ${selectedMode.displayName}",
                                                    color = Color(0xFFE2E2E6),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = "${sessionProgressSec}s / ${sessionTargetSec}s",
                                                    color = Color(0xFF94979F),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LinearProgressIndicator(
                                                progress = { progressFraction },
                                                color = breathingPhaseColor,
                                                trackColor = Color(0xFF1F2023),
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Button(
                                            onClick = { viewModel.stopBreathingSession(completedSuccessfully = false) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFF06292).copy(alpha = 0.2f),
                                                contentColor = Color(0xFFF06292)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFFF06292).copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth().height(54.dp),
                                            shape = RoundedCornerShape(100.dp)
                                        ) {
                                            Text("DETENER PRÁCTICA", fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                                        }
                                    } else {
                                        ZenMusicPlayerCard(
                                            isPlaying = isZenMusicPlaying,
                                            onToggle = { turnOn ->
                                                isZenMusicPlaying = turnOn
                                                if (turnOn) {
                                                    ZenSoundEngine.start()
                                                } else {
                                                    ZenSoundEngine.stop()
                                                }
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Configure practice parameters
                                        Text(
                                            text = "Técnica de respiración:",
                                            color = Color(0xFF94979F),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                        )

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            BreathingMode.values().forEach { mode ->
                                                val isSelected = selectedMode == mode
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            color = if (isSelected) Color(0xFFD0BCFF).copy(alpha = 0.1f) else Color(0xFF16181C),
                                                            shape = RoundedCornerShape(18.dp)
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF2C2F36),
                                                            shape = RoundedCornerShape(18.dp)
                                                        )
                                                        .clickable { viewModel.selectBreathingMode(mode) }
                                                        .padding(14.dp)
                                                ) {
                                                    Column {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = mode.displayName,
                                                                color = if (isSelected) Color(0xFFD0BCFF) else Color.White,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            
                                                            val cadenceStr = buildString {
                                                                append("${mode.inhaleSec}s")
                                                                if (mode.holdInSec > 0) append("/${mode.holdInSec}s")
                                                                append("/${mode.exhaleSec}s")
                                                                if (mode.holdOutSec > 0) append("/${mode.holdOutSec}s")
                                                            }
                                                            Text(
                                                                text = cadenceStr,
                                                                color = Color(0xFF94979F),
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = mode.description,
                                                            color = Color(0xFF94979F),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            lineHeight = 16.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Target minutes selection row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Duración:",
                                                color = Color(0xFF94979F),
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(1, 2, 5, 10).forEach { mins ->
                                                    val isMinSel = targetSessionMinutes == mins
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                color = if (isMinSel) Color(0xFFD0BCFF).copy(alpha = 0.15f) else Color(0xFF16181C),
                                                                shape = RoundedCornerShape(100.dp)
                                                            )
                                                            .border(
                                                                width = 1.dp,
                                                                color = if (isMinSel) Color(0xFFD0BCFF) else Color(0xFF2C2F36),
                                                                shape = RoundedCornerShape(100.dp)
                                                            )
                                                            .clickable { viewModel.setTargetSessionMinutes(mins) }
                                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = "$mins min",
                                                            color = if (isMinSel) Color(0xFFD0BCFF) else Color(0xFFC4C6D0),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Button(
                                            onClick = { viewModel.startBreathingSession() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFD0BCFF),
                                                contentColor = Color(0xFF381E72)
                                            ),
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = RoundedCornerShape(100.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Iniciar",
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Text("INICIAR PAUSA CONSCIENTE", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Screen.ESCAPE -> {
                            val keyboardController = LocalSoftwareKeyboardController.current
                            val focusManager = LocalFocusManager.current

                            LazyColumn(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top,
                                contentPadding = PaddingValues(24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = Color(0xFF80DEEA).copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(100.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color(0xFF80DEEA).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(100.dp)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "SANTUARIO VISUAL LOCAL",
                                            color = Color(0xFF80DEEA),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Escapismo Trascendental IA",
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Light,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // Interactive Viewport Card
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(262.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Color(0xFF16181C))
                                            .border(1.dp, Color(0xFF2C2F36), RoundedCornerShape(24.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isGeneratingImage) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                CircularProgressIndicator(color = Color(0xFF80DEEA))
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "Traduciendo tu estrés en paz visual...",
                                                    color = Color(0xFF80DEEA),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontStyle = FontStyle.Italic
                                                )
                                            }
                                        } else if (generatedImage != null) {
                                            Image(
                                                bitmap = generatedImage!!.asImageBitmap(),
                                                contentDescription = "Oasis Visual Forjado",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )

                                            // Bookmark Action Icon overlay
                                            Box(
                                                modifier = Modifier.fillMaxSize().padding(14.dp),
                                                contentAlignment = Alignment.TopEnd
                                            ) {
                                                var isSavedSc by remember { mutableStateOf(false) }
                                                IconButton(
                                                    onClick = {
                                                        val title = searchTitle.ifEmpty { "Mi Oasis ${savedScenarios.size + 1}" }
                                                        val metaphor = "Tu carga de estrés, envuelta en un sutil santuario de paz infinita."
                                                        val pr = translatedPromptUsed.ifEmpty { "Serene abstract landscape watercolor background." }
                                                        viewModel.saveScenario(
                                                            title = title,
                                                            metaphor = metaphor,
                                                            prompt = pr,
                                                            color1Hex = "#80DEEA",
                                                            color2Hex = "#006064"
                                                        )
                                                        isSavedSc = true
                                                    },
                                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Favorite,
                                                        contentDescription = "Guardar",
                                                        tint = if (isSavedSc) Color(0xFFF06292) else Color.White
                                                    )
                                                }
                                            }
                                        } else {
                                            // Procedural elegant custom canvas using color gradients
                                            val scColor1 = currentScenarioColors.first
                                            val scColor2 = currentScenarioColors.second
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawRect(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(scColor1.copy(alpha = 0.35f), Color(0xFF0C0D0F))
                                                    )
                                                )
                                                drawCircle(
                                                    color = scColor1.copy(alpha = 0.3f),
                                                    radius = 110.dp.toPx(),
                                                    center = Offset(size.width / 2f, size.height / 1.4f)
                                                )
                                                drawCircle(
                                                    color = Color.White.copy(alpha = 0.15f),
                                                    radius = 45.dp.toPx(),
                                                    center = Offset(size.width / 2f, size.height / 1.6f)
                                                )
                                            }

                                            // Text Metaphor Overlay
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.5f))
                                                    .padding(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = currentScenarioMetaphor,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 24.sp,
                                                    fontWeight = FontWeight.Light,
                                                    fontStyle = FontStyle.Italic
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                }

                                // Preset Sanctuaries Choose Title
                                item {
                                    Text(
                                        text = "Santuarios predefinidos:",
                                        color = Color(0xFF94979F),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        baseScenarios.forEachIndexed { index, scenario ->
                                            val isActSc = !isScenarioCustomLoaded && activeScenarioIndex == index && generatedImage == null
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        color = if (isActSc) Color(0xFF80DEEA).copy(alpha = 0.15f) else Color(0xFF16181C),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isActSc) Color(0xFF80DEEA) else Color(0xFF2C2F36),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        generatedImage = null
                                                        isScenarioCustomLoaded = false
                                                        activeScenarioIndex = index
                                                        stressInput = ""
                                                        imageMessage = scenario.metaphor
                                                    }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = scenario.title,
                                                    color = if (isActSc) Color(0xFF80DEEA) else Color(0xFFE2E2E6),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                // Personal Custom Scenarios (Room persist)
                                if (savedScenarios.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Tus Santuarios guardados:",
                                            color = Color(0xFF94979F),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp)
                                        )

                                        // Horizontal row of custom entities
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            savedScenarios.forEachIndexed { sIdx, sc ->
                                                val isActCustom = isScenarioCustomLoaded && activeScenarioIndex == sIdx && generatedImage == null
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = if (isActCustom) Color(0xFFF06292).copy(alpha = 0.15f) else Color(0xFF16181C),
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isActCustom) Color(0xFFF06292) else Color(0xFF2C2F36),
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable {
                                                            generatedImage = null
                                                            isScenarioCustomLoaded = true
                                                            activeScenarioIndex = sIdx
                                                            stressInput = ""
                                                        }
                                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = sc.title,
                                                            color = if (isActCustom) Color(0xFFF06292) else Color(0xFFE2E2E6),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Borrar",
                                                            tint = Color(0xFFF06292).copy(alpha = 0.7f),
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .clickable { viewModel.removeScenario(sc) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Custom Input Section
                                item {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Redactar nuevo Santuario personalizado:",
                                        color = Color(0xFF94979F),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                                    )

                                    // Title field
                                    OutlinedTextField(
                                        value = searchTitle,
                                        onValueChange = { searchTitle = it },
                                        placeholder = { Text("Escribe un título (ej. Mi Bosque Lluvioso)", color = Color(0xFF686B73), fontSize = 13.sp) },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF16181C),
                                            unfocusedContainerColor = Color(0xFF16181C),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedIndicatorColor = Color(0xFF80DEEA),
                                            unfocusedIndicatorColor = Color(0xFF2C2F36)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = stressInput,
                                        onValueChange = { stressInput = it },
                                        placeholder = {
                                            Text(
                                                text = "Describe tu estrés o carga de oficina...",
                                                color = Color(0xFF686B73),
                                                fontSize = 13.sp
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF16181C),
                                            unfocusedContainerColor = Color(0xFF16181C),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedIndicatorColor = Color(0xFF80DEEA),
                                            unfocusedIndicatorColor = Color(0xFF2C2F36)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        maxLines = 3,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        })
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Button(
                                        onClick = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            coroutineScope.launch {
                                                isGeneratingImage = true
                                                delay(2500)
                                                if (stressInput.isNotEmpty()) {
                                                    imageMessage = "Tu carga laboral de '${stressInput}' ha sido diluida bajo dunas infinitas de serenidad procedimental local."
                                                }
                                                generatedImage = null
                                                isGeneratingImage = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF80DEEA),
                                            contentColor = Color(0xFF003737)
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(54.dp),
                                        shape = RoundedCornerShape(100.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Generar",
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = "PROYECTAR SANTUARIO VISUAL",
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                // Informative labels
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Opciones Avanzadas de Calma Visual & Auditiva:",
                                        color = Color(0xFF94979F),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    CalmVideoPlayer()
                                }
                            }
                        }

                        Screen.PAZ -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = Color(0xFFF06292).copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(100.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFFF06292).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(100.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "MANTRAS DE CONCIENCIA",
                                        color = Color(0xFFF06292),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Real-time mantra Display Area
                                Box(
                                    modifier = Modifier
                                        .weight(0.4f)
                                        .fillMaxWidth()
                                        .background(Color(0xFF16181C), RoundedCornerShape(24.dp))
                                        .border(BorderStroke(1.dp, Color(0xFF2C2F36)), RoundedCornerShape(24.dp))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isGeneratingMantra) {
                                        CircularProgressIndicator(color = Color(0xFFF06292))
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = generatedMantra,
                                                color = Color(0xFFE2E2E6),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraLight,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 32.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                            
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            // Action items: Copy & Favorite
                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                val isFav = savedMantras.any { it.text == generatedMantra }
                                                IconButton(
                                                    onClick = {
                                                        if (isFav) {
                                                            val rem = savedMantras.find { it.text == generatedMantra }
                                                            if (rem != null) viewModel.removeMantra(rem)
                                                        } else {
                                                            viewModel.saveMantra(generatedMantra, "IA")
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Favorite,
                                                        contentDescription = "Favorito",
                                                        tint = if (isFav) Color(0xFFF06292) else Color(0xFF94979F)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        val clip = android.content.ClipData.newPlainText("Zen Mantra", generatedMantra)
                                                        clipboard.setPrimaryClip(clip)
                                                        android.widget.Toast.makeText(context, "Mantra copiado", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "Copiar",
                                                        tint = Color(0xFF94979F)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Custom input Mantra Creator
                                    OutlinedTextField(
                                        value = myCustomMantraInput,
                                        onValueChange = { myCustomMantraInput = it },
                                        placeholder = { Text("Escribe tu propia afirmación de hoy...", color = Color(0xFF686B73), fontSize = 13.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF16181C),
                                            unfocusedContainerColor = Color(0xFF16181C),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedIndicatorColor = Color(0xFFF06292),
                                            unfocusedIndicatorColor = Color(0xFF2C2F36)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    IconButton(
                                        onClick = {
                                            if (myCustomMantraInput.isNotEmpty()) {
                                                viewModel.saveMantra(myCustomMantraInput, "Personal")
                                                myCustomMantraInput = ""
                                                android.widget.Toast.makeText(context, "Afirmación grabada en favoritos", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.background(Color(0xFFF06292), RoundedCornerShape(12.dp)).size(54.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Guardar", tint = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isGeneratingMantra = true
                                            delay(1200)
                                            val localMantras = listOf(
                                                "Afloja los hombros de inmediato. Todo lo que te pesa desaparecerá de todos modos. Eres libre.",
                                                "No eres una máquina de producción. Tienes pleno derecho de pausar y contemplar las luces.",
                                                "Hay una fuerza mística velando por ti. Suelta las riendas de tu control.",
                                                "Tu respiración sabe fluir sola. Disfruta de este segundo exacto de respiración.",
                                                "La paz no es el destino, es tu estado nativo cuando desconectas la prisa.",
                                                "Tacha de tu mente la tarea del mañana. El mañana no existe en este espacio de aire."
                                            )
                                            generatedMantra = localMantras.random()
                                            isGeneratingMantra = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF06292),
                                        contentColor = Color(0xFFFFFFFF)
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text("GENERAR REFLEXIÓN ZEN", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Saved Mantras List (Room)
                                Text(
                                    text = "Tus Mantras Guardados:",
                                    color = Color(0xFF94979F),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    textAlign = TextAlign.Start
                                )

                                Box(modifier = Modifier.weight(0.6f)) {
                                    if (savedMantras.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No has guardado mantras aún. Toca el ❤️ en tu reflexión favorita para anclarla aquí.",
                                                color = Color(0xFF686B73),
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 24.dp)
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(savedMantras) { mantra ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                                                    shape = RoundedCornerShape(18.dp),
                                                    border = BorderStroke(1.dp, Color(0xFF2C2F36))
                                                ) {
                                                    Column(modifier = Modifier.padding(16.dp)) {
                                                        Text(
                                                            text = mantra.text,
                                                            color = Color(0xFFE2E2E6),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            lineHeight = 22.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = mantra.category,
                                                                color = Color(0xFFF06292).copy(alpha = 0.7f),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )

                                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                IconButton(
                                                                    onClick = {
                                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                                        val clip = android.content.ClipData.newPlainText("Zen Mantra", mantra.text)
                                                                        clipboard.setPrimaryClip(clip)
                                                                        android.widget.Toast.makeText(context, "Copiado", android.widget.Toast.LENGTH_SHORT).show()
                                                                    },
                                                                    modifier = Modifier.size(34.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Share,
                                                                        contentDescription = "Copiar",
                                                                        tint = Color(0xFF94979F),
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }

                                                                IconButton(
                                                                    onClick = { viewModel.removeMantra(mantra) },
                                                                    modifier = Modifier.size(34.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Delete,
                                                                        contentDescription = "Borrar",
                                                                        tint = Color(0xFFF06292),
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Screen.AJUSTES -> {
                            var newNameInput by remember { mutableStateOf(userName) }
                            var showGroundingGuide by remember { mutableStateOf(false) }

                            LazyColumn(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top,
                                contentPadding = PaddingValues(24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    // Header Icon info
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .background(Color(0xFFD0BCFF).copy(alpha = 0.1f), CircleShape)
                                            .border(1.dp, Color(0xFFD0BCFF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Oasis",
                                            tint = Color(0xFFD0BCFF),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Configuración del Santuario",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = "Versión de Meditación 1.5 - Pro",
                                        color = Color(0xFF94979F),
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))
                                }

                                // Personalization: Naming card
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, Color(0xFF2C2F36))
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Text(
                                                text = "👤 ¿Cómo te llamaremos hoy?",
                                                color = Color(0xFFD0BCFF),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Personaliza tus saludos en el Santuario.",
                                                color = Color(0xFF94979F),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = newNameInput,
                                                    onValueChange = { newNameInput = it },
                                                    modifier = Modifier.weight(1f),
                                                    colors = TextFieldDefaults.colors(
                                                        focusedContainerColor = Color(0xFF0F1113),
                                                        unfocusedContainerColor = Color(0xFF0F1113),
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White,
                                                        focusedIndicatorColor = Color(0xFFD0BCFF)
                                                    ),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true
                                                )

                                                IconButton(
                                                    onClick = {
                                                        if (newNameInput.isNotEmpty()) {
                                                            sharedPreferences.edit().putString("user_name", newNameInput).apply()
                                                            userName = newNameInput
                                                            android.widget.Toast.makeText(context, "Nombre guardado: $newNameInput", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier.background(Color(0xFFD0BCFF), RoundedCornerShape(12.dp)).size(54.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Guardar", tint = Color(0xFF381E72))
                                                }
                                            }
                                        }
                                    }
                                }

                                // Settings details: METRONOME & LOCAL IA toggle
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, Color(0xFF2C2F36))
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "🔊 Metrónomo acústico",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "Genera suaves pulsos CDMAN en los cambios de fase respiratoria para guiarte sin mirar la pantalla.",
                                                        color = Color(0xFF94979F),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Switch(
                                                    checked = isAudioAlertsEnabled,
                                                    onCheckedChange = { viewModel.toggleAudioAlerts() },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color(0xFFD0BCFF),
                                                        checkedTrackColor = Color(0xFF49454F)
                                                    )
                                                )
                                            }

                                        }
                                    }
                                }

                                // Interactive Grounding emergency assistance tool (5-4-3-2-1 SOS Grounding)
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, Color(0xFF2C2F36))
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "🚨 Botón de SOS Antiestrés",
                                                        color = Color(0xFFFFB74D),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "Guía rápida interactiva 5-4-3-2-1 contra ansiedad extrema.",
                                                        color = Color(0xFF94979F),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                                Button(
                                                    onClick = { showGroundingGuide = !showGroundingGuide },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFFFFB74D).copy(alpha = 0.15f),
                                                        contentColor = Color(0xFFFFB74D)
                                                    ),
                                                    border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.3f))
                                                ) {
                                                    Text(if (showGroundingGuide) "Cerrar" else "Ver SOS")
                                                }
                                            }

                                            if (showGroundingGuide) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "Método de Anclaje 5-4-3-2-1:",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Busca en el espacio donde estés y concéntrate:\n\n" +
                                                           "👀 5 Cosas que puedas VER: Identifica formas o colores sutiles a tu alrededor.\n" +
                                                           "🖐️ 4 Cosas que puedas TOCAR: El teclado, tu ropa, la textura de tu mesa.\n" +
                                                           "👂 3 Cosas que puedas ESCUCHAR: La lluvia, los zumbidos mecánicos lejanos, tu propio pecho.\n" +
                                                           "👃 2 Cosas que puedas OLER: El aroma del café, el perfume, el aroma fresco del viento.\n" +
                                                           "👅 1 Cosa que puedas SABOREAR: Tu boca limpia, un vaso de agua pura.\n\n" +
                                                           "Siente tu peso firme sobre la silla, estás enraizado en la tierra. Todo está bien ahora.",
                                                    color = Color(0xFFE2E2E6),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // Interactive administrative controls (Delete local DB stats)
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, Color(0xFF2C2F36))
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Text(
                                                text = "⚙️ Administrar Datos",
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Reinicia tu contador de racha y estadísticas purificando el historial del santuario.",
                                                color = Color(0xFF94979F),
                                                style = MaterialTheme.typography.bodySmall,
                                                lineHeight = 16.sp
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))

                                            Button(
                                                onClick = {
                                                    viewModel.clearAllStats()
                                                    android.widget.Toast.makeText(context, "Historial de paz purificado. ¡Comienza de cero!", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFF06292).copy(alpha = 0.12f),
                                                    contentColor = Color(0xFFF06292)
                                                ),
                                                border = BorderStroke(1.dp, Color(0xFFF06292).copy(alpha = 0.25f)),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(100.dp)
                                            ) {
                                                Text("LIMPIAR HISTORIAL DE PAZ", fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                                            }
                                        }
                                    }
                                }

                                // Creator Information Panel
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, Color(0xFF2C2F36))
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Text(
                                                text = "✍️ Creador & Concepto",
                                                color = Color(0xFFD0BCFF),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Alberto Arce",
                                                color = Color.White,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "alberto.arce.ti@gmail.com",
                                                color = Color(0xFF94979F),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Diseñado para brindar momentos de alegría, purgar el estrés diario y recordarnos la mística belleza de respirar.",
                                                color = Color(0xFFE2E2E6),
                                                style = MaterialTheme.typography.bodySmall,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }

                                // AI Configuration guide Instructions
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, Color(0xFF2C2F36))
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Text(
                                                text = "💡 Cómo activar la Inteligencia de Servidor",
                                                color = Color(0xFFFFB74D),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "1. Genera una API Key en Google AI Studio.\n" +
                                                       "2. Abre el 'Secrets panel' en el menú de AI Studio de este editor web.\n" +
                                                       "3. Inserta tu clave con el nombre 'GEMINI_API_KEY'.\n" +
                                                       "4. Desactiva la casilla de 'Modo Simulado' superior para comenzar a generar de verdad con el servidor.",
                                                color = Color(0xFFC4C6D0),
                                                style = MaterialTheme.typography.bodySmall,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Celebration alert triggered on fully finished breathing timer targets
    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { showCompletionDialog = false },
            title = {
                Text(
                    text = "✨ ¡Momento Completado!",
                    color = Color(0xFFD0BCFF),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Felicidades, has consagrado $lastPracticedMinutes ${if (lastPracticedMinutes == 1) "minuto" else "minutos"} de atención plena con la técnica '$lastPracticedMode'. Tu mente te lo agradece profundamente. Sigue nutriendo tu racha diaria.",
                    color = Color(0xFFE2E2E6),
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCompletionDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72))
                ) {
                    Text("Aceptar")
                }
            },
            containerColor = Color(0xFF16181C),
            textContentColor = Color(0xFFE2E2E6)
        )
    }
}

// Custom Navigation Item mimicking rounded background selection logic (M3 style)
@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val animatedOpacity by animateFloatAsState(targetValue = if (isSelected) 1f else 0.55f, label = "tabsOpacity")

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(
                    if (isSelected) Color(0xFF2C2F36) else Color.Transparent
                )
                .padding(horizontal = 16.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFE8DEF8).copy(alpha = 0.55f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFE8DEF8).copy(alpha = animatedOpacity),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    iconText: String,
    colorBg: Color,
    colorBorder: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(colorBg, RoundedCornerShape(20.dp))
            .border(1.dp, colorBorder.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(text = iconText, fontSize = 20.sp, modifier = Modifier.padding(bottom = 6.dp))
            Text(
                text = title, 
                color = Color(0xFF94979F), 
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value, 
                color = Color.White, 
                style = MaterialTheme.typography.bodyMedium, 
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Embeds a looping Lofi/Rain YouTube stream to provide calming audio/video without need for internal heavy ExoPlayer implementations
@Composable
fun CalmVideoPlayer() {
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF2C2F36), RoundedCornerShape(16.dp)),
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = true
                webChromeClient = android.webkit.WebChromeClient()
                // Embed "Lofi chill" radio or a rain sound video. 
                // ID jfKfPfyJRdk is a widely known 24/7 Lofi Radio on youtube.
                val htmlData = """
                    <html><body style="margin:0;padding:0;background-color:#0A0C0E;">
                    <iframe width="100%" height="100%" 
                    src="https://www.youtube.com/embed/jfKfPfyJRdk?autoplay=1&loop=1&playlist=jfKfPfyJRdk&controls=1" 
                    frameborder="0" allow="autoplay; encrypted-media" allowfullscreen></iframe>
                    </body></html>
                """.trimIndent()
                loadDataWithBaseURL("https://www.youtube.com", htmlData, "text/html", "utf-8", null)
            }
        }
    )
}

@Composable
fun ZenMusicPlayerCard(isPlaying: Boolean, onToggle: (Boolean) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
    
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w4"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isPlaying) Color(0xFF80DEEA) else Color(0xFF2C2F36))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isPlaying) Color(0xFF80DEEA).copy(alpha = 0.15f) else Color(0xFF23252E),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(width = 3.dp, height = h1.dp).background(Color(0xFF80DEEA), RoundedCornerShape(1.dp)))
                            Box(modifier = Modifier.size(width = 3.dp, height = h2.dp).background(Color(0xFFD0BCFF), RoundedCornerShape(1.dp)))
                            Box(modifier = Modifier.size(width = 3.dp, height = h3.dp).background(Color(0xFFF06292), RoundedCornerShape(1.dp)))
                            Box(modifier = Modifier.size(width = 3.dp, height = h4.dp).background(Color(0xFF80DEEA), RoundedCornerShape(1.dp)))
                        }
                    } else {
                        Text("🎶", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Frecuencia de Sanación 432Hz",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isPlaying) "Ondas Theta + Campanas Zen activas" else "Binaural offline (toca para activar)",
                        color = if (isPlaying) Color(0xFF80DEEA) else Color(0xFF94979F),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 32.dp)
                    .background(
                        color = if (isPlaying) Color(0xFF80DEEA) else Color(0xFF2C2F36),
                        shape = RoundedCornerShape(100.dp)
                    )
                    .clickable { onToggle(!isPlaying) }
                    .padding(4.dp),
                contentAlignment = if (isPlaying) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(if (isPlaying) Color(0xFF0F1115) else Color(0xFF94979F), CircleShape)
                )
            }
        }
    }
}
