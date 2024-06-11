package com.jiaoay.expandabletextview.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.jiaoay.expandabletextview.R

class ExpandableIcon @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs), BaseExpandableButton {
    // 展开按钮
    private var expandIconResource = R.drawable.ic_expand_down

    // 收起按钮
    private var foldIconResource = R.drawable.ic_fold_up

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ExpandableIcon)
            expandIconResource = typedArray.getResourceId(
                R.styleable.ExpandableIcon_expandIcon,
                R.drawable.ic_expand_down
            )
            foldIconResource = typedArray.getResourceId(
                R.styleable.ExpandableIcon_foldIcon,
                R.drawable.ic_fold_up
            )
            typedArray.recycle()
        }
    }

    override fun toExpanded() {
        setImageResource(expandIconResource)
    }

    override fun toFolded() {
        setImageResource(foldIconResource)
    }

    override fun getButtonWidth(): Float {
        if (layoutParams.width > 0) {
            return layoutParams.width.toFloat()
        }
        val buttonDrawable = ResourcesCompat.getDrawable(context.resources, expandIconResource, context.theme) ?: return 0f
        val imageWidth = buttonDrawable.intrinsicWidth.toFloat()
        val imageHeight = buttonDrawable.intrinsicHeight.toFloat()

        val paramsHeight = layoutParams.height

        return when (scaleType) {
            ScaleType.MATRIX -> imageWidth
            ScaleType.FIT_XY -> imageWidth
            ScaleType.FIT_START -> imageWidth
            ScaleType.FIT_CENTER -> imageWidth
            ScaleType.FIT_END -> imageWidth
            ScaleType.CENTER -> imageWidth
            ScaleType.CENTER_CROP -> imageWidth
            ScaleType.CENTER_INSIDE -> {
                if (paramsHeight > 0) {
                    (imageWidth / imageHeight) * paramsHeight
                } else {
                    imageWidth
                }
            }

            else -> imageWidth
        }
    }

    override fun getView(): View {
        return this
    }

    override fun isVisible(): Boolean {
        return isVisible
    }

    override fun setVisible(isVisible: Boolean) {
        this.isVisible = isVisible
    }

    override fun getButtonMeasuredWidth(): Int {
        return getButtonWidth().toInt()
    }

    override fun getButtonMeasuredHeight(): Int {
        return measuredHeight
    }

    fun setExpandableIcon(
        @DrawableRes expandIconResource: Int,
        @DrawableRes foldIconResource: Int
    ) {
        if (expandIconResource == this.expandIconResource && foldIconResource == this.foldIconResource) {
            return
        }
        this.expandIconResource = expandIconResource
        this.foldIconResource = foldIconResource

        requestLayout()
    }
}