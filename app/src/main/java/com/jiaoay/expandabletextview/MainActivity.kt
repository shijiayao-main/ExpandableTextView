package com.jiaoay.expandabletextview

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiaoay.expandabletextview.databinding.ActivityMainBinding
import com.jiaoay.expandabletextview.demo.ExpandableTextDataInfo
import com.jiaoay.expandabletextview.demo.ExpandableTextDataInfo2
import com.jiaoay.expandabletextview.demo.ListAdapter
import com.jiaoay.expandabletextview.demo.SectionHeaderInfo

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
        adapter.itemReset(list = buildDemoList())
    }

    private fun buildDemoList() = listOf(
        // ── Section 1: Icon Button ────────────────────────────────────────────
        SectionHeaderInfo(getString(R.string.header_icon_button)),

        ExpandableTextDataInfo(
            label = "短文字（不展示按钮）",
            text = getString(R.string.demo_short)
        ),
        ExpandableTextDataInfo(
            label = "较长中文内容",
            text = getString(R.string.demo_long_zh)
        ),
        ExpandableTextDataInfo(
            label = "较长英文内容",
            text = getString(R.string.demo_long_en)
        ),
        ExpandableTextDataInfo(
            label = "含 Emoji 文字（验证代理对截断）",
            text = getString(R.string.demo_emoji)
        ),
        ExpandableTextDataInfo(
            label = "默认展开状态（isDefaultExpanded = true）",
            text = getString(R.string.demo_default_expanded),
            isDefaultExpanded = true
        ),

        // ── Section 2: Text Button ────────────────────────────────────────────
        SectionHeaderInfo(getString(R.string.header_text_button)),

        ExpandableTextDataInfo2(
            label = "短文字（不展示按钮）",
            text = getString(R.string.demo_short)
        ),
        ExpandableTextDataInfo2(
            label = "较长中文内容",
            text = getString(R.string.demo_long_zh)
        ),
        ExpandableTextDataInfo2(
            label = "自定义按钮文字",
            text = getString(R.string.demo_long_en),
            expandText = "查看更多",
            foldText = "收起"
        ),
        ExpandableTextDataInfo2(
            label = "默认展开状态（isDefaultExpanded = true）",
            text = getString(R.string.demo_default_expanded),
            isDefaultExpanded = true
        ),

        // ── Section 3: Special Scenarios ─────────────────────────────────────
        SectionHeaderInfo(getString(R.string.header_special)),

        ExpandableTextDataInfo(
            label = getString(R.string.demo_spannable_label),
            text = buildSpannableWithLink()
        ),
        ExpandableTextDataInfo2(
            label = getString(R.string.demo_color_span_label),
            text = buildSpannableWithColors()
        ),
        ExpandableTextDataInfo(
            label = getString(R.string.demo_unicode_label),
            text = getString(R.string.test1)
        ),
        ExpandableTextDataInfo2(
            label = getString(R.string.demo_mixed_label),
            text = getString(R.string.test6)
        ),
    )

    private fun buildSpannableWithLink(): CharSequence {
        val prefix = "ExpandableTextView 已开源，欢迎访问项目主页："
        val linkText = "github.com/shijiayao-main/ExpandableTextView"
        val suffix = "，欢迎 Star 和提交 Issue。如果你在项目中使用了这个控件，也欢迎告知作者！"

        return SpannableStringBuilder().apply {
            append(prefix)
            val linkStart = length
            append(linkText)
            setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        Toast.makeText(widget.context, "打开链接：$linkText", Toast.LENGTH_SHORT).show()
                    }
                },
                linkStart, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            append(suffix)
        }
    }

    private fun buildSpannableWithColors(): CharSequence {
        val parts = listOf(
            "这是" to Color.parseColor("#333333"),
            "红色" to Color.parseColor("#E53935"),
            "、" to Color.parseColor("#333333"),
            "蓝色" to Color.parseColor("#1E88E5"),
            "、" to Color.parseColor("#333333"),
            "绿色" to Color.parseColor("#43A047"),
            "混合的富文本内容，用于验证 ExpandableTextView 在含有 ForegroundColorSpan 时的截断效果是否正常。超过最大行数的部分会被隐藏，展开按钮也应正常显示在末尾。" to Color.parseColor("#555555"),
        )
        return SpannableStringBuilder().apply {
            for ((text, color) in parts) {
                val start = length
                append(text)
                setSpan(ForegroundColorSpan(color), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}