package com.bartovapps.gpstriprec;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bartovapps.gpstriprec.adapters.TripsListAdapter;
import com.bartovapps.gpstriprec.adapters.TripsRecyclerAdapter;
import com.bartovapps.gpstriprec.core.db.TripsDataSource;
import com.bartovapps.gpstriprec.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import data.model.Trip;

@AndroidEntryPoint
public class GpsRecTripsList extends AppCompatActivity implements MultiChoiceModeListener {
    public static final String USERNAME = "pref_username";
    public static final String VIEWIMAGE = "pref_viewimages";
    private static final String LOG_TAG = GpsRecTripsList.class.getSimpleName();

    public static final String TRIPS_DB = "trips.db";
    public static final String TRIPS_DB_JOURNAL = "trips.db-journal";
    public static final String DB_ROOT = "/data/com.bartovapps.gpstriprec/databases/";
    public static final int TRIP_NAME_MAX_LENGTH = 25;

    private SharedPreferences settings;
    private OnSharedPreferenceChangeListener listener;
    public int position = -1;
    private Trip selectedTrip;
    ActionMode actionMode;
    int itemsCount = 0;


    @Inject
    TripsDataSource datasource;
    List<Trip> trips;
    TextView tvTripsSummary;
    List<Trip> selectedTrips;
    RecyclerView tripsRecyclerView;
    TripsRecyclerAdapter tripsRecyclerAdapter;


    ProgressDialog mergePd;
    Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recorderd_trips);
        toolbar = findViewById(R.id.app_bar);

        try {
            setSupportActionBar(toolbar);
        } catch (Throwable t) {
            // WTF SAMSUNG!
        }
        getSupportActionBar().setLogo(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_back);
        //getSupportActionBar().setHomeButtonEnabled(true);


        //getActionBar().hide();
        tvTripsSummary = (TextView) findViewById(R.id.tvTripsListSummary);
        tripsRecyclerView = (RecyclerView) findViewById(R.id.tripsRecyclerView);
        tripsRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(GpsRecTripsList.this, tripsRecyclerView, GpsRecTripsList.this, new ClickListener() {
            @Override
            public void onClick(View v, int position) {
             //   Toast.makeText(GpsRecTripsList.this, "RecyclerView item " + position + " clicked..", Toast.LENGTH_SHORT).show();
                if (actionMode != null) {
                    recyclerToggleSelection(position);

                } else {
                    GpsRecTripsList.this.startTabsActivity(trips.get(position));

                }
            }

            @Override
            public void onLongClick(View v, int position) {
                //Longclick is handled by the onLongPress of the RecyclerTouchListener down in this activity..
            }
        }));
        tripsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tripsRecyclerView.setCameraDistance(100);


        // tripListView = (ListView) findViewById(R.id.lvTripList);
