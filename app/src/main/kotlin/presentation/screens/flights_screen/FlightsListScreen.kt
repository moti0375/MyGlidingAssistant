package com.dunihuliapps.myglidingassistnat.presentation.screens.flights_screen

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistant.databinding.LayoutRecordedFlightsBinding
import com.dunihuliapps.myglidingassistnat.adapters.TripsRecyclerAdapter
import com.dunihuliapps.myglidingassistnat.domain.db.TripsDataSource
import com.dunihuliapps.myglidingassistnat.presentation.screens.trip_details_screen.TripDetailsActivity
import com.dunihuliapps.myglidingassistnat.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import data.model.Trip
import java.io.File
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class FlightsListScreen : AppCompatActivity(), MultiChoiceModeListener {
    private var settings: SharedPreferences? = null
    private var listener: OnSharedPreferenceChangeListener? = null
    var position: Int = -1
    private var selectedTrip: Trip? = null
    var actionMode: ActionMode? = null
    var itemsCount: Int = 0


    @Inject
    lateinit var datasource: TripsDataSource

    private val trips = mutableListOf<Trip>()
    private lateinit var tvTripsSummary: TextView
    private val selectedTrips = mutableListOf<Trip>()
    private lateinit var tripsRecyclerAdapter: TripsRecyclerAdapter


    private var mergePd: ProgressDialog? = null


    private lateinit var binding: LayoutRecordedFlightsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutRecordedFlightsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBar) { view, insets ->
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

        tripsRecyclerAdapter = TripsRecyclerAdapter()

        binding.tripsRecyclerView.addOnItemTouchListener(
            RecyclerTouchListener(
                this@FlightsListScreen,
                binding.tripsRecyclerView,
                this@FlightsListScreen,
                object : ClickListener {
                    override fun onClick(v: View?, position: Int) {
                        //   Toast.makeText(GpsRecTripsList.this, "RecyclerView item " + position + " clicked..", Toast.LENGTH_SHORT).show();
                        if (actionMode != null) {
                            recyclerToggleSelection(position)
                        } else {
                            this@FlightsListScreen.startTabsActivity(trips!!.get(position))
                        }
                    }

                    override fun onLongClick(v: View?, position: Int) {
                        //Longclick is handled by the onLongPress of the RecyclerTouchListener down in this activity..
                    }
                })
        )
        binding.tripsRecyclerView.apply {
            setLayoutManager(LinearLayoutManager(this@FlightsListScreen))
            setCameraDistance(100f)
            setAdapter(tripsRecyclerAdapter)
        }

        settings = getSharedPreferences("GPS_TRIP_RECORDER", MODE_PRIVATE)
        listener =
            OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences?, key: String? -> this@FlightsListScreen.refreshDisplay() }
        settings!!.registerOnSharedPreferenceChangeListener(listener)

        refreshDisplay()
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

    var itemListener: OnItemClickListener = object : OnItemClickListener {
        override fun onItemClick(
            parent: AdapterView<*>?, view: View?, position: Int,
            id: Long
        ) {
            this@FlightsListScreen.position = position
            selectedTrip = trips!!.get(position)
            this@FlightsListScreen.startTabsActivity(selectedTrip!!)
        }
    }

    fun startTabsActivity(selectedTrip: Trip) {
        val myIntent = Intent(this, TripDetailsActivity::class.java)
        myIntent.putExtra("trip_id", selectedTrip.id)
        startActivity(myIntent)
    }


    fun refreshDisplay() {
//        Log.i(LOG_TAG, "refreshDisplay was called");

        actionMode?.finish()

        trips.let {
            it.clear()
            it.addAll(datasource.findAll())
        }

        Log.i(LOG_TAG, "refreshDisplay: $trips")
        val size = trips.size

        //        Log.i(LOG_TAG, "Got " + size + " trips for database");
        if (size == 0) {
            binding.tvTripsListSummary.text = getResources().getString(
                R.string.NoTripsHint
            )
        } else {
            binding.tvTripsListSummary.text = if (size > 1) (size.toString() + " "
                    + getResources().getString(R.string.Trips)) else (size.toString() + " "
                    + getResources().getString(R.string.Trip))
        }

        //        tripsListAdapter = new TripsListAdapter(this, trips);
        tripsRecyclerAdapter.updateTrips(trips)

        //   tripListView.setAdapter(tripsListAdapter);

//        if (position != -1) {
//            tripListView.setSelection(position);
//        }
    }

    override fun onResume() {
        super.onResume()

        //	refreshDisplay();
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
                getResources().getString(R.string.YES),
                object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, id: Int) {
                        for (trip in selectedTrips!!) {
                            datasource!!.removeSavedTrip(trip)
                            datasource!!.deleteMarkersForTrip(trip.id.toDouble())
                        }
                        refreshDisplay()
                        dialog.cancel()
                    }
                })
            .setNegativeButton(
                getResources().getString(R.string.NO),
                object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, id: Int) {
                        dialog.cancel()
                    }
                })

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog?.apply {
            setTitle(getString(R.string.app_name))
            setIcon(getResources().getDrawable(R.mipmap.ic_launcher))
            show()
        }
    }


    fun userInputDialog() {
        val alert = AlertDialog.Builder(this)
        alert.setIcon(getResources().getDrawable(R.mipmap.ic_launcher))
        alert.setTitle(getResources().getString(R.string.TripTitleDialogTitle))
        alert.setMessage(getResources().getString(R.string.EnterTripTitle))
        val layout = LinearLayout(this)
        val parms = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layout.setOrientation(LinearLayout.VERTICAL)
        layout.setLayoutParams(parms)

        // Set an EditText view to get user input
        val input = EditText(this)
        val chars = TextView(this)

        input.setMaxLines(1)
        input.setFilters(
            arrayOf<InputFilter>(
                LengthFilter(
                    TRIP_NAME_MAX_LENGTH
                )
            )
        )

        chars.setPadding(5, 0, 0, 2)
        if (selectedTrips!!.get(0).tripName != null) {
            chars.setText(selectedTrips!!.get(0).tripName!!.length.toString() + "/" + TRIP_NAME_MAX_LENGTH)
            input.setText(selectedTrips!!.get(0).tripName)
        } else {
            chars.setText("0/" + TRIP_NAME_MAX_LENGTH)
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
            val value = input.getText().toString()
            // Toast.makeText(context, value + " entered..",
            // Toast.LENGTH_LONG).show();
            val status = datasource.updateTripTitle(selectedTrips[0], value)
            if (status) {
                // Toast.makeText(context, "Saved..",
                // Toast.LENGTH_LONG).show();
                refreshDisplay()
            }
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
        tripsRecyclerAdapter.clearSelection()
        selectedTrips.clear()
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
            R.id.action_edit_trip -> userInputDialog()
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
        selectedTrips.clear()
        tripsRecyclerAdapter.clearSelection()
    }


    //    };


    //    private class MergeTripsLongOperation extends AsyncTask<String, Void, String> {
    //
    //        @Override
    //        protected String doInBackground(String... params) {
    //
    //            int MergeStatus = com.bartovapps.gpstriprec.presentation.screens.main_screen.TripManager.mergeTrips(selectedTrips.get(1), selectedTrips.get(0), GpsRecTripsList.this, datasource);
    //            String result = new String();
    //            switch (MergeStatus) {
    //                case MERGE_SUCCESS:
    //                    result = getResources().getString(R.string.Completed);
    //                    break;
    //                case UNABLE_TO_MERGE:
    //                    result = getResources().getString(R.string.ContinuousMergerTrips);
    //                    break;
    //                case KML_NOT_FOUND:
    //                    result = "Trips details cannot be found!, Merge Failed!";
    //            }
    //            return result;
    //        }
    //
    //        @Override
    //        protected void onPostExecute(String result) {
    //            Toast.makeText(GpsRecTripsList.this, result, Toast.LENGTH_SHORT).show();
    //            mergePd.dismiss();
    //            actionMode.finish();
    //            refreshDisplay();
    //        }
    //
    //        @Override
    //        protected void onPreExecute() {
    //        }
    //
    //        @Override
    //        protected void onProgressUpdate(Void... values) {
    //        }
    //    }
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
                    dialog!!.dismiss()
                    //								Toast.makeText(GpsRecTripsList.this, "Uploading this trip...", Toast.LENGTH_LONG).show();
                    val intent = this@FlightsListScreen.intent
                    //intent.putExtra("UploadedTrip", selectedTrips.get(0));
                    this@FlightsListScreen.setResult(RESULT_OK, intent)
                    finish()
                })
            .setNegativeButton(
                getResources().getString(R.string.NO),
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                    dialog!!.dismiss()
                    actionMode!!.finish()
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
        private val LOG_TAG: String = RecyclerTouchListener::class.java.getSimpleName()
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
                    Log.i(LOG_TAG, "onLongPress was invoked..: " + e)
                    val childView = recyclerView.findChildViewUnder(e.getX(), e.getY())
                    if (actionMode != null) {
                        return
                    } else {
                        actionMode = context.startActionMode(actionModeCallback)
                        Log.i(LOG_TAG, "actionMode = " + actionMode)
                        //                        int idx = recyclerView.getChildPosition(childView);   //this method was deprecated and caused app to crash! replaced with the one below..
                        val idx = recyclerView.getChildLayoutPosition(childView!!)
                        Log.i(LOG_TAG, "indx = " + idx)
                        recyclerToggleSelection(idx)
                    }

                    super.onLongPress(e)
                }
            })
        }


        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
