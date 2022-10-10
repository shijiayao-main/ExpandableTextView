package com.jiaoay.expandabletextview.widget

import android.view.View

interface BaseExpandableButton {
    // 被展开
    fun isExpanded()
    // 被折叠
    fun isFolded()

    // 获取展开按钮的宽度(由于可能还未measure, 需要手动测量)
    fun getButtonWidth(): Float

    fun getView(): View

    fun isVisible(): Boolean

    fun setVisible(isVisible: Boolean)

    fun getMeasuredWidth(): Int
    fun getMeasuredHeight(): Int
}