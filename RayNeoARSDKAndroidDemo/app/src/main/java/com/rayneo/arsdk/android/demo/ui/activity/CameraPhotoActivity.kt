package com.rayneo.arsdk.android.demo.ui.activity

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rayneo.arsdk.android.demo.R
import com.rayneo.arsdk.android.demo.api.RetrofitClient
import com.rayneo.arsdk.android.demo.databinding.ActivityCameraPhotoBinding
import com.rayneo.arsdk.android.demo.ui.wedget.CameraButtonView
import com.rayneo.arsdk.android.touch.TempleActionViewModel
import com.rayneo.arsdk.android.ui.activity.BaseEventActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPhotoActivity : BaseEventActivity() {

    private lateinit var binding: ActivityCameraPhotoBinding
    private lateinit var buttonView: CameraButtonView
    private lateinit var cameraExecutor: ExecutorService
    private val TAG = "CameraPhotoActivity"
    private var imageCapture: ImageCapture? = null
    private var lastCapturedImageUri: Uri? = null
    private var lastCapturedImageFile: File? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val REQUEST_CODE_PERMISSIONS = 10
    private var isPreviewMode = true // 是否处于预览模式
    private var isUploading = false // 是否正在上传
    
    // 任务相关
    private var missionId: Int = -1
    private var missionName: String = ""
    private var missionTask: String = ""
    private var placeInfo: String = ""
    private var completeButton: Button? = null
    
    // 是否处理任务模式
    private var isTaskMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 获取任务和位置信息
        missionId = intent.getIntExtra("MISSION_ID", -1)
        missionName = intent.getStringExtra("MISSION_NAME") ?: ""
        missionTask = intent.getStringExtra("MISSION_TASK") ?: ""
        placeInfo = intent.getStringExtra("PLACE_INFO") ?: ""
        
        // 记录接收到的任务信息
        Log.d(TAG, "接收到的任务信息: ID=$missionId, 名称=$missionTask, 执行人=$missionName, 位置=$placeInfo")
        
        // 判断是否处于任务模式
        isTaskMode = missionId != -1 && missionName.isNotEmpty()
        
        // 如果在任务模式下，显示任务信息
        if (isTaskMode) {
            binding.taskInfo.text = "任务: $missionTask\n执行人: $missionName\n位置: $placeInfo"
            binding.taskInfoRight.text = binding.taskInfo.text
            binding.taskInfoContainer.visibility = View.VISIBLE
            binding.taskInfoContainerRight.visibility = View.VISIBLE
        } else {
            binding.taskInfoContainer.visibility = View.GONE
            binding.taskInfoContainerRight.visibility = View.GONE
        }

        // 初始化相机执行器
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 检查相机权限
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // 初始化拍照按钮控制
        initCameraButtonView()
        
        // 如果在任务模式下，显示任务完成按钮（在按钮控制视图初始化后添加）
        if (isTaskMode) {
            addCompleteTaskButton()
        }
    }
    
    /**
     * 添加任务完成按钮
     */
    private fun addCompleteTaskButton() {
        completeButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                resources.getDimensionPixelSize(R.dimen.camera_button_height),
                1.0f
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.button_margin)
            }
            text = "完成任务"
            setBackgroundResource(R.drawable.bg_complete_button)
            setTextColor(ContextCompat.getColor(this@CameraPhotoActivity, android.R.color.white))
            textSize = 16f
            setOnClickListener {
                completeTask()
            }
        }
        
        // 添加到布局中
        binding.buttonContainer.addView(completeButton)
        
        // 右侧界面添加相同按钮（不可点击）
        val completeButtonRight = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                resources.getDimensionPixelSize(R.dimen.camera_button_height),
                1.0f
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.button_margin)
            }
            text = "完成任务"
            setBackgroundResource(R.drawable.bg_complete_button)
            setTextColor(ContextCompat.getColor(this@CameraPhotoActivity, android.R.color.white))
            textSize = 16f
            isClickable = false
            isFocusable = false
        }
        binding.buttonContainerRight.addView(completeButtonRight)
        
        // 将按钮添加到焦点控制
        buttonView.addCompleteButton(completeButton!!)
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
     * 显示确认对话框
     */
    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定") { _, _ -> onConfirm() }
            .setNegativeButton("取消") { _, _ -> onCancel() }
            .create()
        
        dialog.show()
    }
    
    /**
     * 调用API将任务标记为已完成
     */
    private fun markTaskAsCompleted() {
        lifecycleScope.launch {
            try {
                // 显示上传中对话框
                showUploadingOverlay(true)
                binding.uploadStatusText.text = "正在获取CSRF令牌..."
                
                // 1. 获取CSRF令牌
                val csrfResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getCsrfToken()
                }
                
                if (!csrfResponse.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CameraPhotoActivity, "获取CSRF令牌失败", Toast.LENGTH_SHORT).show()
                        showUploadingOverlay(false)
                    }
                    return@launch
                }
                
                val csrfToken = csrfResponse.body()?.csrfToken ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CameraPhotoActivity, "CSRF令牌为空", Toast.LENGTH_SHORT).show()
                        showUploadingOverlay(false)
                    }
                    return@launch
                }
                
                // 更新状态
                withContext(Dispatchers.Main) {
                    binding.uploadStatusText.text = "正在完成任务..."
                }
                
                // 2. 调用完成任务API
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.completeMission(missionId, csrfToken)
                }
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CameraPhotoActivity, "任务已完成", Toast.LENGTH_SHORT).show()
                        // 返回任务选择界面
                        val intent = Intent(this@CameraPhotoActivity, TaskSelectionActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@CameraPhotoActivity, "完成任务失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                    showUploadingOverlay(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "完成任务出错", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CameraPhotoActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                    showUploadingOverlay(false)
                }
            }
        }
    }

    private fun initCameraButtonView() {
        // 初始化按钮控制视图
        buttonView = CameraButtonView(this)
        buttonView.setupButtons(binding.backButton, binding.captureButton, binding.retakeButton, binding.uploadButton)
        
        // 设置返回按钮点击事件
        buttonView.onBackButtonClickListener = {
            // 如果正在任务模式，则返回到二维码扫描界面，否则直接结束
            if (isTaskMode && placeInfo.isNotEmpty()) {
                val intent = Intent(this, FunctionSelectionActivity::class.java).apply {
                    putExtra("MISSION_ID", missionId)
                    putExtra("MISSION_NAME", missionName)
                    putExtra("MISSION_TASK", missionTask)
                }
                startActivity(intent)
            }
            finish()
        }
        
        // 设置拍照按钮点击事件
        buttonView.onCaptureButtonClickListener = {
            takePhoto()
        }
        
        // 设置重拍按钮点击事件
        buttonView.onRetakeButtonClickListener = {
            switchToPreviewMode()
        }
        
        // 设置上传按钮点击事件
        buttonView.onUploadButtonClickListener = {
            if (!isUploading) {
                uploadPhoto()
            }
        }
        
        // 监听触控板事件
        buttonView.watchAction(this, templeActionViewModel)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // 获取相机提供者实例
            cameraProvider = cameraProviderFuture.get()

            // 设置预览
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // 右眼预览（复制左眼内容）
            val previewRight = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinderRight.surfaceProvider)
                }

            // 设置图片捕获
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // 选择后置相机
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // 解绑所有之前的用例
                cameraProvider?.unbindAll()

                // 绑定用例到相机
                camera = cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, previewRight, imageCapture
                )
                
                // 确保处于预览模式
                switchToPreviewMode()
                
            } catch (exc: Exception) {
                Log.e(TAG, "相机绑定失败", exc)
                Toast.makeText(this, "相机启动失败", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // 获取ImageCapture实例引用
        val imageCapture = imageCapture ?: return

        // 创建时间戳名称和MediaStore条目
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.CHINA)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RayNeoDemo-Image")
            }
        }

        // 创建输出选项对象
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // 设置图片捕获监听器
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "拍照失败: ${exc.message}", exc)
                    Toast.makeText(this@CameraPhotoActivity, "拍照失败", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    lastCapturedImageUri = savedUri
                    
                    // 将Uri转换为File以便上传
                    savedUri?.let { uri ->
                        try {
                            val inputStream = contentResolver.openInputStream(uri)
                            val tempFile = File(cacheDir, "upload_photo_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(tempFile).use { outputStream ->
                                inputStream?.use { input ->
                                    input.copyTo(outputStream)
                                }
                            }
                            lastCapturedImageFile = tempFile
                        } catch (e: IOException) {
                            Log.e(TAG, "创建临时文件失败: ${e.message}", e)
                        }
                    }
                    
                    val msg = "拍照成功: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    
                    // 切换到拍照完成模式
                    switchToRetakeMode()
                }
            }
        )
    }
    
    // 上传照片到服务器
    private fun uploadPhoto() {
        val file = lastCapturedImageFile ?: run {
            Toast.makeText(this, "没有可上传的照片", Toast.LENGTH_SHORT).show()
            return
        }
        
        isUploading = true
        showUploadingOverlay(true)
        binding.uploadStatusText.text = "正在获取CSRF令牌..."
        
        lifecycleScope.launch {
            try {
                // 1. 获取CSRF令牌
                val csrfResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getCsrfToken()
                }

                Log.d(TAG, "CSRF响应: 状态码=${csrfResponse.code()}, 头部=${csrfResponse.headers()}")

                if (!csrfResponse.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CameraPhotoActivity, "获取CSRF令牌失败: ${csrfResponse.code()}", Toast.LENGTH_SHORT).show()
                        isUploading = false
                        showUploadingOverlay(false)
                    }
                    return@launch
                }
                
                // 获取令牌
                val csrfToken = csrfResponse.body()?.csrfToken ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CameraPhotoActivity, "CSRF令牌为空", Toast.LENGTH_SHORT).show()
                        isUploading = false
                        showUploadingOverlay(false)
                    }
                    return@launch
                }

                Log.d(TAG, "获取到的CSRF令牌: $csrfToken")
                
                // 更新上传状态
                withContext(Dispatchers.Main) {
                    binding.uploadStatusText.text = "正在上传照片..."
                }
                
                // 2. 准备上传参数
                // 使用任务执行人作为照片名称，如果没有则使用文件名
                val photoName = if (missionName.isNotEmpty()) missionName else file.name
                val nameRequestBody = photoName.toRequestBody("text/plain".toMediaTypeOrNull())
                val namePart = MultipartBody.Part.createFormData("name", photoName, nameRequestBody)
                
                // 准备照片文件部分 - 键为"image_file"
                val fileRequestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("image_file", file.name, fileRequestBody)
                
                // 3. 发送上传请求
                val response = if (isTaskMode && placeInfo.isNotEmpty()) {
                    // 创建位置信息部分
                    val placeRequestBody = placeInfo.toRequestBody("text/plain".toMediaTypeOrNull())
                    val placePart = MultipartBody.Part.createFormData("place", placeInfo, placeRequestBody)
                    
                    Log.d(TAG, "上传照片信息: 位置=$placeInfo, 任务名称=$missionTask, 执行人=$missionName, 是否是任务模式=$isTaskMode")
                    
                    // 如果有任务名称，添加任务名称
                    if (missionTask.isNotEmpty()) {
                        Log.d(TAG, "使用带完整任务信息的上传方式")
                        // 创建任务名称部分
                        val taskRequestBody = missionTask.toRequestBody("text/plain".toMediaTypeOrNull())
                        // 注意：这里使用服务器期望的参数名称"task_name"
                        val taskPart = MultipartBody.Part.createFormData("task_name", missionTask, taskRequestBody)
                        
                        // 创建执行人名称部分（重用missionName）
                        val executorRequestBody = missionName.toRequestBody("text/plain".toMediaTypeOrNull())
                        // 注意：这里使用服务器期望的参数名称"executor"
                        val executorPart = MultipartBody.Part.createFormData("executor", missionName, executorRequestBody)
                        
                        // 发送带有完整任务信息的请求
                        withContext(Dispatchers.IO) {
                            RetrofitClient.apiService.uploadPhotoWithTaskInfo(
                                csrfToken,
                                namePart,
                                placePart,
                                taskPart,
                                executorPart,
                                filePart
                            )
                        }
                    } else {
                        Log.d(TAG, "使用仅带位置信息的上传方式")
                        // 仅发送位置信息请求
                        withContext(Dispatchers.IO) {
                            RetrofitClient.apiService.uploadPhotoWithPlace(
                                csrfToken,
                                namePart,
                                placePart,
                                filePart
                            )
                        }
                    }
                } else {
                    Log.d(TAG, "使用基本上传方式，没有任何附加信息")
                    // 发送普通上传请求
                    withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.uploadPhoto(
                            csrfToken,
                            namePart,
                            filePart
                        )
                    }
                }
                
                Log.d(TAG, "上传响应: 状态码=${response.code()}")
                
                // 添加更多日志记录，尝试记录响应体内容
                try {
                    val responseBody = response.body()?.string()
                    Log.d(TAG, "上传响应体: $responseBody")
                } catch (e: Exception) {
                    Log.e(TAG, "读取响应体失败", e)
                }
                
                // 4. 在主线程处理结果
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val successMsg = if (isTaskMode) {
                            "上传成功! 任务: $missionTask, 位置: $placeInfo"
                        } else {
                            "上传成功!"
                        }
                        Toast.makeText(this@CameraPhotoActivity, successMsg, Toast.LENGTH_LONG).show()
                        
                        // 如果在任务模式，上传成功后返回到扫码页面继续拍照
                        if (isTaskMode) {
                            val intent = Intent(this@CameraPhotoActivity, FunctionSelectionActivity::class.java).apply {
                                putExtra("MISSION_ID", missionId)
                                putExtra("MISSION_NAME", missionName)
                                putExtra("MISSION_TASK", missionTask)
                            }
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Toast.makeText(this@CameraPhotoActivity, "上传失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                    
                    isUploading = false
                    showUploadingOverlay(false)
                }
            } catch (e: Exception) {
                // 处理异常
                Log.e(TAG, "上传错误", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CameraPhotoActivity, "上传出错: ${e.message}", Toast.LENGTH_SHORT).show()
                    
                    isUploading = false
                    showUploadingOverlay(false)
                }
            }
        }
    }
    
    // 显示/隐藏上传中覆盖层
    private fun showUploadingOverlay(show: Boolean) {
        binding.uploadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    // 切换到预览模式（可以拍照）
    private fun switchToPreviewMode() {
        isPreviewMode = true
        
        // 启用相机预览
        if (camera?.cameraInfo?.torchState?.value == TorchState.ON) {
            camera?.cameraControl?.enableTorch(false)
        }
        
        // 更新UI状态
        buttonView.switchToCaptureMode()
        
        // 更新右侧镜像UI
        binding.captureButtonRight.visibility = View.VISIBLE
        binding.retakeButtonRight.visibility = View.GONE
        binding.uploadButtonRight.visibility = View.GONE
    }
    
    // 切换到重拍模式（已拍照，可以重拍或上传）
    private fun switchToRetakeMode() {
        isPreviewMode = false
        
        // 更新UI状态
        buttonView.switchToRetakeMode()
        
        // 更新右侧镜像UI
        binding.captureButtonRight.visibility = View.GONE
        binding.retakeButtonRight.visibility = View.VISIBLE
        binding.uploadButtonRight.visibility = View.VISIBLE
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "需要相机权限才能运行此功能",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
} 