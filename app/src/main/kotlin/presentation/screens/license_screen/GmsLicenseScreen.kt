package presentation.screens.license_screen
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistant.databinding.GooglemapLicenseBinding

class GmsLicenseScreen : AppCompatActivity() {
    var toolbar: Toolbar? = null

    lateinit var binding: GooglemapLicenseBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GooglemapLicenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setLogo(R.drawable.ic_launcher)
            it.setDisplayUseLogoEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_action_back)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return true
    }
}