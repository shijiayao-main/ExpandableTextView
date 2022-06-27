package com.jiaoay.expandabletextview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.jiaoay.expandabletextview.databinding.ActivityMainBinding
import kotlin.math.exp

class MainActivity : AppCompatActivity() {
	private val binding by lazy {
		ActivityMainBinding.inflate(layoutInflater)
	}

	private val textList by lazy {
		listOf(R.string.long_test_text, R.string.short_test_text, R.string.num_test_text, R.string.hear_me)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)

		listOf(binding.text1, binding.text2, binding.text3, binding.text4).forEachIndexed { index, expandableTextView ->
			expandableTextView.setContent(
				text = getString(textList[index]),
				expanded = index % 2 != 0
			)
		}
	}
}