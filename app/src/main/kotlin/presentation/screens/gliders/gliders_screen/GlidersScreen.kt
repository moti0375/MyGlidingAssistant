package com.dunihuliapps.myglidingassistnat.presentation.screens.gliders.gliders_screen

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dunihuliapps.myglidingassistnat.data.model.Glider

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun GlidersScreen(
    viewModel: GlidersViewModel,
    onAddClick: (glider: Glider?) -> Unit
) {
    val gliders by viewModel.gliders.collectAsState()

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text("My Gliders") },
                actions = {
                    androidx.compose.material3.IconButton(onClick = {
                        onAddClick(null)
                    }) {
                        androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = "Add Glider")
                        Log.i("GlidersScreen", "IconButton clicked")
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(modifier = androidx.compose.ui.Modifier.padding(padding)) {
            items(gliders) { glider ->
                androidx.compose.material3.ListItem(headlineContent = { androidx.compose.material3.Text(glider.callsign) })
            }
        }
    }
}