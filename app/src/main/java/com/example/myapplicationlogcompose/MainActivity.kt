package com.example.myapplicationlogcompose

import android.app.Activity
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.myapplicationlogcompose.ui.theme.MyApplicationlogcomposeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- IMPORT VICO CHARTS ---
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.compose.component.shape.shader.toDynamicShader

// ==========================================
// CONFIG & THEME
// ==========================================
const val GOOGLE_SCRIPT_URL = ""

val PrimaryGreen = Color(0xFF2E7D32)
val LightGreen = Color(0xFFE8F5E9)
val AccentOrange = Color(0xFFFF9800)
val TextDark = Color(0xFF1B1B1B)
val TextGray = Color(0xFF757575)
val CardBg = Color.White
val BackgroundApp = Color(0xFFF5F7FA)

// ==========================================
// DATA MODELS & LISTS
// ==========================================

data class LogHistoryItem(
    val date: String,
    val fullDate: String,
    val operator: String,
    val machine: String,
    val category: String,
    val issue: String,
    val time: String,
    val durationMin: Int
)

val rawMachineCodes = listOf("A", "B", "P", "Q", "E", "F", "H", "I", "J", "K", "L", "AA", "AB", "AC", "AD", "AE", "AF", "AG", "AH", "AI", "AJ", "AK", "N")
val machineList = rawMachineCodes.map { "ZAP$it" }

// UPDATE: MENAMBAHKAN LIST IJP KE DALAM DROPDOWN (OPSI B)
// Operator tetap bisa input manual kode IJP lewat Opsi A
val downtimeData = mapOf(
    "IJP (Masalah Umum)" to listOf( // Saya beri nama beda dikit biar gak bingung sama IJP manual
        "Perbaikan video jet alarm",
        "Perbaikan video jet EHT",
        "Perbaikan expired",
        "Perbaikan expired rusak",
        "Perbaikan expired kadang-kadang",
        "Perbaikan expired sebagian",
        "Perbaikan expired hilang",
        "Perbaikan alarm brake",
        "Perbaikan sensor Hitachi",
        "Perbaikan sensor film eror",
        "Perbaikan alarm convertor",
        "Perbaikan Hitachi eror"
    ),
    "KLIP" to listOf("Ganti per end close", "Perbaikan Unklip", "Perbaikan klip tajam", "Perbaikan klip numpuk", "Perbaikan klip rusak", "Ganti per push pin", "Perbaikan cam cluth tidak narik"),
    "OTH" to listOf("Lain-lain / Other Issues"),
    "DUC" to listOf("Perbaikan DUC"),
    "NON DT" to listOf("Nunggu adonan telat", "Tray susun telat"),
    "PM" to listOf("Pembersihan bongkar end close"),
    "SEAL" to listOf("Perbaikan unseal", "Perbaikan seal lemah", "Perbaikan seal mati", "Setting seal", "Perbaikan seal current tidak stabil", "Perbaikan seal hilang", "Perbaikan seal", "Perbaikan roll feeding tidak stabil")
)

val operatorList = listOf("Rian Budi Rahayu", "Hanif Mustofa", "Santoso B", "Dwi Saputro", "Tomi Nugroho", "Anggit Anggoro", "Mahammad Nur Sahid", "Endriyanto", "Lilik Utomo", "Yhoga Mahardika", "Diyan Dwi Prabowo Setiyanto", "Muhammad Rafli", "Agung Budiyono", "Santoso A", "Ivanka Mahendra Jaya", "Mufit Riyadi", "Muhamad Husen Alfachrian(polybag)", "Yusuf(polybag)", "Arif Nur Ikhsan", "Muhammad Alddani Meista Muam", "Abdul Rochim", "Muh Hanung Prakoso", "Ahmad Nur Cahyo", "Muhammad Haqqul Mubin", "Eko Rahmanto", "Andrean Dimas Aditya", "Andi Saputro", "Muhammad Alip Alhabib", "Andre Dzakwan Pratama", "Aril Isman", "Agung Slamet Supriyadi", "Santosoo B")

