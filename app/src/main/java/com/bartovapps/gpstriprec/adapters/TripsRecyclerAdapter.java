package com.bartovapps.gpstriprec.adapters;

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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bartovapps.gpstriprec.R;
import com.bartovapps.gpstriprec.presentation.units_formatters.HmsFormatter;
import com.bartovapps.gpstriprec.domain.formatters.TimeFormatter;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import data.model.Trip;

/**
 * Created by BartovMoti on 12/11/15.
 */
public class TripsRecyclerAdapter extends RecyclerView.Adapter<TripsRecyclerAdapter.TripsViewHolder> {

    private static final String LOG_TAG = TripsRecyclerAdapter.class.getSimpleName();
    private final List<Trip> data = new ArrayList<>();
    private final SparseBooleanArray selectedItems;
    private final TimeFormatter timeDisplayer = new HmsFormatter();

    public TripsRecyclerAdapter() {
        selectedItems = new SparseBooleanArray();
    }

    @Override
    @NonNull
    public TripsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new TripsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TripsViewHolder holder, int position) {
        Log.i(LOG_TAG, "onBindViewHolder was called");
        Trip trip = data.get(position);
        String tripTitle = trip.getTripName();
        if (tripTitle == null || tripTitle.isEmpty()) {
            holder.tvTitle.setText("");
        } else {
            holder.tvTitle.setText(tripTitle);
        }
        holder.tvDate.setText(trip.getDate());
        holder.tvDuration.setText(timeDisplayer.formatTime(trip.getDuration()));
        holder.itemView.setActivated(selectedItems.get(position, false));
        Picasso.with(holder.itemView.getContext())
                .load(new File(trip.getImageFileName())).transform(new CircleTransform())
                .error(R.drawable.ic_google_map_hdpi_active).transform(new CircleTransform())
                .placeholder(R.drawable.ic_google_map_hdpi_active).transform(new CircleTransform())
                .fit()
                .centerInside()
                .into(holder.ivMapImage);
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

    public List<Trip> getSelectedItems() {
        List<Trip> items = new ArrayList<>();
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(data.get(selectedItems.keyAt(i)));
        }
        return items;
    }




    public void updateTrips(List<Trip> data) {
        this.data.clear();
        this.data.addAll(data);
        notifyDataSetChanged();
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


    static class TripsViewHolder extends RecyclerView.ViewHolder {

        TextView tvDate;
        TextView tvDuration;
        TextView tvTitle;
        ImageView ivMapImage;

        public TripsViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvListRowTripTitle);
            tvDate = itemView.findViewById(R.id.tvListRowDate);
            tvDuration = itemView.findViewById(R.id.tvListRowDuration);
            ivMapImage = itemView.findViewById(R.id.ivListItemImage);
        }

    }
}
