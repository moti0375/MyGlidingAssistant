package com.dunihuliapps.myglidingassistnat;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dunihuliapps.myglidingassistant.R;
import com.dunihuliapps.myglidingassistnat.adapters.GalleryRecyclerAdapter;
import com.dunihuliapps.myglidingassistnat.domain.db.TripsDataSource;
import com.squareup.picasso.Picasso;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GpsTripRecGallery extends AppCompatActivity implements View.OnClickListener , View.OnTouchListener{

    private static final String LOG_TAG = GpsTripRecGallery.class.getSimpleName();
    ImageView galleryImage;
    Uri imageUri;
    long tripId;
    RecyclerView galleryRecyclerView;
    GalleryRecyclerAdapter galleryRecyclerAdapter;
    List<Uri> markers;
    boolean recyclerViewVisible = true;
    int position = 0;
    float x1 = 0;
    float x2 = 0;
    Toolbar toolbar;
    RecyclerView.LayoutManager layoutManager;
    private final static float MIN_DISTANCE = 150;

    @Inject
    TripsDataSource tripsDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "onCreate was called");
        setContentView(R.layout.activity_gps_trip_rec_gallery);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        try {
            setSupportActionBar(toolbar);
        } catch (Throwable t) {
            // WTF SAMSUNG!
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_back);


            galleryImage = (ImageView) findViewById(R.id.ivGalleryImage);
            galleryImage.setOnClickListener(this);
            galleryImage.setOnTouchListener(this);

            galleryRecyclerView = (RecyclerView) findViewById(R.id.gallery_recycler_view);
            layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            galleryRecyclerView.setLayoutManager(layoutManager);

            tripId = getIntent().getLongExtra("TripId", 0);

            markers = getImageMarkers();

            galleryRecyclerAdapter = new GalleryRecyclerAdapter(this, markers);
            galleryRecyclerView.setAdapter(galleryRecyclerAdapter);

            galleryRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(GpsTripRecGallery.this, galleryRecyclerView, new ClickListener() {
                @Override
                public void onClick(View v, int position) {
                    //   Toast.makeText(GpsRecTripsList.this, "RecyclerView item " + position + " clicked..", Toast.LENGTH_SHORT).show();
                    //        recyclerToggleSelection(position);
                }

                @Override
                public void onLongClick(View v, int position) {
                    //Longclick is handled by the onLongPress of the RecyclerTouchListener down in this activity..
                }
            }));

        if (savedInstanceState == null) {
            Log.i(LOG_TAG, "savedInstanceStated is null");
            imageUri = getIntent().getData();
            position = markers.indexOf(imageUri);
            Log.i(LOG_TAG, "position is " + position);
        } else {
            Log.i(LOG_TAG, "savedInstanceStated not null");
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_OK);
            finish();
        }
        return true;
    }

    @Override
    protected void onResume() {
        Log.i(LOG_TAG, "onResume was called");
        galleryRecyclerAdapter.setSelection(position);
        scrollRecyclerView();
        super.onResume();
    }

    private void scrollRecyclerView(){
        LinearLayoutManager llm = (LinearLayoutManager) galleryRecyclerView.getLayoutManager();
        llm.scrollToPositionWithOffset(position, markers.size());
        setGalleryImage(imageUri);
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.ivGalleryImage) {
//            Toast.makeText(GpsTripRecGallery.this, "Image was clicked", Toast.LENGTH_SHORT).show();
            if (recyclerViewVisible) {
                galleryRecyclerView.animate().translationY(galleryRecyclerView.getHeight()).alpha(0.0f);
                recyclerViewVisible = false;
            } else {
                galleryRecyclerView.animate().translationY(0).alpha(1.0f);
                recyclerViewVisible = true;
            }
        }

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

//        Log.i(LOG_TAG, "onTouch event was called");


        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();

                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;
//                Log.i(LOG_TAG, "deltaX = " + deltaX);

                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    // Left to Right swipe action
                    if (x2 < x1)
                    {
//                        Toast.makeText(this, "Left to Right swipe [Next]", Toast.LENGTH_SHORT).show ();
                        nextImage();
                    }

                    // Right to left swipe action
                    else
                    {
//                        Toast.makeText(this, "Right to Left swipe [Previous]", Toast.LENGTH_SHORT).show ();
                        previousImage();
                    }
                    return true;
                }
                break;
        }
        return false;
    }

    private List<Uri> getImageMarkers() {
        List<Uri> markers;
        markers = tripsDataSource.findAllMarkersUrisForTrip(tripId);
//        Log.i(LOG_TAG, "got " + markers.size() + " markers from databae");
        return markers;
    }




    class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private final String LOG_TAG = RecyclerTouchListener.class.getSimpleName();
        GestureDetector gestureDetector;
        ClickListener clickListener;

        public RecyclerTouchListener(final Activity context, final RecyclerView recyclerView, final ClickListener clickListener) {
//            Log.i(LOG_TAG, "constructor was invoked");
            this.clickListener = clickListener;

            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
//                    return super.onSingleTapUp(e);
                    View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    position = recyclerView.getChildLayoutPosition(childView);
                    setGalleryImage(markers.get(position));
//                    Log.i(LOG_TAG, "onSingleTapUp was invoked..: index =  " + position);
                    recyclerToggleSelection(position);
                    return true;
                }


                @Override
                public void onLongPress(MotionEvent e) {
//                    Log.i(LOG_TAG, "onLongPress was invoked..: " + e);

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
//            Log.i(LOG_TAG, "onTouchEvent was called: " + e);

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }

    private void recyclerToggleSelection(int idx) {
        galleryRecyclerAdapter.setSelection(idx);
    }

    public interface ClickListener {
        public void onClick(View v, int position);

        public void onLongClick(View v, int position);
    }

    private void setGalleryImage(Uri imageUri) {
            Picasso.with(this).load(imageUri)
                    .noFade()
                    .noPlaceholder()
                    .error(R.drawable.image_broken)
                    .fit()
                    .centerInside().into(galleryImage);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
//        Log.i(LOG_TAG, "onSaveInstanceState was called");
        savedInstanceState.putInt("position", position);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
//        Log.i(LOG_TAG, "onRestoreInstanceState was called");
        position = savedInstanceState.getInt("position");
        imageUri = markers.get(position);
    }

    private void nextImage(){
        if(position < markers.size()-1){
            position++;
//            Log.i(LOG_TAG, "position: " + position);
            imageUri = markers.get(position);
            setGalleryImage(imageUri);
            galleryRecyclerAdapter.setSelection(position);
            scrollRecyclerView();
        }
    }

    private void previousImage(){
        if(position > 0){
            position--;
//            Log.i(LOG_TAG, "position: " + position);
            imageUri = markers.get(position);
            setGalleryImage(imageUri);
            galleryRecyclerAdapter.setSelection(position);
            scrollRecyclerView();
        }
    }
}
