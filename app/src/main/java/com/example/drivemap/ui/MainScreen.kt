package com.example.drivemap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.drivemap.model.RoadSegment
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    state: MapUiState,
    onRequestPermission: () -> Unit,
    onStartTracking: () -> Unit,
    onEditSpeedLimit: suspend (Long, Int) -> Unit,
    onSegmentTapped: (Long?) -> Unit,
    onEditDismissed: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val editingSegmentId = state.selectedSegmentId
    val editSpeedValue = remember(editingSegmentId) { mutableStateOf("") }
    LaunchedEffect(editingSegmentId, state.segments) {
        val defaultValue = state.segments.firstOrNull { it.id == editingSegmentId }?.speedLimitKmh
        editSpeedValue.value = defaultValue?.toString().orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF10151F))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(state)
        Spacer(modifier = Modifier.height(12.dp))
        MapCanvas(
            state = state,
            onSegmentTapped = { segmentId ->
                if (state.isStationary) {
                    onSegmentTapped(segmentId)
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        )
        Spacer(modifier = Modifier.height(12.dp))
        ActionButtons(onRequestPermission, onStartTracking)
    }

    if (editingSegmentId != null) {
        SpeedLimitDialog(
            state = state,
            segmentId = editingSegmentId,
            onDismiss = {
                onEditDismissed()
                editSpeedValue.value = ""
            },
            onSave = { speed ->
                coroutineScope.launch {
                    onEditSpeedLimit(editingSegmentId, speed)
                }
            },
            textState = editSpeedValue
        )
    }
}

@Composable
private fun Header(state: MapUiState) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1A2130))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = state.streetName ?: "Straßenname unbekannt",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = state.localityName ?: "Ort unbekannt",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0B4C3)
            )
            val speedText = state.selectedSegmentId?.let { selectedId ->
                state.segments.firstOrNull { it.id == selectedId }?.speedLimitKmh
            } ?: state.segments.lastOrNull()?.speedLimitKmh
            val formattedSpeed = speedText?.let { "Begrenzung: ${it} km/h" } ?: "Keine Begrenzung"
            Text(
                text = formattedSpeed,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFE3E5F1)
            )
        }
    }
}

@Composable
private fun ActionButtons(
    onRequestPermission: () -> Unit,
    onStartTracking: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onRequestPermission) {
            Text(text = "Standortfreigabe anfragen")
        }
        Button(onClick = onStartTracking) {
            Text(text = "Aufzeichnung starten")
        }
    }
}

private data class RenderedSegment(
    val segment: RoadSegment,
    val start: Offset,
    val end: Offset
)

@Composable
private fun MapCanvas(
    state: MapUiState,
    onSegmentTapped: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = state.segments
    val currentLocation = state.currentLocation
    val points = remember(segments, currentLocation) {
        buildList {
            segments.forEach { segment ->
                add(segment.start)
                add(segment.end)
            }
            currentLocation?.let { add(it) }
        }
    }

    if (segments.isEmpty()) {
        Box(
            modifier = modifier
                .background(Color(0xFF0C111C), shape = RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyState()
        }
        return
    }

    Canvas(
        modifier = modifier
            .background(Color(0xFF0C111C), shape = RoundedCornerShape(16.dp))
            .pointerInput(segments, currentLocation) {
                detectTapGestures { offset ->
                    val projection = MapProjection.fromSegments(
                        size = size,
                        center = currentLocation ?: segments.first().start,
                        heading = state.currentHeading,
                        points = points
                    )
                    val rendered = segments.map { segment ->
                        val start = projection.toOffset(segment.start)
                        val end = projection.toOffset(segment.end)
                        RenderedSegment(segment, start, end)
                    }
                    val tappedSegment = rendered.minByOrNull { renderedSegment ->
                        distanceToSegment(offset, renderedSegment.start, renderedSegment.end)
                    }
                    if (tappedSegment != null) {
                        val distance = distanceToSegment(offset, tappedSegment.start, tappedSegment.end)
                        if (distance <= 48f) {
                            onSegmentTapped(tappedSegment.segment.id)
                        }
                    }
                }
            }
            .padding(8.dp)
    ) {
        val projection = MapProjection.fromSegments(
            size = size,
            center = currentLocation ?: segments.first().start,
            heading = state.currentHeading,
            points = points
        )
        segments.forEach { segment ->
            val start = projection.toOffset(segment.start)
            val end = projection.toOffset(segment.end)
            drawLine(
                color = if (segment.id == state.selectedSegmentId) Color(0xFFE9C46A) else Color(0xFF1DB954),
                start = start,
                end = end,
                strokeWidth = if (segment.id == state.selectedSegmentId) 12f else 8f,
                cap = StrokeCap.Round
            )
            val midPoint = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 32f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
                canvas.nativeCanvas.drawText("${segment.speedLimitKmh} km/h", midPoint.x, midPoint.y - 12f, paint)
            }
        }

        currentLocation?.let {
            val offset = projection.toOffset(it)
            drawCircle(
                color = Color(0xFF5F0FFF),
                radius = 18f,
                center = offset
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = Color.White)
        Text(text = "Noch keine Straßenabschnitte erfasst.", color = Color.White)
        Text(text = "Starte eine Fahrt, um Daten zu sammeln.", color = Color(0xFFB0B4C3))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedLimitDialog(
    state: MapUiState,
    segmentId: Long,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    textState: MutableState<String>
) {
    val segment = state.segments.firstOrNull { it.id == segmentId }
    if (segment == null) {
        onDismiss()
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Geschwindigkeitsbegrenzung anpassen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Aktuell: ${segment.speedLimitKmh} km/h")
                OutlinedTextField(
                    value = textState.value,
                    onValueChange = { value -> textState.value = value.filter { it.isDigit() } },
                    label = { Text("Neue Geschwindigkeitsbegrenzung") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val speed = textState.value.toIntOrNull()
                    if (speed != null && speed > 0) {
                        onSave(speed)
                        onDismiss()
                    }
                }
            ) {
                Text(text = "Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Abbrechen")
            }
        }
    )
}
