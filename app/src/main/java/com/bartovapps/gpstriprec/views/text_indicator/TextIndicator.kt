package com.bartovapps.gpstriprec.views.text_indicator

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.bartovapps.gpstriprec.R
import com.bartovapps.gpstriprec.databinding.TextIndicatorBinding

class TextIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: TextIndicatorBinding = TextIndicatorBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.TextIndicator, 0, 0)

            // Set Text
            val text = typedArray.getString(R.styleable.TextIndicator_indicator_text)
            binding.tvIndicatorText.text = text

            // Set Start Icon
            val startRes = typedArray.getResourceId(R.styleable.TextIndicator_start_icon, -1)
            if (startRes != -1) {
                binding.ivStartIcon.setImageResource(startRes)
                binding.ivStartIcon.visibility = VISIBLE
            } else {
                binding.ivStartIcon.visibility = INVISIBLE
            }

            // Set End Icon
            val endRes = typedArray.getResourceId(R.styleable.TextIndicator_end_icon, -1)
            if (endRes != -1) {
                binding.ivEndIcon.setImageResource(endRes)
                binding.ivEndIcon.visibility = VISIBLE
            } else {
                binding.ivEndIcon.visibility = INVISIBLE
            }

            typedArray.recycle()
        }
    }

    // Programmatic accessors
    var text: SpannableString
        get() = SpannableString(binding.tvIndicatorText.text)
        set(value) {
            binding.tvIndicatorText.text = value
        }

    fun setStartIcon(resId: Int) {
        binding.ivStartIcon.setImageResource(resId)
        binding.ivStartIcon.visibility = if (resId != -1) VISIBLE else INVISIBLE
    }

    fun setEndIcon(resId: Int) {
        binding.ivEndIcon.setImageResource(resId)
        binding.ivEndIcon.visibility = if (resId != -1) VISIBLE else INVISIBLE
    }
}