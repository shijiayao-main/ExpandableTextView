package com.jiaoay.expandabletextview.demo

import android.content.Context
import android.view.View
import com.jiaoay.expandabletextview.databinding.ViewHolderExpandableBinding

class ExpandableViewHolder(
    context: Context,
    private val binding: ViewHolderExpandableBinding
) : AbstractListViewHolder<ExpandableTextDataInfo>(
    context,
    binding.root
) {
    override fun setData(data: ExpandableTextDataInfo) {
        if (data.label.isNotEmpty()) {
            binding.label.visibility = View.VISIBLE
            binding.label.text = data.label
        } else {
            binding.label.visibility = View.GONE
        }

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