package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Circuit
import com.example.data.model.DrsZone
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun InteractiveCircuitMap(
    circuit: Circuit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Map viewport state
    var zoomScale by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showGrid by remember { mutableStateOf(true) }

    // Selected DRS zone
    var selectedDrsZone by remember(circuit.id) {
        mutableStateOf(circuit.drsZonesList.firstOrNull())
    }

    // Telemetry simulation state
    var simulatingDrs by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(245f) }
    var wingOpenPercent by remember { mutableStateOf(0f) }

    // Pulsing animation for active DRS zone highlight
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val dRSPulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )

    // Simulate DRS speed acceleration when activated
    LaunchedEffect(simulatingDrs) {
        if (simulatingDrs) {
            // Speed ramps up from 245 to 332
            val speedTarget = 332f
            val wingTarget = 100f
            
            // Animate wing opening
            animate(
                initialValue = 0f,
                targetValue = wingTarget,
                animationSpec = tween(500, easing = EaseOutQuad)
            ) { value, _ -> wingOpenPercent = value }

            // Ramping up speed
            animate(
                initialValue = 245f,
                targetValue = speedTarget,
                animationSpec = tween(2500, easing = EaseOutCubic)
            ) { value, _ -> currentSpeed = value }

            delay(1500) // Sustain high speed

            // Ramping speed back down to normal
            animate(
                initialValue = speedTarget,
                targetValue = 245f,
                animationSpec = tween(1200, easing = EaseInOutQuad)
            ) { value, _ -> currentSpeed = value }

            // Animate wing closing
            animate(
                initialValue = wingTarget,
                targetValue = 0f,
                animationSpec = tween(400, easing = EaseInQuad)
            ) { value, _ -> wingOpenPercent = value }

            simulatingDrs = false
        } else {
            currentSpeed = 245f
            wingOpenPercent = 0f
        }
    }

    // Focus / center on selected DRS zone
    fun focusOnDrsZone(drsZone: DrsZone) {
        selectedDrsZone = drsZone
        val points = circuit.trackPoints
        if (points.isNotEmpty()) {
            val startPoint = points[drsZone.startNodeIndex]
            val endPoint = points[drsZone.endNodeIndex]
            
            // Calculate midpoint of the DRS zone
            val midX = (startPoint.first + endPoint.first) / 2f
            val midY = (startPoint.second + endPoint.second) / 2f
            
            // Zoom in slightly and center viewport on midpoint (relative to 0.5f center)
            zoomScale = 2.2f
            // We shift panOffset to center the selected coordinates
            // Map coordinates are 0f..1f, so midX - 0.5f is the shift from center.
            // Scale by a factor to translate on screen coordinates.
            panOffset = Offset(
                x = -(midX - 0.5f) * 450f,
                y = -(midY - 0.5f) * 450f
            )
        }
    }

    // Automatically focus on the first zone when selected or loaded
    LaunchedEffect(circuit.id) {
        circuit.drsZonesList.firstOrNull()?.let { focusOnDrsZone(it) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_map_component"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Map Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = F1Red,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "LIVE INTERACTIVE TRACK MAP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .background(F1Green.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .border(1.dp, F1Green.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(F1Green)
                    )
                    Text(
                        text = "DRS TELEMETRY SYNCED",
                        color = F1Green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Map Canvas Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(F1CarbonDark)
                .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(1f, 5f)
                        // Smoothly accumulate pan with limits
                        val maxPan = 400f * zoomScale
                        panOffset = Offset(
                            x = (panOffset.x + pan.x).coerceIn(-maxPan, maxPan),
                            y = (panOffset.y + pan.y).coerceIn(-maxPan, maxPan)
                        )
                    }
                }
        ) {
            val points = circuit.trackPoints

            Canvas(modifier = Modifier.fillMaxSize().testTag("track_map_canvas")) {
                val canvasSize = size

                // Bounding box of original track points
                val minX = points.minOfOrNull { it.first } ?: 0f
                val maxX = points.maxOfOrNull { it.first } ?: 1f
                val minY = points.minOfOrNull { it.second } ?: 0f
                val maxY = points.maxOfOrNull { it.second } ?: 1f

                val trackWidth = if (maxX - minX > 0) maxX - minX else 1f
                val trackHeight = if (maxY - minY > 0) maxY - minY else 1f

                // Helper to map 0f..1f normalized coordinates to our centered Canvas
                fun getCanvasCoord(point: Pair<Float, Float>, padding: Float = 48f): Offset {
                    val usableWidth = canvasSize.width - padding * 2
                    val usableHeight = canvasSize.height - padding * 2

                    // Scale to fit while maintaining aspect ratio
                    val scale = min(usableWidth / trackWidth, usableHeight / trackHeight)

                    val offsetX = padding + (usableWidth - trackWidth * scale) / 2
                    val offsetY = padding + (usableHeight - trackHeight * scale) / 2

                    val x = offsetX + (point.first - minX) * scale
                    val y = offsetY + (point.second - minY) * scale
                    return Offset(x, y)
                }

                // 1. Draw high-tech grid lines (if enabled)
                if (showGrid) {
                    val gridSpacing = 40f
                    val columns = (canvasSize.width / gridSpacing).toInt()
                    val rows = (canvasSize.height / gridSpacing).toInt()
                    
                    for (i in 0..columns) {
                        val x = i * gridSpacing
                        drawLine(
                            color = Color(0xFF1E1E28),
                            start = Offset(x, 0f),
                            end = Offset(x, canvasSize.height),
                            strokeWidth = 1f
                        )
                    }
                    for (j in 0..rows) {
                        val y = j * gridSpacing
                        drawLine(
                            color = Color(0xFF1E1E28),
                            start = Offset(0f, y),
                            end = Offset(canvasSize.width, y),
                            strokeWidth = 1f
                        )
                    }
                }

                // Apply Interactive Zoom and Pan transformations
                withTransform({
                    translate(panOffset.x, panOffset.y)
                    scale(zoomScale, zoomScale, pivot = Offset(canvasSize.width / 2f, canvasSize.height / 2f))
                }) {
                    if (points.isNotEmpty()) {
                        // 2. Build the main F1 track path
                        val trackPath = Path()
                        val startOffset = getCanvasCoord(points[0])
                        trackPath.moveTo(startOffset.x, startOffset.y)

                        for (i in 1 until points.size) {
                            val nextOffset = getCanvasCoord(points[i])
                            trackPath.lineTo(nextOffset.x, nextOffset.y)
                        }
                        trackPath.close()

                        // Draw track base border (outer outline) for modern neon glow depth
                        drawPath(
                            path = trackPath,
                            color = Color(0xFF2C2C35),
                            style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Draw track inner line
                        drawPath(
                            path = trackPath,
                            color = Color.White,
                            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // 3. Highlight DRS Zones
                        circuit.drsZonesList.forEach { drsZone ->
                            val isSelected = selectedDrsZone?.id == drsZone.id
                            val drsColor = if (isSelected) Color(0xFF00FF66) else F1Green

                            // Build path segment for this DRS zone
                            val drsPath = Path()
                            var nodeIdx = drsZone.startNodeIndex
                            val firstNodeOffset = getCanvasCoord(points[nodeIdx])
                            drsPath.moveTo(firstNodeOffset.x, firstNodeOffset.y)

                            while (true) {
                                if (nodeIdx == drsZone.endNodeIndex) break
                                nodeIdx = (nodeIdx + 1) % points.size
                                val nextNodeOffset = getCanvasCoord(points[nodeIdx])
                                drsPath.lineTo(nextNodeOffset.x, nextNodeOffset.y)
                            }

                            // Draw glowing highlighted DRS lane
                            drawPath(
                                path = drsPath,
                                color = drsColor.copy(alpha = if (isSelected) pulseAlpha else 0.7f),
                                style = Stroke(
                                    width = if (isSelected) 10f else 6f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            // Animated pulsing signal dot at start of selected DRS zone
                            if (isSelected) {
                                val pulseOrigin = getCanvasCoord(points[drsZone.startNodeIndex])
                                drawCircle(
                                    color = Color(0xFF00FF66).copy(alpha = 1f - (dRSPulseRadius - 10f) / 14f),
                                    radius = dRSPulseRadius,
                                    center = pulseOrigin
                                )
                                drawCircle(
                                    color = Color(0xFF00FF66),
                                    radius = 6f,
                                    center = pulseOrigin
                                )
                            }
                        }

                        // 4. Draw Start/Finish grid checkered marker (Index 0)
                        val startCoord = getCanvasCoord(points[0])
                        drawCircle(
                            color = F1Red,
                            radius = 6f,
                            center = startCoord
                        )
                        // Tiny checkerboard cross
                        drawLine(
                            color = Color.White,
                            start = Offset(startCoord.x - 8f, startCoord.y),
                            end = Offset(startCoord.x + 8f, startCoord.y),
                            strokeWidth = 3f
                        )

                        // 5. Draw small Turn Markers (dots and numbers)
                        points.forEachIndexed { index, pair ->
                            val coord = getCanvasCoord(pair)
                            // Skip index 0 start/finish
                            if (index > 0 && index % 2 == 1) {
                                drawCircle(
                                    color = Color.DarkGray,
                                    radius = 3f,
                                    center = coord
                                )
                                
                                // Draw Turn labels (T1, T3, etc.)
                                val textLayoutResult = drawContext.canvas.nativeCanvas
                                // Jetpack Compose's native Canvas can write text, but to keep it perfectly cross-platform
                                // we can render Turn text overlay comfortably or use simple draw circles.
                            }
                        }
                    }
                }
            }

            // Legend / Overlay Info (Checkered start & DRS indicators)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(F1Red, CircleShape))
                    Text("START / FINISH", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(F1Green, RoundedCornerShape(1.dp)))
                    Text("DRS ZONE", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Interactive Controls floating on top-right of the map
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Zoom In
                IconButton(
                    onClick = { zoomScale = (zoomScale + 0.5f).coerceAtMost(5f) },
                    modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(16.dp))
                }

                // Zoom Out
                IconButton(
                    onClick = { zoomScale = (zoomScale - 0.5f).coerceAtLeast(1f) },
                    modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(16.dp))
                }

                // Reset View
                IconButton(
                    onClick = {
                        zoomScale = 1f
                        panOffset = Offset.Zero
                    },
                    modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset View", tint = Color.White, modifier = Modifier.size(14.dp))
                }

                // Show/Hide Grid toggle
                IconButton(
                    onClick = { showGrid = !showGrid },
                    modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = "Toggle Grid",
                        tint = if (showGrid) F1Green else Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Pinch-to-zoom / swipe gesture tip
            Text(
                text = "Pinch to zoom / Drag to pan",
                fontSize = 8.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }

        // DRS Zone Selector chips
        if (circuit.drsZonesList.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SELECT DRS ZONE TO HIGHLIGHT:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    fontFamily = FontFamily.Monospace
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(circuit.drsZonesList) { zone ->
                        val isSelected = selectedDrsZone?.id == zone.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) F1Green.copy(alpha = 0.15f) 
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) F1Green else Color.Gray.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { focusOnDrsZone(zone) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("drs_zone_chip_${zone.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Adjust,
                                    contentDescription = null,
                                    tint = if (isSelected) F1Green else Color.Gray,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = zone.name.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isSelected) F1Green else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Selected DRS details + speed telemetry simulator
        selectedDrsZone?.let { zone ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = zone.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = F1Green
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(F1Green, CircleShape)
                            )
                            Text(
                                text = "ACTIVE ZONE",
                                color = F1Green,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))

                    // Location detail
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DETECTION POINT", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                            Text(zone.detectionPoint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DRS OVERTAKING RATE", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = if (circuit.id == "monaco") "VERY LOW (12%)" else "HIGH (76%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (circuit.id == "monaco") F1Red else F1Green
                            )
                        }
                    }

                    Text(
                        text = zone.description,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Interactive Telemetry Simulator Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(F1CarbonDark, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("DRS WING STATUS", fontSize = 7.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (wingOpenPercent > 0) F1Green else Color.DarkGray, CircleShape)
                                        )
                                        Text(
                                            text = if (wingOpenPercent > 0) "OPEN (DRS ACTIVED)" else "CLOSED (MAX DOWNFORCE)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (wingOpenPercent > 0) F1Green else Color.LightGray,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("SIMULATED SPEED", fontSize = 7.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                                    Text(
                                        text = "${currentSpeed.toInt()} KM/H",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = if (wingOpenPercent > 0) F1Green else Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Wing angle graphic representation
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("REAR WING PROFILE:", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Simulated Wing flap opening
                                    Text(
                                        text = "[  DRS FLAP: ${wingOpenPercent.toInt()}% OPEN  ]",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (wingOpenPercent > 0) F1Green else Color.Gray,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Ramping slider representing speed boost
                            LinearProgressIndicator(
                                progress = { (currentSpeed - 200f) / 150f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (wingOpenPercent > 0) F1Green else F1Red,
                                trackColor = Color.DarkGray
                            )

                            Button(
                                onClick = {
                                    if (!simulatingDrs) {
                                        simulatingDrs = true
                                    }
                                },
                                enabled = !simulatingDrs,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = F1Green,
                                    contentColor = Color.Black,
                                    disabledContainerColor = F1Green.copy(alpha = 0.2f),
                                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .testTag("simulate_drs_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (simulatingDrs) Icons.Default.Speed else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (simulatingDrs) "DRS SPEED BOOST ENGAGED!" else "SIMULATE DRS TELEMETRY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
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
