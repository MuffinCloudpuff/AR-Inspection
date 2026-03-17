package com.rayneo.arsdk.android.demo.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.rayneo.arsdk.android.demo.databinding.ActivityScanResultBinding
import com.rayneo.arsdk.android.demo.ui.wedget.ResultButtonView
import com.rayneo.arsdk.android.ui.activity.BaseEventActivity

class ScanResultActivity : BaseEventActivity() {

    private lateinit var binding: ActivityScanResultBinding
    private lateinit var buttonView: ResultButtonView
    private var placeInfo: String = ""
    
    companion object {
        const val EXTRA_SCAN_RESULT = "scan_result"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取扫描结果
        val scanResult = intent.getStringExtra(EXTRA_SCAN_RESULT) ?: "未获取到扫描结果"
        
        // 从扫描结果中提取位置信息【】
        placeInfo = extractPlaceInfo(scanResult)
        
        // 显示扫描结果和提取的位置信息
        val displayText = if (placeInfo.isNotEmpty()) {
            "$scanResult\n\n提取到的位置：$placeInfo"
        } else {
            "$scanResult\n\n未找到有效位置信息"
        }
        
        binding.resultText.text = displayText
        binding.resultTextRight.text = displayText
        
        // 初始化按钮控制
        initButtonView()
        
        // 获取传递的任务信息
        val missionId = intent.getIntExtra("MISSION_ID", -1)
        val missionName = intent.getStringExtra("MISSION_NAME") ?: ""
        val missionTask = intent.getStringExtra("MISSION_TASK") ?: ""
        
        // 记录日志
        android.util.Log.d("ScanResultActivity", "收到的任务信息: ID=$missionId, 名称=$missionTask, 执行人=$missionName, 位置=$placeInfo")
        
        // 延迟2秒后自动跳转到相机拍照界面
        binding.root.postDelayed({
            if (placeInfo.isNotEmpty()) {
                // 跳转到相机拍照界面，并传递位置和任务信息
                val intent = Intent(this, CameraPhotoActivity::class.java).apply {
                    putExtra("PLACE_INFO", placeInfo)
                    putExtra("MISSION_ID", missionId)
                    putExtra("MISSION_NAME", missionName)
                    putExtra("MISSION_TASK", missionTask)
                }
                android.util.Log.d("ScanResultActivity", "传递给CameraPhotoActivity的信息: ID=$missionId, 名称=$missionTask, 执行人=$missionName, 位置=$placeInfo")
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "无法识别位置信息，请重新扫描", Toast.LENGTH_LONG).show()
            }
        }, 2000)
    }
    
    /**
     * 从扫描结果中提取位置信息，格式为【位置信息】
     */
    private fun extractPlaceInfo(scanResult: String): String {
        // 正则表达式匹配【】中的内容
        val regex = Regex("【(.*?)】")
        val matchResult = regex.find(scanResult)
        
        // 返回匹配到的内容，如果没有匹配到则返回空字符串
        return matchResult?.groupValues?.get(1) ?: ""
    }
    
    private fun initButtonView() {
        // 初始化按钮控制视图
        buttonView = ResultButtonView(this)
        buttonView.setupButton(binding.backButton)
        
        // 设置返回按钮点击事件
        buttonView.onBackButtonClickListener = {
            finish()
        }
        
        // 监听触控板事件
        buttonView.watchAction(this, templeActionViewModel)
    }
} 