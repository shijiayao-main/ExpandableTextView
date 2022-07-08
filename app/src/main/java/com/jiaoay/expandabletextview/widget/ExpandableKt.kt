package com.jiaoay.expandabletextview.widget

import android.graphics.RectF
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

sealed class ExpandableIconType(var iconPaddingLeft: Int, var iconPaddingBottom: Int)

class ExpandableImageIcon(
    @DrawableRes val expandIcon: Int,
    @DrawableRes val foldIcon: Int,
    val iconWidth: Int = 0,
    val iconHeight: Int = 0,
    iconPaddingLeft: Int = 0,
    iconPaddingBottom: Int = 0,
) : ExpandableIconType(
    iconPaddingLeft = iconPaddingLeft,
    iconPaddingBottom = iconPaddingBottom
)

class ExpandableTextIcon(
    val expandText: String,
    val foldText: String,
    val textSize: Int = 0,
    @ColorRes val expandTextColor: Int = 0,
    @ColorRes val foldTextColor: Int = 0,
    iconPaddingLeft: Int = 0,
    iconPaddingBottom: Int = 0,
) : ExpandableIconType(
    iconPaddingLeft = iconPaddingLeft,
    iconPaddingBottom = iconPaddingBottom
)

data class ExpandableViewInfo(
    val charSequence: CharSequence,
    val height: Int,
    val iconRect: RectF
)