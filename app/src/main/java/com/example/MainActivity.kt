package com.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.*
import com.example.ui.theme.MyApplicationTheme
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

data class ZenScenario(
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

    // API Key Configuration check
    val rawApiKey = BuildConfig.GEMINI_API_KEY
    val isApiKeyConfigured = remember(rawApiKey) {
        rawApiKey.isNotEmpty() && rawApiKey != "MY_GEMINI_API_KEY" && !rawApiKey.startsWith("YOUR_")
    }

    // IA / Demo Mode State
    var isDemoMode by remember { mutableStateOf(!isApiKeyConfigured) }

    // --- State: Escape (AI Image Gen) ---
    var stressInput by remember { mutableStateOf("") }
    var isGeneratingImage by remember { mutableStateOf(false) }
    var generatedImage by remember { mutableStateOf<Bitmap?>(null) }
    var imageMessage by remember { mutableStateOf("Personaliza tu oasis de serenidad escribiendo tu estrés debajo o seleccionando un templo visual de paz.") }
    var translatedPromptUsed by remember { mutableStateOf("") }
    var activeLocalScenarioIndex by remember { mutableStateOf(0) }

    // Local scenarios list for demo or inspirations
    val localScenarios = remember {
        listOf(
            ZenScenario(
                title = "Río Calmo",
                metaphor = "Tu mente es un río de aguas tibias. Cada estrés es una hoja dorada que cae suavemente de los sauces y navega lejos, sin tocar el fondo.",
                prompt = "Serene watercolor of willow branch leaves floating on crystal clear misty river, magical golden light, calm ripples.",
                color1 = Color(0xFFD0BCFF),
                color2 = Color(0xFF49454F)
            ),
            ZenScenario(
                title = "Nebulosa Paz",
                metaphor = "Los plazos pendientes se desvanecen en una nebulosa cósmica violeta. Flotas sin peso, envuelto por estrellas tímidas que parpadean.",
                prompt = "Enchanting cosmic cloud nebula, deep purple and violet background with glowing soft spheres, minimalist abstract vector.",
                color1 = Color(0xFF64B5F6),
                color2 = Color(0xFF2C1B33)
            ),
            ZenScenario(
                title = "Dunas de Luz",
                metaphor = "La fatiga se disuelve en dunas de arena fina que el viento nocturno peina en silencio. Cada grano de arena brilla bajo la luna.",
                prompt = "Minimalist smooth sand dunes under soft moonlight dark sky, pink pastel highlights, calming atmosphere, highly cinematic.",
                color1 = Color(0xFFFFB74D),
                color2 = Color(0xFF2D2F36)
            ),
            ZenScenario(
                title = "Templo Lluvia",
                metaphor = "Las conversaciones se silencian bajo el compás rítmico de la lluvia sobre techos de bambú. Estás abrigado dentro del templo, respirando paz.",
                prompt = "Traditional Japanese architecture, wet stones reflection, gentle raindrops, warm soft paper lantern light, dreamy painting.",
                color1 = Color(0xFFF06292),
                color2 = Color(0xFF1C1326)
            )
        )
    }

    // --- State: Paz (Zen Reflection Generator) ---
    var generatedMantra by remember { mutableStateOf("El trabajo es solo una parte del paisaje, tú eres el cielo entero que lo rodea. Respira, no tienes nada que demostrar en este segundo.") }
    var isGeneratingMantra by remember { mutableStateOf(false) }

    // --- State: Breathing (Inicio) ---
    var isInspiring by remember { mutableStateOf(true) }
    var phaseText by remember { mutableStateOf("Inspira profundo...") }

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
        "Haz una pausa. Tómate el tiempo de simplemente ser.",
        "No eres tu trabajo. Eres el silencio místico entre tus pensamientos."
    )
    val currentQuote = remember(currentScreen) { quotes.random() }

    // Main Scaffold enclosing our entire polished experience
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Elegant Navigation Bar replicating the exact Sophisticated Dark profile
            Column {
                Divider(color = Color(0xFF44474F), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1C1E))
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(80.dp)
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
                        icon = Icons.Default.Search, // Represents Explorer/Escape
                        label = "Escape",
                        isSelected = currentScreen == Screen.ESCAPE,
                        onClick = { currentScreen = Screen.ESCAPE }
                    )
                    NavItem(
                        icon = Icons.Default.Favorite, // Represents Paz
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
                .background(Color(0xFF0F1113))
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Screen outer decorative frame box
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
                        shape = RoundedCornerShape(40.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF44474F),
                        shape = RoundedCornerShape(40.dp)
                    )
            ) {
                // Main content renderer per Selected Screen
                Crossfade(targetState = currentScreen, label = "screenTransition") { screen ->
                    when (screen) {
                        Screen.INICIO -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            ) {
                                // Supertitle
                                Text(
                                    text = "BIENVENIDO A TU OASIS",
                                    color = Color(0xFFD0BCFF),
                                    style = MaterialTheme.typography.titleSmall,
                                    letterSpacing = 4.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Text(
                                    text = "Desconéctate",
                                    color = Color(0xFF94979F),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 48.dp)
                                )

                                // Animated dynamic canvas breathing circle
                                Box(
                                    modifier = Modifier
                                        .size(220.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        // Draw smooth ambient glows
                                        drawCircle(
                                            color = Color(0xFFD0BCFF),
                                            radius = (size.minDimension / 2.2f) * scale,
                                            alpha = alpha * 0.4f
                                        )
                                        drawCircle(
                                            color = Color(0xFFD0BCFF).copy(alpha = alpha / 3),
                                            radius = (size.minDimension / 2.2f) * (scale * 1.25f),
                                            alpha = alpha * 0.2f
                                        )
                                        // Center core
                                        drawCircle(
                                            color = Color(0xFFD0BCFF),
                                            radius = 35.dp.toPx(),
                                            alpha = 0.8f
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(48.dp))

                                // Instruction text
                                Text(
                                    text = phaseText,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                // Subtitle quote
                                Text(
                                    text = currentQuote,
                                    color = Color(0xFF94979F),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 28.sp,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }

                        Screen.ESCAPE -> {
                            // Close Keyboard hooks
                            val keyboardController = LocalSoftwareKeyboardController.current
                            val focusManager = LocalFocusManager.current

                            LazyColumn(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top,
                                contentPadding = PaddingValues(24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    // Header tag
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = Color(0xFFD0BCFF).copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(100.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color(0xFFD0BCFF).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(100.dp)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isDemoMode) "MODO DEMO ACTIVO (LOCAL)" else "SABIDURÍA DE IA ACTIVA",
                                            color = Color(0xFFD0BCFF),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Oasis Visual IA",
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Light,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // Interactive display card
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(260.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Color(0xFF1F2023))
                                            .border(1.dp, Color(0xFF44474F), RoundedCornerShape(24.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isGeneratingImage) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                CircularProgressIndicator(color = Color(0xFFD0BCFF))
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "Invocando paz con la IA...",
                                                    color = Color(0xFFD0BCFF),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontStyle = FontStyle.Italic
                                                )
                                            }
                                        } else if (generatedImage != null) {
                                            Image(
                                                bitmap = generatedImage!!.asImageBitmap(),
                                                contentDescription = "Oasis Visual",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            // Procedural elegant custom canvas for offline/demo/initial screen
                                            val scenario = localScenarios[activeLocalScenarioIndex]
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawRect(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(scenario.color1.copy(alpha = 0.2f), Color(0xFF111318))
                                                    )
                                                )
                                                // Dynamic procedural smooth glowing rings representing nature
                                                drawCircle(
                                                    color = scenario.color1.copy(alpha = 0.4f),
                                                    radius = 120.dp.toPx(),
                                                    center = Offset(size.width / 2, size.height / 1.5f)
                                                )
                                                drawCircle(
                                                    color = Color.White.copy(alpha = 0.15f),
                                                    radius = 40.dp.toPx(),
                                                    center = Offset(size.width / 2, size.height / 1.7f)
                                                )
                                            }

                                            // Description overlay
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.4f))
                                                    .padding(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = scenario.metaphor,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 24.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))
                                }

                                // Quick serene preset selection row
                                item {
                                    Text(
                                        text = "Selecciona un paisaje de meditación:",
                                        color = Color(0xFF94979F),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        localScenarios.forEachIndexed { index, scenario ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        color = if (activeLocalScenarioIndex == index && generatedImage == null) {
                                                            Color(0xFFD0BCFF).copy(alpha = 0.2f)
                                                        } else {
                                                            Color(0xFF1F2023)
                                                        },
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (activeLocalScenarioIndex == index && generatedImage == null) {
                                                            Color(0xFFD0BCFF)
                                                        } else {
                                                            Color(0xFF44474F)
                                                        },
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        generatedImage = null
                                                        activeLocalScenarioIndex = index
                                                        stressInput = ""
                                                        imageMessage = scenario.metaphor
                                                    }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = scenario.title,
                                                    color = if (activeLocalScenarioIndex == index && generatedImage == null) {
                                                        Color(0xFFD0BCFF)
                                                    } else {
                                                        Color(0xFFE2E2E6)
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Custom stressful translator input field
                                item {
                                    OutlinedTextField(
                                        value = stressInput,
                                        onValueChange = { stressInput = it },
                                        placeholder = {
                                            Text(
                                                text = "O redacta tu propia carga (ej. 'Reunión estresante')",
                                                color = Color(0xFF94979F),
                                                fontSize = 13.sp
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF1A1C1E),
                                            unfocusedContainerColor = Color(0xFF1A1C1E),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedIndicatorColor = Color(0xFFD0BCFF),
                                            unfocusedIndicatorColor = Color(0xFF44474F)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        maxLines = 2,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        })
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Generator Action Button
                                    Button(
                                        onClick = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            coroutineScope.launch {
                                                isGeneratingImage = true
                                                if (isDemoMode) {
                                                    // Simulation of AI artwork loading locally
                                                    delay(3000)
                                                    // Move to another preset or construct customized metaphor
                                                    if (stressInput.isNotEmpty()) {
                                                        imageMessage = "Tu estrés ('$stressInput') ha sido absorbido por dunas infinitas de serenidad. Siente cómo todo se detiene."
                                                    }
                                                    generatedImage = null
                                                } else {
                                                    // Real API Call with translated metaphor using gemini
                                                    try {
                                                        val userStress = stressInput.ifEmpty { localScenarios[activeLocalScenarioIndex].title }
                                                        
                                                        // 1. Get relaxing Zen prompt in english from gemini-3.5-flash
                                                        val translationPrompt = "Eres un filósofo Zen y diseñador digital. Convierte este pesadumbre o estrés del usuario ('$userStress') en un prompt para un generador de imágenes en inglés que describa un paisaje de paz absoluta, abstracto, místico, maderas oscuras, acuarelas, orbes de luz blanda, sin palabras negativas, hermoso y relajante. Responde estrictamente con la descripción en inglés."
                                                        val textRequest = GenerateTextRequest(
                                                            contents = listOf(ContentText(parts = listOf(PartText(text = translationPrompt))))
                                                        )
                                                        val textResponse = withContext(Dispatchers.IO) {
                                                            RetrofitClient.textService.generateText(BuildConfig.GEMINI_API_KEY, textRequest)
                                                        }
                                                        val finalPr = textResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                                                            ?: "An incredibly peaceful abstract pastel color illustration of calm waters, glowing soft spheres and smooth mist"

                                                        translatedPromptUsed = finalPr

                                                        // 2. Call gemini-2.5-flash-image to generate the actual picture
                                                        val imageRequest = GenerateContentRequest(
                                                            contents = listOf(Content(parts = listOf(Part(text = finalPr)))),
                                                            generationConfig = GenerationConfig(
                                                                imageConfig = ImageConfig(aspectRatio = "1:1", imageSize = "1K"),
                                                                responseModalities = listOf("TEXT", "IMAGE")
                                                            )
                                                        )
                                                        val imageResponse = withContext(Dispatchers.IO) {
                                                            RetrofitClient.imageService.generateImage(BuildConfig.GEMINI_API_KEY, imageRequest)
                                                        }

                                                        val base64Str = imageResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData?.data
                                                        if (base64Str != null) {
                                                            val decoded = decodeBase64ToBitmap(base64Str)
                                                            if (decoded != null) {
                                                                generatedImage = decoded
                                                                imageMessage = "Tu oasis mental ha sido forjado con éxito."
                                                            } else {
                                                                imageMessage = "Se recibió la respuesta del servidor pero no pudo decodificarse como imagen."
                                                            }
                                                        } else {
                                                            imageMessage = "La IA no entregó datos visuales. Comprueba que el modelo tenga cuotas disponibles."
                                                        }
                                                    } catch (e: Exception) {
                                                        imageMessage = "Fallo de conexión IA: ${e.localizedMessage}. Tacha el modo offline en Ajustes para probar."
                                                    }
                                                }
                                                isGeneratingImage = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD0BCFF),
                                            contentColor = Color(0xFF381E72)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        shape = RoundedCornerShape(100.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Generar",
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = if (isDemoMode) "PROCESAR EN MODO LOCAL" else "GENERAR OASIS IA",
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                // Status warning if API key isn't setup
                                if (!isApiKeyConfigured) {
                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "⚠️ Llave API de Gemini vacía. Usando el motor de simulación local. Sigue las instrucciones del panel 'Ajustes' para conectar la IA del servidor.",
                                            color = Color(0xFFFFB74D),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else if (translatedPromptUsed.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Prompt IA Traducido: \"$translatedPromptUsed\"",
                                            color = Color(0xFF94979F),
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Screen.PAZ -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp)
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
                                        text = "MANTRAS DEL DESCANSO",
                                        color = Color(0xFFF06292),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Dynamic generated reflection container
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isGeneratingMantra) {
                                        CircularProgressIndicator(color = Color(0xFFF06292))
                                    } else {
                                        Text(
                                            text = generatedMantra,
                                            color = Color(0xFFE2E2E6),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Light,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 36.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isGeneratingMantra = true
                                            if (isDemoMode) {
                                                delay(1500)
                                                val localMantras = listOf(
                                                    "Afloja los hombros. Todo lo que te pesa desaparecerá de todos modos. Eres libre.",
                                                    "No eres un robot programado para producir. Tienes derecho a tumbarte y mirar los colores de la pared.",
                                                    "Hay una fuerza silenciosa en el universo velando por ti. Encomiéndate al momento actual.",
                                                    "Tu cuerpo sabe respirar solo. Abandona las riendas de tu control. Déjate fluir."
                                                )
                                                generatedMantra = localMantras.random()
                                            } else {
                                                try {
                                                    val request = GenerateTextRequest(
                                                        contents = listOf(ContentText(parts = listOf(PartText(text = "Escribe una reflexión corta y trascendental (máximo 25 palabras) en español, poética y sanadora, inspirada en la meditación zen y en aliviar el estrés laboral diciéndole al usuario que deje de preocuparse por el trabajo. Devuelve solo la reflexión."))))
                                                    )
                                                    val response = withContext(Dispatchers.IO) {
                                                        RetrofitClient.textService.generateText(BuildConfig.GEMINI_API_KEY, request)
                                                    }
                                                    generatedMantra = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                                        ?: "La calma ya está en ti. Solo debes parar y escucharla."
                                                } catch (e: Exception) {
                                                    generatedMantra = "Paz mística: Siente la respiración recorrer tus pulmones. La calma es la única constante."
                                                }
                                            }
                                            isGeneratingMantra = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF06292),
                                        contentColor = Color(0xFF4A148C)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text(
                                        text = "OTRA REFLEXIÓN IA",
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        Screen.AJUSTES -> {
                            LazyColumn(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top,
                                contentPadding = PaddingValues(24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    // Author info
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(
                                                Color(0xFFD0BCFF).copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(100.dp)
                                            )
                                            .border(1.dp, Color(0xFFD0BCFF), RoundedCornerShape(100.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Alberto Arce",
                                            tint = Color(0xFFD0BCFF),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Información del Santuario",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = "Versión de Meditación 1.2",
                                        color = Color(0xFF94979F),
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))
                                }

                                // Interactive IA Toggle
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1F2023), RoundedCornerShape(20.dp))
                                            .border(1.dp, Color(0xFF44474F), RoundedCornerShape(20.dp))
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Modo Simulado / Demo Local",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Activa este casillero si no dispones de un API Key en AI Studio, para disfrutar de simulaciones offline hermosas.",
                                                    color = Color(0xFF94979F),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            Switch(
                                                checked = isDemoMode,
                                                onCheckedChange = { isDemoMode = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color(0xFFD0BCFF),
                                                    checkedTrackColor = Color(0xFF49454F)
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // Author panel attribution
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2023)),
                                        shape = RoundedCornerShape(20.dp),
                                        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(Color(0xFF44474F), Color(0xFF44474F))))
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
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
                                                text = "Diseñado para brindar alegría, aliviar el estrés corporativo y recordarnos que vivir y respirar es el regalo definitivo.",
                                                color = Color(0xFFE2E2E6),
                                                style = MaterialTheme.typography.bodySmall,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // How to guide instructions
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2023)),
                                        shape = RoundedCornerShape(20.dp),
                                        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(Color(0xFF44474F), Color(0xFF44474F))))
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Text(
                                                text = "💡 Cómo configurar tu IA",
                                                color = Color(0xFFFFB74D),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "1. Consigue tu API Key gratuita en Google AI Studio.\n" +
                                                       "2. Abre el 'Secrets panel' en el menú de AI Studio de este editor web.\n" +
                                                       "3. Inserta tu llave con el nombre de 'GEMINI_API_KEY'.\n" +
                                                       "4. Desactiva la 'Modo Simulado' superior para comenzar a generar de verdad con el servidor.",
                                                color = Color(0xFFE2E2E6),
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
}

// Custom Navigation Item replicating the exact rounded background dynamic selection of Sophisticated Dark
@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val animatedOpacity by animateFloatAsState(targetValue = if (isSelected) 1f else 0.6f, label = "unselectedOpacity")

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
                    if (isSelected) Color(0xFF49454F) else Color.Transparent
                )
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFFE8DEF8) else Color(0xFFE8DEF8).copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFFE8DEF8) else Color(0xFFE8DEF8).copy(alpha = animatedOpacity),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Decodes standard Base64 response strings back into an interactive android Bitmap
fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}
