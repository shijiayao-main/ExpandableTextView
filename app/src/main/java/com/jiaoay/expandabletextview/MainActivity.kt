package com.jiaoay.expandabletextview

import android.graphics.ColorSpace
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import androidx.core.content.res.ResourcesCompat
import com.jiaoay.expandabletextview.databinding.ActivityMainBinding
import com.jiaoay.expandabletextview.widget.ExpandableIconType
import com.jiaoay.expandabletextview.widget.ExpandableImageIcon
import com.jiaoay.expandabletextview.widget.ExpandableTextIcon

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val textList by lazy {
        listOf(
            R.string.num_test_text,
            R.string.emoji
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        listOf(binding.text3, binding.text5).forEachIndexed { index, expandableTextView ->
            expandableTextView.setContent(
                content = SpannableString(getString(textList[index])),
                expandState = false,
                iconType = ExpandableImageIcon(
                    expandIcon = R.mipmap.ic_launcher,
                    foldIcon = R.mipmap.ic_launcher_round
                )
            )
        }

        binding.text1.setContent(
            content = SpannableString(getString(R.string.long_test_text)),
            expandState = false,
            iconType = ExpandableTextIcon(
                expandText = "【把文字展开】",
                foldText = "【把文字收起】",
                expandTextColor = R.color.purple_200,
                foldTextColor = R.color.teal_200
            )
        )

        binding.text4.setContent(
            content = SpannableString(getString(R.string.hear_me)),
            expandState = false,
            iconType = ExpandableTextIcon(
                expandText = "展开",
                foldText = "收起"
            )
        )

        val contentText = getString(R.string.short_test_text)

        val spannableStringBuilder = SpannableStringBuilder(contentText)
        spannableStringBuilder.setSpan(
            ForegroundColorSpan(ResourcesCompat.getColor(resources, R.color.purple_500, theme)),
            contentText.length / 2,
            contentText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.text2.setContent(
            iconType = ExpandableImageIcon(
                expandIcon = R.drawable.ic_expand_down,
                foldIcon = R.drawable.ic_fold_up
            ),
            content = spannableStringBuilder,
            expandState = false
        )
    }
}