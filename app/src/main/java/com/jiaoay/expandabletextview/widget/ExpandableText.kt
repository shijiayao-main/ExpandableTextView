package com.jiaoay.expandabletextview.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.jiaoay.expandabletextview.R

class ExpandableText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs), BaseExpandableButton {

    // 展开按钮
    private var expandText = "[展开]"

    // 展开按钮文字颜色
    private var expandTextColor: Int = 0

    // 收起按钮
    private var foldText = "[收起]"

    // 收起按钮文字颜色
    private var foldTextColor: Int = 0

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ExpandableText)
            expandText = typedArray.getString(
                R.styleable.ExpandableText_expandText,
            ) ?: "[展开]"

            expandTextColor = typedArray.getColor(
                R.styleable.ExpandableText_expandTextColor,
                0
            )

            foldText = typedArray.getString(
                R.styleable.ExpandableText_foldText,
            ) ?: "[收起]"

            foldTextColor = typedArray.getColor(
                R.styleable.ExpandableText_foldTextColor,
                0
            )
            typedArray.recycle()
        }
    }

    override fun toExpanded() {
        text = expandText
        if (expandTextColor != 0) {
            setTextColor(expandTextColor)
        } else {
            setTextColor(textColors)
        }
    }

    override fun toFolded() {
        text = foldText
        if (foldTextColor != 0) {
            setTextColor(foldTextColor)
        } else {
            setTextColor(textColors)
        }
    }

    override fun getButtonWidth(): Float {
        return paint.measureText(expandText)
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
        return measuredWidth
    }

    override fun getButtonMeasuredHeight(): Int {
        return measuredHeight
    }

    fun setExpandableText(
        expandText: String,
        foldText: String
    ) {
        if (expandText == this.expandText && foldText == this.foldText) {
            return
        }
        this.expandText = expandText
        this.foldText = foldText

        requestLayout()
    }
}