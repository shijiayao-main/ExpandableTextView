package com.jiaoay.expandabletextview.demo

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder

abstract class AbstractListViewHolder<T : ListDataInfo>(
    val context: Context,
    itemView: View
) : ViewHolder(itemView) {
    abstract fun setData(data: T)
}