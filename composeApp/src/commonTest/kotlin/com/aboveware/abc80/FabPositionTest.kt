package com.aboveware.abc80

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class FabPositionTest {
    @Test
    fun preservesPositionsInsideViewport() {
        assertEquals(
            Offset(-200f, -100f),
            constrainFabMenuOffset(Offset(-200f, -100f), IntSize(800, 600), IntSize(88, 88))
        )
        assertEquals(
            Offset.Zero,
            constrainFabMenuOffset(Offset.Zero, IntSize(800, 600), IntSize(88, 88))
        )
    }

    @Test
    fun clampsDraggingAtEveryEdge() {
        val viewport = IntSize(800, 600)
        val menu = IntSize(88, 88)
        assertEquals(Offset(-712f, -512f), constrainFabMenuOffset(Offset(-900f, -900f), viewport, menu))
        assertEquals(Offset.Zero, constrainFabMenuOffset(Offset(100f, 100f), viewport, menu))
    }

    @Test
    fun keepsExpandedMenuAndResizedViewportInBounds() {
        val position = Offset(-600f, -400f)
        assertEquals(
            Offset(-500f, -100f),
            constrainFabMenuOffset(position, IntSize(800, 600), IntSize(300, 500))
        )
        assertEquals(
            Offset(-312f, -212f),
            constrainFabMenuOffset(position, IntSize(400, 300), IntSize(88, 88))
        )
    }

    @Test
    fun handlesViewportSmallerThanMenu() {
        assertEquals(
            Offset.Zero,
            constrainFabMenuOffset(Offset(-100f, -100f), IntSize(40, 40), IntSize(88, 88))
        )
    }
}
