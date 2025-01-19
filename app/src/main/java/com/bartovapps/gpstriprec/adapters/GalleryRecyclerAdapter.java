package com.bartovapps.gpstriprec.adapters;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bartovapps.gpstriprec.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by BartovMoti on 03/16/16.
 */
public class GalleryRecyclerAdapter extends RecyclerView.Adapter<GalleryRecyclerAdapter.GalleryViewHolder> {

    private static final String LOG_TAG = GalleryRecyclerAdapter.class.getSimpleName();
    LayoutInflater inflater;
    Activity activity;
    List<Uri> markers;
    private SparseBooleanArray selectedItems;



    public GalleryRecyclerAdapter(Activity context, List<Uri> data) {
        activity = context;
        this.markers = data;
        inflater = LayoutInflater.from(context);
        selectedItems = new SparseBooleanArray();

        Log.i(LOG_TAG, "Gallery Recycler view created");
    }

    @Override
    public GalleryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.gallery_image_item, parent, false);
        GalleryViewHolder viewHolder = new GalleryViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(GalleryViewHolder holder, int position) {

        File imgFile = new File(markers.get(position).getPath());

        if(!imgFile.exists()) {  //This can happen if user erased the image from gallery
            Picasso.with(activity)
                    .load(R.drawable.image_broken)
                    .fit()
                    .centerCrop()
                    .into(holder.ivImageThumbnail);
        }else{
            Picasso.with(activity)
                    .load(markers.get(position))
                    .noFade()
                    .fit()
                    .centerCrop()
                    .into(holder.ivImageThumbnail);
        }

        holder.itemView.setSelected(selectedItems.get(position, false));

    }

    @Override
    public int getItemCount() {

        return markers.size();
    }

    class GalleryViewHolder extends RecyclerView.ViewHolder {

        ImageView ivImageThumbnail;

        public GalleryViewHolder(View itemView) {
            super(itemView);
            ivImageThumbnail = (ImageView) itemView.findViewById(R.id.ivGalleryThumbnail);
        }

    }

    public void setSelection(int position) {
        Log.i(LOG_TAG, "toggleSelection was called");
        clearSelection();

        selectedItems.put(position, true);
        Log.i(LOG_TAG, selectedItems.toString());
        notifyItemChanged(position);
    }

    public void clearSelection(){
        selectedItems.clear();
        notifyDataSetChanged();
    }
}