// ==========================================
// HELPER FUNCTIONS
// ==========================================
fun calculateDurationMinutes(timeRange: String): Int {
    return try {
        val times = timeRange.split(" - ")
        if (times.size != 2) return 0

        val startParts = times[0].trim().split(":")
        val endParts = times[1].trim().split(":")

        val startH = startParts[0].toInt()
        val startM = startParts[1].toInt()
        val endH = endParts[0].toInt()
        val endM = endParts[1].toInt()

        val startTotal = startH * 60 + startM
        val endTotal = endH * 60 + endM

        var diff = endTotal - startTotal
        if (diff < 0) diff = 0
        diff
    } catch (e: Exception) {
        0
    }
}

fun getTodayDate(): String = SimpleDateFormat("dd/MM", Locale.US).format(Date())

// ==========================================
// MAIN ACTIVITY
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationlogcomposeTheme {
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = PrimaryGreen.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                    }
                }
                var currentScreen by remember { mutableStateOf("splash") }
                LaunchedEffect(Unit) { delay(2500); currentScreen = "dashboard" }
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundApp) {
                    when (currentScreen) {
                        "splash" -> FancySplashScreen()
                        else -> MainAppStructure(currentScreen, { currentScreen = it })
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppStructure(currentScreen: String, onNavigate: (String) -> Unit) {
    var logs by remember { mutableStateOf<List<LogHistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val fetchData: () -> Unit = {
        isLoading = true
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = URL(GOOGLE_SCRIPT_URL).readText()
                val jsonArray = JSONArray(response)
                val fetchedLogs = mutableListOf<LogHistoryItem>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val rawDate = obj.optString("date", "")
                    val timeRange = obj.optString("time", "00:00 - 00:00")

                    fetchedLogs.add(LogHistoryItem(
                        date = rawDate,
                        fullDate = rawDate.split(" ")[0],
                        operator = obj.optString("operator", "Unknown"),
                        machine = obj.optString("machine", "-"),
                        category = obj.optString("category", "-"),
                        issue = obj.optString("issue", "-"),
                        time = timeRange,
                        durationMin = calculateDurationMinutes(timeRange)
                    ))
                }
                withContext(Dispatchers.Main) {
                    logs = fetchedLogs
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    Scaffold(
        bottomBar = {
            if (currentScreen != "add_log") {
                NavigationBar(containerColor = CardBg, tonalElevation = 10.dp, modifier = Modifier.shadow(16.dp)) {
                    NavigationBarItem(selected = currentScreen == "dashboard", onClick = { onNavigate("dashboard") }, icon = { Icon(if (currentScreen == "dashboard") Icons.Filled.Home else Icons.Outlined.Home, null) }, label = { Text("Home") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryGreen, indicatorColor = LightGreen))
                    NavigationBarItem(selected = currentScreen == "analytics", onClick = { onNavigate("analytics") }, icon = { Icon(if (currentScreen == "analytics") Icons.Filled.DateRange else Icons.Outlined.DateRange, null) }, label = { Text("Analytics") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryGreen, indicatorColor = LightGreen))
                    NavigationBarItem(selected = currentScreen == "history", onClick = { onNavigate("history") }, icon = { Icon(if (currentScreen == "history") Icons.Filled.List else Icons.Outlined.List, null) }, label = { Text("History") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryGreen, indicatorColor = LightGreen))
                }
            }
        },
        floatingActionButton = {
            if (currentScreen == "dashboard") {
                ExtendedFloatingActionButton(onClick = { onNavigate("add_log") }, containerColor = PrimaryGreen, contentColor = Color.White, elevation = FloatingActionButtonDefaults.elevation(8.dp), icon = { Icon(Icons.Default.Add, null) }, text = { Text("LOG BARU", fontWeight = FontWeight.Bold) })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                "dashboard" -> DashboardUltraScreen(logs, isLoading, fetchData) { onNavigate("analytics") }
                "analytics" -> AnalyticsDetailScreen(logs, onBack = { onNavigate("dashboard") })
                "history" -> HistoryLogScreen(logs, isLoading, fetchData)
                "add_log" -> AddDowntimeLogScreen(onBack = { onNavigate("dashboard") })
            }
        }
    }
}

// ==========================================
// SCREEN 1: DASHBOARD (EDITED: ACTIVE MACHINE REMOVED)
// ==========================================
@Composable
fun DashboardUltraScreen(logs: List<LogHistoryItem>, isLoading: Boolean, onRefresh: () -> Unit, onNavigateToAnalytics: () -> Unit) {
    val todayDate = getTodayDate()
    val todaysLogs = logs.filter { it.date.startsWith(todayDate) }

    val totalDowntimeToday = todaysLogs.sumOf { it.durationMin }
    val hours = totalDowntimeToday / 60
    val mins = totalDowntimeToday % 60
    val downtimeString = "${hours}h ${mins}m"

    val uptimePercent = if(machineList.isNotEmpty()) {
        val totalProductionMinutes = machineList.size * 8 * 60
        val uptime = ((totalProductionMinutes - totalDowntimeToday).toFloat() / totalProductionMinutes) * 100
        "%.1f%%".format(uptime.coerceAtMost(100f))
    } else "100%"

    val rotateAnim by animateFloatAsState(targetValue = if (isLoading) 360f else 0f, label = "rotate")

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(PrimaryGreen, Color(0xFF1B5E20)))).padding(bottom = 32.dp, top = 48.dp, start = 24.dp, end = 24.dp)) {
            Column {
                Text("Madusari Foods", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Text("Dashboard", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    HeaderStat("Today's Logs", "${todaysLogs.size} Issues")
                }
            }
        }
        Column(modifier = Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp)) {
            Card(modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.DateRange, null, tint = PrimaryGreen); Spacer(modifier = Modifier.width(8.dp)); Text("Downtime Trend", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark) }
                    Text("Last 7 entries duration (mins)", fontSize = 12.sp, color = TextGray); Spacer(modifier = Modifier.height(16.dp))

                    val recentLogs = logs.takeLast(7).map { it.durationMin.toFloat() }
                    val chartModel = if(recentLogs.isNotEmpty()) entryModelOf(*recentLogs.toTypedArray()) else entryModelOf(0f,0f,0f,0f)

                    Chart(chart = lineChart(lines = listOf(LineChart.LineSpec(lineColor = PrimaryGreen.toArgb(), lineBackgroundShader = Brush.verticalGradient(colors = listOf(PrimaryGreen.copy(alpha = 0.4f), Color.Transparent)).toDynamicShader()))), model = chartModel, startAxis = rememberStartAxis(), bottomAxis = rememberBottomAxis(), modifier = Modifier.fillMaxWidth().height(180.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryCardModern("Total Downtime", downtimeString, Icons.Default.Warning, Color(0xFFD32F2F), Modifier.weight(1f))
                SummaryCardModern("System Uptime", uptimePercent, Icons.Default.CheckCircle, PrimaryGreen, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("SYSTEM HEALTH", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextGray, letterSpacing = 1.sp, modifier = Modifier.padding(start = 8.dp))
            Spacer(modifier = Modifier.height(12.dp))

            ServerStatusItem("Main API Server", "Online", PrimaryGreen, {})
            ServerStatusItem("Database Sync", "Online", PrimaryGreen, {})
            ServerStatusItem(
                name = if(isLoading) "Refreshing..." else "Data Refreshed",
                status = if(isLoading) "Loading" else "Just Now",
                color = if(isLoading) AccentOrange else PrimaryGreen,
                onClick = onRefresh,
                isRefreshBtn = true,
                rotation = rotateAnim
            )
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ==========================================
// SCREEN 2: ANALYTICS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDetailScreen(logs: List<LogHistoryItem>, onBack: () -> Unit) {
    val categoryStats = logs.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.durationMin } }
        .toList().sortedByDescending { it.second }

    val chartData = logs.takeLast(7).map { it.durationMin.toFloat() }
    val chartModel = if(chartData.isNotEmpty()) entryModelOf(*chartData.toTypedArray()) else entryModelOf(0f)

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Production Report", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundApp)) }, containerColor = BackgroundApp) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Daily Breakdown", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark); Spacer(modifier = Modifier.height(16.dp))
                    Chart(chart = columnChart(), model = chartModel, startAxis = rememberStartAxis(), bottomAxis = rememberBottomAxis(), modifier = Modifier.fillMaxWidth().height(200.dp))
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Top Category Issues", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(20.dp))

                    if (categoryStats.isEmpty()) {
                        Text("No data available yet.", color = TextGray, fontSize = 14.sp)
                    } else {
                        val totalAll = categoryStats.sumOf { it.second }.coerceAtLeast(1).toFloat()

                        categoryStats.forEach { (cat, duration) ->
                            val percent = (duration.toFloat() / totalAll)
                            val color = when(cat) {
                                "SEAL" -> Color(0xFFD32F2F)
                                "KLIP" -> AccentOrange
                                "IJP" -> PrimaryGreen
                                else -> Color.Gray
                            }
                            MachineProgressBar("$cat (${duration}m)", percent, "${(percent*100).toInt()}%", color)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// ==========================================
// SCREEN 3: HISTORY LOG (EDITED: ICON UNIFORMITY)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryLogScreen(logs: List<LogHistoryItem>, isLoading: Boolean, onRefresh: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Log History", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null, tint = PrimaryGreen) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundApp)
            )
        },
        containerColor = BackgroundApp
    ) { padding ->
        if (isLoading) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryGreen) } }
        else {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No logs found.", color = TextGray) }
            } else {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(logs.reversed()) { log -> LogItemCard(log) }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun LogItemCard(log: LogHistoryItem) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val iconBg = LightGreen
            val iconTint = PrimaryGreen

            Box(modifier = Modifier.size(48.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Settings, null, tint = iconTint)
            }

            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.issue, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Text("${log.machine} • ${log.operator}", fontSize = 12.sp, color = TextGray)
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(4.dp)) { Text(log.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(log.time, fontSize = 12.sp, color = TextGray)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(log.date.split(" ")[0], fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryGreen)
                Text("${log.durationMin} min", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// SCREEN 4: INPUT LOG (OPSI A & B TETAP ADA)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDowntimeLogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var operatorText by remember { mutableStateOf("") }
    var selectedMachine by remember { mutableStateOf("") }
    var textIJP by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedIssue by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitStatus by remember { mutableStateOf("") }
    var expandedOperator by remember { mutableStateOf(false) }
    var expandedMachine by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedIssue by remember { mutableStateOf(false) }

    val filteredOperators = operatorList.filter { it.contains(operatorText, ignoreCase = true) }
    val currentIssues = downtimeData[selectedCategory] ?: emptyList()
    val scope = rememberCoroutineScope()

    fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        TimePickerDialog(context, { _, h, m -> onTimeSelected(String.format("%02d:%02d", h, m)) }, hour, minute, true).show()
    }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Input Downtime Log", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, null) } }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundApp)) }, containerColor = BackgroundApp) { padding ->
        if (isSubmitting) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryGreen) } }
        else {
            Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                if (submitStatus.isNotEmpty()) { Card(colors = CardDefaults.cardColors(containerColor = if (submitStatus.contains("Sukses")) LightGreen else Color(0xFFFFEBEE))) { Text(submitStatus, color = if (submitStatus.contains("Sukses")) PrimaryGreen else Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }

                InputSectionCard("Info Dasar", Icons.Default.Person) {
                    Text("Operator", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    ExposedDropdownMenuBox(expanded = expandedOperator, onExpandedChange = { expandedOperator = !expandedOperator }) {
                        OutlinedTextField(value = operatorText, onValueChange = { operatorText = it; expandedOperator = true }, placeholder = { Text("Cari nama...") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOperator) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                        if (filteredOperators.isNotEmpty()) { ExposedDropdownMenu(expanded = expandedOperator, onDismissRequest = { expandedOperator = false }, modifier = Modifier.heightIn(max = 200.dp)) { filteredOperators.forEach { name -> DropdownMenuItem(text = { Text(name) }, onClick = { operatorText = name; expandedOperator = false }) } } }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Mesin", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    ExposedDropdownMenuBox(expanded = expandedMachine, onExpandedChange = { expandedMachine = !expandedMachine }) {
                        OutlinedTextField(value = selectedMachine, onValueChange = {}, readOnly = true, placeholder = { Text("Pilih Mesin") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMachine) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                        ExposedDropdownMenu(expanded = expandedMachine, onDismissRequest = { expandedMachine = false }) { machineList.forEach { m -> DropdownMenuItem(text = { Text(m) }, onClick = { selectedMachine = m; expandedMachine = false }) } }
                    }
                }

                InputSectionCard("Detail Masalah (Pilih Salah Satu)", Icons.Default.Warning) {
                    // OPSI A: INPUT MANUAL IJP
                    val isIjpActive = textIJP.isNotEmpty(); val ijpColor = if(isIjpActive) Color(0xFFD32F2F) else TextGray
                    Text("OPSI A: Input Manual Kode IJP", fontWeight = FontWeight.Bold, color = ijpColor)
                    OutlinedTextField(value = textIJP, onValueChange = { textIJP = it; if(it.isNotEmpty()) { selectedCategory=""; selectedIssue="" } }, placeholder = { Text("Ketik masalah/kode IJP di sini...") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD32F2F), unfocusedBorderColor = if(isIjpActive) Color(0xFFD32F2F) else Color.LightGray, focusedContainerColor = if(isIjpActive) Color(0xFFFFEBEE) else Color.Transparent))

                    Divider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp, color = Color.LightGray)

                    // OPSI B: PILIH KATEGORI (IJP JUGA ADA DI SINI)
                    val isCatActive = selectedCategory.isNotEmpty(); val catColor = if(isCatActive) PrimaryGreen else TextGray
                    Text("OPSI B: Pilih Kategori Masalah", fontWeight = FontWeight.Bold, color = catColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(expanded = expandedCategory, onExpandedChange = { expandedCategory = !expandedCategory }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = selectedCategory, onValueChange = {}, readOnly = true, placeholder = { Text("Kategori") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) }, modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGreen, unfocusedBorderColor = if(isCatActive) PrimaryGreen else Color.LightGray, focusedContainerColor = if(isCatActive) LightGreen else Color.Transparent))
                            ExposedDropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) { downtimeData.keys.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { selectedCategory = c; selectedIssue = ""; expandedCategory = false; textIJP = "" }) } }
                        }
                        ExposedDropdownMenuBox(expanded = expandedIssue, onExpandedChange = { expandedIssue = !expandedIssue }, modifier = Modifier.weight(1.5f)) {
                            OutlinedTextField(value = selectedIssue, onValueChange = {}, readOnly = true, placeholder = { Text("Masalah") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIssue) }, modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(12.dp), enabled = selectedCategory.isNotEmpty(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGreen, unfocusedBorderColor = if(isCatActive) PrimaryGreen else Color.LightGray, focusedContainerColor = if(isCatActive) LightGreen else Color.Transparent))
                            ExposedDropdownMenu(expanded = expandedIssue, onDismissRequest = { expandedIssue = false }, modifier = Modifier.heightIn(max = 250.dp)) { currentIssues.forEach { i -> DropdownMenuItem(text = { Text(i) }, onClick = { selectedIssue = i; expandedIssue = false }) } }
                        }
                    }
                }

                InputSectionCard("Waktu & Catatan", Icons.Default.Info) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = startTime, onValueChange = {}, readOnly = true, placeholder = { Text("Jam Mulai") }, trailingIcon = { Icon(Icons.Default.DateRange, null) }, modifier = Modifier.weight(1f).clickable { showTimePicker { startTime = it } }, enabled = false, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(disabledTextColor = TextDark, disabledBorderColor = Color.LightGray))
                        OutlinedTextField(value = endTime, onValueChange = {}, readOnly = true, placeholder = { Text("Jam Selesai") }, trailingIcon = { Icon(Icons.Default.DateRange, null) }, modifier = Modifier.weight(1f).clickable { showTimePicker { endTime = it } }, enabled = false, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(disabledTextColor = TextDark, disabledBorderColor = Color.LightGray))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, placeholder = { Text("Catatan Tambahan (Opsional)") }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(12.dp))
                }

                Button(
                    onClick = {
                        isSubmitting = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val finalCategory = if (textIJP.isNotEmpty()) "IJP" else selectedCategory
                                val finalIssue = if (textIJP.isNotEmpty()) textIJP else selectedIssue
                                val url = URL(GOOGLE_SCRIPT_URL)
                                val conn = url.openConnection() as HttpURLConnection
                                conn.requestMethod = "POST"; conn.doOutput = true
                                val postData = "operator=${java.net.URLEncoder.encode(operatorText,"UTF-8")}&machine=${java.net.URLEncoder.encode(selectedMachine,"UTF-8")}&category=${java.net.URLEncoder.encode(finalCategory,"UTF-8")}&issue=${java.net.URLEncoder.encode(finalIssue,"UTF-8")}&startTime=${java.net.URLEncoder.encode(startTime,"UTF-8")}&endTime=${java.net.URLEncoder.encode(endTime,"UTF-8")}&notes=${java.net.URLEncoder.encode(notes,"UTF-8")}"
                                val wr = java.io.OutputStreamWriter(conn.outputStream); wr.write(postData); wr.flush(); wr.close()
                                if(conn.responseCode==200) { isSubmitting=false; submitStatus="Sukses Disimpan!"; delay(1500); withContext(Dispatchers.Main){onBack()} } else { isSubmitting=false; submitStatus="Error ${conn.responseCode}" }
                            } catch(e:Exception) { isSubmitting=false; submitStatus="Error: ${e.message}" }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    enabled = operatorText.isNotEmpty() && selectedMachine.isNotEmpty() && (textIJP.isNotEmpty() || (selectedCategory.isNotEmpty() && selectedIssue.isNotEmpty())) && startTime.isNotEmpty() && endTime.isNotEmpty()
                ) { Text("SIMPAN LOG", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ==========================================
// HELPERS & VISUALS
// ==========================================
@Composable fun InputSectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) { Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), elevation = CardDefaults.cardElevation(2.dp)) { Column(modifier = Modifier.padding(20.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(32.dp).background(LightGreen, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp)) }; Spacer(modifier = Modifier.width(12.dp)); Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark) }; Spacer(modifier = Modifier.height(16.dp)); content() } } }
@Composable fun SummaryCardModern(title: String, value: String, icon: ImageVector, iconColor: Color, modifier: Modifier) { Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardBg), elevation = CardDefaults.cardElevation(2.dp)) { Column(modifier = Modifier.padding(20.dp)) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(32.dp)); Spacer(modifier = Modifier.height(16.dp)); Text(title, color = TextGray, fontSize = 12.sp); Text(value, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextDark) } } }
@Composable fun HeaderStat(label: String, value: String) { Column { Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp); Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) } }

@Composable fun ServerStatusItem(name: String, status: String, color: Color, onClick: () -> Unit, isRefreshBtn: Boolean = false, rotation: Float = 0f) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(CardBg).clickable(enabled = isRefreshBtn, onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (isRefreshBtn) Icon(Icons.Default.Refresh, null, tint = color, modifier = Modifier.size(18.dp).rotate(rotation)) else Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(12.dp)); Text(name, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f)); Text(status, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable fun MachineProgressBar(name: String, progress: Float, valueText: String, color: Color) { Column(modifier = Modifier.padding(vertical = 8.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, fontSize = 14.sp); Text(valueText, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp) }; Spacer(modifier = Modifier.height(6.dp)); LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = color, trackColor = Color(0xFFF1F3F4)) } }

@Composable
fun FancySplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimaryGreen, Color(0xFF1B5E20)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo_hatchery),
                contentDescription = "Logo Aplikasi",
                modifier = Modifier.size(150.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Madusari Foods", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, letterSpacing = 2.sp)
            Text("DIGITAL DOWNTIME TRACKER", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, letterSpacing = 1.sp)
        }
    }
}


