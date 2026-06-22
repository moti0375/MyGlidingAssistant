package com.dunihuliapps.myglidingassistnat.presentation.composables

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.dunihuliapps.myglidingassistant.R

@Composable
fun GliderIcon(modifier: Modifier = Modifier, tint: Color) {
    val context = LocalContext.current
    val tintArgb = tint.toArgb()
    val drawable = remember(tintArgb) {
        AppCompatResources.getDrawable(context, R.drawable.ic_glider_icon)?.mutate()?.also {
            it.setTint(tintArgb)
        }
    }
    Canvas(modifier = modifier) {
        drawable?.let { d ->
            d.setBounds(0, 0, size.width.toInt(), size.height.toInt())
            drawIntoCanvas { canvas ->
                d.draw(canvas.nativeCanvas)
            }
        }
    }
}
