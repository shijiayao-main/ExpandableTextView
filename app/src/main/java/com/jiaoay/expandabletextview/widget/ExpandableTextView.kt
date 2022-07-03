package com.jiaoay.expandabletextview.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.*
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import com.jiaoay.expandabletextview.R
import com.jiaoay.expandabletextview.dp2px
import com.jiaoay.expandabletextview.scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpandableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs), OnClickListener {

    var expandableCallback: Callback? = null

    var expandState: Boolean = false

    var contentText: CharSequence = ""

    var maxLineCount = 3

    private val ellipsizeText = "…"

    var expandableType: ExpandableIconType = ExpandableTextIcon(
        expandText = "[展开]",
        foldText = "[收起]"
    )

    // fix click span
    private var preventClick = false
    private var clickListener: OnClickListener? = null
    var ignoreSpannableClick = false
        private set

    private val iconRect: RectF = RectF()

    init {
        movementMethod = LinkMovementMethod.getInstance()
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ExpandableTextView)
            maxLineCount =
                typedArray.getInt(R.styleable.ExpandableTextView_max_expand_line, 3)
            typedArray.recycle()
        }
    }

    private fun getStaticLayout(text: CharSequence): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder
                .obtain(
                    text,
                    0,
                    text.length,
                    paint,
                    measuredWidth
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        setUseLineSpacingFromFallbacks(isFallbackLineSpacing)
                    }
                }
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                .build()
        } else {
            StaticLayout(
                text,
                paint,
                measuredWidth,
                Layout.Alignment.ALIGN_CENTER,
                lineSpacingMultiplier,
                lineSpacingExtra,
                true
            )
        }
    }

    private var iconDrawable: Drawable? = null

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (measuredWidth == 0) {
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
            if (measuredWidth == 0) {
                return
            }
        }
        if (contentText.isEmpty()) {
            return
        }
        context.scope.launch {
            val staticLayout = getStaticLayout(text = contentText)
            val result = if (staticLayout.lineCount > maxLineCount) {
                if (expandState) {
                    onBuildFoldText(staticLayout = staticLayout)
                } else {
                    // 最终显示的文字
                    onBuildExpendText(staticLayout = staticLayout)
                }
            } else {
                expandableCallback?.onLoss()
                // 重新计算高度
                setMeasuredDimension(measuredWidth, staticLayout.height)
                SpannableString(contentText)
            }
            setText(result, BufferType.SPANNABLE)
        }
    }

    private suspend fun getEndTextIndex(lineText: CharSequence, usedWidth: Float): Int =
        withContext(Dispatchers.IO) {
            for (i in lineText.length - 1 downTo 0) {
                val str = lineText.subSequence(i, lineText.length)
                val strWidth = getStaticLayout(str).getLineWidth(0)
                if (strWidth >= usedWidth) {
                    if (i > 1) {
                        val secondChar = lineText[i]
                        val firstChar = lineText[i - 1]
                        if (Character.isSurrogatePair(firstChar, secondChar)) {
                            return@withContext i - 1
                        }
                    }
                    return@withContext i
                }
            }
            return@withContext lineText.length - 1
        }

    // TODO: gravity == Gravity.TOP or Gravity.CENTER_HORIZONTAL
    private fun getUsedWidth(): Float {
        val ellipsizeTextWidth = paint.measureText(ellipsizeText)
        val drawableWidth = iconDrawable?.bounds?.width()?.toFloat() ?: 0f
        var usedWidth = drawableWidth + ellipsizeTextWidth + expandableType.iconPaddingLeft
        if (gravity == Gravity.TOP or Gravity.CENTER_HORIZONTAL) {
            usedWidth *= 2
        }
        return usedWidth
    }

    private suspend fun onBuildExpendText(staticLayout: StaticLayout): SpannableStringBuilder {
        val lineCount = maxLineCount

        val usedWidth = getUsedWidth()

        val start = staticLayout.getLineStart(lineCount - 1)
        val end = staticLayout.getLineEnd(lineCount - 1)
        val lineTextSpannable = contentText
            .subSequence(start, end)
            .removeSuffix("\r\n")
            .removeSuffix("\n")

        val newText = SpannableStringBuilder()

        val lineStaticLayout = getStaticLayout(lineTextSpannable)
        val lineWidth = lineStaticLayout.getLineWidth(0)
        val lineHeight = lineStaticLayout.height
        val lineTextWidth: Float
        if ((lineWidth + usedWidth) > measuredWidth) {
            val endIndex = getEndTextIndex(lineText = lineTextSpannable, usedWidth = usedWidth)
            lineTextWidth = getStaticLayout(
                lineTextSpannable.subSequence(
                    0,
                    endIndex
                )
            ).getLineWidth(0) + paint.measureText(ellipsizeText)
            newText.append(contentText.subSequence(0, start))
                .append(lineTextSpannable.subSequence(0, endIndex))
                .append(ellipsizeText)
        } else {
            lineTextWidth = lineWidth + paint.measureText(ellipsizeText)
            newText.append(contentText.subSequence(0, start))
                .append(lineTextSpannable)
                .append(ellipsizeText)
        }

        val newStaticLayout = getStaticLayout(newText)

        val iconLeft: Float
        val iconTop: Float = (newStaticLayout.height - lineHeight).toFloat()
        val iconRight: Float
        val iconBottom: Float = newStaticLayout.height.toFloat()
        if (gravity == Gravity.TOP or Gravity.CENTER_HORIZONTAL) {
            val spaceLeft = (measuredWidth - lineTextWidth - paddingLeft - paddingRight) / 2
            iconLeft = spaceLeft + lineTextWidth
            iconRight =
                spaceLeft + lineTextWidth + (iconDrawable?.bounds?.width()
                    ?: 0) + expandableType.iconPaddingLeft
        } else {
            iconLeft = lineTextWidth + paddingLeft
            iconRight =
                lineTextWidth + paddingLeft + (iconDrawable?.bounds?.width()
                    ?: 0) + expandableType.iconPaddingLeft
        }
        iconRect.set(iconLeft, iconTop, iconRight, iconBottom)

        expandableCallback?.onCollapse()
        // 重新计算高度
        setMeasuredDimension(measuredWidth, newStaticLayout.height)
        return newText
    }

    private fun onBuildFoldText(
        staticLayout: StaticLayout,
    ): CharSequence {

        val usedWidth = getUsedWidth()

        val lastLine = staticLayout.lineCount - 1

        val lastLineStart = staticLayout.getLineStart(lastLine)
        val lastLineEnd = staticLayout.getLineEnd(lastLine)

        val lastLineSpan = contentText.subSequence(lastLineStart, lastLineEnd)
            .removeSuffix("\r\n")
            .removeSuffix("\n")

        val lastLineStaticLayout = getStaticLayout(lastLineSpan)

        val lineTextWidth = lastLineStaticLayout.getLineWidth(0)
        val lineTextHeight = lastLineStaticLayout.height
        val textHeight: Int
        if ((lineTextWidth + usedWidth) > measuredWidth) {
            val iconTop = staticLayout.height.toFloat()
            val iconBottom = (staticLayout.height + lineTextHeight).toFloat()
            val iconLeft: Float
            val iconRight: Float
            // TODO:
            if (gravity == Gravity.TOP or Gravity.CENTER_HORIZONTAL) {
                val spaceWidth =
                    (measuredWidth - paddingLeft - paddingRight - (iconDrawable?.bounds?.width()
                        ?: 0) + expandableType.iconPaddingLeft.toFloat()) / 2
                iconLeft = spaceWidth
                iconRight =
                    spaceWidth + (iconDrawable?.bounds?.width()
                        ?: 0) + expandableType.iconPaddingLeft.toFloat()
            } else {
                iconLeft = paddingLeft.toFloat()
                iconRight =
                    paddingLeft + (iconDrawable?.bounds?.width()
                        ?: 0) + expandableType.iconPaddingLeft.toFloat()
            }
            textHeight = staticLayout.height + lineTextHeight.coerceAtLeast(
                iconDrawable?.bounds?.height() ?: 0
            )
            iconRect.set(iconLeft, iconTop, iconRight, iconBottom)
        } else {
            val iconTop = (staticLayout.height - lineTextHeight).toFloat()
            val iconBottom = staticLayout.height.toFloat()
            val iconLeft: Float
            val iconRight: Float
            // TODO:
            if (gravity == Gravity.TOP or Gravity.CENTER_HORIZONTAL) {
                val spaceLeft = (measuredWidth - lineTextWidth - paddingLeft - paddingRight) / 2
                iconLeft = spaceLeft + lineTextWidth
                iconRight = spaceLeft + lineTextWidth + (iconDrawable?.bounds?.width()
                    ?: 0) + expandableType.iconPaddingLeft
            } else {
                iconLeft = lineTextWidth + paddingLeft
                iconRight = lineTextWidth + paddingLeft + (iconDrawable?.bounds?.width()
                    ?: 0) + expandableType.iconPaddingLeft
            }
            textHeight = staticLayout.height
            iconRect.set(iconLeft, iconTop, iconRight, iconBottom)
        }
        expandableCallback?.onExpand()
        setMeasuredDimension(measuredWidth, textHeight)
        return contentText
    }

    private val iconPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp2px
        color = ContextCompat.getColor(context, R.color.purple_700)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(iconRect, iconPaint)
        canvas.save()
        val iconTop = iconDrawable?.bounds?.height()?.let {
            iconRect.height() - it
        } ?: 0f
        canvas.translate(iconRect.left + expandableType.iconPaddingLeft, iconRect.top + iconTop)
        iconDrawable?.draw(canvas)
        canvas.restore()
    }

    fun preventNextClick() {
        preventClick = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            if (isClickIcon(it)) {
                return true
            }
        }

        text?.let {
            if (it is Spannable) {
                movementMethod?.onTouchEvent(this, it, event)
            }
        }
        this.ignoreSpannableClick = true
        val ret = super.onTouchEvent(event)
        this.ignoreSpannableClick = false
        return ret
    }

    private var lastX: Float = -1f
    private var lastY: Float = -1f

    private fun isClickIcon(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.getX(0)
                lastY = event.getY(0)
            }
            MotionEvent.ACTION_UP -> {
                if (iconRect.contains(lastX, lastY) && iconRect.contains(
                        event.getX(0),
                        event.getY(0)
                    )
                ) {
                    context.scope.launch {
                        if (expandState) {
                            expandableCallback?.onExpandClick()
                        } else {
                            expandableCallback?.onFoldClick()
                        }
                    }
                    cancelLongPress()
                    return true
                }
            }
        }
        return false
    }

    override fun setOnClickListener(l: OnClickListener?) {
        this.clickListener = l
        super.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (preventClick) {
            preventClick = false
            return
        }
        clickListener?.onClick(v)
    }

    interface Callback {
        fun onExpand() {}
        fun onCollapse() {}
        fun onLoss() {}

        fun onExpandClick()
        fun onFoldClick()
    }

    /**
     * 展开状态 true：展开，false：收起
     */
    fun changeExpendState(expandState: Boolean) {
        this.expandState = expandState
        iconDrawable = initDrawable(expandState)
        requestLayout()
    }

    private val textIconPaint: TextPaint by lazy {
        TextPaint()
    }

    private fun initDrawable(expandState: Boolean): Drawable? {
        when (val type = expandableType) {
            is ExpandableImageIcon -> {
                val iconResource = if (expandState) type.foldIcon else type.expandIcon
                val textHeight = paint.fontMetrics.descent - paint.fontMetrics.ascent
                return ResourcesCompat.getDrawable(resources, iconResource, context.theme)?.apply {
                    var iconWidth: Float = if (type.iconWidth <= 0) {
                        intrinsicWidth.toFloat()
                    } else {
                        type.iconWidth.toFloat()
                    }
                    var iconHeight: Float = if (type.iconHeight <= 0) {
                        intrinsicHeight.toFloat()
                    } else {
                        type.iconHeight.toFloat()
                    }
                    if (iconHeight > textHeight) {
                        iconWidth *= (textHeight / iconHeight)
                        iconHeight = textHeight
                    }
                    setBounds(
                        0,
                        0,
                        iconWidth.toInt(),
                        iconHeight.toInt()
                    )
                }
            }
            is ExpandableTextIcon -> {
                val iconText = if (expandState) type.foldText else type.expandText

                textIconPaint.textSize = if (type.textSize <= 0) {
                    textSize
                } else {
                    type.textSize.toFloat()
                }

                textIconPaint.color = if (expandState) {
                    if (type.foldTextColor <= 0) {
                        textColors.defaultColor
                    } else {
                        ResourcesCompat.getColor(resources, type.foldTextColor, context.theme)
                    }
                } else {
                    if (type.expandTextColor <= 0) {
                        textColors.defaultColor
                    } else {
                        ResourcesCompat.getColor(resources, type.expandTextColor, context.theme)
                    }
                }

                val iconHeight =
                    textIconPaint.fontMetrics.descent - textIconPaint.fontMetrics.ascent
                val iconWidth = textIconPaint.measureText(iconText)

                val bitmap = Bitmap.createBitmap(
                    iconWidth.toInt(),
                    iconHeight.toInt(),
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                canvas.drawText(
                    iconText,
                    0f,
                    iconHeight - textIconPaint.descent(),
                    textIconPaint
                )
                canvas.save()
                return bitmap.toDrawable(resources).apply {
                    bounds.set(
                        0,
                        0,
                        iconWidth.toInt(),
                        iconHeight.toInt()
                    )
                }
            }
        }
    }
}
