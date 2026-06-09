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
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class ExpandableTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), OnClickListener {

    // tracks the currently running setText coroutine so we can cancel it on new calls
    private var currentTextJob: Job? = null

    // pending Runnable posted via post{} — kept so it can be cancelled by a subsequent setText call
    private var pendingSetTextRunnable: Runnable? = null

    // tracks which text state is currently set on textView to avoid redundant setText in onMeasure
    private var lastTextViewIsExpanded: Boolean? = null

    private data class TextComputationCache(
        val textRef: CharSequence,
        val width: Int,
        val isExceed: Boolean,
        val expandedLastLineWidth: Float = 0f,
        val foldedLastLineWidth: Float = 0f,
        val foldedText: CharSequence? = null
    )
    // cached result of the last background computation for this view instance
    private var textComputationCache: TextComputationCache? = null

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
            expandableIconMarginLeft = typedArray.getDimension(
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
            if (lastTextViewIsExpanded != true) {
                textView.setText(expandedText, TextView.BufferType.SPANNABLE)
                lastTextViewIsExpanded = true
            }
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
            if (lastTextViewIsExpanded != false) {
                textView.setText(foldedText, TextView.BufferType.SPANNABLE)
                lastTextViewIsExpanded = false
            }
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
        ev ?: return super.dispatchTouchEvent(null)
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = ev.getX(0)
                lastY = ev.getY(0)
                if (imageRect.xYInIconRect(lastX, lastY)) {
                    consumedDown = true
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (consumedDown) return true
            }
            MotionEvent.ACTION_UP -> {
                if (consumedDown) {
                    consumedDown = false
                    // only treat as a click if both DOWN and UP landed inside the icon area
                    if (imageRect.xYInIconRect(lastX, lastY) &&
                        imageRect.xYInIconRect(ev.getX(0), ev.getY(0))
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
                    }
                    // always consume UP when we owned the DOWN, so children don't get
                    // an orphaned UP event
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                consumedDown = false
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private var consumedDown = false
    private var lastX: Float = -1f
    private var lastY: Float = -1f
    private val touchSlop = 10.dp2px

    private fun Rect.xYInIconRect(x: Float, y: Float): Boolean {
        val isLegal = left < right && top < bottom
        return isLegal &&
                x >= (left - touchSlop) &&
                x < (right + touchSlop) &&
                y >= (top - touchSlop) &&
                y < (bottom + touchSlop)
    }

    private var expandedLastLineWidth = 0f
    private var foldedLastLineWidth = 0f
    private val ellipsizeText = "…"

    fun setText(
        text: CharSequence,
        isExpanded: Boolean = false,
        expandedListener: ((Boolean) -> Unit)? = null
    ) {
        this@ExpandableTextView.expandedListener = expandedListener
        lastTextViewIsExpanded = null

        // Cancel any pending post and running job immediately to avoid queue pile-up
        pendingSetTextRunnable?.let { removeCallbacks(it) }
        pendingSetTextRunnable = null
        currentTextJob?.cancel()
        currentTextJob = null

        if (text.isEmpty()) {
            textComputationCache = null
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

        if (measuredWidth > 0) {
            // View is already measured (e.g. reused from RecyclerView pool) — skip post()
            launchTextComputation(text, isExpanded)
        } else {
            val runnable = Runnable {
                pendingSetTextRunnable = null
                launchTextComputation(text, isExpanded)
            }
            pendingSetTextRunnable = runnable
            post(runnable)
        }
    }

    private fun launchTextComputation(text: CharSequence, isExpandedState: Boolean) {
        val currentWidth = measuredWidth
        val cache = textComputationCache

        // Fast path: same text object and same width — reuse cached computation without background work
        if (cache != null && cache.textRef === text && cache.width == currentWidth) {
            applyComputationResult(text, isExpandedState, cache)
            return
        }

        currentTextJob = context.scopeOrNull?.launch {
            this@ExpandableTextView.isExpanded = isExpandedState

            val calcJob = launch(Dispatchers.Default) {
                val staticLayout = getStaticLayout(text = text)
                if (staticLayout.lineCount > maxExpandLineNum) {
                    val calcResult = calculateText(staticLayout = staticLayout, text = text)

                    // 判断协程是否需要终止
                    ensureActive()

                    // Apply all computed state atomically on Main so that no layout pass
                    // can see a partially-updated state (e.g. isExceed=true but foldedText=null).
                    withContext(Dispatchers.Main) {
                        expandedText = text
                        expandedLastLineWidth = calcResult.expandedLastLineWidth
                        foldedLastLineWidth = calcResult.foldedLastLineWidth
                        foldedText = calcResult.foldedText
                        if (isExpandedState.not()) {
                            expandableButton.toExpanded()
                        } else {
                            expandableButton.toFolded()
                        }
                        isExceed = true
                        expandableButton.setVisible(true)
                        textComputationCache = TextComputationCache(
                            textRef = text,
                            width = currentWidth,
                            isExceed = true,
                            expandedLastLineWidth = calcResult.expandedLastLineWidth,
                            foldedLastLineWidth = calcResult.foldedLastLineWidth,
                            foldedText = calcResult.foldedText
                        )
                    }
                } else {
                    // Update all shared state on Main to avoid data races with onMeasure
                    withContext(Dispatchers.Main) {
                        expandedText = text
                        foldedText = text
                        isExceed = false
                        expandableButton.setVisible(false)
                        textComputationCache = TextComputationCache(
                            textRef = text,
                            width = currentWidth,
                            isExceed = false
                        )
                    }
                }
            }

            calcJob.join()
            if (isActive) {
                textView.setText(text, TextView.BufferType.SPANNABLE)
                requestLayout()
            }
        }
    }

    /**
     * Applies a cached computation result synchronously on the main thread, skipping background work.
     */
    private fun applyComputationResult(text: CharSequence, isExpandedState: Boolean, cache: TextComputationCache) {
        this.isExpanded = isExpandedState
        expandedText = text
        if (cache.isExceed) {
            expandedLastLineWidth = cache.expandedLastLineWidth
            foldedLastLineWidth = cache.foldedLastLineWidth
            foldedText = cache.foldedText
            isExceed = true
            expandableButton.setVisible(true)
            if (isExpandedState) {
                expandableButton.toFolded()
                textView.setText(text, TextView.BufferType.SPANNABLE)
                lastTextViewIsExpanded = true
            } else {
                expandableButton.toExpanded()
                textView.setText(cache.foldedText, TextView.BufferType.SPANNABLE)
                lastTextViewIsExpanded = false
            }
        } else {
            foldedText = text
            isExceed = false
            expandableButton.setVisible(false)
            textView.setText(text, TextView.BufferType.SPANNABLE)
            lastTextViewIsExpanded = null
        }
        requestLayout()
    }

    private data class TextCalculationResult(
        val expandedLastLineWidth: Float,
        val foldedLastLineWidth: Float,
        val foldedText: CharSequence
    )

    private suspend fun calculateText(
        staticLayout: StaticLayout,
        text: CharSequence
    ): TextCalculationResult = withContext(Dispatchers.Default) {
        // 获取展开状态下最后一行文字的宽度以便确定收起按钮应在的位置
        val lastLine = staticLayout.lineCount - 1
        val lastLineStart = staticLayout.getLineStart(lastLine).coerceAtMost(text.length)
        val lastLineEnd = staticLayout.getLineEnd(lastLine).coerceAtMost(text.length)
        val lastLineSpan = text.subSequence(lastLineStart, lastLineEnd)
            .removeSuffix("\r\n")
            .removeSuffix("\n")
        val computedExpandedLastLineWidth = measureLineWidth(lastLineSpan)

        // 获取折叠状态下最后一行文字的宽度以便确定展开按钮应在的位置
        val start = staticLayout.getLineStart(maxExpandLineNum - 1).coerceAtMost(text.length)
        val end = staticLayout.getLineEnd(maxExpandLineNum - 1).coerceAtMost(text.length)
        val lineTextSpannable = text
            .subSequence(start, end)
            .removeSuffix("\r\n")
            .removeSuffix("\n")

        val lineWidth = measureLineWidth(lineTextSpannable)
        val ellipsizeTextWidth = textView.paint.measureText(ellipsizeText)
        val expandableButtonWidth = expandableButton.getButtonWidth() + ellipsizeTextWidth
        val currentWidth = lineWidth + paddingLeft + expandableButtonWidth + expandableIconMarginLeft

        val newText = SpannableStringBuilder()
        val computedFoldedLastLineWidth: Float
        if (currentWidth > measuredWidth) {
            val endIndex = getEndTextIndex(
                lineText = lineTextSpannable,
                usedWidth = expandableIconMarginLeft + expandableButtonWidth
            )
            computedFoldedLastLineWidth = measureLineWidth(
                lineTextSpannable.subSequence(0, endIndex.coerceAtMost(lineTextSpannable.length))
            ) + ellipsizeTextWidth
            newText.append(text.subSequence(0, start.coerceAtMost(text.length)))
                .append(lineTextSpannable.subSequence(0, endIndex.coerceAtMost(lineTextSpannable.length)))
                .append(ellipsizeText)
        } else {
            computedFoldedLastLineWidth = lineWidth + ellipsizeTextWidth
            newText.append(text.subSequence(0, start.coerceAtMost(text.length)))
                .append(lineTextSpannable)
                .append(ellipsizeText)
        }
        TextCalculationResult(computedExpandedLastLineWidth, computedFoldedLastLineWidth, newText)
    }

    /**
     * @return 返回需要被裁剪掉的文字的序号
     */
    private suspend fun getEndTextIndex(lineText: CharSequence, usedWidth: Float): Int {
        return withContext(Dispatchers.Default) {
            val lineTextLength = lineText.length
            if (lineTextLength <= 0) {
                return@withContext lineTextLength
            }
            val lineWidth = measureLineWidth(lineText)
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
            if (lastIndex < lineTextLength) {
                val str = lineText.subSequence(lastIndex, lineTextLength)
                val strWidth = measureLineWidth(str)
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
            val strLastWidth = measureLineWidth(strLast)

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
                    val strWidth = measureLineWidth(str)
                    if (strWidth < usedWidth) {
                        return@withContext safeClip(
                            lineText = lineText,
                            index = index - 1
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
            val strLastWidth = measureLineWidth(strLast)

            if (strLastWidth == usedWidth) {
                return@withContext safeClip(
                    lineText = lineText,
                    index = strLastIndex
                )
            } else if (strLastWidth < usedWidth) {
                for (j in 1..<calculateNum) {
                    val index = strLastIndex - j
                    val str = lineText.subSequence(index, lineTextLength)
                    val strWidth = measureLineWidth(str)
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
                val strWidth = measureLineWidth(str)
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
        if (index > 0 && index < lineText.length) {
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
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(textView.lineSpacingExtra, textView.lineSpacingMultiplier)
                .build()
        } else {
            StaticLayout(
                text,
                textView.paint,
                width,
                Layout.Alignment.ALIGN_NORMAL,
                textView.lineSpacingMultiplier,
                textView.lineSpacingExtra,
                true
            )
        }
    }

    /**
     * Measures the width of a single-line text span using [Layout.getDesiredWidth].
     * Much cheaper than constructing a [StaticLayout] when line-count is not needed.
     */
    private fun measureLineWidth(text: CharSequence): Float =
        Layout.getDesiredWidth(text, textView.paint)

    private var expandableButton: BaseExpandableButton = ExpandableText(context)

    override fun onFinishInflate() {
        super.onFinishInflate()

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