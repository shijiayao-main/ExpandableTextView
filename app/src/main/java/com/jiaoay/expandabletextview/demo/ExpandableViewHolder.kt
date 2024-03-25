package com.jiaoay.expandabletextview.demo

import android.content.Context
import com.jiaoay.expandabletextview.databinding.ViewHolderExpandableBinding

class ExpandableViewHolder(
    context: Context,
    private val binding: ViewHolderExpandableBinding
) : AbstractListViewHolder<ExpandableTextDataInfo>(
    context,
    binding.root
) {
    override fun setData(data: ExpandableTextDataInfo) {
        binding.text.setText(
            text = data.text,
            isExpanded = data.isExpanded
        ) {
            data.isExpanded = it
        }

        binding.expandableIcon.setExpandableIcon(
            expandIconResource = data.expandIconResource,
            foldIconResource = data.foldIconResource,
        )
    }
}