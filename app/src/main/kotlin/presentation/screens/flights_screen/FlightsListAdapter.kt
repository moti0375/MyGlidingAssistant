package com.dunihuliapps.myglidingassistnat.presentation.screens.flights_screen
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistnat.domain.formatters.TimeFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.HmsFormatter
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import data.model.Trip
import java.io.File
import kotlin.math.min

/**
 * Created by BartovMoti on 12/11/15.
 * Migrated to Kotlin on 18/6/2026
 */
class FlightsListAdapter : RecyclerView.Adapter<FlightsListAdapter.FlightsListViewHolder>() {
    private val data: MutableList<Trip> = ArrayList<Trip>()
    private val selectedItems: SparseBooleanArray = SparseBooleanArray()
    private val timeDisplayer: TimeFormatter = HmsFormatter()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlightsListViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return FlightsListViewHolder(view)
    }

    override fun onBindViewHolder(holder: FlightsListViewHolder, position: Int) {
        Log.i(LOG_TAG, "onBindViewHolder was called")
        val trip = data.get(position)
        val tripTitle = trip.tripName
        if (tripTitle == null || tripTitle.isEmpty()) {
            holder.tvTitle.setText("")
        } else {
            holder.tvTitle.setText(tripTitle)
        }
        holder.tvDate.setText(trip.date)
        holder.tvDuration.setText(timeDisplayer.formatTime(trip.duration))
        holder.itemView.setActivated(selectedItems.get(position, false))
        Picasso.with(holder.itemView.getContext())
            .load(File(trip.imageFileName)).transform(CircleTransform())
            .error(R.drawable.ic_google_map_hdpi_active).transform(CircleTransform())
            .placeholder(R.drawable.ic_google_map_hdpi_active).transform(CircleTransform())
            .fit()
            .centerInside()
            .into(holder.ivMapImage)
    }

    override fun getItemCount(): Int {
        return data.size
    }


    fun toggleSelection(position: Int) {
        Log.i(LOG_TAG, "toggleSelection was called")
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
    }

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    val selectedItemsCount: Int
        get() = selectedItems.size()

    fun getSelectedItems(): MutableList<Trip> {
        val items: MutableList<Trip> = ArrayList()
        for (i in 0..<selectedItems.size()) {
            items.add(data[selectedItems.keyAt(i)])
        }
        return items
    }


    fun updateTrips(data: MutableList<Trip>) {
        this.data.apply {
            clear()
            addAll(data)
        }
        notifyDataSetChanged()
    }

    private class CircleTransform : Transformation {
        override fun transform(source: Bitmap): Bitmap {
            val size = min(source.width, source.height)

            val x = (source.width - size) / 2
            val y = (source.height - size) / 2

            val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
            if (squaredBitmap != source) {
                source.recycle()
            }

            val bitmap = Bitmap.createBitmap(size, size, source.config ?: Bitmap.Config.ARGB_8888)

            val canvas = Canvas(bitmap)
            val paint = Paint()
            val shader = BitmapShader(
                squaredBitmap,
                Shader.TileMode.CLAMP, Shader.TileMode.CLAMP
            )
            paint.shader = shader
            paint.isAntiAlias = true

            val r = size / 2f
            canvas.drawCircle(r, r, r, paint)

            squaredBitmap.recycle()
            return bitmap
        }

        override fun key(): String {
            return "circle"
        }
    }


    class FlightsListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvDate: TextView
        var tvDuration: TextView
        var tvTitle: TextView
        var ivMapImage: ImageView?

        init {
            tvTitle = itemView.findViewById<TextView?>(R.id.tvListRowTripTitle)
            tvDate = itemView.findViewById<TextView?>(R.id.tvListRowDate)
            tvDuration = itemView.findViewById<TextView?>(R.id.tvListRowDuration)
            ivMapImage = itemView.findViewById<ImageView?>(R.id.ivListItemImage)
        }
    }

    companion object {
        private val LOG_TAG: String = FlightsListAdapter::class.java.getSimpleName()
    }
}