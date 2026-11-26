package com.playcrastination.feature.backlog

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.playcrastination.domain.model.BacklogPlacement
import kotlin.math.abs

internal data class BacklogDrop(val anchorId: Long, val placement: BacklogPlacement)

internal data class DraggedBacklogRow(val id: Long, val y: Float)

internal class BacklogDrag {
    val handles = mutableMapOf<Long, Rect>()
    var listOrigin = Offset.Zero
    var row by mutableStateOf<DraggedBacklogRow?>(null)

    fun target(listState: LazyListState, dragged: DraggedBacklogRow): BacklogDrop? {
        val nearest = listState.layoutInfo.visibleItemsInfo.asSequence()
            .filter { it.key is Long && it.offset + it.size > listState.layoutInfo.viewportStartOffset }
            .minByOrNull { abs(dragged.y - (it.offset + it.size / 2f)) } ?: return null
        val targetId = nearest.key as Long
        if (targetId == dragged.id) return null
        val placement = if (dragged.y < nearest.offset + nearest.size / 2f) {
            BacklogPlacement.BEFORE
        } else {
            BacklogPlacement.AFTER
        }
        return BacklogDrop(targetId, placement)
    }
}

internal fun Modifier.dragBacklogRows(
    drag: BacklogDrag,
    listState: LazyListState,
    onDrop: (Long, BacklogDrop) -> Unit
): Modifier = onGloballyPositioned { drag.listOrigin = it.positionInRoot() }
    .pointerInput(drag, listState) {
        awaitEachGesture {
            // Take over only gestures that start on a reorder handle.
            val first = awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Initial)
            val positionInRoot = first.position + drag.listOrigin
            val id = drag.handles.entries.firstOrNull { positionInRoot in it.value }?.key
                ?: return@awaitEachGesture

            first.consume()
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == first.id } ?: break
                    if (event.changes.any { it.id != first.id && it.pressed } || change.isConsumed) break
                    change.consume()

                    val y = change.position.y
                    if (!change.pressed) {
                        if (drag.row != null && change.position.x in 0f..size.width.toFloat() &&
                            y in 0f..size.height.toFloat()
                        ) {
                            drag.target(listState, DraggedBacklogRow(id, y))?.let { onDrop(id, it) }
                        }
                        break
                    }
                    if (drag.row != null || abs(y - first.position.y) >= viewConfiguration.touchSlop) {
                        drag.row = DraggedBacklogRow(id, y)
                    }
                }
            } finally {
                drag.row = null
            }
        }
    }

@Composable
internal fun ScrollBacklogDuringDrag(drag: BacklogDrag, listState: LazyListState) {
    val edge = with(LocalDensity.current) { 64.dp.toPx() }
    LaunchedEffect(drag.row?.id, listState, edge) {
        while (drag.row != null) {
            withFrameNanos { }
            val row = drag.row ?: break
            val info = listState.layoutInfo
            val fromTop = row.y - info.viewportStartOffset
            val fromBottom = info.viewportEndOffset - row.y
            val scroll = when {
                fromTop < edge -> -((edge - fromTop) / edge).coerceIn(0f, 1f)
                fromBottom < edge -> ((edge - fromBottom) / edge).coerceIn(0f, 1f)
                else -> 0f
            }
            if (scroll != 0f) listState.scrollBy(scroll * edge / 6f)
        }
    }
}
