package com.dunihuliapps.myglidingassistnat.presentation.screens.flights_screen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import dagger.hilt.android.AndroidEntryPoint
import presentation.screens.flight_details_screen.FlightDetailsActivity

@AndroidEntryPoint
class FlightsListScreen : ComponentActivity() {

    private val viewModel: FlightsListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FlightsListContent(
                    viewModel = viewModel,
                    onFlightClick = { flight ->
                        startActivity(
                            Intent(this, FlightDetailsActivity::class.java)
                                .putExtra("trip_id", flight.id)
                        )
                    },
                    onUploadConfirmed = {
                        setResult(RESULT_OK, intent)
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}