package com.dunihuliapps.myglidingassistnat.presentation.screens.gliders.edit_glider_screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import com.dunihuliapps.myglidingassistnat.data.model.Glider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditGliderActivity : ComponentActivity() {
    private val viewModel by viewModels<EditGliderViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EditGliderScreen(
                viewModel = viewModel,
                onSaved = { finish() }
            )
        }
    }

    companion object{
        fun newIntent(context: Context, glider: Glider?) : Intent {
            val intent = Intent(context, EditGliderActivity::class.java).apply {
                glider?.let {
                    putExtra("type", it.type)
                    putExtra("callsign", it.callsign)
                    putExtra("seats", it.seats)
                    putExtra("ratio", it.ratio)
                    putExtra("image", it.gliderImage)
                }
            }
            return intent
        }
    }
}