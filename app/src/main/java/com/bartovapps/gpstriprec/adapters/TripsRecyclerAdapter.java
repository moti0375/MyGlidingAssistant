package com.bartovapps.gpstriprec.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bartovapps.gpstriprec.R;
import com.bartovapps.gpstriprec.displayers.HmsDisplayer;
import com.bartovapps.gpstriprec.displayers.TimeDisplayer;
import com.bartovapps.gpstriprec.trip.Trip;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by BartovMoti on 12/11/15.
 */
public class TripsRecyclerAdapter extends RecyclerView.Adapter<TripsRecyclerAdapter.TripsViewHolder> {

    private static final String LOG_TAG = TripsRecyclerAdapter.class.getSimpleName();
    LayoutInflater inflater;
    ArrayList<Trip> data = new ArrayList<>();
    Activity activity;
    private File imgFile;
    private SparseBooleanArray selectedItems;
    private TimeDisplayer timeDisplayer = new HmsDisplayer();


    public TripsRecyclerAdapter(Activity context, ArrayList<Trip> data) {
        inflater = LayoutInflater.from(context);
        this.activity = context;
        this.data = data;
        selectedItems = new SparseBooleanArray();
    }

    @Override
    public TripsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.list_item, parent, false);
        TripsViewHolder viewHolder = new TripsViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(TripsViewHolder holder, int position) {
        Log.i(LOG_TAG, "onBindViewHolder was called");
        Trip trip = new Trip();
        trip.setTripName(data.get(position).getTripName());
        trip.setDate(data.get(position).getDate());
        trip.setDuration(data.get(position).getDuration());
        trip.setImageFileName(data.get(position).getImageFileName());

        String tripTitle = trip.getTripName();
        if (tripTitle == null || tripTitle.length() == 0) {
            holder.tvTitle.setText("");
        } else {
            holder.tvTitle.setText(tripTitle);
        }

        holder.tvDate.setText(trip.getDate());
        timeDisplayer.displayTime(holder.tvDuration, trip.getDuration());

        holder.itemView.setActivated(selectedItems.get(position, false));


        if (trip.getImageFileName() == null) {
            Picasso.with(activity).load(R.drawable.ic_google_map_hdpi_active).transform(new CircleTransform()).fit().centerInside().
                    into(holder.ivMapImage);
        } else if (trip.getImageFileName().length() == 0) {
            Picasso.with(activity).load(R.drawable.ic_google_map_hdpi_active).transform(new CircleTransform()).fit().centerInside().
                    into(holder.ivMapImage);
        } else {
            imgFile = new File(trip.getImageFileName());

            Picasso.with(activity)
                    .load(imgFile).transform(new CircleTransform())
                    .error(R.drawable.ic_google_map_hdpi_active)
                    .fit()
                    .centerInside()
                    .into(holder.ivMapImage);

        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    public void toggleSelection(int position) {
        Log.i(LOG_TAG, "toggleSelection was called");
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
        } else {
            selectedItems.put(position, true);
        }
        notifyItemChanged(position);
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemsCount() {
        return selectedItems.size();
    }

    public ArrayList<Trip> getSelectedItems() {
        ArrayList<Trip> items = new ArrayList<>();
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(data.get(selectedItems.keyAt(i)));
        }
        return items;
    }

    class TripsViewHolder extends RecyclerView.ViewHolder {

        TextView tvDate;
        TextView tvDuration;
        TextView tvTitle;
        ImageView ivMapImage;

        public TripsViewHolder(View itemView) {
            super(itemView);
            tvTitle = (TextView) itemView.findViewById(R.id.tvListRowTripTitle);
            tvDate = (TextView) itemView.findViewById(R.id.tvListRowDate);
            tvDuration = (TextView) itemView.findViewById(R.id.tvListRowDuration);
            ivMapImage = (ImageView) itemView.findViewById(R.id.ivListItemImage);
            //drawerRowIcon.setOnClickListener(this);

        }

    }


    public void updateTrips(ArrayList<Trip> data) {
        if (this.data == null) {
            this.data = data;
        } else {
            this.data.clear();
            this.data.addAll(data);
        }

        Log.i(LOG_TAG, "data size: " + this.data.size());
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });

    }

    private static class CircleTransform implements Transformation {
        @Override
        public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());

            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
            if (squaredBitmap != source) {
                source.recycle();
            }

            Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            BitmapShader shader = new BitmapShader(squaredBitmap,
                    BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);

            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);

            squaredBitmap.recycle();
            return bitmap;
        }

        @Override
        public String key() {
            return "circle";
        }
    }
}
