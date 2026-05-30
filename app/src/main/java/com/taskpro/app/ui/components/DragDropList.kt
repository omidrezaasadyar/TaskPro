package com.taskpro.app.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * State holder for a long-press drag-to-reorder [androidx.compose.foundation.lazy.LazyColumn].
 *
 * [onMove] is called continuously as the dragged row passes over its
 * neighbours so the visible list updates live; persist the final order when
 * the drag ends (see [rememberDragDropState]).
 */
class DragDropState internal constructor(
    val listState: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit,
    private val onDragEnd: () -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    internal val scrollChannel = Channel<Float>()

    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableFloatStateOf(0f)

    /** Pixel offset to apply to the currently dragged row. */
    val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
        } ?: 0f

    private val draggingItemLayoutInfo
        get() = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == draggingItemIndex }

    internal fun onDragStart(offset: androidx.compose.ui.geometry.Offset) {
        listState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                draggingItemIndex = it.index
                draggingItemInitialOffset = it.offset.toFloat()
            }
    }

    internal fun onDragInterrupted() {
        if (draggingItemIndex != null) onDragEnd()
        draggingItemIndex = null
        draggingItemDraggedDelta = 0f
        draggingItemInitialOffset = 0f
    }

    internal fun onDrag(offsetY: Float) {
        draggingItemDraggedDelta += offsetY

        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middleOffset = startOffset + (endOffset - startOffset) / 2f

        val target = listState.layoutInfo.visibleItemsInfo.find { item ->
            middleOffset.toInt() in item.offset..(item.offset + item.size) &&
                draggingItem.index != item.index
        }

        if (target != null) {
            val from = draggingItemIndex
            if (from != null) {
                onMove(from, target.index)
                draggingItemIndex = target.index
            }
        } else {
            // Auto-scroll when dragging past the top/bottom edge.
            val overscroll = when {
                draggingItemDraggedDelta > 0 ->
                    (endOffset - listState.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
                draggingItemDraggedDelta < 0 ->
                    (startOffset - listState.layoutInfo.viewportStartOffset).coerceAtMost(0f)
                else -> 0f
            }
            if (overscroll != 0f) scope.launch { scrollChannel.send(overscroll) }
        }
    }
}

@Composable
fun rememberDragDropState(
    listState: LazyListState = rememberLazyListState(),
    onMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit = {}
): DragDropState {
    val scope = rememberCoroutineScope()
    // Keep the latest callbacks without recreating the state on every recomposition.
    val moveState = androidx.compose.runtime.rememberUpdatedState(onMove)
    val endState = androidx.compose.runtime.rememberUpdatedState(onDragEnd)
    val state = remember(listState) {
        DragDropState(
            listState = listState,
            scope = scope,
            onMove = { from, to -> moveState.value(from, to) },
            onDragEnd = { endState.value() }
        )
    }
    // Drain the auto-scroll channel.
    androidx.compose.runtime.LaunchedEffect(state) {
        state.scrollChannel.receiveAsFlow().collect { diff ->
            state.listState.scrollBy(diff)
        }
    }
    return state
}

/** Apply to a LazyColumn to enable long-press drag reordering. */
fun Modifier.dragContainer(dragDropState: DragDropState): Modifier = pointerInput(dragDropState) {
    detectDragGesturesAfterLongPress(
        onDrag = { change, offset ->
            change.consume()
            dragDropState.onDrag(offset.y)
        },
        onDragStart = { offset -> dragDropState.onDragStart(offset) },
        onDragEnd = { dragDropState.onDragInterrupted() },
        onDragCancel = { dragDropState.onDragInterrupted() }
    )
}
