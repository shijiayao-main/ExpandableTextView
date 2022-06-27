package com.jiaoay.expandabletextview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
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
import kotlinx.coroutines.launch

class ExpandableTextView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs), OnClickListener {

	var spannableBuildExtension: ((SpannableString) -> Unit)? = null

	var expandableCallback: Callback? = null

	// 展开状态 true：展开，false：收起
	var expandState: Boolean = false

	// 源文字内容
	var contentText: String? = ""

	// 最多展示的行数
	var maxLineCount = 3

	// 省略文字
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
				typedArray.getInt(R.styleable.ExpandableTextView_max_expand_line, 3).toInt()
			typedArray.recycle()
		}
	}

	private fun getStaticLayout(canUseWidth: Int): StaticLayout {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			StaticLayout.Builder
				.obtain(
					contentText ?: "",
					0,
					contentText?.length ?: 0,
					paint,
					canUseWidth
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
				contentText,
				paint,
				canUseWidth,
				Layout.Alignment.ALIGN_CENTER,
				lineSpacingMultiplier,
				lineSpacingExtra,
				true
			)
		}
	}

	private var iconDrawable: Drawable? = null

	private fun onBuildExpendText(
		staticLayout: StaticLayout,
		canUseWidth: Int,
		lineHeight: Float
	): SpannableString {
		val lineCount = maxLineCount
		iconDrawable = ContextCompat.getDrawable(context, expandTextIcon)?.apply {
			setBounds(0, 0, intrinsicWidth, intrinsicHeight)
		}

		// 省略文字和展开文案的宽度
		var dotWidth = paint.measureText(ellipsizeText) + (
				iconDrawable?.bounds?.width()
					?: 0
				) + iconPaddingLeft

		if (gravity == Gravity.TOP or Gravity.CENTER_HORIZONTAL) {
			dotWidth *= 2
		}

		// 找出显示最后一行的文字
		val start = staticLayout.getLineStart(lineCount - 1)
		val end = staticLayout.getLineEnd(lineCount - 1)
		var lineText = contentText?.substring(start, end) ?: ""
		// 将第最后一行最后的文字替换为 ellipsizeText和expandText
		// 如果有换行符的话，会出现收齐状态显示未占满全文状态，那么先判断收齐状态的情况下是否有换行符，然后文字内容加上省略符号是否超过可用宽度
		lineText = lineText.replace("\r\n", "", true)
		lineText = lineText.replace("\n", "", true)
		val newText: CharSequence
		val lineTextWidth: Float
		if ((paint.measureText(lineText) + dotWidth) > canUseWidth) {
			var endIndex = 0
			for (i in lineText.length - 1 downTo 0) {
				val str = lineText.substring(i, lineText.length)
				// 找出文字宽度大于 ellipsizeText 的字符
				val strWidth = paint.measureText(str)
				if (strWidth >= dotWidth) {
					if (i > 1) {
						val secondChar = lineText[i]
						val firstChar = lineText[i - 1]
						if (Character.isSurrogatePair(firstChar, secondChar)) {
							endIndex = i - 1
							break
						}
					}
					endIndex = i
					break
				}
			}
			val lastLineText = lineText.substring(0, endIndex) + ellipsizeText
			lineTextWidth = paint.measureText(lastLineText)
			// 新的文字
			newText = (contentText?.substring(0, start) ?: "") + lastLineText
		} else {
			lineTextWidth = paint.measureText(lineText + ellipsizeText)
			newText = (contentText?.substring(0, start) ?: "") + lineText + ellipsizeText
		}

		val spannableString = SpannableString(newText)

		// 仅处理center_horizontal的情况
		if (gravity == Gravity.TOP or Gravity.CENTER_HORIZONTAL) {
			val spaceLeft = (measuredWidth - lineTextWidth - paddingLeft - paddingRight) / 2
			iconRect.set(
				spaceLeft + lineTextWidth,
				lineHeight * (lineCount - 1),
				spaceLeft + lineTextWidth + (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft,
				lineHeight * (lineCount)
			)
		} else {
			iconRect.set(
				lineTextWidth + paddingLeft,
				lineHeight * (lineCount - 1),
				lineTextWidth + paddingLeft + (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft,
				lineHeight * (lineCount)
			)
		}

		return spannableString
	}

	@SuppressLint("DrawAllocation")
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)

		if (measuredWidth == 0) {
			setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
			if (measuredWidth == 0) {
				return
			}
		}
		if ((contentText?.length ?: 0) == 0) {
			return
		}
		measureText()
	}

	private fun measureText() {
		context.scope.launch {
			val canUseWidth = measuredWidth
			val staticLayout = getStaticLayout(canUseWidth)
			// 总计行数
			var staticLineCount = staticLayout.lineCount
			// 总行数大于最大行数
			val lineHeight: Float = staticLayout.height.toFloat() / staticLayout.lineCount
			if (staticLineCount > maxLineCount) {
				if (expandState) {
					val result = SpannableString(contentText)
					// 是否支持收起功能
					iconDrawable = ContextCompat.getDrawable(context, foldTextIcon)?.apply {
						setBounds(
							0,
							0,
							intrinsicWidth,
							intrinsicHeight
						)
					}

					// 省略文字和展开文案的宽度
					var dotWidth = paint.measureText(ellipsizeText) + (
							iconDrawable?.bounds?.width()
								?: 0
							) + iconPaddingLeft

					if (gravity == Gravity.TOP or Gravity.CENTER_HORIZONTAL) {
						dotWidth *= 2
					}

					// 找出显示最后一行的文字
					val start = staticLayout.getLineStart(staticLineCount - 1)
					val end = staticLayout.getLineEnd(staticLineCount - 1)
					val lineText = (contentText?.substring(start, end) ?: "")

					val lineTextWidth = paint.measureText(lineText)
					if ((lineTextWidth + dotWidth) > canUseWidth) {
						staticLineCount += 1
						if (gravity == Gravity.TOP or Gravity.CENTER_HORIZONTAL) {
							val spaceWidth = (measuredWidth - paddingLeft - paddingRight - (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft.toFloat()) / 2
							iconRect.set(
								spaceWidth,
								lineHeight * (staticLineCount - 1),
								spaceWidth + (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft.toFloat(),
								lineHeight * (staticLineCount)
							)
						} else {
							iconRect.set(
								paddingLeft.toFloat(),
								lineHeight * (staticLineCount - 1),
								paddingLeft + (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft.toFloat(),
								lineHeight * (staticLineCount)
							)
						}
					} else {
						if (gravity == Gravity.TOP or Gravity.CENTER_HORIZONTAL) {
							val spaceLeft = (measuredWidth - lineTextWidth - paddingLeft - paddingRight) / 2
							iconRect.set(
								spaceLeft + lineTextWidth,
								lineHeight * (staticLineCount - 1),
								spaceLeft + lineTextWidth + (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft,
								lineHeight * (staticLineCount)
							)
						} else {
							iconRect.set(
								lineTextWidth + paddingLeft,
								lineHeight * (staticLineCount - 1),
								lineTextWidth + paddingLeft + (iconDrawable?.bounds?.width() ?: 0) + iconPaddingLeft,
								lineHeight * (staticLineCount)
							)
						}
					}
					// 重新计算高度
					spannableBuildExtension?.invoke(result)
					expandableCallback?.onExpand()
					measureTextHeight(staticLineCount = staticLineCount, lineHeight = lineHeight)
					setText(result, BufferType.SPANNABLE)
				} else {
					staticLineCount = maxLineCount
					// 最终显示的文字
					val result = onBuildExpendText(staticLayout = staticLayout, canUseWidth = canUseWidth, lineHeight = lineHeight)
					spannableBuildExtension?.invoke(result)
					expandableCallback?.onCollapse()
					// 重新计算高度
					measureTextHeight(staticLineCount = staticLineCount, lineHeight = lineHeight)
					setText(result, BufferType.SPANNABLE)
				}
			} else {
				val result = SpannableString(contentText)
				spannableBuildExtension?.invoke(result)
				expandableCallback?.onLoss()
				// 重新计算高度
				measureTextHeight(staticLineCount = staticLineCount, lineHeight = lineHeight)
				setText(result, BufferType.SPANNABLE)
			}
		}
	}

	private fun measureTextHeight(staticLineCount: Int, lineHeight: Float) {
		var viewHeight: Int = (lineHeight * staticLineCount).toInt()
		viewHeight += (paddingTop + paddingBottom)
		setMeasuredDimension(measuredWidth, viewHeight + 1)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
//        canvas.drawRect(iconRect, iconPaint)
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
