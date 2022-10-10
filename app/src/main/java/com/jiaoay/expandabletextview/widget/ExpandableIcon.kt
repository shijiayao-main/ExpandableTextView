package com.jiaoay.expandabletextview.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
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

    override fun isExpanded() {
        setImageResource(expandIconResource)
    }

    override fun isFolded() {
        setImageResource(foldIconResource)
    }

    override fun getButtonWidth(): Float {
        return ResourcesCompat.getDrawable(context.resources, expandIconResource, context.theme)?.intrinsicWidth?.toFloat() ?: 0f
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

}