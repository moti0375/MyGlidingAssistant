package presentation.composables.main_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun GaugesPanel(
    timerText: AnnotatedString,
    speedText: AnnotatedString,
    distanceText: AnnotatedString,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    onSpeedClick: (() -> Unit)? = null,
    onDistanceClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        // Row 1: recording indicator and timer share the same height
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RecordingIndicator(
                isRecording = isRecording,
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
            )
            MetricIndicator(
                value = timerText,
                endIcon = Icons.Default.Timer,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(4.dp))
        // Row 2: speed and distance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MetricIndicator(
                value = speedText,
                startIcon = Icons.Default.Speed,
                modifier = Modifier.weight(1f),
                onClick = onSpeedClick
            )
            MetricIndicator(
                value = distanceText,
                endIcon = Icons.Default.Straighten,
                modifier = Modifier.weight(1f),
                onClick = onDistanceClick
            )
        }
    }
}
