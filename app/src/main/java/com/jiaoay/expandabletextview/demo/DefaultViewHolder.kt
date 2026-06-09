package com.jiaoay.expandabletextview.demo

import android.content.Context
import com.jiaoay.expandabletextview.databinding.ViewHolderDefaultBinding

class DefaultViewHolder(
    context: Context,
    private val binding: ViewHolderDefaultBinding
) : AbstractListViewHolder<SectionHeaderInfo>(
    context,
    binding.root
) {
    override fun setData(data: SectionHeaderInfo) {
        binding.title.text = data.title
    }
}