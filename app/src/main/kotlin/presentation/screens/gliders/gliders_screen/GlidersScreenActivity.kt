package com.dunihuliapps.myglidingassistnat.presentation.screens.gliders.gliders_screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GlidersActivity : ComponentActivity() {
    private val viewModel by viewModels<GlidersViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlidersScreen(viewModel){
               // startActivity(GliderActivity.newIntent(this))
            }
        }
    }
}