//            Log.i(LOG_TAG, "onInterceptTouchEvent was called: " + gestureDetector.onTouchEvent(e));
            val child = rv.findChildViewUnder(e.getX(), e.getY())
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e) == true) {
                // clickListener.onClick(child, rv.getChildPosition(child));
                clickListener!!.onClick(child, rv.getChildAdapterPosition(child))
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
        tripsRecyclerAdapter!!.toggleSelection(idx)
        actionMode?.let { actMode ->
            itemsCount = tripsRecyclerAdapter!!.getSelectedItemsCount()

            if (itemsCount == 0) {
                actMode.finish()
            } else {
                val title = "Selected $itemsCount"
                actMode.title = title

                //selectedTrip = trips.get(idx);
                selectedTrips?.apply {
                    clear()
                    addAll(tripsRecyclerAdapter!!.getSelectedItems())
                }
                Log.i(LOG_TAG, "Selected " + selectedTrips!!.size + " trips for action..")

                if (itemsCount == 1) {
                    position = -1
                    actMode.apply {
                        menu.findItem(R.id.action_edit_trip).isVisible = true
                        menu.findItem(R.id.action_upload_trip).isVisible = true
                    }
                } else {
                    actMode.apply {
                        menu.findItem(R.id.action_edit_trip).isVisible = false
                        menu.findItem(R.id.action_upload_trip).isVisible = false
                    }
                }

                if (itemsCount == 2) {
                    actMode.menu.findItem(R.id.action_merge_trips).isVisible = true
                } else {
                    actMode.menu.findItem(R.id.action_merge_trips).isVisible = false
                }

                this@FlightsListScreen.invalidateOptionsMenu()
            }
            Log.v(LOG_TAG, "actionMode is not null")

        } ?: Log.v(LOG_TAG, "actionMode is null")
    }

    interface ClickListener {
        fun onClick(v: View?, position: Int)

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