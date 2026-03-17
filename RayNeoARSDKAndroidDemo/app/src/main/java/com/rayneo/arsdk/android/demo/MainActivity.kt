package com.rayneo.arsdk.android.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.rayneo.arsdk.android.demo.databinding.ActivityMainBinding
import com.rayneo.arsdk.android.demo.ui.activity.*
import com.rayneo.arsdk.android.demo.ui.wedget.OnTitleSelectListener
import com.rayneo.arsdk.android.ui.activity.BaseEventActivity

class MainActivity : BaseEventActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate started")
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.phoneTitleView.apply {
            setTitles(
                arrayOf(
                    "任务列表",
                    "功能选择",
                    "相机预览",
                    "对话框",
                    "固定焦点",
                    "移动焦点",
                    "Fragment示例"
                )
            )
            watchAction(this@MainActivity, templeActionViewModel)
            hasFocus = true
        }
        initEvent()

        // 默认打开任务选择界面，但不要结束当前Activity
        Log.d(TAG, "Starting TaskSelectionActivity")
        startActivity(Intent(this, TaskSelectionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        })
    }

    private fun initEvent() {
        binding.phoneTitleView.onTitleSelectListener = object : OnTitleSelectListener {
            override fun onTitleSelect(pos: Int, titleView: TextView) {
                val intent = when (pos) {
                    0 -> Intent(this@MainActivity, TaskSelectionActivity::class.java)
                    1 -> Intent(this@MainActivity, FunctionSelectionActivity::class.java)
                    2 -> Intent(this@MainActivity, CameraPreviewActivity::class.java)
                    3 -> Intent(this@MainActivity, DialogActivity::class.java)
                    4 -> Intent(this@MainActivity, FixedFocusPosRVActivity::class.java)
                    5 -> Intent(this@MainActivity, MovedFocusPosRVActivity::class.java)
                    6 -> Intent(this@MainActivity, FragmentDemoActivity::class.java)
                    else -> return
                }
                // 添加标志以防止创建多个实例
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }
        }
    }
} 