package hr.jkacan.setmaker.editor.layout

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val NODE_WIDTH = 120.dp
val NODE_HEIGHT = 150.dp
val HORIZONTAL_SPACING = 180.dp
val VERTICAL_SPACING = 220.dp

fun Dp.withDensity(density: Density): Float {
    return with(density) { this@withDensity.toPx() }
}

//fun Dp.toPx(): Float {
//    return value
//}
