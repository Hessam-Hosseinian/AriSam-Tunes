package eu.wewox.minabox

// Modified for AriSam Tunes: drag deltas are coalesced and gesture velocity is reset so rapid
// two-dimensional scrolling stays responsive. The upstream source is documented in third_party/minabox.

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * Lazy layout to display data on the two directional plane.
 * Items should be provided with [content] lambda.
 *
 * @param modifier The modifier instance for the root composable.
 * @param state The state which could be used to observe and change translation offset.
 * @param contentPadding A padding around the whole content. This will add padding for the content
 * after it has been clipped, which is not possible via modifier param.
 * @param scrollDirection Determines which directions are allowed to scroll.
 * @param content The lambda block which describes the content. Inside this block you can use
 * [MinaBoxScope.items] method to add items.
 */
@Composable
public fun MinaBox(
    modifier: Modifier = Modifier,
    state: MinaBoxState = rememberSaveableMinaBoxState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scrollDirection: MinaBoxScrollDirection = MinaBoxScrollDirection.BOTH,
    content: MinaBoxScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val contentPaddingPx = contentPadding.toPx()

    val itemProvider = rememberItemProvider(content)

    var positionProvider by remember { mutableStateOf<MinaBoxPositionProviderImpl?>(null) }

    LazyLayout(
        modifier = modifier
            .clipToBounds()
            .lazyLayoutPointerInput(state, scrollDirection),
        itemProvider = { itemProvider },
    ) { constraints ->
        val size = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

        positionProvider = positionProvider.update(
            state = state,
            itemProvider = itemProvider,
            layoutDirection = layoutDirection,
            size = size,
            contentPaddingPx = contentPaddingPx,
            scope = scope
        )

        val items = itemProvider.getItems(
            state.layoutTranslateX,
            state.layoutTranslateY,
            contentPaddingPx,
            size,
        )

        val placeables = items.map { (index, bounds) ->
            measure(
                index,
                Constraints.fixed(bounds.width.toInt(), bounds.height.toInt())
            ) to bounds.topLeft
        }

        val itemsSize = itemProvider.getItemsSize(contentPaddingPx)
        val width = min(itemsSize.width.toInt(), constraints.maxWidth)
        val height = min(itemsSize.height.toInt(), constraints.maxHeight)

        layout(width, height) {
            placeables.forEach { (itemPlaceables, position) ->
                itemPlaceables.forEach { placeable ->
                    placeable.placeRelative(
                        x = position.x.toInt(),
                        y = position.y.toInt(),
                    )
                }
            }
        }
    }
}

private fun MinaBoxItemProvider.getItemsSize(contentPaddingPx: Rect): Size =
    size.let {
        Size(
            width = it.width + contentPaddingPx.left + contentPaddingPx.right,
            height = it.height + contentPaddingPx.top + contentPaddingPx.bottom,
        )
    }

private fun MinaBoxPositionProviderImpl?.update(
    state: MinaBoxState,
    itemProvider: MinaBoxItemProvider,
    layoutDirection: LayoutDirection,
    size: Size,
    contentPaddingPx: Rect,
    scope: CoroutineScope,
): MinaBoxPositionProviderImpl =
    if (
        this != null &&
        this.items == itemProvider.items &&
        this.layoutDirection == layoutDirection &&
        this.size == size
    ) {
        this
    } else {
        MinaBoxPositionProviderImpl(itemProvider.items, layoutDirection, size).also {
            val itemsSize = itemProvider.getItemsSize(contentPaddingPx)
            val bounds = Rect(
                left = 0f,
                top = 0f,
                right = (itemsSize.width - size.width).coerceAtLeast(0f),
                bottom = (itemsSize.height - size.height).coerceAtLeast(0f)
            )
            state.updateBounds(it, bounds, size, scope)
        }
    }

@Composable
private fun PaddingValues.toPx(): Rect {
    val layoutDirection = LocalLayoutDirection.current
    return LocalDensity.current.run {
        Rect(
            calculateLeftPadding(layoutDirection).toPx(),
            calculateTopPadding().toPx(),
            calculateRightPadding(layoutDirection).toPx(),
            calculateBottomPadding().toPx()
        )
    }
}

