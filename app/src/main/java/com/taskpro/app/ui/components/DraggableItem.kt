package com.taskpro.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Wraps a row inside a reorderable LazyColumn, lifting and translating it
 * while it is being dragged.
 */
@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(isDragging: Boolean) -> Unit
) {
    val dragging = index == dragDropState.draggingItemIndex
    val draggingModifier = if (dragging) {
        Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = dragDropState.draggingItemOffset }
    } else {
        Modifier.animateItem()
    }

    val elevation by animateDpAsState(if (dragging) 8.dp else 0.dp, label = "drag-elevation")

    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .then(draggingModifier)
            .shadow(elevation)
    ) {
        content(dragging)
    }
}
