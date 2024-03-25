package com.jiaoay.expandabletextview.demo

import android.content.Context
import com.jiaoay.expandabletextview.databinding.ViewHolderDefaultBinding

class DefaultViewHolder(
    context: Context,
    binding: ViewHolderDefaultBinding
) : AbstractListViewHolder<DefaultDataInfo>(
    context,
    binding.root
) {
    override fun setData(data: DefaultDataInfo) {
    }
}