//        tripListView.setLongClickable(true);
//        tripListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
//        tripListView.setMultiChoiceModeListener(multiChoiceModeListener);
//        tripListView.setOnItemClickListener(itemListener);

        selectedTrips = new ArrayList<>();

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        listener = new OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPreferences, String key) {
                GpsRecTripsList.this.refreshDisplay();
            }
        };
        settings.registerOnSharedPreferenceChangeListener(listener);

        tripsRecyclerAdapter = new TripsRecyclerAdapter(this, trips);
        tripsRecyclerView.setAdapter(tripsRecyclerAdapter);
        refreshDisplay();
        // AdBuddiz.showAd(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.trips_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }
        if (item.getItemId() == R.id.action_backup_trips) {
            Toast.makeText(this, "Export Trips selected", Toast.LENGTH_SHORT).show();
            exportTrips();
        }

        if (item.getItemId() == R.id.action_import_trips) {
            Toast.makeText(this, "Import Trips selected", Toast.LENGTH_SHORT).show();
            importTrips();
        }

        return true;
    }


    @Override
    protected void onDestroy() {

        setPosition(-1);
//        Log.i(LOG_TAG, "onDestroy was called");
        super.onDestroy();
    }

    OnItemClickListener itemListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            GpsRecTripsList.this.position = position;
            selectedTrip = trips.get(position);
            GpsRecTripsList.this.startTabsActivity(selectedTrip);

        }
    };

    public void startTabsActivity(Trip selectedTrip) {
        Intent myIntent = new Intent(this, TripDetailsActivity.class);
        myIntent.putExtra("trip_id", selectedTrip.getId());
        startActivity(myIntent);
    }


    public void refreshDisplay() {
//        Log.i(LOG_TAG, "refreshDisplay was called");

        if (actionMode != null) {
            actionMode.finish();
        }

        if (trips != null) {
            trips.clear();
        }
        datasource.open();
        trips = datasource.findAll();
        datasource.close();
        Log.i(LOG_TAG, "refreshDisplay: " + trips);

        int size = trips.size();
//        Log.i(LOG_TAG, "Got " + size + " trips for database");

        if (size == 0) {
            tvTripsSummary.setText(getResources().getString(
                    R.string.NoTripsHint));
        } else {
            tvTripsSummary.setText(size > 1 ? size + " "
                    + getResources().getString(R.string.Trips) : size + " "
                    + getResources().getString(R.string.Trip));
        }

//        tripsListAdapter = new TripsListAdapter(this, trips);
        tripsRecyclerAdapter.updateTrips(trips);

        //   tripListView.setAdapter(tripsListAdapter);

//        if (position != -1) {
//            tripListView.setSelection(position);
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //	refreshDisplay();

    }


    public void deleteTripAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(getResources().getString(
                R.string.DELETE_TRIP));

        // set dialog message
        alertDialogBuilder
                .setMessage(getResources().getString(R.string.DeleteDialog))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.YES),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                datasource.open();
                                for (Trip trip : selectedTrips) {
                                    datasource.removeSavedTrip(trip);
                                    datasource.deleteMarkersForTrip(trip.getId());
                                }
                                datasource.close();
                                refreshDisplay();
                                dialog.cancel();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.NO),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.setTitle(getString(R.string.app_name));
        alertDialog.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
        alertDialog.show();
    }


    public void userInputDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
        alert.setTitle(getResources().getString(R.string.TripTitleDialogTitle));
        alert.setMessage(getResources().getString(R.string.EnterTripTitle));
        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(parms);

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        final TextView chars = new TextView(this);

        input.setMaxLines(1);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(
                TRIP_NAME_MAX_LENGTH)});

        chars.setPadding(5, 0, 0, 2);
        if (selectedTrips.get(0).getTripName() != null) {
            chars.setText(selectedTrips.get(0).getTripName().length() + "/" + TRIP_NAME_MAX_LENGTH);
            input.setText(selectedTrips.get(0).getTripName());
        } else {
            chars.setText("0/" + TRIP_NAME_MAX_LENGTH);
            input.setText("");
        }
        input.addTextChangedListener(new TextWatcher() {
            // StringBuilder builder = new StringBuilder();
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                String text = input.getText().toString();
                chars.setText(text.length() + "/" + TRIP_NAME_MAX_LENGTH);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        LinearLayout.LayoutParams tv1Params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tv1Params.bottomMargin = 5;
        layout.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(chars, tv1Params);

        alert.setView(layout);

        alert.setPositiveButton(getResources().getString(R.string.Done),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        // Toast.makeText(context, value + " entered..",
                        // Toast.LENGTH_LONG).show();
                        datasource.open();
                        boolean status = datasource.updateTripTitle(selectedTrips.get(0), value);
                        datasource.close();
                        if (status) {
                            // Toast.makeText(context, "Saved..",
                            // Toast.LENGTH_LONG).show();
                            refreshDisplay();
                        }
                    }
                });

        alert.setNegativeButton(getResources().getString(R.string.Cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

        alert.setTitle(getString(R.string.app_name));
        alert.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
        alert.show();
    }

    private void setPosition(int position) {
        this.position = position;
    }


    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
  //      actionMode = mode;
        return true;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.trips_list_actionbar, menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.primaryColorDark));

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
            params.setMargins(0, 0, 0, 0);
            toolbar.setLayoutParams(params);
        }
        tripsRecyclerAdapter.clearSelection();
        selectedTrips.clear();
        return true;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position,
                                          long id, boolean checked) {
    }


    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_trip:
                deleteTripAlertDialog();
                break;
            case R.id.action_edit_trip:
                userInputDialog();
                break;
            case R.id.action_merge_trips:
                setPosition(-1);
                MergeTripsDialog();
                break;
            case R.id.action_upload_trip:
                setPosition(-1);
                uploadTripDialog();
                break;
        }

        //  mode.finish();
        //	refreshDisplay();
        return true;
    }


    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
        //tripListView.removeAllViews();
        //   tripListView.setAdapter(checkedListAdapter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
            params.setMargins(0, getResources().getDimensionPixelOffset(R.dimen.appBarTopMargin), 0, 0);
            toolbar.setLayoutParams(params);
        }
        selectedTrips.clear();
        tripsRecyclerAdapter.clearSelection();
    }


