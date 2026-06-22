package presentation.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dunihuliapps.myglidingassistnat.presentation.composables.FlightDetailRow
import com.dunihuliapps.myglidingassistnat.presentation.composables.GliderIcon
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.HmsFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.MetricFormatter
import data.model.Flight

@Composable
fun FlightDetailsPanel(
    modifier: Modifier = Modifier,
    flight: Flight?,
) {
    val timeFormatter = remember { HmsFormatter() }
    val distanceFormatter = remember { MetricFormatter() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        flight?.let { f ->
            f.date?.let {
                FlightDetailRow(Icons.Default.CalendarToday, it)
            }
            FlightDetailRow(
                icon = Icons.Default.Timer,
                value = timeFormatter.formatTime(f.duration)
            )
            FlightDetailRow(
                icon = Icons.Default.Explore,
                value = distanceFormatter.formatUnits(f.maxDistance.toDouble()).toString()
            )
            FlightDetailRow(
                icon = Icons.Default.Straighten,
                value = distanceFormatter.formatUnits(f.overallDistance.toDouble()).toString()
            )
            f.glider?.let { gliderName ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GliderIcon(
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = gliderName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            f.firstPilot?.let {
                FlightDetailRow(Icons.Default.Person, it)
            }
            f.secondPilot?.let {
                FlightDetailRow(Icons.Default.Person, it)
            }
            f.name?.let {
                FlightDetailRow(Icons.Default.Notes, it)
            }
        }
    }
}
