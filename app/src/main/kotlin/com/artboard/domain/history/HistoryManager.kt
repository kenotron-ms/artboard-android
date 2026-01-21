package com.artboard.domain.history

/**
 * Manages undo/redo history with command pattern
 */
class HistoryManager(
    private val maxHistorySize: Int = 100
) {
    private val undoStack = ArrayDeque<Command>()
    private val redoStack = ArrayDeque<Command>()
    
    /**
     * Execute a command and add it to history
     */
    fun execute(command: Command): CommandResult {
        val result = command.execute()
        
        // Add to undo stack
        undoStack.addLast(command)
        
        // Limit history size
        if (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }
        
        // Clear redo stack (new action invalidates redo)
        redoStack.clear()
        
        return result
    }
    
    /**
     * Undo the last command
     */
    fun undo(): CommandResult? {
        if (undoStack.isEmpty()) return null
        
        val command = undoStack.removeLast()
        val result = command.undo()
        
        // Add to redo stack
        redoStack.addLast(command)
        
        return result
    }
    
    /**
     * Redo the last undone command
     */
    fun redo(): CommandResult? {
        if (redoStack.isEmpty()) return null
        
        val command = redoStack.removeLast()
        val result = command.execute()
        
        // Add back to undo stack
        undoStack.addLast(command)
        
        return result
    }
    
    /**
     * Check if undo is available
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    
    /**
     * Check if redo is available
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    /**
     * Clear all history
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
    
    /**
     * Get history size
     */
    fun getHistorySize(): Int = undoStack.size
}
