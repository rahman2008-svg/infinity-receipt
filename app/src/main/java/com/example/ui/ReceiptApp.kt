package com.example.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.CustomerEntity
import com.example.data.ProductEntity
import com.example.data.ReceiptEntity
import com.example.data.ReceiptWithProducts
import com.example.data.SettingsEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptApp(
    viewModel: ReceiptViewModel,
    isDarkThemeGlobal: MutableState<Boolean>
) {
    val context = LocalContext.current
    val currentScreen = viewModel.currentScreen
    val settingsState by viewModel.settings.collectAsStateWithLifecycle()
    val settings = settingsState ?: SettingsEntity()

    // Sync theme toggle with settings entity
    LaunchedEffect(settings.theme) {
        if (settingsState != null) {
            isDarkThemeGlobal.value = settings.theme == "Dark"
        }
    }

    // Dynamic launch timer for splash screen
    if (currentScreen == AppScreen.Splash) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            if (settings.businessName == "NexVora Labs") {
                viewModel.navigateToScreen(AppScreen.Intro)
            } else {
                viewModel.navigateToScreen(AppScreen.Dashboard)
            }
        }
    }

    // Intercept back actions
    BackHandler(enabled = currentScreen != AppScreen.Dashboard && currentScreen != AppScreen.Splash) {
        when (currentScreen) {
            AppScreen.Intro -> viewModel.navigateToScreen(AppScreen.Splash)
            AppScreen.ProfileSetup -> viewModel.navigateToScreen(AppScreen.Intro)
            AppScreen.CreateReceipt -> viewModel.navigateToScreen(AppScreen.Dashboard)
            AppScreen.History -> viewModel.navigateToScreen(AppScreen.Dashboard)
            AppScreen.Reports -> viewModel.navigateToScreen(AppScreen.Dashboard)
            AppScreen.Settings -> viewModel.navigateToScreen(AppScreen.Dashboard)
            else -> viewModel.navigateToScreen(AppScreen.Dashboard)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            AppScreen.Splash -> SplashScreen()
            AppScreen.Intro -> OnboardingIntroScreen(viewModel)
            AppScreen.ProfileSetup -> ProfileSetupScreen(viewModel)
            AppScreen.Dashboard -> DashboardScreen(viewModel)
            AppScreen.CreateReceipt -> CreateReceiptScreen(viewModel)
            AppScreen.History -> HistoryScreen(viewModel)
            AppScreen.Reports -> ReportsScreen(viewModel)
            AppScreen.Settings -> SettingsScreen(viewModel)
        }
    }
}

