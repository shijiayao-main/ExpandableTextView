package com.jiaoay.expandabletextview.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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

	private var iconPaddingLeft: Int = 8.dp2px

	@DrawableRes
	private var expandTextIcon: Int = R.drawable.ic_expand_down

	@DrawableRes
	private var foldTextIcon: Int = R.drawable.ic_fold_up

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
			expandTextIcon = typedArray
				.getResourceId(
					R.styleable.ExpandableTextView_expand_icon,
					R.drawable.ic_expand_down
				)
			foldTextIcon = typedArray
				.getResourceId(
					R.styleable.ExpandableTextView_fold_icon,
					R.drawable.ic_fold_up
				)
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
		measureText()
	}

	private fun initDrawable(@DrawableRes iconResource: Int): Drawable? {
		val textHeight = paint.fontMetrics.descent - paint.fontMetrics.ascent
		return ResourcesCompat.getDrawable(resources, iconResource, context.theme)?.apply {
			var iconWidth: Float = intrinsicWidth.toFloat()
			var iconHeight: Float = intrinsicHeight.toFloat()
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

	private suspend fun getEndTextIndex(lineText: CharSequence, usedWidth: Float): Int = withContext(Dispatchers.IO) {
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
		var usedWidth = drawableWidth + ellipsizeTextWidth + iconPaddingLeft
		if (gravity == Gravity.TOP or Gravity.CENTER_HORIZONTAL) {
			usedWidth *= 2
		}
		return usedWidth
	}

	private suspend fun onBuildExpendText(staticLayout: StaticLayout): SpannableStringBuilder {
		val lineCount = maxLineCount
		iconDrawable = initDrawable(expandTextIcon)

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
			lineTextWidth = getStaticLayout(lineTextSpannable.subSequence(0, endIndex)).getLineWidth(0) + paint.measureText(ellipsizeText)
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
			iconRight = spaceLeft + lineTextWidth + (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft
		} else {
			iconLeft = lineTextWidth + paddingLeft
			iconRight = lineTextWidth + paddingLeft + (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft
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
		iconDrawable = initDrawable(foldTextIcon)

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
				val spaceWidth = (measuredWidth - paddingLeft - paddingRight - (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft.toFloat()) / 2
				iconLeft = spaceWidth
				iconRight = spaceWidth + (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft.toFloat()
			} else {
				iconLeft = paddingLeft.toFloat()
				iconRight = paddingLeft + (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft.toFloat()
			}
			textHeight = staticLayout.height + lineTextHeight.coerceAtLeast(iconDrawable?.bounds?.height() ?: 0)
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
				iconRight = spaceLeft + lineTextWidth + (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft
			} else {
				iconLeft = lineTextWidth + paddingLeft
				iconRight = lineTextWidth + paddingLeft + (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft
			}
			textHeight = staticLayout.height
			iconRect.set(iconLeft, iconTop, iconRight, iconBottom)
		}
		expandableCallback?.onExpand()
		setMeasuredDimension(measuredWidth, textHeight)
		return contentText
	}

	private fun measureText() {
		context.scope.launch {

			val staticLayout = getStaticLayout(text = contentText)
			val staticLineCount = staticLayout.lineCount

			if (staticLineCount > maxLineCount) {
				if (expandState) {
					val result = onBuildFoldText(staticLayout = staticLayout)
					setText(result, BufferType.SPANNABLE)
				} else {
					// 最终显示的文字
					val result = onBuildExpendText(staticLayout = staticLayout)
					setText(result, BufferType.SPANNABLE)
				}
			} else {
				val result = SpannableString(contentText)
				expandableCallback?.onLoss()
				// 重新计算高度
				setMeasuredDimension(measuredWidth, staticLayout.height)
				setText(result, BufferType.SPANNABLE)
			}
		}
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
		val iconTop = iconDrawable?.intrinsicHeight?.let {
			iconRect.height() - it
		} ?: 0f
		canvas.translate(iconRect.left + iconPaddingLeft, iconRect.top + iconTop)
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
		requestLayout()
	}
}
