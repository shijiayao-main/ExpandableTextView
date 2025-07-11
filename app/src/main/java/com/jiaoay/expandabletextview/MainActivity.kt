package com.jiaoay.expandabletextview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiaoay.expandabletextview.databinding.ActivityMainBinding
import com.jiaoay.expandabletextview.demo.DefaultDataInfo
import com.jiaoay.expandabletextview.demo.ExpandableTextDataInfo
import com.jiaoay.expandabletextview.demo.ExpandableTextDataInfo2
import com.jiaoay.expandabletextview.demo.ListAdapter

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val adapter = ListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.recyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.recyclerView.adapter = adapter

        val list = listOf(
            ExpandableTextDataInfo(
                text = getString(R.string.test2),
                expandIconResource = R.drawable.ic_launcher_background,
                foldIconResource = R.drawable.ic_launcher_background,
            ),
            ExpandableTextDataInfo(
                text = getString(R.string.test1),
                expandIconResource = R.drawable.ic_launcher_background,
                foldIconResource = R.drawable.ic_launcher_background,
            ),
            ExpandableTextDataInfo2(
                text = getString(R.string.test1),
                expandText = "[我喊你一声你敢点我吗]",
                foldText = "[?真点啊!]"
            ),
            ExpandableTextDataInfo2(
                text = getString(R.string.test2),
                expandText = "[我喊你一声你敢点我吗]",
                foldText = "[?真点啊!]"
            ),
            ExpandableTextDataInfo(
                text = getString(R.string.test3)
            ),
            ExpandableTextDataInfo2(
                text = getString(R.string.test4)
            ),
            ExpandableTextDataInfo(
                text = getString(R.string.test5)
            ),
            ExpandableTextDataInfo2(
                text = getString(R.string.test6)
            ),
            ExpandableTextDataInfo2(
                text = getString(R.string.test7)
            ),
        )

        adapter.itemReset(list = list)
    }
}