// 1. Splash Screen with beautiful icon and branding
@Composable
fun SplashScreen() {
    val context = LocalContext.current
    val logoBitmap = remember {
        val baseDir = context.getExternalFilesDir(null)
        val file = if (baseDir != null) {
            val path = File(baseDir, "NexReceipt/Receipts").parentFile?.parentFile?.absolutePath + "/drawable/img_app_logo.jpg"
            File(path)
        } else {
            null
        }

        if (file != null && file.exists() && file.isFile) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        } ?: BitmapFactory.decodeResource(context.resources, R.drawable.img_app_logo).asImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                bitmap = logoBitmap,
                contentDescription = "Infinity Receipt Logo",
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
                    .shadow(12.dp, RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Infinity Receipt",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )
            )

            Text(
                text = "Smart, Local-First Invoice Manager",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color(0xFF38BDF8),
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// 2. Beautiful Onboarding Intro and Setup
@Composable
fun OnboardingIntroScreen(viewModel: ReceiptViewModel) {
    var step by remember { mutableStateOf(1) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (step == index + 1) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (step == index + 1) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            // Step Content
            when (step) {
                1 -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Professional Receipts Instantly",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "তৈরি করুন যেকোনো ব্যবসা বা রেস্টুরেন্টের জন্য প্রফেশনাল রিসিট মাত্র কয়েক সেকেন্ডে।",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                2 -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Local-First & Offline Secure",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "সম্পূর্ণ অফলাইন সাপোর্ট। আপনার কোনো ডাটা সার্ভারে সেভ হয় না, সবকিছু আপনার মোবাইলেই সুরক্ষিত থাকে।",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                3 -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Configure Preferences",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Currency Selection Card
                    Text(
                        text = "Default Currency নির্বাচন করুন",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (viewModel.selectedCurrency == "BDT") MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    2.dp,
                                    if (viewModel.selectedCurrency == "BDT") MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectedCurrency = "BDT" }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("৳ BDT (Taka)", fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (viewModel.selectedCurrency == "USD") MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    2.dp,
                                    if (viewModel.selectedCurrency == "USD") MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectedCurrency = "USD" }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("\$ USD (Dollar)", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Theme Selection Card
                    Text(
                        text = "App Theme নির্বাচন করুন",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (viewModel.selectedTheme == "Dark") MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    2.dp,
                                    if (viewModel.selectedTheme == "Dark") MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectedTheme = "Dark" }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌙 Dark Mode", fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (viewModel.selectedTheme == "Light") MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    2.dp,
                                    if (viewModel.selectedTheme == "Light") MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectedTheme = "Light" }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("☀️ Light Mode", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    TextButton(onClick = { step-- }) {
                        Text("Back", fontSize = 16.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Button(
                    onClick = {
                        if (step < 3) {
                            step++
                        } else {
                            viewModel.navigateToScreen(AppScreen.ProfileSetup)
                        }
                    },
                    modifier = Modifier.testTag("onboarding_next_button")
                ) {
                    Text(if (step == 3) "Get Started" else "Next", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                }
            }
        }
    }
}

// 3. Profile Setup Screen for business metadata
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(viewModel: ReceiptViewModel) {
    var businessName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateToScreen(AppScreen.Intro) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "আপনার ব্যবসার প্রোফাইল সেটআপ করুন",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "এই তথ্যগুলো প্রতিটি রিসিটের উপরে প্রিন্ট আকারে থাকবে।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Business/Shop Name *") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_business_name_input")
                )
            }

            item {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address *") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number *") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address *") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = taxId,
                    onValueChange = { taxId = it },
                    label = { Text("Tax / BIN ID (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (businessName.trim().isEmpty() || address.trim().isEmpty() || phone.trim().isEmpty()) {
                            // simple alert placeholder
                        } else {
                            viewModel.saveInitialSetup(
                                businessName = businessName.trim(),
                                address = address.trim(),
                                phone = phone.trim(),
                                email = email.trim()
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_button"),
                    enabled = businessName.trim().isNotEmpty() && address.trim().isNotEmpty() && phone.trim().isNotEmpty()
                ) {
                    Text("Save & Open Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 4. Home Dashboard Screen with elegant Bento Grid Layout
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: ReceiptViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val settingsState by viewModel.settings.collectAsStateWithLifecycle()
    val settings = settingsState ?: SettingsEntity()

    // Calculate Stats
    val totalReceiptsCount = receipts.size
    
    // Today's receipts
    val todayDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
    val todaysReceipts = receipts.filter { it.date == todayDateStr }
    val todayReceiptsCount = todaysReceipts.size

    // Monthly Income calculation
    val currentMonthStr = SimpleDateFormat("MM/yyyy", Locale.US).format(Date())
    val monthlyIncome = receipts
        .filter { it.date.endsWith(currentMonthStr) }
        .sumOf { it.total }

    val lastGeneratedReceipt = receipts.firstOrNull()

    Scaffold(
        topBar = {
            // High fidelity Bento Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0061A4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = settings.businessName.ifEmpty { "Infinity Receipt" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "LOCAL FIRST STORAGE",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0061A4).copy(alpha = 0.7f)
                        )
                    }
                }
                
                IconButton(
                    onClick = { viewModel.navigateToScreen(AppScreen.Settings) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Row Bento Stats
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bento Card 1: Total Receipts
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD1E4FF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Receipts",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36)
                            )
                            Text(
                                text = "$totalReceiptsCount",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36)
                            )
                        }
                    }

                    // Bento Card 2: Today's Receipts
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1E2EC))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Today's Count",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191C1E)
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$todayReceiptsCount",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF191C1E)
                                )
                                if (todayReceiptsCount > 0) {
                                    Text(
                                        text = "+$todayReceiptsCount",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Monthly Income Bento Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF191C1E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val currentMonthName = SimpleDateFormat("MMMM", Locale.US).format(Date())
                            Text(
                                text = "Monthly Income ($currentMonthName)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%s %,.2f", settings.currency, monthlyIncome),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // 3. Quick Actions Asymmetric Bento Grid
            item {
                Column {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Column: Create Receipt (Larger Bento Action block)
                        Card(
                            modifier = Modifier
                                .weight(1.8f)
                                .height(180.dp)
                                .testTag("action_create_receipt")
                                .clickable {
                                    viewModel.startNewReceipt()
                                    viewModel.navigateToScreen(AppScreen.CreateReceipt)
                                },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0061A4))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = "Create\nReceipt",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    lineHeight = 24.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Right Column: Two stacked smaller cards
                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // History Action Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp)
                                    .testTag("action_history")
                                    .clickable {
                                        viewModel.navigateToScreen(AppScreen.History)
                                    },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFFE1E2EC))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = Color(0xFF0061A4),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "History",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF44474E)
                                    )
                                }
                            }

                            // Reports Action Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp)
                                    .clickable {
                                        viewModel.navigateToScreen(AppScreen.Reports)
                                    },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFFE1E2EC))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = Color(0xFF0061A4),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Reports",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF44474E)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Spanning bottom action row: Backup & Settings
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable {
                                viewModel.navigateToScreen(AppScreen.Settings)
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F0FF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = Color(0xFF0061A4),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Backup & Settings",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF001D36),
                                    fontSize = 14.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF001D36),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 4. Last Generated Receipt Bento Box
            item {
                Column {
                    Text(
                        text = "Last Generated Receipt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (lastGeneratedReceipt != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.activeReceiptWithProducts = null
                                    coroutineScope.launch {
                                        val detailed = viewModel.getReceiptWithProducts(lastGeneratedReceipt.id)
                                        viewModel.activeReceiptWithProducts = detailed
                                    }
                                },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFC4C6D0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "LAST GENERATED",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF74777F),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "#${lastGeneratedReceipt.receiptNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF74777F),
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Simulated miniature receipt thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(width = 44.dp, height = 50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF1F3FB))
                                            .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(8.dp))
                                            .padding(6.dp)
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color(0xFFE1E2EC).copy(alpha = 0.8f)))
                                            Box(modifier = Modifier.fillMaxWidth(0.7f).height(3.dp).background(Color(0xFFE1E2EC).copy(alpha = 0.8f)))
                                            Spacer(modifier = Modifier.weight(1f))
                                            Box(modifier = Modifier.fillMaxWidth(0.5f).height(3.dp).background(Color(0xFFE1E2EC).copy(alpha = 0.8f)))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = lastGeneratedReceipt.customerName.ifEmpty { "Guest Customer" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%s %,.2f • %s", settings.currency, lastGeneratedReceipt.total, lastGeneratedReceipt.date),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF44474E),
                                            fontSize = 12.sp
                                        )
                                    }
                                    
                                    Button(
                                        onClick = {
                                            viewModel.activeReceiptWithProducts = null
                                            coroutineScope.launch {
                                                val detailed = viewModel.getReceiptWithProducts(lastGeneratedReceipt.id)
                                                viewModel.activeReceiptWithProducts = detailed
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(32.dp),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "VIEW",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No receipts created yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Overlay Detailed Receipt Dialog if selected
        viewModel.activeReceiptWithProducts?.let { detailed ->
            ReceiptDetailsDialog(detailed, viewModel) {
                viewModel.activeReceiptWithProducts = null
            }
        }
    }
}

@Composable
fun DashboardActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// 5. Create Receipt & dynamic Live Preview Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReceiptScreen(viewModel: ReceiptViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val settingsState by viewModel.settings.collectAsStateWithLifecycle()
    val settings = settingsState ?: SettingsEntity()

    var showAiAssistant by remember { mutableStateOf(false) }
    var aiRawText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Receipt", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateToScreen(AppScreen.Dashboard) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAiAssistant = true }) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Auto-Fill", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Preview & Input Layout
            // We use vertical scrolling layout, but we could make it dual-pane on wide screens.
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Customer Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Customer Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = viewModel.customerName,
                                onValueChange = { viewModel.customerName = it },
                                label = { Text("Customer Name *") },
                                modifier = Modifier.fillMaxWidth().testTag("receipt_customer_name_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = viewModel.customerPhone,
                                onValueChange = { viewModel.customerPhone = it },
                                label = { Text("Phone Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = viewModel.customerEmail,
                                onValueChange = { viewModel.customerEmail = it },
                                label = { Text("Email Address") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = viewModel.customerAddress,
                                onValueChange = { viewModel.customerAddress = it },
                                label = { Text("Customer Address") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Receipt Info (Autogen values)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Receipt Information", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = viewModel.receiptNumber,
                                    onValueChange = { viewModel.receiptNumber = it },
                                    label = { Text("Receipt Number") },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = viewModel.dateStr,
                                    onValueChange = { viewModel.dateStr = it },
                                    label = { Text("Date") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                // Products Dynamic Addition
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Products & Items", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                Button(
                                    onClick = { viewModel.addEmptyProduct() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("add_item_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Item", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // List of fields for each added product line
                            viewModel.editingProducts.forEachIndexed { index, item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Item #${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        if (viewModel.editingProducts.size > 1) {
                                            IconButton(onClick = { viewModel.removeProductAt(index) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = item.name,
                                        onValueChange = { nameVal ->
                                            viewModel.editingProducts[index] = item.copy(name = nameVal)
                                        },
                                        label = { Text("Product Name *") },
                                        modifier = Modifier.fillMaxWidth().testTag("product_name_input_$index"),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = if (item.quantity == 0.0) "" else item.quantity.toString(),
                                            onValueChange = { qtyVal ->
                                                val q = qtyVal.toDoubleOrNull() ?: 0.0
                                                viewModel.editingProducts[index] = item.copy(quantity = q)
                                            },
                                            label = { Text("Qty *") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f).testTag("product_qty_input_$index"),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = if (item.price == 0.0) "" else item.price.toString(),
                                            onValueChange = { priceVal ->
                                                val p = priceVal.toDoubleOrNull() ?: 0.0
                                                viewModel.editingProducts[index] = item.copy(price = p)
                                            },
                                            label = { Text("Price *") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1.5f).testTag("product_price_input_$index"),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Discount & VAT Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Discount & Taxes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = viewModel.discountStr,
                                    onValueChange = { viewModel.discountStr = it },
                                    label = { Text("Flat Discount (${settings.currency})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = viewModel.vatRateStr,
                                    onValueChange = { viewModel.vatRateStr = it },
                                    label = { Text("VAT / Tax Rate (%)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                // Layout / Template chooser
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Choose Receipt Template", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))

                            val templates = listOf("Professional", "Modern", "Restaurant", "Shop", "Grocery", "Pharmacy", "Electronics")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(templates) { template ->
                                    val isSelected = viewModel.templateName == template
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.templateName = template }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = template,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Additional Notes field
                item {
                    OutlinedTextField(
                        value = viewModel.notes,
                        onValueChange = { viewModel.notes = it },
                        label = { Text("Notes / Terms printed at bottom") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Real-time Receipt Live Preview rendering mock
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "RECEIPT LIVE PREVIEW",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(settings.businessName, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 18.sp)
                        Text(settings.businessAddress, color = Color.Gray, fontSize = 11.sp)
                        Text("Phone: ${settings.businessPhone}", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Receipt: ${viewModel.receiptNumber}", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
                                Text("Customer: ${viewModel.customerName.ifEmpty { "Guest Customer" }}", color = Color.DarkGray, fontSize = 11.sp)
                            }
                            Text("Date: ${viewModel.dateStr}", color = Color.Gray, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()

                        // Products inline mock rows
                        var calculatedSub = 0.0
                        viewModel.editingProducts.forEach { p ->
                            if (p.name.isNotEmpty()) {
                                val itemTot = p.price * p.quantity
                                calculatedSub += itemTot
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${p.name} x${p.quantity.toInt()}", color = Color.Black, fontSize = 12.sp)
                                    Text(String.format(Locale.US, "%.2f", itemTot), color = Color.Black, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()

                        // Calculations summary
                        val flatDisc = viewModel.discountStr.toDoubleOrNull() ?: 0.0
                        val vRate = viewModel.vatRateStr.toDoubleOrNull() ?: 0.0
                        val vAmount = (calculatedSub - flatDisc) * (vRate / 100.0)
                        val totalBill = calculatedSub - flatDisc + vAmount

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("Subtotal: ${settings.currency} ${String.format(Locale.US, "%.2f", calculatedSub)}", color = Color.Black, fontSize = 11.sp)
                            if (flatDisc > 0) {
                                Text("Discount: -${settings.currency} ${String.format(Locale.US, "%.2f", flatDisc)}", color = Color.Red, fontSize = 11.sp)
                            }
                            if (vAmount > 0) {
                                Text("VAT (${vRate}%): ${settings.currency} ${String.format(Locale.US, "%.2f", vAmount)}", color = Color.Black, fontSize = 11.sp)
                            }
                            Text(
                                text = "Grand Total: ${settings.currency} ${String.format(Locale.US, "%.2f", totalBill)}",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Bottom Generate Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        // validation check
                        if (viewModel.editingProducts.none { it.name.trim().isNotEmpty() && it.price > 0 }) {
                            Toast.makeText(context, "কমপক্ষে ১ টি বৈধ আইটেম যোগ করুন!", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        
                        viewModel.generateReceipt { generatedId ->
                            Toast.makeText(context, "রিসিট সফলভাবে তৈরি ও লোকাল স্টোরেজে সেভ হয়েছে!", Toast.LENGTH_LONG).show()
                            
                            // Load detail and preview dialog
                            coroutineScope.launch {
                                val detailed = viewModel.getReceiptWithProducts(generatedId)
                                viewModel.activeReceiptWithProducts = detailed
                            }
                            viewModel.navigateToScreen(AppScreen.Dashboard)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_receipt_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate & Save Offline", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // AI Text-Assistant Dialog
        if (showAiAssistant) {
            Dialog(onDismissRequest = { showAiAssistant = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini AI Auto-Fill", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "টাইপ করুন বা ভয়েস মেসেজ পেস্ট করুন। Gemini AI আপনার টেক্সট থেকে কাস্টমার, আইটেম ও দাম নিজে থেকেই বের করে ফর্ম পূরণ করে দিবে!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = aiRawText,
                            onValueChange = { aiRawText = it },
                            placeholder = { Text("যেমন: Rafiq bought 3 apple for 15tk each, discount 10tk, address Dhaka") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (viewModel.aiParsingStatus.isNotEmpty()) {
                            Text(
                                text = viewModel.aiParsingStatus,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(onClick = { showAiAssistant = false }, modifier = Modifier.weight(1f)) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    viewModel.parseWithAI(aiRawText)
                                },
                                modifier = Modifier.weight(1.5f).testTag("ai_parse_submit"),
                                enabled = aiRawText.trim().isNotEmpty()
                            ) {
                                Text("Parse & Fill")
                            }
                        }
                    }
                }
            }
        }
    }
}

// 6. History Screen with dynamic filter and action menu
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: ReceiptViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val settingsState by viewModel.settings.collectAsStateWithLifecycle()
    val settings = settingsState ?: SettingsEntity()

    var showActionMenuForReceipt by remember { mutableStateOf<ReceiptEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receipt History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateToScreen(AppScreen.Dashboard) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("কাস্টমার, মোবাইল নাম্বার, রিসিট আইডি দিয়ে খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("search_receipt_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Receipts List organized by date/year folder view concept
            if (receipts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No matching receipts found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(receipts) { receipt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        val detailed = viewModel.getReceiptWithProducts(receipt.id)
                                        viewModel.activeReceiptWithProducts = detailed
                                    }
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic Thumbnail loaded from disk
                                val thumbFile = File(receipt.jsonPath.replace(".json", ".thumb"))
                                if (thumbFile.exists()) {
                                    AsyncImage(
                                        model = thumbFile,
                                        contentDescription = "Receipt Thumbnail",
                                        modifier = Modifier
                                            .size(50.dp, 70.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp, 70.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = receipt.receiptNumber,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = receipt.customerName.ifEmpty { "Guest Customer" },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${receipt.date} ${receipt.time}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format(Locale.US, "%s %,.2f", settings.currency, receipt.total),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    IconButton(
                                        onClick = { showActionMenuForReceipt = receipt },
                                        modifier = Modifier.testTag("receipt_options_button_${receipt.receiptNumber}")
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overlay Detailed Dialog if selected
        viewModel.activeReceiptWithProducts?.let { detailed ->
            ReceiptDetailsDialog(detailed, viewModel) {
                viewModel.activeReceiptWithProducts = null
            }
        }

        // Action menu sheet mock
        showActionMenuForReceipt?.let { receipt ->
            AlertDialog(
                onDismissRequest = { showActionMenuForReceipt = null },
                title = { Text(receipt.receiptNumber, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Divider()
                        // 1. Share
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenuForReceipt = null
                                    val uri = viewModel.getShareUri(receipt.pdfPath)
                                    if (uri != null) {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Receipt PDF"))
                                    } else {
                                        Toast.makeText(context, "রিসিট ফাইল পাওয়া যায়নি!", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Share PDF Invoice", style = MaterialTheme.typography.bodyLarge)
                        }

                        // 2. Edit
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenuForReceipt = null
                                    viewModel.startNewReceipt(receipt)
                                    viewModel.navigateToScreen(AppScreen.CreateReceipt)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Edit & Re-Generate", style = MaterialTheme.typography.bodyLarge)
                        }

                        // 3. Duplicate
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenuForReceipt = null
                                    viewModel.duplicateReceipt(receipt)
                                    Toast.makeText(context, "রিসিটটি কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Duplicate Receipt", style = MaterialTheme.typography.bodyLarge)
                        }

                        // 4. Delete
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenuForReceipt = null
                                    viewModel.deleteReceipt(receipt)
                                    Toast.makeText(context, "রিসিট মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Delete Receipt", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showActionMenuForReceipt = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

// Detailed Receipt Interactive Dialog
@Composable
fun ReceiptDetailsDialog(
    detailed: ReceiptWithProducts,
    viewModel: ReceiptViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageFile = File(detailed.receipt.imagePath)

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of Dialog
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = detailed.receipt.receiptNumber,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    )

                    IconButton(onClick = { onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                // Body containing the generated high-quality PNG of the receipt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.LightGray)
                ) {
                    if (imageFile.exists()) {
                        AsyncImage(
                            model = imageFile,
                            contentDescription = "Receipt Full View",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("প্রিভিউ ইমেজ লোড করা সম্ভব হয়নি!", color = Color.DarkGray)
                        }
                    }
                }

                // Share PDF / Share Image Options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val uri = viewModel.getShareUri(detailed.receipt.imagePath)
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Image Invoice"))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share PNG", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val uri = viewModel.getShareUri(detailed.receipt.pdfPath)
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share PDF Invoice"))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share PDF", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// 7. Analytics & Graphic Reports Screen (Canvas Custom Graphs)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReceiptViewModel) {
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val settingsState by viewModel.settings.collectAsStateWithLifecycle()
    val settings = settingsState ?: SettingsEntity()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateToScreen(AppScreen.Dashboard) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats summary card
            item {
                val totalSales = receipts.sumOf { it.total }
                val averageReceipt = if (receipts.isNotEmpty()) totalSales / receipts.size else 0.0
                val totalTax = receipts.sumOf { it.vat }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sales Summary Reports", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Sales Amount:")
                            Text(String.format(Locale.US, "%s %,.2f", settings.currency, totalSales), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Average Receipt Size:")
                            Text(String.format(Locale.US, "%s %,.2f", settings.currency, averageReceipt), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total VAT Collected:")
                            Text(String.format(Locale.US, "%s %,.2f", settings.currency, totalTax), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Sales Canvas Graph Representation
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Sales Graph",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom drawing daily trend charts
                        val barColor = MaterialTheme.colorScheme.primary
                        val lineColor = MaterialTheme.colorScheme.outline
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw horizontal lines
                                val gridLineCount = 4
                                for (i in 0..gridLineCount) {
                                    val y = (size.height / gridLineCount) * i
                                    drawLine(
                                        color = lineColor.copy(alpha = 0.2f),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1f
                                    )
                                }

                                // Mock Bar charts
                                val barWidth = 35f
                                val spacing = 45f
                                val itemsList = listOf(400.0, 800.0, 1500.0, 1100.0, 2400.0, 1900.0)
                                val maxVal = 2500.0

                                itemsList.forEachIndexed { index, value ->
                                    val barHeight = (value / maxVal) * size.height
                                    val left = spacing + index * (barWidth + spacing)
                                    val top = size.height - barHeight.toFloat()
                                    
                                    // Draw actual column
                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight.toFloat()),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                                    )
                                }
                            }
                        }

                        // Label
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            days.forEach { day ->
                                Text(day, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Spend Leaders customers
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Top Customer Analytics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        val leaderList = receipts.groupBy { it.customerName }
                            .mapValues { entry -> entry.value.sumOf { it.total } }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(3)

                        if (leaderList.isNotEmpty()) {
                            leaderList.forEach { (name, amt) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name.ifEmpty { "Guest Customer" }, fontWeight = FontWeight.Medium)
                                    Text(String.format(Locale.US, "%s %,.2f", settings.currency, amt), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        } else {
                            Text("No customer records yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

// 8. General Settings & Backup/Restore Configuration
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ReceiptViewModel) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsStateWithLifecycle()
    val settings = settingsState ?: SettingsEntity()

    var businessName by remember { mutableStateOf(settings.businessName) }
    var address by remember { mutableStateOf(settings.businessAddress) }
    var phone by remember { mutableStateOf(settings.businessPhone) }
    var email by remember { mutableStateOf(settings.businessEmail) }
    var taxId by remember { mutableStateOf(settings.businessTaxId) }

    // Backup restore launchers
    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                viewModel.restoreBackup(it) { success ->
                    if (success) {
                        Toast.makeText(context, "রিসিট ডাটাবেজ সফলভাবে রিস্টোর হয়েছে!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "রিস্টোর ব্যর্থ হয়েছে! সঠিক Backup.zip নির্বাচন করুন।", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    // Sync input fields when settingsState updates
    LaunchedEffect(settingsState) {
        settingsState?.let {
            businessName = it.businessName
            address = it.businessAddress
            phone = it.businessPhone
            email = it.businessEmail
            taxId = it.businessTaxId
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateToScreen(AppScreen.Dashboard) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General business profile edit
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Edit Business Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Business Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = taxId,
                            onValueChange = { taxId = it },
                            label = { Text("Tax ID") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.updateGeneralSettings(
                                    settings.copy(
                                        businessName = businessName,
                                        businessAddress = address,
                                        businessPhone = phone,
                                        businessEmail = email,
                                        businessTaxId = taxId
                                    )
                                )
                                Toast.makeText(context, "ব্যবসার প্রোফাইল সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_general_settings")
                        ) {
                            Text("Update Profile")
                        }
                    }
                }
            }

            // Preferences selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("App Preferences", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Toggle Currency
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Currency Unit:")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val currencies = listOf("BDT", "USD")
                                currencies.forEach { c ->
                                    val isSel = settings.currency == c
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Gray, RoundedCornerShape(6.dp))
                                            .clickable {
                                                viewModel.updateGeneralSettings(settings.copy(currency = c))
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(c, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Toggle Theme
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Theme Mode:")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val themes = listOf("Dark", "Light")
                                themes.forEach { t ->
                                    val isSel = settings.theme == t
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Gray, RoundedCornerShape(6.dp))
                                            .clickable {
                                                viewModel.updateGeneralSettings(settings.copy(theme = t))
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(t, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Backup & Restore Block
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("One-Click Backup & Restore", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "আপনার ফোনের সমস্ত রিসিট ডাটা, ইনভয়েস ইমেজ এবং পিডিএফ ফাইল একসাথে জিপ আকারে সেভ করে রাখতে পারেন এবং যেকোনো সময় আবার রিস্টোর করতে পারেন।",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    documentPicker.launch(arrayOf("application/zip"))
                                },
                                modifier = Modifier.weight(1f).testTag("restore_backup_button")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore ZIP")
                            }

                            Button(
                                onClick = {
                                    viewModel.createBackup { path ->
                                        if (path != null) {
                                            // Share ZIP file
                                            val uri = viewModel.getShareUri(path)
                                            if (uri != null) {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/zip"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share/Export Backup ZIP"))
                                            }
                                        } else {
                                            Toast.makeText(context, "ব্যাকআপ তৈরিতে সমস্যা হয়েছে!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("export_backup_button")
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Backup ZIP")
                            }
                        }
                    }
                }
            }

            // 4. About Developer Bento Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0061A4))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "About Developer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Prince AR Abdur Rahman",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                        
                        Text(
                            text = "Independent App Developer",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Independent App Developer passionate about building modern Android applications, productivity tools, AI-powered experiences, media players, educational apps, and next-generation digital products.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "CONTACT & SOCIALS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801707424006"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF0061A4),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp 1", color = Color(0xFF0061A4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801796951709"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF0061A4),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp 2", color = Color(0xFF0061A4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/share/1BNn32qoJo/"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Facebook", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/ur___abdur____rahman__2008"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Instagram", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 5. About Company Bento Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE1E2EC))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE7F0FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF0061A4),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "About Company",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "NexVora Lab's Ofc",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "NexVora Lab's Ofc focuses on creating innovative Android applications designed to improve productivity, entertainment, learning, and digital experiences.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3FB))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "OUR MISSION",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color(0xFF0061A4)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Build fast, beautiful, privacy-friendly, and user-focused applications accessible to everyone.",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF44474E)
                                )
                            }
                        }
                    }
                }
            }

            // 6. Technical Info & Credits Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF191C1E))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Technical Information",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "v1.0.0",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        Text(
                            text = "Credits",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF0061A4)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Developed by Prince AR Abdur Rahman",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF0061A4)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Published by NexVora Lab's Ofc",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "© 2026 NexVora Lab's Ofc. All Rights Reserved.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}
