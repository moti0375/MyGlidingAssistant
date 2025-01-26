package com.bartovapps.gpstriprec.adapters;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bartovapps.gpstriprec.R;
import com.bartovapps.gpstriprec.presentation.displayers.HmsFormatter;
import com.bartovapps.gpstriprec.presentation.displayers.TimeFormatter;
import com.bartovapps.gpstriprec.utils.Utils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.File;
import java.util.List;

import data.model.Trip;

public class TripsListAdapter extends BaseAdapter {

    private List<Trip> trips;
    private static LayoutInflater inflater = null;
    private Trip trip;
    private TimeFormatter timeDisplayer = new HmsFormatter();
    private File imgFile;
    private Bitmap myBitmap;
    private SparseBooleanArray mSelectedItemsIds;
    private int viewItemHeight = 0;

    @SuppressLint("NewApi")
    public TripsListAdapter(List<Trip> d) {
        trips = d;
        mSelectedItemsIds = new SparseBooleanArray();
    }

    public int getCount() {
        return trips.size();
    }

    public Trip getItem(int position) {
        return trips.get(position);
    }

    public long getItemId(int position) {
        return position;
    }



    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public View getView(int position, View convertView, ViewGroup parent) {
        trip = trips.get(position);
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item, null);
            viewHolder = new ViewHolder();
            viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.tvListRowTripTitle);
            viewHolder.tvDuration = (TextView) convertView.findViewById(R.id.tvListRowDuration);
            viewHolder.tvDate = (TextView) convertView.findViewById(R.id.tvListRowDate);
            viewHolder.ivMapImage = (ImageView) convertView.findViewById(R.id.item_image);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }


        if (trip.getImageFileName() == null) {
            Picasso.with(convertView.getContext()).load(R.drawable.ic_google_map_hdpi_active).transform(new CircleTransform()).
                    into(viewHolder.ivMapImage);
        } else if (trip.getImageFileName().length() == 0) {
            Picasso.with(convertView.getContext()).load(R.drawable.ic_google_map_hdpi_active).transform(new CircleTransform()).
                    into(viewHolder.ivMapImage);
        } else {
            imgFile = new File(trip.getImageFileName());
            if (!imgFile.exists()) {
                Picasso.with(convertView.getContext()).load(R.drawable.ic_google_map_hdpi_active).transform(new CircleTransform()).
                        into(viewHolder.ivMapImage);
            } else {
                Picasso.with(convertView.getContext())
                        .load(imgFile).transform(new CircleTransform())
                        .into(viewHolder.ivMapImage);
            }
        }

        // Setting all values in listview
        viewHolder.tvDate.setText(trip.getDate());
        viewHolder.tvDuration.setText(timeDisplayer.displayTime(trip.getDuration()));
        String tripTitle = trip.getTripName();
        if (tripTitle == null || tripTitle.length() == 0) {
            viewHolder.tvTitle.setText("");
        } else {
            viewHolder.tvTitle.setText(tripTitle);
        }
        viewItemHeight = convertView.getHeight();
        return convertView;
    }

    public void toggleSelection(int position) {
        selectView(position, !mSelectedItemsIds.get(position));
    }

    public void removeSelection() {
        mSelectedItemsIds = new SparseBooleanArray();
        notifyDataSetChanged();
    }

    public void selectView(int position, boolean value) {
        if (value)
            mSelectedItemsIds.put(position, value);
        else
            mSelectedItemsIds.delete(position);
        notifyDataSetChanged();
    }


    private class Container {
        public File file;
        public ImageView view;
        public Bitmap bitmap;
        public long tripId;
    }

    private static class ViewHolder {
        TextView tvDate;
        TextView tvDuration;
        TextView tvTitle;
        ImageView ivMapImage;
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

    public int getViewItemHeight(){
        return this.viewItemHeight;
    }
}
