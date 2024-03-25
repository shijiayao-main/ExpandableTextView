package com.jiaoay.expandabletextview.demo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.jiaoay.expandabletextview.databinding.ViewHolderDefaultBinding
import com.jiaoay.expandabletextview.databinding.ViewHolderExpandable2Binding
import com.jiaoay.expandabletextview.databinding.ViewHolderExpandableBinding

class ListAdapter : Adapter<AbstractListViewHolder<out ListDataInfo>>() {

    private val list: MutableList<ListDataInfo> = ArrayList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AbstractListViewHolder<*> {
        val context = parent.context
        return when (viewType) {
            DataInfoType.Expandable.type -> {
                val binding = ViewHolderExpandableBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                ExpandableViewHolder(
                    binding = binding,
                    context = context
                )
            }

            DataInfoType.Expandable2.type -> {
                val binding = ViewHolderExpandable2Binding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                ExpandableViewHolder2(
                    binding = binding,
                    context = context
                )
            }

            else -> {
                val binding = ViewHolderDefaultBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                DefaultViewHolder(
                    binding = binding,
                    context = context
                )
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val data = safeGetData(position)
        return data?.type ?: DataInfoType.Default.type
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: AbstractListViewHolder<*>, position: Int) {
        val data = safeGetData(position) ?: return
        when (data) {
            is ExpandableTextDataInfo -> {
                (holder as? ExpandableViewHolder)?.let {
                    holder.setData(data = data)
                }
            }

            is ExpandableTextDataInfo2 -> {
                (holder as? ExpandableViewHolder2)?.let {
                    holder.setData(data = data)
                }
            }

            else -> {}
        }
    }

    private fun safeGetData(position: Int): ListDataInfo? {
        if (list.isEmpty()) {
            return null
        }
        val listSize = list.size
        if (position in 0 until listSize) {
            return list[position]
        }
        return null
    }

    fun itemReset(list: List<ListDataInfo>) {
        this.list.clear()
        this.list.addAll(list)
        notifyDataSetChanged()
    }
}