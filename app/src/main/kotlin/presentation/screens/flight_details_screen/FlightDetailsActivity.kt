package presentation.screens.flight_details_screen

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistnat.utils.Utils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlightDetailsActivity : AppCompatActivity() {

    private val viewModel: FlightDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tripId = intent.getLongExtra("trip_id", 0)

        setContent {
            MaterialTheme {
                FlightDetailsContent(
                    viewModel = viewModel,
                    tripId = tripId,
                    onBack = { finish() },
                    onShareMapImage = { bitmap, imageFileName, tripTitle ->
                        shareImage(bitmap, imageFileName, tripTitle)
                    }
                )
            }
        }
    }

    private fun shareImage(bitmap: Bitmap, imageFileName: String, tripTitle: String?) {
        val icon = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher)
        val image = Utils.overlay(bitmap, icon, getString(R.string.app_name))

        val share = Intent(Intent.ACTION_SEND).apply { type = "image/png" }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, tripTitle ?: getString(R.string.app_name))
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TripRecorder")
        }

        val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        try {
            uri?.let {
                contentResolver.openOutputStream(it)?.use { os ->
                    image?.compress(Bitmap.CompressFormat.PNG, 100, os)
                    image?.recycle()
                }
                share.putExtra(Intent.EXTRA_STREAM, it)
                startActivity(Intent.createChooser(share, getString(R.string.share_map_image)))
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to share map image", e)
            Toast.makeText(this, "Failed to share map image", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val LOG_TAG = "FlightDetailsActivity"
    }
}