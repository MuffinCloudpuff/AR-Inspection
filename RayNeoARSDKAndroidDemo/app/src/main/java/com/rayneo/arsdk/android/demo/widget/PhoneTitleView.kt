package com.rayneo.arsdk.android.demo.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rayneo.arsdk.android.demo.R
import com.rayneo.arsdk.android.touch.TempleActionViewModel

class PhoneTitleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val recyclerView: RecyclerView
    private val adapter: TitleAdapter
    var onTitleSelectListener: OnTitleSelectListener? = null
    var hasFocus: Boolean = false
        set(value) {
            field = value
            adapter.notifyDataSetChanged()
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_phone_title_view, this, true)
        recyclerView = findViewById(R.id.recyclerView)
        adapter = TitleAdapter()
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter
    }

    fun setTitles(titles: Array<String>) {
        adapter.setData(titles.toList())
    }

    fun watchAction(lifecycleOwner: LifecycleOwner, viewModel: TempleActionViewModel) {
        // 实现触摸事件监听
    }
} 