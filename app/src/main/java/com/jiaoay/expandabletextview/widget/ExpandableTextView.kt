package com.jiaoay.expandabletextview.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import com.jiaoay.expandabletextview.R
import com.jiaoay.expandabletextview.dp2px
import com.jiaoay.expandabletextview.scopeOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.time.measureTime

class ExpandableTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), OnClickListener {

    companion object {
        private const val TAG = "ExpandableTextView"
    }

    private val textView: AppCompatTextView = AppCompatTextView(context).apply {
        movementMethod = LinkMovementMethod.getInstance()
        textMetricsParamsCompat = PrecomputedTextCompat.Params.Builder(paint)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setBreakStrategy(0)
                    setHyphenationFrequency(0)
                }
            }
            .build()
        setOnClickListener(this@ExpandableTextView)
    }

    // 最大显示的行数，超过的将被折叠
    private var maxExpandLineNum = 3

    // 按钮距离左侧文字距离
    private var expandableIconMarginLeft = 8f.dp2px

    private var expandedListener: ((Boolean) -> Unit)? = null

    // 当前是否已经被展开
    private var isExpanded: Boolean = false
        set(value) {
            expandedListener?.invoke(value)
            field = value
        }

    // 是否超过了最大行数
    private var isExceed: Boolean = false

    // 被展开时的文字
    private var expandedText: CharSequence? = null

    // 被折叠时的文字
    private var foldedText: CharSequence? = null

    // 文字的高度
    private val textHeight: Float

    init {
        clipChildren = false
        clipToPadding = false
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ExpandableTextView)
            maxExpandLineNum = typedArray.getInt(
                R.styleable.ExpandableTextView_maxExpandLine,
                3
            )
            isExpanded = typedArray.getBoolean(
                R.styleable.ExpandableTextView_defaultExpanded,
                false
            )
            val textColor = typedArray.getColor(
                R.styleable.ExpandableTextView_textColor,
                Color.BLACK
            )
            val textSize = typedArray.getDimension(
                R.styleable.ExpandableTextView_textSize,
                12f.dp2px
            )
            typedArray.getDimension(
                R.styleable.ExpandableTextView_expandedIconMarginLeft,
                8f.dp2px
            )
            val lineSpacingExtra = typedArray.getDimension(
                R.styleable.ExpandableTextView_lineSpacingExtra,
                0f
            )
            val lineSpacingMultiplier = typedArray.getFloat(
                R.styleable.ExpandableTextView_lineSpacingMultiplier,
                1f
            )
            typedArray.recycle()
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            textView.setTextColor(textColor)
            textView.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
        }

        addView(
            textView,
            MarginLayoutParams(
                MarginLayoutParams.MATCH_PARENT,
                MarginLayoutParams.WRAP_CONTENT
            )
        )

        val fontMetrics = textView.paint.fontMetrics
        textHeight = fontMetrics.descent - fontMetrics.ascent
    }

    private var clickListener: OnClickListener? = null

    override fun setOnClickListener(l: OnClickListener?) {
        clickListener = l
    }

    // fix click span
    private var preventClick = false

    fun preventNextClick() {
        preventClick = true
    }

    override fun onClick(v: View?) {
        if (preventClick) {
            preventClick = false
            return
        }
        clickListener?.onClick(v)
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        textView.setOnLongClickListener(l)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        measureChildWithMargins(
            expandableButton.getView(),
            widthMeasureSpec,
            0,
            heightMeasureSpec,
            0
        )
        if (isExceed.not()) {
            measureChildWithMargins(
                textView,
                widthMeasureSpec,
                0,
                heightMeasureSpec,
                0
            )
            setMeasuredDimension(width, textView.measuredHeight)
            return
        }

        if (isExpanded) {
            // 应展示为展开状态
            textView.setText(expandedText, TextView.BufferType.SPANNABLE)
            measureChildWithMargins(
                textView,
                widthMeasureSpec,
                0,
                heightMeasureSpec,
                0
            )

            if (width + paddingRight - expandedLastLineWidth > expandableButton.getButtonMeasuredWidth() + expandableIconMarginLeft) {
                // 最后一行能够放下收起按钮
                setMeasuredDimension(width, textView.measuredHeight)
            } else {
                setMeasuredDimension(
                    width,
                    textView.measuredHeight + textHeight.coerceAtLeast(expandableButton.getButtonMeasuredHeight().toFloat()).toInt()
                )
            }
        } else {
            // 应展示为收起状态
            textView.setText(foldedText, TextView.BufferType.SPANNABLE)
            measureChildWithMargins(
                textView,
                widthMeasureSpec,
                0,
                heightMeasureSpec,
                0
            )
            setMeasuredDimension(width, textView.measuredHeight)
        }
    }

    private val imageRect = Rect()

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        textView.layout(
            paddingLeft,
            paddingTop,
            paddingLeft + textView.measuredWidth,
            paddingTop + textView.measuredHeight
        )

        if (isExceed.not()) {
            expandableButton.getView().layout(0, 0, 0, 0)
            return
        }

        val imageLeft: Int
        val imageTop: Int
        val imageRight: Int
        val imageBottom: Int
        if (isExpanded) {
            // 应展示为展开状态
            val imageSpace = (textHeight - expandableButton.getButtonMeasuredHeight()) / 2f
            if (width + paddingRight - expandedLastLineWidth > expandableButton.getButtonMeasuredWidth() + expandableIconMarginLeft) {
                // 最后一行能够放下收起按钮
                imageLeft = (expandedLastLineWidth + paddingLeft + expandableIconMarginLeft).toInt()
                imageTop = (paddingTop + textView.measuredHeight - textHeight + imageSpace).toInt()
                imageRight = (expandedLastLineWidth + paddingLeft + expandableIconMarginLeft + expandableButton.getButtonMeasuredWidth()).toInt()
                imageBottom = (paddingTop + textView.measuredHeight - imageSpace).toInt()
            } else {
                imageLeft = paddingLeft
                imageTop = (paddingTop + textView.measuredHeight + imageSpace).toInt()
                imageRight = paddingLeft + expandableButton.getButtonMeasuredWidth()
                imageBottom = (paddingTop + textView.measuredHeight + textHeight - imageSpace).toInt()
            }
        } else {
            // 应展示为收起状态
            val imageSpace = (textHeight - expandableButton.getButtonMeasuredHeight()) / 2f
            imageLeft = (foldedLastLineWidth + paddingLeft + expandableIconMarginLeft).toInt()
            imageTop = (paddingTop + textView.measuredHeight - textHeight + imageSpace).toInt()
            imageRight = (foldedLastLineWidth + paddingLeft + expandableIconMarginLeft + expandableButton.getButtonMeasuredWidth()).toInt()
            imageBottom = (paddingTop + textView.measuredHeight - imageSpace).toInt()
        }
        imageRect.set(imageLeft, imageTop, imageRight, imageBottom)
        expandableButton.getView().layout(imageLeft, imageTop, imageRight, imageBottom)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        ev?.let {
            if (isClickIcon(it)) {
                return true
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private var lastX: Float = -1f
    private var lastY: Float = -1f

    private fun isClickIcon(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.getX(0)
                lastY = event.getY(0)
                if (imageRect.xYInIconRect(lastX, lastY)) {
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (imageRect.xYInIconRect(lastX, lastY) && imageRect.xYInIconRect(
                        event.getX(0),
                        event.getY(0)
                    )
                ) {
                    if (isExceed) {
                        if (isExpanded) {
                            isExpanded = false
                            expandableButton.toExpanded()
                        } else {
                            isExpanded = true
                            expandableButton.toFolded()
                        }
                        requestLayout()
                    }
                    cancelLongPress()
                    return true
                }
            }
        }
        return false
    }

    private fun Rect.xYInIconRect(x: Float, y: Float): Boolean {
        val isLegal = left < right && top < bottom
        return isLegal &&
                x >= (left - 10.dp2px) &&
                x < (right + 10.dp2px) &&
                y >= (top - 10.dp2px) &&
                y < (bottom + 10.dp2px)
    }

    private var expandedLastLineWidth = 0f
    private var foldedLastLineWidth = 0f
    private val ellipsizeText = "…"

    fun setText(
        text: CharSequence,
        isExpanded: Boolean = false,
        expandedListener: ((Boolean) -> Unit)? = null
    ) {
        if (text.isEmpty()) {
            expandedText = null
            foldedText = null
            isExceed = false
            expandableButton.setVisible(false)
            this.isExpanded = false
            textView.text = ""
            requestLayout()
            this.expandedListener = null
            return
        }
        post {
            context.scopeOrNull?.launch {
                val job = launch(Dispatchers.Default) {
                    this@ExpandableTextView.isExpanded = isExpanded

                    val staticLayout = getStaticLayout(text = text)
                    if (staticLayout.lineCount > maxExpandLineNum) {
                        withContext(Dispatchers.Main) {
                            if (isExpanded.not()) {
                                expandableButton.toExpanded()
                            } else {
                                expandableButton.toFolded()
                            }
                            isExceed = true
                            expandableButton.setVisible(true)
                        }
                        expandedText = text

                        // 判断协程是否需要终止
                        ensureActive()

                        calculateText(
                            staticLayout = staticLayout,
                            text = text
                        )
                    } else {
                        expandedText = text
                        foldedText = text
                        isExceed = false
                        withContext(Dispatchers.Main) {
                            expandableButton.setVisible(false)
                        }
                    }
                }

                job.join()
                textView.setText(text, TextView.BufferType.SPANNABLE)
                requestLayout()
            }
        }
        this@ExpandableTextView.expandedListener = expandedListener
    }

    private suspend fun calculateText(
        staticLayout: StaticLayout,
        text: CharSequence
    ) = withContext(Dispatchers.Default) {
        // 获取展开状态下最后一行文字的高度以便确定收起按钮应在的位置
        val lastLine = staticLayout.lineCount - 1
        val lastLineStart = staticLayout.getLineStart(lastLine).coerceAtMost(text.length)
        val lastLineEnd = staticLayout.getLineEnd(lastLine).coerceAtMost(text.length)
        val lastLineSpan = text.subSequence(lastLineStart, lastLineEnd)
            .removeSuffix("\r\n")
            .removeSuffix("\n")
        val lastLineStaticLayout = getStaticLayout(lastLineSpan)
        expandedLastLineWidth = lastLineStaticLayout.getLineWidth(0)

        // 获取折叠状态下最后一行文字的高度以便确定展开按钮应在的位置
        val start = staticLayout.getLineStart(maxExpandLineNum - 1).coerceAtMost(text.length)
        val end = staticLayout.getLineEnd(maxExpandLineNum - 1).coerceAtMost(text.length)
        val lineTextSpannable = text
            .subSequence(start, end)
            .removeSuffix("\r\n")
            .removeSuffix("\n")

        val lineStaticLayout = getStaticLayout(lineTextSpannable)
        val lineWidth = lineStaticLayout.getLineWidth(0)
        val ellipsizeTextWidth = textView.paint.measureText(ellipsizeText)
        // 获取展开按钮的宽度
        val expandableButtonWidth = expandableButton.getButtonWidth() + ellipsizeTextWidth
        val currentWidth = lineWidth + paddingLeft + expandableButtonWidth + expandableIconMarginLeft

        val newText = SpannableStringBuilder()
        if (currentWidth > measuredWidth) {
            val endIndex = getEndTextIndex(
                lineText = lineTextSpannable,
                usedWidth = expandableIconMarginLeft + expandableButtonWidth
            )
            foldedLastLineWidth = getStaticLayout(
                lineTextSpannable.subSequence(
                    0,
                    endIndex.coerceAtMost(lineTextSpannable.length)
                )
            ).getLineWidth(0) + ellipsizeTextWidth
            newText.append(text.subSequence(0, start.coerceAtMost(text.length)))
                .append(lineTextSpannable.subSequence(0, endIndex.coerceAtMost(lineTextSpannable.length)))
                .append(ellipsizeText)
        } else {
            foldedLastLineWidth = lineWidth + ellipsizeTextWidth
            newText.append(text.subSequence(0, start.coerceAtMost(text.length)))
                .append(lineTextSpannable)
                .append(ellipsizeText)
        }
        foldedText = newText
    }

    /**
     * @return 返回需要被裁剪掉的文字的序号
     */
    private suspend fun getEndTextIndex(lineText: CharSequence, usedWidth: Float): Int {
        val result: Int
        val functionDuration = measureTime {
            result = withContext(Dispatchers.Default) {
                val lineTextLength = lineText.length
                if (lineTextLength <= 0) {
                    return@withContext lineTextLength
                }
                val lineWidth = getStaticLayout(lineText).getLineWidth(0)
                val averageTextWidth: Float = lineWidth / lineTextLength
                val lastIndex: Int = if (averageTextWidth > 0) {
                    // 根据已使用的宽度和粗略计算出的文字宽度, 估算出大致的截取位置
                    val index = ((usedWidth / averageTextWidth).roundToInt()).coerceAtLeast(3)
                    lineTextLength - index
                } else {
                    lineTextLength - 1
                }

                val calculateNum: Int = if (averageTextWidth > 10) {
                    4
                } else {
                    if (lineTextLength < 50) {
                        4
                    } else {
                        8
                    }
                }
//                Log.d(TAG, "getEndTextIndex: usedWidth: $usedWidth, lineTextLength: $lineTextLength, lastIndex: $lastIndex, lineWidth: $lineWidth, averageTextWidth: $averageTextWidth, calculateNum: $calculateNum")
                if (lastIndex < lineTextLength) {
                    val str = lineText.subSequence(lastIndex, lineTextLength)
                    val strWidth = getStaticLayout(str).getLineWidth(0)
//                    Log.d(TAG, "getEndTextIndex: strWidth: $strWidth, usedWidth: $usedWidth")
                    if (strWidth == usedWidth) {
                        return@withContext lastIndex
                    } else if (strWidth > usedWidth) {
                        return@withContext calculateWhenMore(
                            lineText = lineText,
                            lineTextLength = lineTextLength,
                            lastIndex = lastIndex,
                            usedWidth = usedWidth,
                            calculateNum = calculateNum
                        )
                    }
                }
                return@withContext calculateWhenLess(
                    lineText = lineText,
                    lineTextLength = lineTextLength,
                    lastIndex = lastIndex,
                    usedWidth = usedWidth,
                    calculateNum = calculateNum
                )
            }
        }
        Log.d(TAG, "getEndTextIndex: text: '${lineText.substring(0, lineText.length)}', duration: $functionDuration, result: $result")
        return result
    }

    private suspend fun calculateWhenLess(
        lineText: CharSequence,
        lineTextLength: Int,
        lastIndex: Int,
        usedWidth: Float,
        calculateNum: Int,
    ): Int = withContext(Dispatchers.Default) {
        val remainder = lastIndex % calculateNum
        val last = lastIndex / calculateNum

        for (i in last downTo 1) {
            val strLastIndex = (i - 1) * calculateNum + 1
            val strLast = lineText.subSequence(strLastIndex, lineTextLength)
            val strLastWidth = getStaticLayout(strLast).getLineWidth(0)

            if (strLastWidth == usedWidth) {
                return@withContext safeClip(
                    lineText = lineText,
                    index = strLastIndex
                )
            } else if (strLastWidth > usedWidth) {
                val endJ = if (i == last) {
                    lineTextLength - strLastIndex
                } else {
                    calculateNum
                }
                for (j in 1..<endJ) {
                    val index = strLastIndex + j
                    val str = lineText.subSequence(index, lineTextLength)
                    val strWidth = getStaticLayout(str).getLineWidth(0)
//                    Log.d(TAG, "calculateWhenLess: index: $index, lineTextLength: $lineTextLength, strLastIndex: $strLastIndex, str: '$str', strWidth: $strWidth, usedWidth: $usedWidth")
                    if (strWidth < usedWidth) {
                        return@withContext safeClip(
                            lineText = lineText,
                            index = index - 1
                        )
                    }
                }
            }
//            Log.d(TAG, "calculateWhenLess: ")
        }
        return@withContext defaultCalculate(
            lineText = lineText,
            lineTextLength = lineTextLength,
            remainder = remainder,
            usedWidth = usedWidth
        )
    }

    private suspend fun calculateWhenMore(
        lineText: CharSequence,
        lineTextLength: Int,
        lastIndex: Int,
        usedWidth: Float,
        calculateNum: Int,
    ) = withContext(Dispatchers.Default) {
        val length = lineTextLength - lastIndex
        val remainder = length % calculateNum
        val last = length / calculateNum

        for (i in 0 until last) {
            val strLastIndex = lastIndex + (i + 1) * calculateNum - 1
            val strLast = lineText.subSequence(strLastIndex, lineTextLength)
            val strLastWidth = getStaticLayout(strLast).getLineWidth(0)

            if (strLastWidth == usedWidth) {
                return@withContext safeClip(
                    lineText = lineText,
                    index = strLastIndex
                )
            } else if (strLastWidth < usedWidth) {
                for (j in 1..<calculateNum) {
                    val index = strLastIndex - j
                    val str = lineText.subSequence(index, lineTextLength)
                    val strWidth = getStaticLayout(str).getLineWidth(0)
                    if (strWidth >= usedWidth) {
                        return@withContext safeClip(
                            lineText = lineText,
                            index = index
                        )
                    }
                }
            }
        }
        return@withContext defaultCalculate(
            lineText = lineText,
            lineTextLength = lineTextLength,
            remainder = remainder,
            usedWidth = usedWidth
        )
    }

    private suspend fun defaultCalculate(
        lineText: CharSequence,
        lineTextLength: Int,
        remainder: Int,
        usedWidth: Float
    ): Int = withContext(Dispatchers.Default) {
        if (remainder != 0) {
            for (i in 0..remainder) {
                val index = lineTextLength - i
                val str = lineText.subSequence(index, lineTextLength)
                val strWidth = getStaticLayout(str).getLineWidth(0)
                if (strWidth >= usedWidth) {
                    return@withContext safeClip(
                        lineText = lineText,
                        index = index
                    )
                }
            }
        }
        return@withContext lineTextLength - 1
    }

    private fun safeClip(
        lineText: CharSequence,
        index: Int
    ): Int {
        if (index > 1) {
            val secondChar = lineText[index]
            val firstChar = lineText[index - 1]
            if (Character.isSurrogatePair(firstChar, secondChar)) {
                return index - 1
            }
        }
        return index
    }


    private fun getStaticLayout(text: CharSequence): StaticLayout {
        val width = measuredWidth - paddingLeft - paddingRight
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder
                .obtain(
                    text,
                    0,
                    text.length,
                    textView.paint,
                    width
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        setUseLineSpacingFromFallbacks(textView.isFallbackLineSpacing)
                    }
                }
                .setBreakStrategy(textView.breakStrategy)
                .setHyphenationFrequency(textView.hyphenationFrequency)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(textView.lineSpacingExtra, textView.lineSpacingMultiplier)
                .build()
        } else {
            StaticLayout(
                text,
                textView.paint,
                width,
                Layout.Alignment.ALIGN_CENTER,
                textView.lineSpacingMultiplier,
                textView.lineSpacingExtra,
                true
            )
        }
    }

    private var expandableButton: BaseExpandableButton = ExpandableText(context)

    override fun onFinishInflate() {
        super.onFinishInflate()

        // TODO:
        if (childCount > 2) {
            throw RuntimeException("only support add one child view.")
        }

        if (childCount > 1) {
            val childView = getChildAt(1)
            if (childView !is BaseExpandableButton) {
                throw RuntimeException("only support add ExpandableIcon or ExpandableText.")
            }
            expandableButton = childView
        } else {
            addView(
                expandableButton.getView(),
                MarginLayoutParams(
                    MarginLayoutParams.WRAP_CONTENT,
                    MarginLayoutParams.WRAP_CONTENT
                )
            )
        }
    }
}