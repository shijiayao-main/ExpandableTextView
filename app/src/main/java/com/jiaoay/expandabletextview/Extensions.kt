package com.jiaoay.expandabletextview

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jiaoay.expandabletextview.widget.ExpandableIconType
import com.jiaoay.expandabletextview.widget.ExpandableTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

val mainScope = CoroutineScope(Dispatchers.Main)

val Float.dp2px: Float
	get() {
		val displayMetrics = Resources.getSystem().displayMetrics
		return TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_DIP,
			this,
			displayMetrics
		)
	}

val Int.dp2px: Int
	get() {
		val displayMetrics = Resources.getSystem().displayMetrics
		return TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_DIP,
			this.toFloat(),
			displayMetrics
		).toInt()
	}

val Context.toActivity: AppCompatActivity?
	get() {
		if (this is AppCompatActivity) {
			return this
		}

		if (this is ContextWrapper) {
			val baseContext = this.baseContext
			if (baseContext is AppCompatActivity) {
				return baseContext
			}
		}

		return null
	}

val Context.scope: CoroutineScope
	get() {
		return toActivity?.lifecycleScope ?: mainScope
	}

fun ExpandableTextView.setContent(
	content: CharSequence,
	iconType: ExpandableIconType,
	expandState: Boolean
) {
	expandableType = iconType
	contentText = content
	expandableCallback = object : ExpandableTextView.Callback {
		override fun onExpandClick() {
			changeExpendState(this@setContent.expandState.not())
		}

		override fun onFoldClick() {
			changeExpendState(this@setContent.expandState.not())
		}
	}
	changeExpendState(expandState)
}
