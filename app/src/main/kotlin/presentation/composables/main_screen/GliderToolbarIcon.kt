package presentation.composables.main_screen

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dunihuliapps.myglidingassistant.R

@Composable
fun GliderToolbarIcon() {
    val context = LocalContext.current
    val tint = LocalContentColor.current
    val tintArgb = tint.toArgb()
    val drawable = remember(tintArgb) {
        AppCompatResources.getDrawable(context, R.drawable.ic_glider_icon)?.mutate()?.also {
            it.setTint(tintArgb)
        }
    }
    Canvas(modifier = Modifier.size(24.dp)) {
        drawable?.let { d ->
            d.setBounds(0, 0, size.width.toInt(), size.height.toInt())
            drawIntoCanvas { canvas ->
                d.draw(canvas.nativeCanvas)
            }
        }
    }
}