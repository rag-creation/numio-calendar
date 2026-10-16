package com.rr.numio.calendar.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Swipe left for [onNext], right for [onPrevious] (one step per swipe). */
@Composable
fun Modifier.swipeSideways(onPrevious: () -> Unit, onNext: () -> Unit): Modifier {
    val prev by rememberUpdatedState(onPrevious)
    val next by rememberUpdatedState(onNext)
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    return pointerInput(Unit) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                when {
                    total < -threshold -> next()
                    total > threshold -> prev()
                }
            },
            onHorizontalDrag = { change, amount ->
                total += amount
                change.consume()
            }
        )
    }
}
