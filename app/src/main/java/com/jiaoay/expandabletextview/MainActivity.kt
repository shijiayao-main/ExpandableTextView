package com.jiaoay.expandabletextview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jiaoay.expandabletextview.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val viewList = listOf(binding.text1, binding.text2, binding.text3, binding.text4, binding.text5, binding.text6)
        listOf(
            getString(R.string.test1),
            getString(R.string.test2),
            getString(R.string.test3),
            getString(R.string.test4),
            getString(R.string.test5),
            getString(R.string.test6)
        ).forEachIndexed { index, s ->
            viewList[index].setText(
                text = s,
                isExpanded = false
            )
        }
    }
}