package com.notes.ink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InkToolbar(
    uiState: InkCanvasUiState,
    onSelectTool: (CanvasTool) -> Unit,
    onSelectColor: (InkColor) -> Unit,
    onSelectThickness: (InkThickness) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearPage: () -> Unit,
    onToggleDebug: () -> Unit,
    onTriggerStressTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Canvas") },
            text = { Text("Are you sure you want to erase all strokes on this page? This action can be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearPage()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tools: Pen & Eraser
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolButton(
                    label = "Pen",
                    selected = uiState.tool == CanvasTool.PEN,
                    onClick = { onSelectTool(CanvasTool.PEN) }
                )
                ToolButton(
                    label = "Eraser",
                    selected = uiState.tool == CanvasTool.ERASER,
                    onClick = { onSelectTool(CanvasTool.ERASER) }
                )
            }

            // 3 Colors
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InkColor.values().forEach { color ->
                    ColorCircle(
                        color = color,
                        selected = uiState.color == color && uiState.tool == CanvasTool.PEN,
                        onClick = { onSelectColor(color) }
                    )
                }
            }

            // 3 Sizes
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ThicknessDot(
                    thickness = InkThickness.THIN,
                    selected = uiState.thickness == InkThickness.THIN,
                    onClick = { onSelectThickness(InkThickness.THIN) }
                )
                ThicknessDot(
                    thickness = InkThickness.MEDIUM,
                    selected = uiState.thickness == InkThickness.MEDIUM,
                    onClick = { onSelectThickness(InkThickness.MEDIUM) }
                )
                ThicknessDot(
                    thickness = InkThickness.THICK,
                    selected = uiState.thickness == InkThickness.THICK,
                    onClick = { onSelectThickness(InkThickness.THICK) }
                )
            }

            // Actions: Undo, Redo, Clear, Debug, Stress
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = onUndo,
                    enabled = uiState.canUndo,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text("Undo", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onRedo,
                    enabled = uiState.canRedo,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text("Redo", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text("Clear", fontSize = 12.sp, color = Color(0xFFC62828))
                }
                FilledTonalButton(
                    onClick = onToggleDebug,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(if (uiState.showDebugOverlay) "HUD On" else "HUD Off", fontSize = 11.sp)
                }
                Button(
                    onClick = onTriggerStressTest,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text("2K Stress", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ToolButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(label, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ColorCircle(color: InkColor, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color.composeColor)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) Color(0xFFFFA000) else Color(0x66000000),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ThicknessDot(thickness: InkThickness, selected: Boolean, onClick: () -> Unit) {
    val dotSize = when (thickness) {
        InkThickness.THIN -> 8.dp
        InkThickness.MEDIUM -> 14.dp
        InkThickness.THICK -> 20.dp
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (selected) Color(0xFFE0E0E0) else Color.Transparent)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) Color.Black else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(Color.Black)
        )
    }
}