//    };

    void MergeTripsDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(getResources().getString(
                R.string.app_name));

        // set dialog message
        alertDialogBuilder
                .setMessage(getResources().getString(R.string.MergeTrips))
                .setCancelable(true)
                .setPositiveButton(getResources().getString(R.string.YES),
                        (dialog, id) -> {
                            mergePd = ProgressDialog.show(
                                    GpsRecTripsList.this,
                                    getResources().getString(
                                            R.string.app_name),
                                    getResources().getString(R.string.MergeingTrips));

                            setPosition(-1); //this will prevent list scroll after trip merge
                            mergePd.setCancelable(false);

                            //Todo - Refactor merge trip
//                                new MergeTripsLongOperation().execute("");
                            dialog.dismiss();
                            //mode.finish();
                        })
                .setNegativeButton(getResources().getString(R.string.NO),
                        (dialog, id) -> dialog.dismiss());

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        alertDialog.setTitle(getString(R.string.app_name));
        alertDialog.setIcon(getResources().getDrawable(R.drawable.ic_launcher));

        // show it
        alertDialog.show();

    }

//    private class MergeTripsLongOperation extends AsyncTask<String, Void, String> {
//
//        @Override
//        protected String doInBackground(String... params) {
//
//            int MergeStatus = TripManager.mergeTrips(selectedTrips.get(1), selectedTrips.get(0), GpsRecTripsList.this, datasource);
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


    void uploadTripDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(getResources().getString(
                R.string.app_name));

        // set dialog message
        alertDialogBuilder
                .setMessage(getResources().getString(R.string.UploadTrip))
                .setCancelable(true).setPositiveButton(getResources().getString(R.string.YES),
                        (dialog, id) -> {
                            dialog.dismiss();
    //								Toast.makeText(GpsRecTripsList.this, "Uploading this trip...", Toast.LENGTH_LONG).show();
                            Intent intent = GpsRecTripsList.this.getIntent();
                            //intent.putExtra("UploadedTrip", selectedTrips.get(0));
                            GpsRecTripsList.this.setResult(RESULT_OK, intent);
                            finish();
                        })
                .setNegativeButton(getResources().getString(R.string.NO),
                        (dialog, id) -> {
                            dialog.dismiss();
                            actionMode.finish();
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        alertDialog.setTitle(getString(R.string.app_name));
        alertDialog.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
        // show it
        alertDialog.show();

    }

    private void exportTrips() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(getResources().getString(
                R.string.app_name));

        // set dialog message
        alertDialogBuilder
                .setMessage(getString(R.string.ExportTripsDialogMessage))
                .setCancelable(true)
                .setPositiveButton(getResources().getString(R.string.YES),
                        (dialog, id) -> {
                            Toast.makeText(GpsRecTripsList.this, "Exporting trips!", Toast.LENGTH_SHORT).show();
                            ExportTripsTask importTask = new ExportTripsTask();
                            importTask.execute();
                        })
                .setNegativeButton(getResources().getString(R.string.NO),
                        (dialog, id) -> dialog.dismiss());

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        alertDialog.setTitle(getString(R.string.app_name));
        alertDialog.setIcon(getResources().getDrawable(R.drawable.ic_launcher));

        // show it
        alertDialog.show();

    }

    private void importTrips() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(getResources().getString(
                R.string.app_name));

        // set dialog message
        alertDialogBuilder
                .setMessage(getString(R.string.ImportTripsDialogMessage))
                .setCancelable(true)
                .setPositiveButton(getResources().getString(R.string.YES),
                        (dialog, id) -> {
                            Toast.makeText(GpsRecTripsList.this, "Importing Trips", Toast.LENGTH_SHORT).show();
                            ImportDatabaseTask importDatabaseTask = new ImportDatabaseTask();
                            importDatabaseTask.execute();
                        })
                .setNegativeButton(getResources().getString(R.string.NO),
                        (dialog, id) -> dialog.dismiss());

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setTitle(getString(R.string.app_name));
        alertDialog.setIcon(getResources().getDrawable(R.drawable.ic_launcher));

        // show it
        alertDialog.show();
    }

    private class ExportTripsTask extends AsyncTask<String, Void, Boolean> {


        // can use UI thread here
        protected void onPreExecute() {
//            dialog.show();
        }

        // automatically done on worker thread (separate from UI thread)
        protected Boolean doInBackground(final String... args) {
            String dbFilePath = Environment.getDataDirectory() + DB_ROOT + TRIPS_DB;

            String dbJournalPath = Environment.getDataDirectory() + DB_ROOT + TRIPS_DB_JOURNAL;
//            Log.i(LOG_TAG, "DB File: " + dbFilePath);

            if (!(new File(dbFilePath).exists())) {
//                Log.i(LOG_TAG, "File cannot be found");
            } else {
//                Log.i(LOG_TAG, "db file founded!");

            }

            File dbFile = new File(dbFilePath);
            File dbJournalFile = new File(dbJournalPath);

            File exportDir = new File(Environment.getExternalStorageDirectory(), "");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            File dbBackupFile = new File(exportDir, dbFile.getName());
            File dbJournalBackupFile = new File(exportDir, dbJournalFile.getName());

            try {
                dbBackupFile.createNewFile();
                dbJournalBackupFile.createNewFile();

                Utils.copyFile(dbFile, dbBackupFile);
                Utils.copyFile(dbJournalFile, dbJournalBackupFile);
                return true;
            } catch (IOException e) {
                Log.e("mypck", e.getMessage(), e);
                return false;
            }
        }

        // can use UI thread here
        protected void onPostExecute(final Boolean success) {

            if (success) {
                Toast.makeText(getApplicationContext(), "Export successful!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Export failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ImportDatabaseTask extends AsyncTask<String, Void, Boolean> {


        // can use UI thread here
        protected void onPreExecute() {
//            dialog.show();
        }

        // automatically done on worker thread (separate from UI thread)
        protected Boolean doInBackground(final String... args) {
            String dbFilePath = Environment.getDataDirectory() + DB_ROOT + TRIPS_DB;

            String dbJournalPath = Environment.getDataDirectory() + DB_ROOT + TRIPS_DB_JOURNAL;
//            Log.i(LOG_TAG, "DB File: " + dbFilePath);

            File importDir = new File(Environment.getExternalStorageDirectory(), "");
//            Log.i(LOG_TAG, "External Storage Dir: " + importDir);

            File dbBackupFile = new File(importDir, TRIPS_DB);
            File dbJournalBackupFile = new File(importDir, TRIPS_DB_JOURNAL);

            File dbFile = new File(dbFilePath);
            File dbJournalFile = new File(dbJournalPath);

            if (dbBackupFile.exists() && dbJournalBackupFile.exists()) {
//                Log.i(LOG_TAG, "Backup founded");
                try {
                    dbFile.createNewFile();
                    dbJournalFile.createNewFile();
                    Utils.copyFile(dbBackupFile, dbFile);
                    Utils.copyFile(dbJournalBackupFile, dbJournalFile);
                    return true;
                } catch (IOException e) {
                    Log.e("mypck", e.getMessage(), e);
                    return false;
                }
            } else {
//                Log.i(LOG_TAG, "Can't find backup files: " + dbBackupFile + ", " + dbJournalBackupFile);
                return false;
            }
        }


        // can use UI thread here
        protected void onPostExecute(final Boolean success) {
            if (success) {
                Toast.makeText(getApplicationContext(), "Import successful!", Toast.LENGTH_SHORT).show();
                refreshDisplay();
            } else {
                Toast.makeText(getApplicationContext(), "Import failed", Toast.LENGTH_SHORT).show();
            }
        }
    }


    class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private final String LOG_TAG = RecyclerTouchListener.class.getSimpleName();
        GestureDetector gestureDetector;
        ClickListener clickListener;

        public RecyclerTouchListener(final Activity context, final RecyclerView recyclerView, final android.view.ActionMode.Callback actionModeCallback, final ClickListener clickListener) {
            Log.i(LOG_TAG, "constructor was invoked");
            this.clickListener = clickListener;

            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
//                    Log.i(LOG_TAG, "onSingleTapUp was invoked..: " + e);
//                    return super.onSingleTapUp(e);
                    return true;
                }


                @Override
                public void onLongPress(MotionEvent e) {
                   Log.i(LOG_TAG, "onLongPress was invoked..: " + e);
                    View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (actionMode != null) {
                        return;
                    } else {
                        actionMode = context.startActionMode(actionModeCallback);
                        Log.i(LOG_TAG, "actionMode = " + actionMode);
//                        int idx = recyclerView.getChildPosition(childView);   //this method was deprecated and caused app to crash! replaced with the one below..
                        int idx = recyclerView.getChildLayoutPosition(childView);
                        Log.i(LOG_TAG, "indx = " + idx);
                        recyclerToggleSelection(idx);

                    }

                    super.onLongPress(e);
                }
            });
        }


        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
