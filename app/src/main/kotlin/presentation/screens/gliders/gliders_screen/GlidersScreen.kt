package com.dunihuliapps.myglidingassistnat.presentation.screens.gliders.gliders_screen

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dunihuliapps.myglidingassistnat.data.model.Glider
import com.dunihuliapps.myglidingassistnat.presentation.composables.EmptyGlidersContent
import com.dunihuliapps.myglidingassistnat.presentation.composables.GliderListItem

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun GlidersScreen(
    viewModel: GlidersViewModel,
    onAddClick: (glider: Glider?) -> Unit
) {
    val gliders by viewModel.gliders.collectAsState()

    var selectedGliderForDelete by remember { mutableStateOf<Glider?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && selectedGliderForDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedGliderForDelete = null
            },
            title = { Text("Delete Glider") },
            text = { Text("Are you sure you want to delete ${selectedGliderForDelete?.callsign}?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedGliderForDelete?.let {
                        viewModel.deleteGlider(it)
                    }
                    showDeleteDialog = false
                    selectedGliderForDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    selectedGliderForDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("My Gliders") },
                actions = {
                    if (selectedGliderForDelete != null) {
                        // Delete Icon (Contextual Action)
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        // Clear Selection Icon
                        IconButton(onClick = { selectedGliderForDelete = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                        }
                    } else {
                        IconButton(onClick = {
                            onAddClick(null)
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (gliders.isEmpty()) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
                EmptyGlidersContent{
                    onAddClick(null)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(gliders) { glider ->
                    GliderListItem(
                        glider,
                        onEditClick = {
                            if (selectedGliderForDelete == null) onAddClick(glider)
                            else selectedGliderForDelete = null
                        },
                        onLongClick = {
                            selectedGliderForDelete = it
                        })
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp
                    )
                }
            }

        }

    }
}