private fun Modifier.lazyLayoutPointerInput(
    state: MinaBoxState,
    scrollDirection: MinaBoxScrollDirection,
): Modifier = pointerInput(state, scrollDirection) {
    val velocityTracker = VelocityTracker()
    coroutineScope {
        val dragAccumulator = DragDeltaAccumulator(state, this)
        when (scrollDirection) {
            MinaBoxScrollDirection.BOTH -> detectDragGestures(
                onDragStart = { onDragStart(state, velocityTracker, dragAccumulator, this) },
                onDragEnd = { onDragEnd(state, velocityTracker, scrollDirection, dragAccumulator, this) },
                onDragCancel = { onDragCancel(velocityTracker, dragAccumulator) },
                onDrag = { change, dragAmount ->
                    onDrag(change, dragAmount, velocityTracker, dragAccumulator)
                }
            )

            MinaBoxScrollDirection.HORIZONTAL -> detectHorizontalDragGestures(
                onDragStart = { onDragStart(state, velocityTracker, dragAccumulator, this) },
                onDragEnd = { onDragEnd(state, velocityTracker, scrollDirection, dragAccumulator, this) },
                onDragCancel = { onDragCancel(velocityTracker, dragAccumulator) },
                onHorizontalDrag = { change, dragAmount ->
                    onDrag(change, Offset(dragAmount, 0f), velocityTracker, dragAccumulator)
                }
            )

            MinaBoxScrollDirection.VERTICAL -> detectVerticalDragGestures(
                onDragStart = { onDragStart(state, velocityTracker, dragAccumulator, this) },
                onDragEnd = { onDragEnd(state, velocityTracker, scrollDirection, dragAccumulator, this) },
                onDragCancel = { onDragCancel(velocityTracker, dragAccumulator) },
                onVerticalDrag = { change, dragAmount ->
                    onDrag(change, Offset(0f, dragAmount), velocityTracker, dragAccumulator)
                }
            )
        }
    }
}

private fun onDragStart(
    state: MinaBoxState,
    velocityTracker: VelocityTracker,
    dragAccumulator: DragDeltaAccumulator,
    scope: CoroutineScope,
) {
    velocityTracker.resetTracking()
    dragAccumulator.reset()
    scope.launch { state.stopAnimation() }
}

private fun onDrag(
    change: PointerInputChange,
    dragAmount: Offset,
    velocityTracker: VelocityTracker,
    dragAccumulator: DragDeltaAccumulator,
) {
    change.consume()
    velocityTracker.addPosition(change.uptimeMillis, change.position)
    dragAccumulator.add(dragAmount)
}

private fun onDragEnd(
    state: MinaBoxState,
    velocityTracker: VelocityTracker,
    scrollDirection: MinaBoxScrollDirection,
    dragAccumulator: DragDeltaAccumulator,
    scope: CoroutineScope,
) {
    var velocity = velocityTracker.calculateVelocity()
    velocityTracker.resetTracking()
    velocity = when (scrollDirection) {
        MinaBoxScrollDirection.BOTH -> velocity
        MinaBoxScrollDirection.HORIZONTAL -> velocity.copy(velocity.x, 0f)
        MinaBoxScrollDirection.VERTICAL -> velocity.copy(0f, velocity.y)
    }
    scope.launch {
        dragAccumulator.awaitDrained()
        state.flingBy(velocity)
    }
}

private fun onDragCancel(
    velocityTracker: VelocityTracker,
    dragAccumulator: DragDeltaAccumulator,
) {
    velocityTracker.resetTracking()
    dragAccumulator.reset()
}

private class DragDeltaAccumulator(
    private val state: MinaBoxState,
    private val scope: CoroutineScope,
) {
    private var pending = Offset.Zero
    private var drainJob: Job? = null

    fun add(delta: Offset) {
        pending += delta
        if (drainJob?.isActive != true) {
            drainJob = scope.launch { drain() }
        }
    }

    fun reset() {
        pending = Offset.Zero
        drainJob?.cancel()
        drainJob = null
    }

    suspend fun awaitDrained() {
        drainJob?.join()
        if (pending != Offset.Zero) drain()
    }

    private suspend fun drain() {
        while (pending != Offset.Zero) {
            val delta = pending
            pending = Offset.Zero
            state.dragBy(delta)
        }
    }
}
