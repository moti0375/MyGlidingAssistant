package presentation.composables
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistnat.data.model.Glider
import java.io.File

@Composable
fun GliderListItem(
    glider: Glider,
    onEditClick: (Glider) -> Unit,
    onLongClick: (Glider) -> Unit
) {
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = { onEditClick(glider) },
            onLongClick = { onLongClick(glider) }
        ),
        leadingContent = {
            // Circle Image Prefix
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (glider.gliderImage != null) {
                    AsyncImage(
                        model = File(glider.gliderImage),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_glider_icon),
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        headlineContent = {
            // Upper Row: Callsign
            Text(
                text = glider.callsign,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        },
        supportingContent = {
            // Lower Row: Type
            Text(
                text = glider.type,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            // Leading Forward Arrow (Standard Action Icon)
            IconButton(onClick = { onEditClick(glider) }) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit Glider",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    )
}