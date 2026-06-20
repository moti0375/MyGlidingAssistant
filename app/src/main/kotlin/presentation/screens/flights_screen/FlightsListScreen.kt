package com.dunihuliapps.myglidingassistnat.presentation.screens.flights_screen

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.util.Log
import android.view.ActionMode
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistant.databinding.LayoutRecordedFlightsBinding
import dagger.hilt.android.AndroidEntryPoint
import data.model.Flight
import kotlinx.coroutines.launch
import presentation.screens.flight_details_screen.FlightDetailsActivity

@AndroidEntryPoint
class FlightsListScreen : AppCompatActivity(), MultiChoiceModeListener {

    private val viewModel by viewModels<FlightsListViewModel>()

    private var settings: SharedPreferences? = null
    var position: Int = -1
    var actionMode: ActionMode? = null
    var itemsCount: Int = 0

    private lateinit var flightsListAdapter: FlightsListAdapter

    private lateinit var binding: LayoutRecordedFlightsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutRecordedFlightsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Use padding instead of margins to handle the status bar area
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        setSupportActionBar(binding.appBar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = null
        }

        flightsListAdapter = FlightsListAdapter()

        binding.tripsRecyclerView.addOnItemTouchListener(
            RecyclerTouchListener(
                this@FlightsListScreen,
                binding.tripsRecyclerView,
                this@FlightsListScreen,
                object : ClickListener {
                    override fun onClick(flight: Flight) {
                        //   Toast.makeText(GpsRecTripsList.this, "RecyclerView item " + position + " clicked..", Toast.LENGTH_SHORT).show();
                        if (actionMode != null) {
                            recyclerToggleSelection(position)
                        } else {
                            this@FlightsListScreen.navigateToFlightDetailsScreen(flight)
                        }
                    }

                    override fun onLongClick(v: View?, position: Int) {
                        //Longclick is handled by the onLongPress of the RecyclerTouchListener down in this activity..
                    }
                })
        )
        binding.tripsRecyclerView.apply {
            setLayoutManager(LinearLayoutManager(this@FlightsListScreen))
            cameraDistance = 100f
            setAdapter(flightsListAdapter)
        }

