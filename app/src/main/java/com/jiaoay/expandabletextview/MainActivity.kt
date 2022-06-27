package com.jiaoay.expandabletextview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import androidx.core.content.res.ResourcesCompat
import com.jiaoay.expandabletextview.databinding.ActivityMainBinding

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

//		listOf(binding.text3, binding.text5).forEachIndexed { index, expandableTextView ->
//			expandableTextView.setContent(
//				content = SpannableString(getString(textList[index])),
//				expandState = false
//			)
//		}
//
//		binding.text1.setContent(
//			SpannableString(getString(R.string.long_test_text)),
//			false
//		)
//
//		binding.text4.setContent(
//			SpannableString(getString(R.string.hear_me)),
//			expandState = false
//		)

		val appendText = getString(R.string.num_test_text)
		val contentText = getString(R.string.short_test_text)

		val spannableStringBuilder = SpannableStringBuilder(contentText)
			.append(appendText)
		ResourcesCompat.getDrawable(resources, R.drawable.ic_launcher_background, theme)?.let {
			it.setBounds(0, 0, 16.dp2px, 16.dp2px)
			spannableStringBuilder.setSpan(
				ImageSpan(it),
				contentText.length,
				contentText.length + appendText.length,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}

		binding.text2.setContent(
			content = spannableStringBuilder,
			expandState = false
		)
	}
}