package presentation.screens.splash_screen

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistnat.presentation.screens.splash_screen.PermissionsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import presentation.screens.MainActivity
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

@AndroidEntryPoint
class SplashScreenActivity : AppCompatActivity() {

    private val permissionsViewModel: PermissionsViewModel by viewModels()
    private var attachmentFile: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        attachmentFile = getExternalFilesDir(null).toString() + "/attachment.kml"

        setContent {
            MaterialTheme {
                SplashScreenContent(
                    viewModel = permissionsViewModel,
                    onNavigateToMain = { lifecycleScope.launch { navigateToMain() } },
                    onFinish = { finish() }
                )
            }
        }
    }

    private suspend fun navigateToMain() {
        if (intent.scheme == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        if (intent.scheme == "file") {
            val path = intent.data?.path
            startActivity(Intent(this, MainActivity::class.java).apply {
                path?.let { putExtra("kml_path", it) }
            })
            finish()
            return
        }

        if (intent.scheme == "content") {
            val uri = intent.data ?: return
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    inputStream.close()
                    val destFile = attachmentFile ?: return
                    val success = withContext(Dispatchers.IO) {
                        try {
                            contentResolver.openInputStream(uri)?.use { input ->
                                val bis = BufferedInputStream(input)
                                FileOutputStream(destFile).use { output ->
                                    val buffer = ByteArray(8192)
                                    var read: Int
                                    while (bis.read(buffer).also { read = it } != -1) {
                                        output.write(buffer, 0, read)
                                    }
                                }
                            }
                            true
                        } catch (e: IOException) {
                            e.printStackTrace()
                            false
                        }
                    }
                    if (success) {
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            putExtra("kml_path", destFile)
                        })
                        finish()
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                Toast.makeText(this, getString(R.string.error_access_attachment), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
