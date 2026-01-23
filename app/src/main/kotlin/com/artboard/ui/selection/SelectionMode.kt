package com.artboard.ui.selection

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.artboard.data.model.Layer
import com.artboard.data.model.SelectionMask
import com.artboard.ui.selection.components.*
import com.artboard.ui.selection.tools.*

/**
 * Selection mode composable
 * Manages the selection UI and tool interactions
 */
@Composable
fun SelectionMode(
    viewModel: SelectionViewModel,
    canvasBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    val selectionTool by viewModel.selectionTool.collectAsState()
    val selectionMask by viewModel.selectionMask.collectAsState()
    val featherRadius by viewModel.featherRadius.collectAsState()
    val tolerance by viewModel.tolerance.collectAsState()
    val isSelecting by viewModel.isSelecting.collectAsState()
    
    // Tool instances
    val lassoTool = remember { LassoTool() }
    val rectangleTool = remember { RectangleTool() }
    val ellipseTool = remember { EllipseTool() }
    val magicWandTool = remember { MagicWandTool(tolerance) }
    
    // Update magic wand tolerance
    LaunchedEffect(tolerance) {
        magicWandTool.setTolerance(tolerance)
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Selection overlay with marching ants
        SelectionOverlay(
            selectionMask = selectionMask,
            modifier = Modifier.fillMaxSize()
        )
        
        // Tool preview overlays
        if (isSelecting) {
            when (selectionTool) {
                SelectionToolType.LASSO -> {
                    if (lassoTool.isDrawing()) {
                        LassoPreviewOverlay(
                            path = lassoTool.getCurrentPath(),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                SelectionToolType.RECTANGLE -> {
                    if (rectangleTool.isDrawing()) {
                        RectanglePreviewOverlay(
                            rect = rectangleTool.getCurrentRect(),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                SelectionToolType.ELLIPSE -> {
                    if (ellipseTool.isDrawing()) {
                        EllipsePreviewOverlay(
                            rect = ellipseTool.getCurrentRect(),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                SelectionToolType.MAGIC_WAND -> {
                    // Magic wand has no preview (instant selection on tap)
                }
            }
        }
        
        // Touch input handling
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectionTool) {
                    when (selectionTool) {
                        SelectionToolType.LASSO -> {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    viewModel.setIsSelecting(true)
                                    lassoTool.onTouchDown(offset.x, offset.y)
                                },
                                onDrag = { change, _ ->
                                    lassoTool.onTouchMove(change.position.x, change.position.y)
                                },
                                onDragEnd = {
                                    val mask = lassoTool.onTouchUp(
                                        0f, 0f,
                                        canvasBitmap.width,
                                        canvasBitmap.height,
                                        featherRadius
                                    )
                                    viewModel.setSelection(mask)
                                    viewModel.setIsSelecting(false)
                                },
                                onDragCancel = {
                                    lassoTool.cancel()
                                    viewModel.setIsSelecting(false)
                                }
                            )
                        }
                        
                        SelectionToolType.RECTANGLE -> {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    viewModel.setIsSelecting(true)
                                    rectangleTool.onTouchDown(offset.x, offset.y)
                                },
                                onDrag = { change, _ ->
                                    rectangleTool.onTouchMove(change.position.x, change.position.y)
                                },
                                onDragEnd = {
                                    val mask = rectangleTool.onTouchUp(
                                        0f, 0f,
                                        canvasBitmap.width,
                                        canvasBitmap.height,
                                        featherRadius
                                    )
                                    viewModel.setSelection(mask)
                                    viewModel.setIsSelecting(false)
                                },
                                onDragCancel = {
                                    rectangleTool.cancel()
                                    viewModel.setIsSelecting(false)
                                }
                            )
                        }
                        
                        SelectionToolType.ELLIPSE -> {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    viewModel.setIsSelecting(true)
                                    ellipseTool.onTouchDown(offset.x, offset.y)
                                },
                                onDrag = { change, _ ->
                                    ellipseTool.onTouchMove(change.position.x, change.position.y)
                                },
                                onDragEnd = {
                                    val mask = ellipseTool.onTouchUp(
                                        0f, 0f,
                                        canvasBitmap.width,
                                        canvasBitmap.height,
                                        featherRadius
                                    )
                                    viewModel.setSelection(mask)
                                    viewModel.setIsSelecting(false)
                                },
                                onDragCancel = {
                                    ellipseTool.cancel()
                                    viewModel.setIsSelecting(false)
                                }
                            )
                        }
                        
                        SelectionToolType.MAGIC_WAND -> {
                            detectTapGestures { offset ->
                                val mask = magicWandTool.onTap(
                                    offset.x,
                                    offset.y,
                                    canvasBitmap,
                                    featherRadius
                                )
                                viewModel.setSelection(mask)
                            }
                        }
                    }
                }
        )
        
        // Selection toolbar at bottom
        SelectionToolbar(
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
        
        // Selection mode indicator at top
        selectionMask?.let { mask ->
            if (!mask.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectionModeIndicator(
                        toolName = selectionTool.displayName()
                    )
                    
                    SelectionBoundsIndicator(
                        bounds = android.graphics.Rect(
                            mask.getBounds().left.toInt(),
                            mask.getBounds().top.toInt(),
                            mask.getBounds().right.toInt(),
                            mask.getBounds().bottom.toInt()
                        )
                    )
                }
            }
        }
    }
}

/**
 * Display name for selection tool types
 */
private fun SelectionToolType.displayName(): String = when (this) {
    SelectionToolType.LASSO -> "Lasso"
    SelectionToolType.RECTANGLE -> "Rectangle"
    SelectionToolType.ELLIPSE -> "Ellipse"
    SelectionToolType.MAGIC_WAND -> "Magic Wand"
}
