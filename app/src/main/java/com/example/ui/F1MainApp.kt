package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ChannelMessageEntity
import com.example.data.database.NewsEntity
import com.example.data.model.AlertType
import com.example.data.model.Circuit
import com.example.data.model.Constructor
import com.example.data.model.Driver
import com.example.data.model.NotificationAlert
import com.example.data.model.TelemetryData
import com.example.ui.theme.F1Blue
import com.example.ui.theme.F1Green
import com.example.ui.theme.F1Purple
import com.example.ui.theme.F1Red
import com.example.ui.theme.F1Yellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun F1MainApp(viewModel: F1ViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    val activeNotification by viewModel.activeNotification.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.darkThemeMode.collectAsStateWithLifecycle()

    // Listen to social share events
    LaunchedEffect(Unit) {
        viewModel.socialShareEvent.collectLatest { text ->
            Toast.makeText(context, "Shared to F1 Circle: \"$text\"", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(F1Red, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "APEX F1 HUB",
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setDarkThemeMode(!isDarkTheme) },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Speed, contentDescription = "Telemetry") },
                    label = { Text("Live", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = F1Red,
                        selectedTextColor = F1Red,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = "Standings") },
                    label = { Text("Standings", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = F1Red,
                        selectedTextColor = F1Red,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Fantasy") },
                    label = { Text("Fantasy", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = F1Red,
                        selectedTextColor = F1Red,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Forum, contentDescription = "Voice & Chat") },
                    label = { Text("Channels", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = F1Red,
                        selectedTextColor = F1Red,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Map, contentDescription = "News & Circuits") },
                    label = { Text("Info", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = F1Red,
                        selectedTextColor = F1Red,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Content Switching
            when (selectedTab) {
                0 -> TelemetryScreen(viewModel)
                1 -> StandingsScreen(viewModel)
                2 -> FantasyScreen(viewModel)
                3 -> VoiceChatScreen(viewModel)
                4 -> NewsCircuitsScreen(viewModel)
            }

            // Real-Time In-App Slide-Down Push Notification Overlay
            AnimatedVisibility(
                visible = activeNotification != null,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .zIndex(100f)
            ) {
                activeNotification?.let { alert ->
                    PushNotificationBanner(alert = alert) {
                        viewModel.shareRaceHighlight("ALERT: ${alert.title} - ${alert.message} #F1Live")
                    }
                }
            }
        }
    }
}

@Composable
fun PushNotificationBanner(alert: NotificationAlert, onShareClick: () -> Unit) {
    val bannerBg = when (alert.type) {
        AlertType.REDFLAG -> Color(0xFF3A0B0F)
        AlertType.YELLOWFLAG -> Color(0xFF332908)
        AlertType.GREENFLAG -> Color(0xFF0F3A22)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val accentColor = when (alert.type) {
        AlertType.REDFLAG -> F1Red
        AlertType.YELLOWFLAG -> F1Yellow
        AlertType.GREENFLAG -> F1Green
        AlertType.DRS -> F1Blue
        AlertType.OVERTAKE -> F1Purple
        else -> F1Red
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .shadow(12.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = bannerBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (alert.type) {
                        AlertType.REDFLAG -> Icons.Default.Warning
                        AlertType.YELLOWFLAG -> Icons.Default.Warning
                        AlertType.GREENFLAG -> Icons.Default.CheckCircle
                        AlertType.DRS -> Icons.Default.SettingsInputAntenna
                        AlertType.OVERTAKE -> Icons.Default.TrendingUp
                        AlertType.LAPUPDATE -> Icons.Default.Timer
                        else -> Icons.Default.NotificationsActive
                    },
                    contentDescription = null,
                    tint = accentColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = alert.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onShareClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share highlight",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// 1. LIVE TELEMETRY SCREEN (WITH REAL-TIME TRACK CANVAS GRAPHICS)
@Composable
fun TelemetryScreen(viewModel: F1ViewModel) {
    val drivers = viewModel.drivers
    val selectedDriver by viewModel.selectedTelemetryDriver.collectAsStateWithLifecycle()
    val telemetryState by viewModel.telemetryState.collectAsStateWithLifecycle()
    val circuits = viewModel.circuits
    val selectedCircuit by viewModel.selectedCircuit.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Driver Selector Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SELECT DRIVER CAR TELEMETRY",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontFamily = FontFamily.Monospace
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(drivers) { driver ->
                        val isSelected = driver.id == selectedDriver.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) driver.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) driver.color else MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.2f
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setTelemetryDriver(driver) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("driver_select_${driver.code}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(driver.color, CircleShape)
                                )
                                Text(
                                    text = "${driver.number} ${driver.code}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Telemetry Dial & Speed Overlay
        item {
            telemetryState?.let { telemetry ->
                TelemetryHUDPanel(telemetry = telemetry, driverColor = selectedDriver.color)
            } ?: Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = F1Red)
            }
        }

        // Real-Time Animated Track Map Visualizer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LIVE TRACK TRACKER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${selectedCircuit.name} - ${selectedCircuit.country}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Circuit quick toggle dropdown
                        IconButton(onClick = {
                            val nextCircuit = circuits[(circuits.indexOf(selectedCircuit) + 1) % circuits.size]
                            viewModel.selectCircuit(nextCircuit)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Loop,
                                contentDescription = "Next circuit",
                                tint = F1Red
                            )
                        }
                    }

                    // Live Track Canvas
                    telemetryState?.let { telemetry ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val trackPath = Path()
                                val points = selectedCircuit.trackPoints

                                if (points.isNotEmpty()) {
                                    val startX = points[0].first * size.width
                                    val startY = points[0].second * size.height
                                    trackPath.moveTo(startX, startY)

                                    for (i in 1 until points.size) {
                                        val x = points[i].first * size.width
                                        val y = points[i].second * size.height
                                        trackPath.lineTo(x, y)
                                    }
                                    trackPath.close()

                                    // Draw background track layout
                                    drawPath(
                                        path = trackPath,
                                        color = Color.DarkGray,
                                        style = Stroke(
                                            width = 6.dp.toPx(),
                                            cap = StrokeCap.Round,
                                            pathEffect = PathEffect.cornerPathEffect(40f)
                                        )
                                    )

                                    // Draw sector divisions lines (dashed)
                                    drawPath(
                                        path = trackPath,
                                        color = Color.White.copy(alpha = 0.2f),
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                                        )
                                    )

                                    // Calculate live position dot coordinates on the curve (interpolation)
                                    val pointIndex = (telemetry.lapProgress * points.size).toInt() % points.size
                                    val nextIndex = (pointIndex + 1) % points.size
                                    val subProgress = (telemetry.lapProgress * points.size) % 1.0f

                                    val currentPt = points[pointIndex]
                                    val nextPt = points[nextIndex]

                                    val liveX = (currentPt.first + (nextPt.first - currentPt.first) * subProgress) * size.width
                                    val liveY = (currentPt.second + (nextPt.second - currentPt.second) * subProgress) * size.height

                                    // Draw live racing car point
                                    drawCircle(
                                        color = selectedDriver.color,
                                        radius = 8.dp.toPx(),
                                        center = Offset(liveX, liveY)
                                    )
                                    // Pulse effect
                                    drawCircle(
                                        color = selectedDriver.color.copy(alpha = 0.3f),
                                        radius = 16.dp.toPx() + (sin(System.currentTimeMillis() / 200.0) * 4.dp.toPx()).toFloat(),
                                        center = Offset(liveX, liveY)
                                    )
                                }
                            }

                            // Dynamic sector tags overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SECTOR ${telemetry.sector}",
                                    color = when (telemetry.sector) {
                                        1 -> F1Green
                                        2 -> F1Yellow
                                        else -> F1Purple
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Sector Times & Micro Stats Panel
        item {
            telemetryState?.let { telemetry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectorBox(
                        title = "SECTOR 1",
                        time = telemetry.sector1Time,
                        color = F1Green,
                        modifier = Modifier.weight(1f)
                    )
                    SectorBox(
                        title = "SECTOR 2",
                        time = telemetry.sector2Time,
                        color = F1Yellow,
                        modifier = Modifier.weight(1f)
                    )
                    SectorBox(
                        title = "SECTOR 3",
                        time = telemetry.sector3Time,
                        color = F1Purple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryHUDPanel(telemetry: TelemetryData, driverColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Speed, Gear & RPM Dashboard Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gear Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "GEAR",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .border(2.dp, driverColor, RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = telemetry.gear.toString(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = driverColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Digital Speedometer
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SPEED",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = telemetry.speed.toString(),
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "KM/H",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // DRS & Tyre Compound Status
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // DRS State Indicator
                    Box(
                        modifier = Modifier
                            .background(
                                if (telemetry.drs) F1Green else Color.Gray.copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DRS " + (if (telemetry.drs) "ON" else "OFF"),
                            color = if (telemetry.drs) Color.Black else MaterialTheme.colorScheme.outline,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Tyre Overlay indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .border(1.5.dp, Color.White, CircleShape)
                                .background(
                                    when (telemetry.tyreCompound) {
                                        "Soft" -> Color.Red
                                        "Medium" -> Color(0xFFFFCC00)
                                        "Hard" -> Color.White
                                        else -> F1Blue
                                    },
                                    CircleShape
                                )
                        )
                        Text(
                            text = "${telemetry.tyreCompound} (${telemetry.tyreWear.toInt()}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // RPM LED Bar Gauge
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RPM: ${telemetry.rpm}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "LAP ${telemetry.currentLap}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                }

                // LED lights bar representing high engine RPM
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    val ledCount = 20
                    val activeLeds = (telemetry.rpm.toFloat() / 13500.0f * ledCount).toInt().coerceIn(0, ledCount)
                    for (i in 0 until ledCount) {
                        val color = when {
                            i < 10 -> F1Green
                            i < 16 -> F1Yellow
                            else -> F1Red
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    if (i < activeLeds) color else Color.Transparent
                                )
                                .border(0.5.dp, Color.Black.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            // Throttle & Brake Pedals Overlay Charts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Throttle Bar (Green)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "THROTTLE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.outline)
                        Text(text = "${(telemetry.throttle * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = F1Green)
                    }
                    LinearProgressIndicator(
                        progress = { telemetry.throttle },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = F1Green,
                        trackColor = Color.DarkGray.copy(alpha = 0.2f),
                    )
                }

                // Brake Bar (Red)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "BRAKE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.outline)
                        Text(text = "${(telemetry.brake * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = F1Red)
                    }
                    LinearProgressIndicator(
                        progress = { telemetry.brake },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = F1Red,
                        trackColor = Color.DarkGray.copy(alpha = 0.2f),
                    )
                }
            }

            // G-Forces Display (X, Y)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "G-FORCE SENSORS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "LAT: ${String.format("%.1f", telemetry.gForceX)}G",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (Math.abs(telemetry.gForceX) > 2.5) F1Yellow else MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "LNG: ${String.format("%.1f", telemetry.gForceY)}G",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (Math.abs(telemetry.gForceY) > 3.0) F1Red else MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun SectorBox(title: String, time: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// 2. STANDINGS SCREEN (DRIVERS & CONSTRUCTORS)
@Composable
fun StandingsScreen(viewModel: F1ViewModel) {
    var viewMode by remember { mutableStateOf(0) } // 0: Drivers, 1: Constructors
    val drivers = viewModel.drivers
    val constructors = viewModel.constructors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        // Tab Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (viewMode == 0) F1Red else Color.Transparent)
                    .clickable { viewMode = 0 }
                    .padding(vertical = 8.dp)
                    .testTag("drivers_standings_tab"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DRIVERS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (viewMode == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (viewMode == 1) F1Red else Color.Transparent)
                    .clickable { viewMode = 1 }
                    .padding(vertical = 8.dp)
                    .testTag("teams_standings_tab"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TEAMS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (viewMode == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Titles row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (viewMode == 0) "DRIVER" else "CONSTRUCTOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "POINTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
                fontFamily = FontFamily.Monospace
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (viewMode == 0) {
                items(drivers) { driver ->
                    DriverStandingRow(driver = driver)
                }
            } else {
                items(constructors) { team ->
                    ConstructorStandingRow(team = team)
                }
            }
        }
    }
}

@Composable
fun DriverStandingRow(driver: Driver) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Position number
                Text(
                    text = driver.position.toString(),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )

                // Team color accent pillar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .background(driver.color, RoundedCornerShape(2.dp))
                )

                // Driver details
                Column {
                    Text(
                        text = driver.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${driver.team} | #${driver.number}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Wins tag if any
                if (driver.wins > 0) {
                    Box(
                        modifier = Modifier
                            .background(F1Red.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${driver.wins} WINS",
                            color = F1Red,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Text(
                    text = driver.points.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ConstructorStandingRow(team: Constructor) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Position number
                Text(
                    text = team.position.toString(),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )

                // Team color accent pillar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .background(team.color, RoundedCornerShape(2.dp))
                )

                // Team details
                Column {
                    Text(
                        text = team.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = team.baseCountry,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Text(
                text = team.points.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// 3. F1 FANTASY LEAGUE SCREEN
@Composable
fun FantasyScreen(viewModel: F1ViewModel) {
    val activeTeam by viewModel.fantasyTeam.collectAsStateWithLifecycle()
    val drivers = viewModel.drivers
    val constructors = viewModel.constructors

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Fantasy Intro Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = F1Red.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, F1Red.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = F1Red)
                        Text(
                            text = "F1 GLOBAL FANTASY CHAMPIONSHIP",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = F1Red,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Formulate your dream racing roster of 2 Drivers and 1 Constructor under a strictly monitored $100.0M budget constraints.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Active Saved Team overview
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "YOUR ACTIVE TEAM STATUS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontFamily = FontFamily.Monospace
                )

                if (activeTeam != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeTeam!!.teamName,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = F1Red
                                )
                                Box(
                                    modifier = Modifier
                                        .background(F1Green.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${activeTeam!!.totalPoints} PTS",
                                        color = F1Green,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val d1 = drivers.find { it.id == activeTeam!!.driver1Id }
                                val d2 = drivers.find { it.id == activeTeam!!.driver2Id }
                                val constr = constructors.find { it.id == activeTeam!!.constructorId }

                                d1?.let {
                                    SelectedTeamMiniCard(
                                        name = it.code,
                                        label = "DRIVER 1",
                                        color = it.color,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                d2?.let {
                                    SelectedTeamMiniCard(
                                        name = it.code,
                                        label = "DRIVER 2",
                                        color = it.color,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                constr?.let {
                                    SelectedTeamMiniCard(
                                        name = it.name.take(10),
                                        label = "CONSTRUCTOR",
                                        color = it.color,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.clearFantasyTeam() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_fantasy_button")
                            ) {
                                Text(
                                    text = "DISSOLVE TEAM & CONSTRUCT NEW",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Draft Creator View
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "ROSTER CREATION DESK",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.outline,
                                fontFamily = FontFamily.Monospace
                            )

                            // Team name input
                            OutlinedTextField(
                                value = viewModel.draftTeamName,
                                onValueChange = { viewModel.draftTeamName = it },
                                label = { Text("Assign Team Name") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fantasy_team_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = F1Red,
                                    focusedLabelColor = F1Red
                                )
                            )

                            // Remaining Budget Meter
                            val budgetProgress = (viewModel.draftBudgetUsed / 100.0).toFloat()
                            val progressColor = if (budgetProgress > 1.0f) F1Red else F1Green
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "BUDGET: $${String.format("%.1f", viewModel.draftBudgetUsed)}M / $100.0M",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "REMAINING: $${String.format("%.1f", viewModel.draftBudgetRemaining)}M",
                                        fontSize = 12.sp,
                                        color = progressColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { budgetProgress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = progressColor,
                                    trackColor = MaterialTheme.colorScheme.background
                                )
                                if (viewModel.draftBudgetRemaining < 0) {
                                    Text(
                                        text = "⚠️ OVER BUDGET BY $${String.format("%.1f", -viewModel.draftBudgetRemaining)}M",
                                        color = F1Red,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Selection Summary Blocks
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val activeD1 = drivers.find { it.id == viewModel.draftDriver1Id }
                                val activeD2 = drivers.find { it.id == viewModel.draftDriver2Id }
                                val activeC = constructors.find { it.id == viewModel.draftConstructorId }

                                DraftBox(label = "DRIVER 1", item = activeD1?.code, modifier = Modifier.weight(1f)) { viewModel.draftDriver1Id = null }
                                DraftBox(label = "DRIVER 2", item = activeD2?.code, modifier = Modifier.weight(1f)) { viewModel.draftDriver2Id = null }
                                DraftBox(label = "CONSTRUCTOR", item = activeC?.name?.take(10), modifier = Modifier.weight(1f)) { viewModel.draftConstructorId = null }
                            }

                            // Save locked roster button
                            val isRosterValid = viewModel.draftDriver1Id != null &&
                                    viewModel.draftDriver2Id != null &&
                                    viewModel.draftConstructorId != null &&
                                    viewModel.draftBudgetRemaining >= 0.0

                            Button(
                                onClick = { viewModel.saveFantasyTeam() },
                                enabled = isRosterValid,
                                colors = ButtonDefaults.buttonColors(containerColor = F1Red),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_fantasy_team_button")
                            ) {
                                Text(
                                    text = "LOCK ROSTER & JOIN LEAGUE",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Available drivers grid selection (when drafting)
        if (activeTeam == null) {
            item {
                Text(
                    text = "SELECT DRIVERS (PICK EXACTLY TWO)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontFamily = FontFamily.Monospace
                )
            }

            items(drivers) { driver ->
                val isSelected = driver.id == viewModel.draftDriver1Id || driver.id == viewModel.draftDriver2Id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isSelected) driver.color else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.toggleDraftDriver(driver.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) driver.color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(driver.color, CircleShape)
                            )
                            Column {
                                Text(
                                    text = driver.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Points: ${driver.points} | Age: ${driver.age}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Text(
                            text = "$${driver.cost}M",
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) driver.color else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Text(
                    text = "SELECT CONSTRUCTOR TEAM (PICK ONE)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(constructors) { team ->
                val isSelected = team.id == viewModel.draftConstructorId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isSelected) team.color else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.selectDraftConstructor(team.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) team.color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(team.color, CircleShape)
                            )
                            Column {
                                Text(
                                    text = team.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Points: ${team.points} | Base: ${team.baseCountry}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Text(
                            text = "$${team.cost}M",
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) team.color else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Global Leaderboard Mock Panel
        item {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "GLOBAL LEAGUE LEADERBOARD",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontFamily = FontFamily.Monospace
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        LeaderboardRow(rank = "1", name = "SatoSpeed_GP", points = "1,412", isUser = false)
                        LeaderboardRow(rank = "2", name = "Tifosi_RedPower", points = "1,390", isUser = false)
                        LeaderboardRow(rank = "3", name = "SilverArrowBox", points = "1,384", isUser = false)
                        if (activeTeam != null) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                            LeaderboardRow(
                                rank = "1,842",
                                name = activeTeam!!.teamName,
                                points = activeTeam!!.totalPoints.toString(),
                                isUser = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedTeamMiniCard(name: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .border(0.5.dp, color, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun DraftBox(label: String, item: String?, modifier: Modifier = Modifier, onRemove: () -> Unit) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .border(1.dp, if (item != null) F1Red else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (item != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Remove",
                        tint = F1Red,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onRemove() }
                    )
                }
            } else {
                Text(
                    text = "EMPTY",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun LeaderboardRow(rank: String, name: String, points: String, isUser: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "#$rank",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isUser) F1Red else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = name,
                fontWeight = if (isUser) FontWeight.Black else FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isUser) F1Red else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "$points PTS",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = if (isUser) F1Red else MaterialTheme.colorScheme.onSurface
        )
    }
}

// 4. FAN SOCIAL CHANNELS & VOICE CONNECTIVITY SCREEN
@Composable
fun VoiceChatScreen(viewModel: F1ViewModel) {
    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isVoiceConnected by viewModel.voiceConnected.collectAsStateWithLifecycle()
    val speakers by viewModel.activeVoiceSpeakers.collectAsStateWithLifecycle()

    val availableChannels = listOf("General Fan Voice", "Live Race Chat", "Monaco GP Chat")
    var chatText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll chat to latest
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Channel Selector Dropdown row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            availableChannels.forEach { channelName ->
                val isSel = channelName == selectedChannel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSel) F1Red.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            1.dp,
                            if (isSel) F1Red else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.selectChannel(channelName) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = channelName.split(" ").firstOrNull() ?: "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isSel) F1Red else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Simulated Voice Connectivity Box (AGGRESSIVE DESIGN)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isVoiceConnected) Color(0xFF0D251D) else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                1.dp,
                if (isVoiceConnected) F1Green.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isVoiceConnected) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = null,
                            tint = if (isVoiceConnected) F1Green else MaterialTheme.colorScheme.outline
                        )
                        Column {
                            Text(
                                text = "FAN VOICE SPACE: ${selectedChannel.uppercase()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isVoiceConnected) F1Green else MaterialTheme.colorScheme.outline,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isVoiceConnected) "Live Broadcast Audio Connected" else "Audio Room Offline",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Connection toggle button
                    Button(
                        onClick = { viewModel.toggleVoiceChannel() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVoiceConnected) F1Red else F1Green
                        ),
                        modifier = Modifier.testTag("voice_connect_button")
                    ) {
                        Text(
                            text = if (isVoiceConnected) "LEAVE" else "JOIN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Waveform animation visualizer & Speaker listing if connected
                if (isVoiceConnected) {
                    Divider(color = F1Green.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speaker list
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ACTIVE SPEAKERS:",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (speakers.isEmpty()) {
                                Text(text = "Listening silently...", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    speakers.forEach { speaker ->
                                        Box(
                                            modifier = Modifier
                                                .background(F1Green.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .border(0.5.dp, F1Green, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = speaker, fontSize = 10.sp, color = F1Green, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Cute bouncing Canvas bars representing wave forms
                        Box(modifier = Modifier.width(60.dp).height(24.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barCount = 6
                                val spacing = 4.dp.toPx()
                                val barWidth = (size.width - (spacing * (barCount - 1))) / barCount
                                for (i in 0 until barCount) {
                                    val waveHt = 5.dp.toPx() + (Math.abs(sin(System.currentTimeMillis() / 150.0 + i)) * 14.dp.toPx()).toFloat()
                                    drawRect(
                                        color = F1Green,
                                        topLeft = Offset(i * (barWidth + spacing), size.height - waveHt),
                                        size = androidx.compose.ui.geometry.Size(barWidth, waveHt)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat Text area heading & Quick social highlights sharing button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PADDOCK LIVE DISCUSSION",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                fontFamily = FontFamily.Monospace
            )

            // Social share highlight quick-button
            Box(
                modifier = Modifier
                    .background(F1Red.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .clickable { viewModel.shareRaceHighlight("⚡ Race highlight: Norris takes lead in Monza! Dynamic Telemetry curves are stunning! #ApexF1") }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = F1Red, modifier = Modifier.size(11.dp))
                    Text(text = "SHARE HIGHLIGHT", fontSize = 9.sp, color = F1Red, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat bubble feed list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No discussion yet. Be the first to start the race analysis!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(messages) { message ->
                    ChatBubble(message = message)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat input bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = chatText,
                onValueChange = { chatText = it },
                placeholder = { Text("Comment on telemetry and strategy...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = F1Red,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )

            IconButton(
                onClick = {
                    if (chatText.isNotBlank()) {
                        viewModel.postChatMessage(chatText)
                        chatText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(F1Red, RoundedCornerShape(8.dp))
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChannelMessageEntity) {
    val bubbleColor = try {
        Color(android.graphics.Color.parseColor(message.avatarColorHex))
    } catch (e: Exception) {
        F1Red
    }

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(bubbleColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.username.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = message.username,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp)),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontFamily = FontFamily.Monospace
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = message.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

// 5. NEWS FEED & CIRCUITS LIST SCREEN
@Composable
fun NewsCircuitsScreen(viewModel: F1ViewModel) {
    val newsArticles by viewModel.news.collectAsStateWithLifecycle()
    val circuits = viewModel.circuits
    var infoTab by remember { mutableStateOf(0) } // 0: News, 1: Circuit Maps

    // Selected items for modal detail view
    var selectedNewsArticle by remember { mutableStateOf<NewsEntity?>(null) }
    var selectedCircuitItem by remember { mutableStateOf<Circuit?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Info toggle row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (infoTab == 0) F1Red else Color.Transparent)
                    .clickable { infoTab = 0 }
                    .padding(vertical = 8.dp)
                    .testTag("news_tab"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NEWS FEED",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (infoTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (infoTab == 1) F1Red else Color.Transparent)
                    .clickable { infoTab = 1 }
                    .padding(vertical = 8.dp)
                    .testTag("circuits_tab"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CIRCUITS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (infoTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List Display
        if (infoTab == 0) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(newsArticles) { article ->
                    NewsArticleCard(
                        article = article,
                        onCardClick = { selectedNewsArticle = article },
                        onBookmarkToggle = { viewModel.toggleNewsBookmark(article.id, article.isBookmarked) },
                        onLikeClick = { viewModel.likeNews(article.id) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(circuits) { circuit ->
                    CircuitOverviewCard(
                        circuit = circuit,
                        onCardClick = { selectedCircuitItem = circuit }
                    )
                }
            }
        }
    }

    // Modal News details dialog
    selectedNewsArticle?.let { article ->
        Dialog(onDismissRequest = { selectedNewsArticle = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(F1Red.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = article.category,
                                color = F1Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        IconButton(onClick = { selectedNewsArticle = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close details")
                        }
                    }

                    Text(
                        text = article.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = article.publishedAt.take(10),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontFamily = FontFamily.Monospace
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Text(
                        text = article.content,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { selectedNewsArticle = null },
                        colors = ButtonDefaults.buttonColors(containerColor = F1Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "BACK TO NEWS FEED", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal Circuit details dialog
    selectedCircuitItem?.let { circuit ->
        Dialog(onDismissRequest = { selectedCircuitItem = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = circuit.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = F1Red
                        )

                        IconButton(onClick = { selectedCircuitItem = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close details")
                        }
                    }

                    Text(
                        text = "LOCATION: " + circuit.country.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        fontFamily = FontFamily.Monospace
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Detail items
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "LAP LENGTH", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            Text(text = circuit.length, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "RACE LAPS", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            Text(text = "${circuit.laps} laps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "TURNS COUNT", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            Text(text = "${circuit.turns} turns", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Column {
                        Text(text = "LAP RECORD", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                        Text(text = circuit.lapRecord, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = F1Purple, fontFamily = FontFamily.Monospace)
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Embedded Live Interactive Track Map
                    InteractiveCircuitMap(
                        circuit = circuit,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Text(
                        text = circuit.description,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { selectedCircuitItem = null },
                        colors = ButtonDefaults.buttonColors(containerColor = F1Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "CLOSE DETAILS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NewsArticleCard(
    article: NewsEntity,
    onCardClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onLikeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(F1Red.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = article.category,
                        color = F1Red,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = article.publishedAt.take(10),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = article.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = article.summary,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Likes action
                    Row(
                        modifier = Modifier.clickable { onLikeClick() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Likes",
                            tint = F1Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = article.likesCount.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Bookmark toggle
                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (article.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (article.isBookmarked) F1Red else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CircuitOverviewCard(circuit: Circuit, onCardClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = circuit.country.uppercase(),
                    color = F1Red,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = circuit.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${circuit.length} | ${circuit.laps} Laps | ${circuit.turns} Turns",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
