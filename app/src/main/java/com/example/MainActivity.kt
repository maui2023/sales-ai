package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ExcelExporter
import com.example.data.SaleItem
import com.example.data.SaleViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

// Navigation Route
enum class Screen {
    HOME, CALENDAR, CALCULATOR, SETTINGS
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifikasi diaktifkan! Daily reminder bersedia.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Izin notifikasi ditolak. Anda tidak akan menerima peringatan.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MainAppScreen(
    onRequestPermission: () -> Unit,
    viewModel: SaleViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val allSales by viewModel.allSales.collectAsState()
    val isReminderEnabled by viewModel.isDailyReminderEnabled.collectAsState()

    // Trigger permission requests when notification is enabled on first load or changes
    LaunchedEffect(isReminderEnabled) {
        if (isReminderEnabled) {
            onRequestPermission()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.HOME,
                    onClick = { currentScreen = Screen.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Utama") },
                    label = { Text("Utama", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2E1A47),
                        selectedTextColor = Color(0xFF2E1A47),
                        indicatorColor = Color(0xFFEADDFF),
                        unselectedIconColor = Color(0xFF5D6257),
                        unselectedTextColor = Color(0xFF5D6257)
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.CALENDAR,
                    onClick = { currentScreen = Screen.CALENDAR },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Kalendar") },
                    label = { Text("Kalendar", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2E1A47),
                        selectedTextColor = Color(0xFF2E1A47),
                        indicatorColor = Color(0xFFEADDFF),
                        unselectedIconColor = Color(0xFF5D6257),
                        unselectedTextColor = Color(0xFF5D6257)
                    ),
                    modifier = Modifier.testTag("nav_calendar")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.CALCULATOR,
                    onClick = { currentScreen = Screen.CALCULATOR },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Kira Sandbox") },
                    label = { Text("Kalkulator", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2E1A47),
                        selectedTextColor = Color(0xFF2E1A47),
                        indicatorColor = Color(0xFFEADDFF),
                        unselectedIconColor = Color(0xFF5D6257),
                        unselectedTextColor = Color(0xFF5D6257)
                    ),
                    modifier = Modifier.testTag("nav_calculator")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.SETTINGS,
                    onClick = { currentScreen = Screen.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Tetapan") },
                    label = { Text("Tetapan", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2E1A47),
                        selectedTextColor = Color(0xFF2E1A47),
                        indicatorColor = Color(0xFFEADDFF),
                        unselectedIconColor = Color(0xFF5D6257),
                        unselectedTextColor = Color(0xFF5D6257)
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.HOME -> HomeScreen(viewModel = viewModel, onNavigateToScreen = { currentScreen = it })
                    Screen.CALENDAR -> CalendarScreen(viewModel = viewModel)
                    Screen.CALCULATOR -> CalculatorScreen()
                    Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

// FORMAT UTIL
private val amountFormat = DecimalFormat("RM #,##0.00")

@Composable
fun AppHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        Color(0xFF00220C)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // App Brand Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            // Purple square logo with yellow circle center
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2E1A47)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE1E300))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sale AI",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            
            // Notification bell or active tag from layout
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEADDFF))
                    .clickable { }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifikasi",
                    tint = Color(0xFF2E1A47),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Avatar "AM" from design
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF003914))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE1E300)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AM",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF003914)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Screen Title Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE1E300), // Vibrant Yellow highlight
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        lineHeight = 16.sp
                    )
                }
            }
            
            // Premium/AI badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30))
                    .background(Color(0xFFE1E300))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "AI Active",
                        tint = Color(0xFF003914),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI PINTAR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003914)
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. HOME SCREEN - Monthly Sales Summary & Graphs
// ==========================================
@Composable
fun HomeScreen(viewModel: SaleViewModel, onNavigateToScreen: (Screen) -> Unit = {}) {
    val activeMonth by viewModel.activeMonth.collectAsState()
    val activeMonthSales by viewModel.activeMonthSales.collectAsState()
    val aiAdviceText by viewModel.aiAdviceText.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val isReminderEnabled by viewModel.isDailyReminderEnabled.collectAsState()

    // Aggregate values
    val totalSales = activeMonthSales.sumOf { it.amount }
    val totalCost = activeMonthSales.sumOf { it.cost }
    val totalProfit = totalSales - totalCost

    // Scroll container
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppHeader(
            title = "Ringkasan Sale AI",
            subtitle = "Analitis & penjejakan kewangan pintar mingguan"
        )

        // Daily Reminder Notification Banner from theme mockup
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE1E300)) // Brand bright YellowGold
                .border(BorderStroke(1.dp, Color(0xFF003914).copy(alpha = 0.15f)), RoundedCornerShape(16.dp))
                .clickable {
                    // Click to toggle reminder
                    viewModel.toggleDailyReminder(!isReminderEnabled)
                }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isReminderEnabled) Icons.Default.CheckCircle else Icons.Default.Edit,
                        contentDescription = "Alert icon",
                        tint = Color(0xFF003914),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PERINGATAN HARIAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003914),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isReminderEnabled) "Jangan lupa masukkan jualan hari ini!" 
                               else "Peringatan dimatikan. Ketik untuk mengaktifkan penggera harian.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF003914)
                    )
                }
            }
        }

        // Month Switcher Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    val current = sdfMonthParse(activeMonth)
                    val cal = Calendar.getInstance().apply {
                        time = current
                        add(Calendar.MONTH, -1)
                    }
                    viewModel.activeMonth.value = sdfMonthFormat(cal.time)
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Bulan Lepas", tint = MaterialTheme.colorScheme.primary)
                }

                Text(
                    text = getFormattedMonthYearMalay(activeMonth),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("month_label")
                )

                IconButton(onClick = {
                    val current = sdfMonthParse(activeMonth)
                    val cal = Calendar.getInstance().apply {
                        time = current
                        add(Calendar.MONTH, 1)
                    }
                    viewModel.activeMonth.value = sdfMonthFormat(cal.time)
                }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Bulan Depan", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Monthly Sales Summary Card (Theme design element)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF003914)), // Brand Deep Green
            shape = RoundedCornerShape(24.dp), // 3xl
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Subtle bright circle background ornament to match html design accent
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE1E300).copy(alpha = 0.1f))
                        .align(Alignment.TopEnd)
                        .offset(x = 16.dp, y = (-16).dp)
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Jumlah Jualan " + getFormattedMonthYearMalay(activeMonth),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFC6FFD1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = amountFormat.format(totalSales),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE1E300),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (totalSales > 0) "+12.5%" else "0.0%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE1E300) // Brand Yellow highlight
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "berbanding bulan lepas",
                            fontSize = 12.sp,
                            color = Color(0xFFC6FFD1).copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Metrics Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Total Jualan",
                value = amountFormat.format(totalSales),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                textColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Total Kos",
                value = amountFormat.format(totalCost),
                containerColor = Color(0xFFFFEBEE),
                textColor = Color(0xFFC62828),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Net Profit card with prominent accent
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "UNTUNG BERSIH (MARGIN)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = amountFormat.format(totalProfit),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalProfit >= 0) MaterialTheme.colorScheme.primary else Color(0xFFC62828)
                    )
                }

                val marginPercent = if (totalSales > 0) (totalProfit / totalSales) * 100 else 0.0
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30))
                        .background(
                            if (totalProfit >= 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color(0xFFFFEBEE)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%.1f%%", marginPercent),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalProfit >= 0) MaterialTheme.colorScheme.primary else Color(0xFFC62828)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions Grid
        Text(
            text = "Tindakan Pantas",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Button 1: Kira Untung
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToScreen(Screen.CALCULATOR) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)), // Lilac
                border = BorderStroke(1.dp, Color(0xFF2E1A47).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Kira Untung",
                        tint = Color(0xFF2E1A47),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "KIRA UNTUNG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E1A47),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Button 2: Kalendar
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToScreen(Screen.CALENDAR) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC6FFD1)), // Light Green
                border = BorderStroke(1.dp, Color(0xFF003914).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Kalendar",
                        tint = Color(0xFF003914),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "KALENDAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003914),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Button 3: Eksport
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToScreen(Screen.SETTINGS) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Eksport",
                        tint = Color(0xFF1A1C18),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "EKSPORT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1C18),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Graf Jualan Mengikut Usahawan
        Text(
            text = "Prestasi Jualan Mengikut Usahawan",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val groupSales = activeMonthSales.groupBy { it.entrepreneurName }
                val maxSales = groupSales.map { (_, items) -> items.sumOf { it.amount } }.maxOrNull() ?: 1.0

                if (groupSales.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Tiada Data",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tiada data jualan per usahawan bulan ini.",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                            Text(
                                "Sila ke tab Kalendar untuk mengisi data.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Graf Palang Urusniaga Jualan (RM)",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Native Canvas bar chart
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        val paddingLeft = 30.dp.toPx()
                        val paddingBottom = 40.dp.toPx()
                        val chartHeight = size.height - paddingBottom
                        val chartWidth = size.width - paddingLeft
                        val spacing = 20.dp.toPx()
                        
                        val numBars = groupSales.size
                        val barWidth = (chartWidth - (spacing * (numBars + 1))) / numBars

                        // Draw Grid lines
                        val numGridLines = 4
                        for (i in 0..numGridLines) {
                            val y = i * (chartHeight / numGridLines)
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.6f),
                                start = Offset(paddingLeft, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw bars
                        groupSales.entries.forEachIndexed { index, entry ->
                            val totalAmt = entry.value.sumOf { it.amount }
                            val totalCst = entry.value.sumOf { it.cost }
                            val factor = totalAmt / maxSales
                            val currentBarHeight = (chartHeight * factor).toFloat()

                            val startX = paddingLeft + spacing + index * (barWidth + spacing)
                            val startY = chartHeight - currentBarHeight

                            // Color map
                            val barColor = when (index % 3) {
                                0 -> Color(0xFF2E1A47) // Brand Deep Purple
                                1 -> Color(0xFFE1E300) // Brand Yellow
                                else -> Color(0xFF003914) // Brand Dark forest Green
                            }

                            // Draw rounded bars
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(startX, startY),
                                size = Size(barWidth, currentBarHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }
                        
                        // Draw base line
                        drawLine(
                            color = Color.DarkGray,
                            start = Offset(paddingLeft, chartHeight),
                            end = Offset(size.width, chartHeight),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Draw labels and legends
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                    ) {
                        groupSales.entries.forEachIndexed { index, entry ->
                            val totalAmt = entry.value.sumOf { it.amount }
                            val barColor = when (index % 3) {
                                0 -> Color(0xFF2E1A47)
                                1 -> Color(0xFFE1E300)
                                else -> Color(0xFF003914)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(barColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${entry.key} (${amountFormat.format(totalAmt)})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // AI Advisor Insight Card (Sale AI)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "AI Icon",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Penasihat AI Pintar Gemini",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Dapatkan ulasan prestasi jualan khas daripada AI",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (aiAdviceText.isEmpty()) {
                    Text(
                        text = "Inginkan saranan perniagaan masa nyata daripada AI tentang keuntungan jualan usahawan bulan ini?",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.generateAiAdvice(activeMonthSales) },
                        enabled = !isAiLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI sedang menganalisis...")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Analyze")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bina Saranan Perniagaan AI 🤖", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(12.dp)
                    ) {
                        Column {
                            // Text block with advice
                            Text(
                                text = aiAdviceText,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Re-run advisor if data has evolved
                            TextButton(
                                onClick = { viewModel.generateAiAdvice(activeMonthSales) },
                                modifier = Modifier.align(Alignment.End),
                                enabled = !isAiLoading
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Analisis Semula", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.7f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
        }
    }
}

// FlowRow alternative fallback for older Compose
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

// ==========================================
// 2. CALENDAR SCREEN - Key in Sales data & view list
// ==========================================
@Composable
fun CalendarScreen(viewModel: SaleViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    val selectedDate by viewModel.selectedDate.collectAsState()
    val activeMonth by viewModel.activeMonth.collectAsState()
    val selectedDateSales by viewModel.selectedDateSales.collectAsState()

    var saleToEdit by remember { mutableStateOf<SaleItem?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.testTag("add_sale_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Jualan")
            }
        }
    ) { ip ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ip)
        ) {
            AppHeader(
                title = "Kemukan Data Jualan",
                subtitle = "Pilih tarikh di kalendar untuk mengurus rekod jualan usahawan"
            )

            // Dynamic Month select
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kalendar: " + getFormattedMonthYearMalay(activeMonth),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row {
                    IconButton(onClick = {
                        val current = sdfMonthParse(activeMonth)
                        val cal = Calendar.getInstance().apply {
                            time = current
                            add(Calendar.MONTH, -1)
                        }
                        val newMonthStr = sdfMonthFormat(cal.time)
                        viewModel.activeMonth.value = newMonthStr
                        // Select 1st day of month by default
                        viewModel.selectedDate.value = "$newMonthStr-01"
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Sebelum", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = {
                        val current = sdfMonthParse(activeMonth)
                        val cal = Calendar.getInstance().apply {
                            time = current
                            add(Calendar.MONTH, 1)
                        }
                        val newMonthStr = sdfMonthFormat(cal.time)
                        viewModel.activeMonth.value = newMonthStr
                        viewModel.selectedDate.value = "$newMonthStr-01"
                    }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Seterusnya", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Days grid of selected Month
            DaysGrid(
                activeMonth = activeMonth,
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectedDate.value = it }
            )

            Divider(color = Color.LightGray.copy(alpha = 0.5f))

            // Sales list header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rekod Pada: " + getFormattedDateMalay(selectedDate),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${selectedDateSales.size} rekod",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Sales Lists
            if (selectedDateSales.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Kosong",
                            tint = Color.LightGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tiada jualan direkodkan.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Ketuk FAB (+) untuk memasukkan rekod jualan.",
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(selectedDateSales, key = { it.id }) { sale ->
                        SaleRowItem(
                            sale = sale,
                            onEditClick = { saleToEdit = sale },
                            onDeleteClick = { viewModel.deleteSale(sale) }
                        )
                    }
                }
            }
        }
    }

    // Add dialog sheet
    if (showAddDialog) {
        SaleInputDialog(
            date = selectedDate,
            onDismiss = { showAddDialog = false },
            onSave = { newSale ->
                viewModel.addSale(newSale)
                showAddDialog = false
            }
        )
    }

    // Edit dialog sheet
    if (saleToEdit != null) {
        SaleInputDialog(
            date = saleToEdit!!.date,
            saleToEdit = saleToEdit,
            onDismiss = { saleToEdit = null },
            onSave = { updatedSale ->
                viewModel.updateSale(updatedSale)
                saleToEdit = null
            }
        )
    }
}

@Composable
fun DaysGrid(
    activeMonth: String, // YYYY-MM
    selectedDate: String, // YYYY-MM-DD
    onDateSelected: (String) -> Unit
) {
    val yearAndMonth = activeMonth.split("-")
    if (yearAndMonth.size < 2) return

    val year = yearAndMonth[0].toIntOrNull() ?: 2026
    val month = yearAndMonth[1].toIntOrNull() ?: 6

    val daysInMonth = when (month) {
        2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    // Row layout scrolling horizontally so users aren't cramped on small displays!
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (day in 1..daysInMonth) {
            val dayStr = String.format("%02d", day)
            val fullDateStr = "$activeMonth-$dayStr"

            val isSelected = selectedDate == fullDateStr

            // Calculate day name
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                // Calendar month is 0-indexed
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
            }
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dayName = when (dayOfWeek) {
                Calendar.SUNDAY -> "Ahad"
                Calendar.MONDAY -> "Isnin"
                Calendar.TUESDAY -> "Sel"
                Calendar.WEDNESDAY -> "Rab"
                Calendar.THURSDAY -> "Kha"
                Calendar.FRIDAY -> "Jum"
                Calendar.SATURDAY -> "Sab"
                else -> "Day"
            }

            Box(
                modifier = Modifier
                    .width(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.White
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onDateSelected(fullDateStr) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = day.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SaleRowItem(
    sale: SaleItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Sale",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = sale.entrepreneurName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = sale.category,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Action controls
                Row {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (sale.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = sale.description,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(start = 34.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(start = 34.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // Pricing details inside Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 34.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Jualan Kasar", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text(amountFormat.format(sale.amount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }

                Column {
                    Text("Kos Modal", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text(amountFormat.format(sale.cost), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                }

                Column(horizontalAlignment = Alignment.End) {
                    val profitColor = if (sale.profit >= 0) Color(0xFF1B4332) else Color(0xFFC62828)
                    Text("Untung Bersih", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text(amountFormat.format(sale.profit), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = profitColor)
                }
            }
        }
    }
}

@Composable
fun SaleInputDialog(
    date: String,
    saleToEdit: SaleItem? = null,
    onDismiss: () -> Unit,
    onSave: (SaleItem) -> Unit
) {
    var amount by remember { mutableStateOf(saleToEdit?.amount?.toString() ?: "") }
    var cost by remember { mutableStateOf(saleToEdit?.cost?.toString() ?: "") }
    var usahawan by remember { mutableStateOf(saleToEdit?.entrepreneurName ?: "") }
    var description by remember { mutableStateOf(saleToEdit?.description ?: "") }

    val categories = listOf("Makanan", "Pakaian", "Runcit", "Lain-lain")
    var selectedCategory by remember { mutableStateOf(saleToEdit?.category ?: categories[0]) }

    val usahawanTemplates = listOf("Aminah", "Raju", "Chong", "Siti", "Saya (Self)")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (saleToEdit == null) "📝 Tambah Rekod Jualan" else "✏️ Edit Rekod Jualan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Tarikh Ambilan: " + getFormattedDateMalay(date),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Usahawan Name Text Field
                Text("Nama Usahawan / Pencatat", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = usahawan,
                    onValueChange = { usahawan = it },
                    placeholder = { Text("cth: Aminah, Raju") },
                    isError = usahawan.trim().isEmpty(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("usahawan_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                // Quick selects names
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    usahawanTemplates.forEach { name ->
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp, bottom = 4.dp)
                                .clip(RoundedCornerShape(30))
                                .background(Color(0xFFF1F5F9))
                                .clickable { usahawan = name }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Kategori Selector chips
                Text("Kategori Jualan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color(0xFFF1F5F9)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Price Inputs Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Jualan Input
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Harga Jualan (RM)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .testTag("amount_input"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Kos Input
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kos Modal (RM)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = cost,
                            onValueChange = { cost = it },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .testTag("cost_input"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Description Input
                Text("Keterangan Tambahan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("cth: jualan nasi lemak pagi katering") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Actions Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Batal", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val amtVal = amount.toDoubleOrNull() ?: 0.0
                            val costVal = cost.toDoubleOrNull() ?: 0.0
                            if (usahawan.trim().isEmpty()) {
                                return@Button
                            }
                            
                            val item = SaleItem(
                                id = saleToEdit?.id ?: 0,
                                date = date,
                                amount = amtVal,
                                cost = costVal,
                                entrepreneurName = usahawan.trim(),
                                category = selectedCategory,
                                description = description.trim()
                            )
                            onSave(item)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_button"),
                        shape = RoundedCornerShape(8.dp),
                        enabled = usahawan.trim().isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Simpan")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan Rekod", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// ==========================================
// 3. CALCULATOR SCREEN - Sandbox Profit & Loss calculator
// ==========================================
@Composable
fun CalculatorScreen() {
    var sellingPriceText by remember { mutableStateOf("") }
    var costPriceText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }

    val sellingPrice = sellingPriceText.toDoubleOrNull() ?: 0.0
    val costPrice = costPriceText.toDoubleOrNull() ?: 0.0
    val quantity = quantityText.toIntOrNull() ?: 1

    val totalRevenue = sellingPrice * quantity
    val totalCost = costPrice * quantity
    val totalProfit = totalRevenue - totalCost
    val marginPercent = if (totalRevenue > 0) (totalProfit / totalRevenue) * 100 else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppHeader(
            title = "Kalkulator Untung Rugi",
            subtitle = "Sediakan anggaran perniagaan & simulasi harga jualan di sini"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Simulasi Unit Produk/Servis",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Cost input
                Text("Kos Sediaan Seunit (RM)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                OutlinedTextField(
                    value = costPriceText,
                    onValueChange = { costPriceText = it },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("calc_cost_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Selling Price input
                Text("Harga Jual Seunit (RM)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                OutlinedTextField(
                    value = sellingPriceText,
                    onValueChange = { sellingPriceText = it },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("calc_sell_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quantity input
                Text("Kuantiti Sasaran / Jualan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    placeholder = { Text("1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Live calculations outputs card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    totalProfit > 0 -> Color(0xFFE8F5E9)  // Green
                    totalProfit < 0 -> Color(0xFFFFEBEE)  // Red
                    else -> Color(0xFFF8FAFC)            // Grey neutral
                }
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = 1.dp,
                color = when {
                    totalProfit > 0 -> Color(0xFF40916C)
                    totalProfit < 0 -> Color(0xFFEF9A9A)
                    else -> Color.DarkGray.copy(alpha = 0.2f)
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Status header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUS BELANJAWAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.DarkGray,
                        letterSpacing = 1.sp
                    )

                    val (statusLabel, labelColor, badgeColor) = when {
                        totalProfit > 0 -> Triple("UNTUNG ✅", Color(0xFF1B4332), Color(0xFFD8F3DC))
                        totalProfit < 0 -> Triple("PROFAIL RUGI ⚠️", Color(0xFFC62828), Color(0xFFFFCDD2))
                        else -> Triple("PULANG MODAL 🤝", Color.DarkGray, Color(0xFFE2E8F0))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30))
                            .background(badgeColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = labelColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Revenue and details rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Jumlah Anggaran Jualan:", fontSize = 13.sp, color = Color.DarkGray)
                    Text(amountFormat.format(totalRevenue), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Jumlah Anggaran Kos:", fontSize = 13.sp, color = Color.DarkGray)
                    Text(amountFormat.format(totalCost), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color.DarkGray.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Anggaran Untung Bersih:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text(
                            text = amountFormat.format(totalProfit),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (totalProfit >= 0) Color(0xFF1B4332) else Color(0xFFC62828)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Keuntungan Margin:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text(
                            text = String.format("%.2f%%", marginPercent),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (totalProfit >= 0) Color(0xFF1B4332) else Color(0xFFC62828)
                        )
                    }
                }
                
                // Extra tips based on condition
                if (totalProfit < 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "💡 Cadangan: Cuba naikkan Harga Jual seunit sekurang-kurangnya RM ${String.format("%.2f", (costPrice - sellingPrice))} untuk elak kerugian atau kurangkan kos sediaan seunit.",
                            fontSize = 11.sp,
                            color = Color(0xFFC62828),
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ==========================================
// 4. SETTINGS SCREEN - Excel Export & Reminder setups
// ==========================================
@Composable
fun SettingsScreen(viewModel: SaleViewModel) {
    val context = LocalContext.current
    val isReminderEnabled by viewModel.isDailyReminderEnabled.collectAsState()
    val activeMonth by viewModel.activeMonth.collectAsState()
    val activeMonthSales by viewModel.activeMonthSales.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppHeader(
            title = "Tetapan & Eksport",
            subtitle = "Kawal pemformatan notifikasi dan jana laporan spreadsheet bersepadu"
        )

        // Reminder configuration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sistem Notifikasi Peringatan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Kurangkan risiko keciciran rekod perakaunan dengan mengaktifkan penggera peringatan input harian.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Peringatan Harian (8:00 PM)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = if (isReminderEnabled) "Penggera Aktif" else "Penggera Dimatikan",
                            fontSize = 11.sp,
                            color = if (isReminderEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { viewModel.toggleDailyReminder(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.LightGray
                        ),
                        modifier = Modifier.testTag("reminder_switch")
                    )
                }
            }
        }

        // CSV Excel export Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Eksport Laporan Bulanan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Jana laporan jualan bulanan penuh berformat CSV (serasi dengan Microsoft Excel, Google Sheets, & Numbers) untuk tujuan pemfailan akaun usahawan.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Current selected month feedback
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bulan Laporan terpilih:", fontSize = 11.sp, color = Color.Gray)
                            Text(getFormattedMonthYearMalay(activeMonth), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        Text(
                            text = "${activeMonthSales.size} transaksi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        ExcelExporter.exportSalesToCsvAndShare(context, activeMonthSales, activeMonth)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Excel", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Eksport Fail Laporan Excel (CSV)", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Populate sample data tool (Great Developer UX!)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF4F0)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🛠️ Kotak Peralatan Pembangunan (Demo)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Belum ada rekod jualan sendiri? Suntik 5 baris data contoh premium untuk hari ini & kelmarin secara auto bagi meneroka grafik visual & pembantu kecerdasan buatan (Gemini AI Advisor) secara instan!",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = {
                        injectSampleData(viewModel, activeMonth)
                        Toast.makeText(context, "Selesai suntik 5 jualan urusniaga usahawan! Singgah tab Utama/Kalendar.", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Auto-Suntik Data Jualan Contoh", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// Injects premium simulated usahawan data to help visual testing
fun injectSampleData(viewModel: SaleViewModel, activeMonth: String) {
    val sales = listOf(
        SaleItem(date = "$activeMonth-05", amount = 320.0, cost = 120.0, entrepreneurName = "Aminah", category = "Makanan", description = "Pesanan Nasi Lemak Ayam 50 pax"),
        SaleItem(date = "$activeMonth-05", amount = 1450.0, cost = 600.0, entrepreneurName = "Raju", category = "Pakaian", description = "Jualan borong baju kurung songket"),
        SaleItem(date = "$activeMonth-12", amount = 450.0, cost = 150.0, entrepreneurName = "Chong", category = "Runcit", description = "Sapu tangan & barang plastik sebut harga"),
        SaleItem(date = "$activeMonth-14", amount = 150.0, cost = 25.0, entrepreneurName = "Aminah", category = "Makanan", description = "Desert Puding Karamel Mini 10 set"),
        SaleItem(date = "$activeMonth-15", amount = 850.0, cost = 400.0, entrepreneurName = "Siti", category = "Lain-lain", description = "Servis solekan & dandanan perkahwinan katering")
    )
    sales.forEach { viewModel.addSale(it) }
}

// ==========================================
// DATE HELPERS (Native, reliable)
// ==========================================
private fun sdfMonthParse(monthStr: String): Date {
    return try {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(monthStr) ?: Date()
    } catch (e: Exception) {
        Date()
    }
}

private fun sdfMonthFormat(date: Date): String {
    return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(date)
}

fun getFormattedMonthYearMalay(monthStr: String): String {
    // format "yyyy-MM" to "Jun 2026", etc.
    val parts = monthStr.split("-")
    if (parts.size < 2) return monthStr
    
    val monthNum = parts[1].toIntOrNull() ?: 1
    val year = parts[0]
    
    val monthName = when (monthNum) {
        1 -> "Januari"
        2 -> "Februari"
        3 -> "Mac"
        4 -> "April"
        5 -> "Mei"
        6 -> "Jun"
        7 -> "Julai"
        8 -> "Ogos"
        9 -> "September"
        10 -> "Oktober"
        11 -> "November"
        12 -> "Disember"
        else -> "Januari"
    }
    
    return "$monthName $year"
}

fun getFormattedDateMalay(dateStr: String): String {
    // format "yyyy-MM-dd" to "14 Jun 2026"
    val parts = dateStr.split("-")
    if (parts.size < 3) return dateStr
    
    val day = parts[2].toIntOrNull() ?: 1
    val monthNum = parts[1].toIntOrNull() ?: 1
    val year = parts[0]
    
    val monthName = when (monthNum) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mac"
        4 -> "Apr"
        5 -> "Mei"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Ogos"
        9 -> "Sept"
        10 -> "Okt"
        11 -> "Nov"
        12 -> "Dis"
        else -> "Jan"
    }
    
    return "$day $monthName $year"
}
