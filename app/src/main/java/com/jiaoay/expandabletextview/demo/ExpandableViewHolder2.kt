package com.jiaoay.expandabletextview.demo

import android.content.Context
import com.jiaoay.expandabletextview.databinding.ViewHolderExpandable2Binding

class ExpandableViewHolder2(
    context: Context,
    private val binding: ViewHolderExpandable2Binding
) : AbstractListViewHolder<ExpandableTextDataInfo2>(
    context,
    binding.root
) {
    override fun setData(data: ExpandableTextDataInfo2) {
        binding.text.setText(
            text = data.text,
            isExpanded = data.isExpanded
        ) {
            data.isExpanded = it
        }

        binding.expandableText.setExpandableText(
            expandText = data.expandText,
            foldText = data.foldText
        )
    }
}