        settings = getSharedPreferences("GPS_TRIP_RECORDER", MODE_PRIVATE)
        listenToState()
    }

    private fun listenToState() {
        observeScreenStateFlow()
        observeUpdateFlightFlow()
    }

    private fun observeScreenStateFlow() {
        lifecycleScope.launch {
            viewModel.state.collect {
                when (it) {
                    is FlightsListState.Initiated -> {
                        viewModel.mapEventToState(FlightsListEvent.GetAllFlights)
                    }

                    is FlightsListState.Loading -> {

                    }

                    is FlightsListState.FlightsLoaded -> {
                        refreshDisplay(it.flights)
                    }
                }
            }

        }

    }

    private fun observeUpdateFlightFlow() {
        lifecycleScope.launch {
            viewModel.editSelectedFlight.collect { selectedFlight ->
                selectedFlight?.let {
                    userInputDialog(selectedFlight)
                }
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
        }

        return true
    }


    override fun onDestroy() {
        position = -1
        //        Log.i(LOG_TAG, "onDestroy was called");
        super.onDestroy()
    }

    fun navigateToFlightDetailsScreen(selectedFlight: Flight) {
        val myIntent = Intent(this, FlightDetailsActivity::class.java)
        myIntent.putExtra("trip_id", selectedFlight.id)
        startActivity(myIntent)
    }


    fun refreshDisplay(flights: List<Flight>) {
//        Log.i(LOG_TAG, "refreshDisplay was called");
        actionMode?.finish()
        Log.i(LOG_TAG, "refreshDisplay: $flights")

        if (flights.isEmpty()) {
            binding.tripsRecyclerView.visibility = View.GONE
            binding.noFlightsLayout.root.visibility = View.VISIBLE
        } else {
            binding.tripsRecyclerView.visibility = View.VISIBLE
            binding.noFlightsLayout.root.visibility = View.GONE
        }
        flightsListAdapter.updateTrips(flights)
    }


    fun deleteTripAlertDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)

        // set title
        alertDialogBuilder.setTitle(
            getResources().getString(
                R.string.DELETE_TRIP
            )
        )

        // set dialog message
        alertDialogBuilder
            .setMessage(getResources().getString(R.string.DeleteDialog))
            .setCancelable(false)
            .setPositiveButton(
                getResources().getString(R.string.YES)
            ) { dialog, _ ->
                viewModel.mapEventToState(FlightsListEvent.DeleteSelectedFlights)
                dialog.cancel()
            }
            .setNegativeButton(
                getResources().getString(R.string.NO)
            ) { dialog, id -> dialog.cancel() }

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog?.apply {
            setTitle(getString(R.string.app_name))
            setIcon(getResources().getDrawable(R.mipmap.ic_launcher))
            show()
        }
    }


    fun userInputDialog(flight: Flight) {
        val alert = AlertDialog.Builder(this)
        alert.setIcon(getResources().getDrawable(R.mipmap.ic_launcher))
        alert.setTitle(getResources().getString(R.string.TripTitleDialogTitle))
        alert.setMessage(getResources().getString(R.string.EnterTripTitle))
        val layout = LinearLayout(this)
        val parms = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layout.orientation = LinearLayout.VERTICAL
        layout.layoutParams = parms

        // Set an EditText view to get user input
        val input = EditText(this)
        val chars = TextView(this)

        input.maxLines = 1
        input.filters = arrayOf<InputFilter>(
            LengthFilter(
                TRIP_NAME_MAX_LENGTH
            )
        )

        chars.setPadding(5, 0, 0, 2)
        flight.name?.let {
            input.setText("${it.length} / $TRIP_NAME_MAX_LENGTH")
        } ?: run {
            chars.text = "0/$TRIP_NAME_MAX_LENGTH"
            input.setText("")
        }
        input.addTextChangedListener(object : TextWatcher {
            // StringBuilder builder = new StringBuilder();
            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int,
                count: Int
            ) {
                val text = input.getText().toString()
                chars.setText(text.length.toString() + "/" + TRIP_NAME_MAX_LENGTH)
            }

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        val tv1Params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        tv1Params.bottomMargin = 5
        layout.addView(
            input, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        layout.addView(chars, tv1Params)
        alert.setView(layout)
        alert.setPositiveButton(
            getResources().getString(R.string.Done)
        ) { dialog, whichButton ->
            val value = input.text.toString()
            // Toast.makeText(context, value + " entered..",
            // Toast.LENGTH_LONG).show();
            viewModel.mapEventToState(FlightsListEvent.UpdateFlightName(flight, value))
        }

        alert.setNegativeButton(
            getResources().getString(R.string.Cancel)
        ) { dialog, whichButton ->
            // Canceled.
        }

        alert.setTitle(getString(R.string.app_name))
        alert.setIcon(getResources().getDrawable(R.drawable.ic_launcher))
        alert.show()
    }


    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        //      actionMode = mode;
        return true
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
        mode.menuInflater.inflate(R.menu.trips_list_actionbar, menu)
        flightsListAdapter.clearSelection()
        viewModel.mapEventToState(FlightsListEvent.ClearSelectedFlight)
        return true
    }

    override fun onItemCheckedStateChanged(
        mode: ActionMode?, position: Int,
        id: Long, checked: Boolean
    ) {
    }


    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete_trip -> deleteTripAlertDialog()
            R.id.action_edit_trip -> viewModel.mapEventToState(FlightsListEvent.EditFlightClicked)
            R.id.action_upload_trip -> {
                position = -1
                uploadFlightDialog()
            }
        }

        //  mode.finish();
        //	refreshDisplay();
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        actionMode = null
        viewModel.mapEventToState(FlightsListEvent.ClearSelectedFlight)
        flightsListAdapter.clearSelection()
    }


    //    };


    fun uploadFlightDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)

        // set title
        alertDialogBuilder.setTitle(
            getResources().getString(
                R.string.app_name
            )
        )

        // set dialog message
        alertDialogBuilder
            .setMessage(getResources().getString(R.string.UploadTrip))
            .setCancelable(true).setPositiveButton(
                getResources().getString(R.string.YES),
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                    dialog?.dismiss()
                    //								Toast.makeText(GpsRecTripsList.this, "Uploading this trip...", Toast.LENGTH_LONG).show();
                    val intent = this@FlightsListScreen.intent
                    //intent.putExtra("UploadedTrip", selectedTrips.get(0));
                    this@FlightsListScreen.setResult(RESULT_OK, intent)
                    finish()
                })
            .setNegativeButton(
                getResources().getString(R.string.NO),
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                    dialog?.dismiss()
                    actionMode?.finish()
                })

        // create alert dialog
        alertDialogBuilder.create().apply {
            setTitle(getString(R.string.app_name))
            setIcon(getResources().getDrawable(R.mipmap.ic_launcher))
            show()
        }
    }


    internal inner class RecyclerTouchListener(
        context: Activity,
        recyclerView: RecyclerView,
        actionModeCallback: ActionMode.Callback?,
        clickListener: ClickListener?
    ) : OnItemTouchListener {
        private val LOG_TAG: String = RecyclerTouchListener::class.java.simpleName
        var gestureDetector: GestureDetector
        var clickListener: ClickListener?

        init {
            Log.i(LOG_TAG, "constructor was invoked")
            this.clickListener = clickListener

            gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
//                    Log.i(LOG_TAG, "onSingleTapUp was invoked..: " + e);
//                    return super.onSingleTapUp(e);
                    return true
                }


                override fun onLongPress(e: MotionEvent) {
                    Log.i(LOG_TAG, "onLongPress was invoked..: $e")
                    val childView = recyclerView.findChildViewUnder(e.x, e.y)
                    if (actionMode != null) {
                        return
                    } else {
                        actionMode = context.startActionMode(actionModeCallback)
                        Log.i(LOG_TAG, "actionMode = $actionMode")
                        //                        int idx = recyclerView.getChildPosition(childView);   //this method was deprecated and caused app to crash! replaced with the one below..
                        val idx = recyclerView.getChildLayoutPosition(childView!!)
                        Log.i(LOG_TAG, "indx = $idx")
                        recyclerToggleSelection(idx)
                    }

                    super.onLongPress(e)
                }
            })
        }


        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            val child = rv.findChildViewUnder(e.x, e.y)
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                // clickListener.onClick(child, rv.getChildPosition(child));
                position = rv.getChildAdapterPosition(child)
                val flight = flightsListAdapter.getItemAtPosition(rv.getChildAdapterPosition(child))
                clickListener?.onClick(flight )
            }
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
            Log.i(LOG_TAG, "onTouchEvent was called: " + e)
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        }
    }

    private fun recyclerToggleSelection(idx: Int) {
        flightsListAdapter.toggleSelection(idx)
        actionMode?.let { actMode ->
            itemsCount = flightsListAdapter.selectedItemsCount

            if (itemsCount == 0) {
                actMode.finish()
            } else {
                val title = "Selected $itemsCount"
                actMode.title = title

                //selectedTrip = trips.get(idx);
                val selectedFlights = flightsListAdapter.getSelectedItems()

                if (itemsCount > 1) {
                    actMode.apply {
                        menu.findItem(R.id.action_edit_trip).isVisible = false
                        menu.findItem(R.id.action_upload_trip).isVisible = false
                    }
                } else {
                    position = -1
                    actMode.apply {
                        menu.findItem(R.id.action_edit_trip).isVisible = true
                        menu.findItem(R.id.action_upload_trip).isVisible = true
                    }
                }

                viewModel.mapEventToState(FlightsListEvent.UpdateSelectedFlights(selectedFlights))

                this@FlightsListScreen.invalidateOptionsMenu()
            }
            Log.v(LOG_TAG, "actionMode is not null")

        } ?: Log.v(LOG_TAG, "actionMode is null")
    }

    interface ClickListener {
        fun onClick(flight: Flight)

        fun onLongClick(v: View?, position: Int)
    }


    companion object {
        const val USERNAME: String = "pref_username"
        const val VIEWIMAGE: String = "pref_viewimages"
        private val LOG_TAG: String = FlightsListScreen::class.java.getSimpleName()

        const val TRIPS_DB: String = "trips.db"
        const val TRIPS_DB_JOURNAL: String = "trips.db-journal"
        const val DB_ROOT: String = "/data/com.bartovapps.gpstriprec/databases/"
        const val TRIP_NAME_MAX_LENGTH: Int = 25
    }
}