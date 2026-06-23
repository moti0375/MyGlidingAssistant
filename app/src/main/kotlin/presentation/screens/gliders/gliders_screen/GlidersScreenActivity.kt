package presentation.screens.gliders.gliders_screen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import presentation.screens.gliders.edit_glider_screen.EditGliderActivity

@AndroidEntryPoint
class GlidersActivity : ComponentActivity() {
    private val viewModel by viewModels<GlidersViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlidersScreen(viewModel) { glider ->
                startActivity(EditGliderActivity.newIntent(this@GlidersActivity, glider))
            }
        }
    }
}