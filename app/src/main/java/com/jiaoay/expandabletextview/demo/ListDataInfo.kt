package com.jiaoay.expandabletextview.demo

import androidx.annotation.DrawableRes
import com.jiaoay.expandabletextview.R

sealed class ListDataInfo(val type: Int)

data object DefaultDataInfo : ListDataInfo(type = DataInfoType.Default.type)

data class ExpandableTextDataInfo(
    val text: String,
    @DrawableRes val expandIconResource: Int = R.drawable.ic_expand_down,
    @DrawableRes val foldIconResource: Int = R.drawable.ic_fold_up
) : ListDataInfo(type = DataInfoType.Expandable.type) {
    var isExpanded: Boolean = false
}

data class ExpandableTextDataInfo2(
    val text: String,
    val expandText: String = "[展开]",
    val foldText: String = "[收起]"
) : ListDataInfo(type = DataInfoType.Expandable2.type) {
    var isExpanded: Boolean = false
}
