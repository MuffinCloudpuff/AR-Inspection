package com.rayneo.arsdk.android.demo.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.rayneo.arsdk.android.demo.R
import com.rayneo.arsdk.android.demo.databinding.ActivityFunctionSelectionBinding
import com.rayneo.arsdk.android.demo.ui.wedget.FunctionButtonView
import com.rayneo.arsdk.android.ui.activity.BaseEventActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FunctionSelectionActivity : BaseEventActivity() {

    private lateinit var binding: ActivityFunctionSelectionBinding
    private lateinit var buttonView: FunctionButtonView
    private var completeButton: Button? = null
    
    // 任务信息
    private var missionId: Int = -1
    private var missionTask: String = ""
    private var missionName: String = ""
    
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )
    private val REQUEST_CODE_PERMISSIONS = 10

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            // 跳转到扫描结果页面，并传递任务信息
            val intent = Intent(this, ScanResultActivity::class.java).apply {
                putExtra(ScanResultActivity.EXTRA_SCAN_RESULT, result.contents)
                putExtra("MISSION_ID", missionId)
                putExtra("MISSION_NAME", missionName)
                putExtra("MISSION_TASK", missionTask)
            }
            
            // 输出传递的任务信息日志
            android.util.Log.d("FunctionSelectionActivity", "传递给ScanResultActivity的任务信息: ID=$missionId, 名称=$missionTask, 执行人=$missionName")
            
            startActivity(intent)
        } else {
            // 扫描被取消
            Toast.makeText(this, "扫描已取消", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFunctionSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取任务信息
        missionId = intent.getIntExtra("MISSION_ID", -1)
        missionTask = intent.getStringExtra("MISSION_TASK") ?: ""
        missionName = intent.getStringExtra("MISSION_NAME") ?: ""
        
        // 如果有任务信息，更新标题
        if (missionId != -1 && missionTask.isNotEmpty()) {
            binding.titleText.text = "正在执行任务：$missionTask"
            binding.titleTextRight.text = binding.titleText.text
            
            // 添加完成任务按钮
            addCompleteTaskButton()
        }

        initFunctionButtonView()
        
        // 如果是从任务页面进入，直接启动扫码
        if (missionId != -1) {
            if (allPermissionsGranted()) {
                startBarcodeScanner()
            } else {
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }
    
    /**
     * 添加任务完成按钮
     */
    private fun addCompleteTaskButton() {
        if (missionId <= 0) return
        
        // 创建完成任务按钮
        completeButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.camera_button_height)
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.button_margin)
            }
            text = "完成任务"
            setBackgroundResource(R.drawable.bg_complete_button)
            setTextColor(ContextCompat.getColor(this@FunctionSelectionActivity, android.R.color.white))
            textSize = 16f
            setOnClickListener {
                completeTask()
            }
        }
        
        // 添加到左侧布局
        binding.buttonContainerLeft.addView(completeButton)
        
        // 添加到右侧布局（不可点击）
        val completeButtonRight = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.camera_button_height)
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.button_margin)
            }
            text = "完成任务"
            setBackgroundResource(R.drawable.bg_complete_button)
            setTextColor(ContextCompat.getColor(this@FunctionSelectionActivity, android.R.color.white))
            textSize = 16f
            isClickable = false
            isFocusable = false
        }
        binding.buttonContainerRight.addView(completeButtonRight)
    }
    
    /**
     * 完成任务
     */
    private fun completeTask() {
        if (missionId <= 0) {
            Toast.makeText(this, "任务ID无效", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 不需要二次确认，直接调用API完成任务
        markTaskAsCompleted()
    }
    
    /**
     * 调用API将任务标记为已完成
     */
    private fun markTaskAsCompleted() {
        // 显示上传中提示
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.loadingText.text = "正在完成任务..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. 获取CSRF令牌
                val csrfResponse = com.rayneo.arsdk.android.demo.api.RetrofitClient.apiService.getCsrfToken()
                
                if (!csrfResponse.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FunctionSelectionActivity, "获取CSRF令牌失败", Toast.LENGTH_SHORT).show()
                        binding.loadingOverlay.visibility = View.GONE
                    }
                    return@launch
                }
                
                val csrfToken = csrfResponse.body()?.csrfToken ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FunctionSelectionActivity, "CSRF令牌为空", Toast.LENGTH_SHORT).show()
                        binding.loadingOverlay.visibility = View.GONE
                    }
                    return@launch
                }
                
                // 2. 调用完成任务API
                val response = com.rayneo.arsdk.android.demo.api.RetrofitClient.apiService.completeMission(missionId, csrfToken)
                
                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        Toast.makeText(this@FunctionSelectionActivity, "任务已完成", Toast.LENGTH_SHORT).show()
                        // 返回任务选择界面
                        val intent = Intent(this@FunctionSelectionActivity, TaskSelectionActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@FunctionSelectionActivity, "完成任务失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = View.GONE
                    Toast.makeText(this@FunctionSelectionActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initFunctionButtonView() {
        // 初始化按钮控制视图
        buttonView = FunctionButtonView(this)
        buttonView.setupButtons(binding.scanButton, binding.photoButton)
        
        // 设置按钮点击事件
        buttonView.onScanButtonClickListener = {
            if (allPermissionsGranted()) {
                startBarcodeScanner()
            } else {
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }
        
        buttonView.onPhotoButtonClickListener = {
            if (allPermissionsGranted()) {
                startCameraActivity()
            } else {
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }
        
        // 如果有完成按钮，添加到焦点控制
        completeButton?.let {
            buttonView.addCompleteButton(it)
        }
        
        // 监听触控板事件
        buttonView.watchAction(this, templeActionViewModel)
    }

    private fun startBarcodeScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("将二维码放入扫描区域")
        options.setCameraId(0) // 使用后置摄像头
        options.setBeepEnabled(true)
        options.setOrientationLocked(false)
        barcodeLauncher.launch(options)
    }

    private fun startCameraActivity() {
        // 跳转到相机拍照界面，并传递任务信息
        val intent = Intent(this, CameraPhotoActivity::class.java).apply {
            if (missionId != -1) {
                putExtra("MISSION_ID", missionId)
                putExtra("MISSION_NAME", missionName)
            }
        }
        startActivity(intent)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                // 权限获取成功，如果是从任务页面进入，直接启动扫码
                if (missionId != -1) {
                    startBarcodeScanner()
                }
            } else {
                Toast.makeText(
                    this,
                    "需要相机权限才能使用扫码或拍照功能",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
} 