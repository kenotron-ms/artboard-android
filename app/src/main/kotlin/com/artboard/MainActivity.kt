package com.artboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.artboard.ui.canvas.CanvasView
import com.artboard.ui.canvas.CanvasViewModel
import com.artboard.ui.theme.ArtboardTheme
import com.artboard.ui.toolbar.BrushSelector
import com.artboard.ui.toolbar.ColorPicker
import androidx.compose.ui.graphics.Color as ComposeColor

class MainActivity : ComponentActivity() {
    
    private val viewModel: CanvasViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ArtboardTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: CanvasViewModel) {
    val project by viewModel.project.collectAsState()
    val currentBrush by viewModel.currentBrush.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val activeLayerIndex by viewModel.activeLayerIndex.collectAsState()
    
    var showBrushSelector by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    
    var canvasView by remember { mutableStateOf<CanvasView?>(null) }
    
    // Update canvas when layers change
    LaunchedEffect(project?.layers, activeLayerIndex) {
        project?.let { proj ->
            canvasView?.setLayers(proj.layers, activeLayerIndex)
        }
    }
    
    // Update canvas brush and color
    LaunchedEffect(currentBrush) {
        canvasView?.setBrush(currentBrush)
    }
    
    LaunchedEffect(currentColor) {
        canvasView?.setColor(currentColor)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black)
    ) {
        // Top toolbar
        TopToolbar(
            currentBrush = currentBrush,
            currentColor = ComposeColor(currentColor),
            canUndo = canUndo,
            canRedo = canRedo,
            onBrushClick = { showBrushSelector = true },
            onColorClick = { showColorPicker = true },
            onUndoClick = {
                viewModel.undo()
            },
            onRedoClick = {
                viewModel.redo()
            },
            onClearClick = {
                viewModel.clearActiveLayer()
                canvasView?.refresh()
            },
            onNewLayerClick = {
                viewModel.addLayer()
            }
        )
        
        // Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { context ->
                    CanvasView(context).apply {
                        canvasView = this
                        
                        // Initialize with project dimensions
                        project?.let { proj ->
                            initialize(proj.width, proj.height)
                            setLayers(proj.layers, activeLayerIndex)
                        }
                        
                        setBrush(currentBrush)
                        setColor(currentColor)
                        
                        // Set up stroke callbacks
                        onStrokeEnd = {
                            // Get the completed stroke and add to history
                            // Note: The stroke is already rendered to the layer bitmap
                            // We just need to update the view
                            refresh()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    
    // Dialogs
    if (showBrushSelector) {
        BrushSelector(
            currentBrush = currentBrush,
            onBrushSelected = { viewModel.setBrush(it) },
            onDismiss = { showBrushSelector = false }
        )
    }
    
    if (showColorPicker) {
        ColorPicker(
            currentColor = ComposeColor(currentColor),
            onColorSelected = { viewModel.setColor(it.toArgb()) },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
fun TopToolbar(
    currentBrush: com.artboard.data.model.Brush,
    currentColor: ComposeColor,
    canUndo: Boolean,
    canRedo: Boolean,
    onBrushClick: () -> Unit,
    onColorClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onClearClick: () -> Unit,
    onNewLayerClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - drawing tools
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brush button
                IconButton(onClick = onBrushClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Brush",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Text(
                    text = "${currentBrush.size.toInt()}px",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // Color button
                IconButton(
                    onClick = onColorClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(currentColor, MaterialTheme.shapes.small)
                ) {
                    // Empty - just showing the color
                }
            }
            
            // Center - history
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onUndoClick,
                    enabled = canUndo
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Undo",
                        tint = if (canUndo) MaterialTheme.colorScheme.onBackground
                               else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                }
                
                IconButton(
                    onClick = onRedoClick,
                    enabled = canRedo
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Redo",
                        tint = if (canRedo) MaterialTheme.colorScheme.onBackground
                               else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                }
            }
            
            // Right side - layer tools
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onNewLayerClick) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New Layer",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                IconButton(onClick = onClearClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