//            Log.i(LOG_TAG, "onInterceptTouchEvent was called: " + gestureDetector.onTouchEvent(e));
            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e) == true) {
               // clickListener.onClick(child, rv.getChildPosition(child));
                clickListener.onClick(child, rv.getChildAdapterPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
            Log.i(LOG_TAG, "onTouchEvent was called: " + e);

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }

    private void recyclerToggleSelection(int idx) {
        tripsRecyclerAdapter.toggleSelection(idx);
        if (actionMode != null) {
            itemsCount = tripsRecyclerAdapter.getSelectedItemsCount();

            if (itemsCount == 0) {
                actionMode.finish();
            } else {
                String title = "Selected " + itemsCount;
                actionMode.setTitle(title);

                //selectedTrip = trips.get(idx);
                selectedTrips.clear();
                selectedTrips.addAll(tripsRecyclerAdapter.getSelectedItems());
                Log.i(LOG_TAG, "Selected " + selectedTrips.size() + " trips for action..");

                if (itemsCount == 1) {
                    setPosition(position);
                    actionMode.getMenu().findItem(R.id.action_edit_trip).setVisible(true);
                    actionMode.getMenu().findItem(R.id.action_upload_trip).setVisible(true);
                } else {
                    actionMode.getMenu().findItem(R.id.action_edit_trip).setVisible(false);
                    actionMode.getMenu().findItem(R.id.action_upload_trip).setVisible(false);
                }

                if (itemsCount == 2) {
                    actionMode.getMenu().findItem(R.id.action_merge_trips).setVisible(true);
                } else {
                    actionMode.getMenu().findItem(R.id.action_merge_trips).setVisible(false);
                }

                GpsRecTripsList.this.invalidateOptionsMenu();

            }
            Log.v(LOG_TAG, "actionMode is not null");
        } else {
            Log.v(LOG_TAG, "actionMode is null");
        }
    }

    public static interface ClickListener {
        public void onClick(View v, int position);

        public void onLongClick(View v, int position);
    }


}
