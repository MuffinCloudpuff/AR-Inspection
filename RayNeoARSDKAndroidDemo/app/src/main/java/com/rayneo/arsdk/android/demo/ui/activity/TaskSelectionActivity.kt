package com.rayneo.arsdk.android.demo.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.rayneo.arsdk.android.demo.R
import com.rayneo.arsdk.android.demo.api.MissionResponse
import com.rayneo.arsdk.android.demo.api.RetrofitClient
import com.rayneo.arsdk.android.demo.databinding.ActivityTaskSelectionBinding
import com.rayneo.arsdk.android.demo.ui.wedget.TaskButtonView
import com.rayneo.arsdk.android.ui.activity.BaseEventActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * 任务选择界面
 */
class TaskSelectionActivity : BaseEventActivity() {

    private lateinit var binding: ActivityTaskSelectionBinding
    private lateinit var buttonView: TaskButtonView
    private val TAG = "TaskSelectionActivity"
    
    // 存储获取到的任务列表
    private var missionList: List<MissionResponse> = emptyList()
    
    // 当前选中的任务
    private var selectedMission: MissionResponse? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "TaskSelectionActivity onCreate started")
        
        binding = ActivityTaskSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        Log.d(TAG, "Initial view setup complete")
        
        // 初始化按钮控制视图
        initButtonView()
        
        Log.d(TAG, "Button view initialized, starting to fetch missions")
        
        // 获取未完成任务列表
        fetchUnfinishedMissions()
        
        Log.d(TAG, "onCreate completed")
    }
    
    private fun initButtonView() {
        // 初始化按钮控制视图
        buttonView = TaskButtonView(this)
        
        // 设置返回按钮点击事件
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // 监听触控板事件
        buttonView.watchAction(this, templeActionViewModel)
    }
    
    private fun fetchUnfinishedMissions() {
        // 显示加载中
        binding.loadingView.visibility = View.VISIBLE
        binding.taskContainer.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        
        Log.d(TAG, "Starting to fetch unfinished mission list")
        Log.d(TAG, "API server address: ${RetrofitClient.BASE_URL}")
        
        // 检查网络连接状态
        val networkInfo = checkNetworkConnection()
        Log.d(TAG, "Network connection status: $networkInfo")
        
        if (!isNetworkAvailable()) {
            Log.e(TAG, "Network unavailable, cannot fetch mission list")
            showErrorMessage("网络不可用，请检查网络连接")
            return
        }
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Sending network request to fetch unfinished missions...")
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getUnfinishedMissions()
                }
                
                Log.d(TAG, "Server response received: status code=${response.code()}")
                Log.d(TAG, "Response headers: ${response.headers()}")
                
                if (response.isSuccessful) {
                    // 获取任务列表成功
                    val responseBody = response.body()
                    Log.d(TAG, "Response body: $responseBody")
                    
                    missionList = responseBody ?: emptyList()
                    Log.d(TAG, "Request successful, mission count: ${missionList.size}")
                    
                    if (missionList.isNotEmpty()) {
                        Log.d(TAG, "Mission list content: $missionList")
                        // 记录每个任务的详细信息
                        missionList.forEachIndexed { index, mission ->
                            Log.d(TAG, "Mission[$index]: id=${mission.id}, task=${mission.task}, name=${mission.name}, state=${mission.state}")
                        }
                    } else {
                        Log.d(TAG, "Mission list is empty")
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (missionList.isEmpty()) {
                            // 没有未完成的任务
                            Log.d(TAG, "No unfinished missions")
                            showEmptyMissionMessage("暂无未完成的任务")
                        } else {
                            // 显示任务列表
                            Log.d(TAG, "Displaying mission list, count: ${missionList.size}")
                            try {
                                displayMissions(missionList)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error displaying missions", e)
                                Toast.makeText(this@TaskSelectionActivity, "显示任务列表时出错: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    // 请求失败
                    Log.e(TAG, "Request failed: ${response.code()}, error message: ${response.errorBody()?.string()}")
                    withContext(Dispatchers.Main) {
                        showErrorMessage("获取任务失败：${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching missions: ${e.javaClass.simpleName}", e)
                Log.e(TAG, "Detailed error message: ${e.message}")
                Log.e(TAG, "Error stack trace: ${e.stackTraceToString()}")
                withContext(Dispatchers.Main) {
                    showErrorMessage("网络错误：${e.message}")
                }
            }
        }
    }
    
    private fun displayMissions(missions: List<MissionResponse>) {
        Log.d(TAG, "Starting to display missions, count: ${missions.size}")
        
        // 确保在主线程中更新UI
        runOnUiThread {
            // 更新视图可见性
            binding.loadingView.visibility = View.GONE
            binding.taskContainer.visibility = View.VISIBLE
            binding.errorView.visibility = View.GONE
            
            Log.d(TAG, "Updated view visibility: container=${binding.taskContainer.visibility}, loadingView=${binding.loadingView.visibility}")
            
            // 清空现有任务按钮
            binding.taskButtonContainer.removeAllViews()
            binding.taskButtonContainerRight.removeAllViews()
            
            Log.d(TAG, "Cleared button containers")
            
            // 为每个任务创建按钮
            missions.forEachIndexed { index, mission ->
                Log.d(TAG, "Creating button for mission[$index]: ${mission.task} (${mission.name})")
                
                // 创建左侧按钮
                val button = createTaskButton(mission)
                binding.taskButtonContainer.addView(button)
                
                // 创建右侧按钮（不可点击，仅展示）
                val buttonRight = createTaskButton(mission, false)
                binding.taskButtonContainerRight.addView(buttonRight)
                
                // 设置第一个按钮为选中状态
                if (index == 0) {
                    selectTask(button, mission)
                }
                
                // 添加按钮控制
                buttonView.addFocusTarget(button)
            }
            
            Log.d(TAG, "After adding buttons, container has ${binding.taskButtonContainer.childCount} buttons")
            
            // 将"开始任务"按钮改为"刷新"按钮
            binding.startButton.text = "刷新任务"
            binding.startButton.isEnabled = true
            binding.startButton.setOnClickListener {
                fetchUnfinishedMissions() // 刷新任务列表
            }
            
            // 右侧对应按钮也更新文本
            binding.startButtonRight.text = "刷新任务"
            binding.startButtonRight.isEnabled = true
            binding.startButtonRight.isClickable = false
            binding.startButtonRight.isFocusable = false
            
            // 更新任务详情说明
            binding.taskDetailText.text = "请点击任务直接执行"
            binding.taskDetailTextRight.text = binding.taskDetailText.text
            
            Log.d(TAG, "Task display complete")
            
            // 显示一个Toast通知
            Toast.makeText(this@TaskSelectionActivity, "已加载 ${missions.size} 个任务，点击任务可直接执行", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createTaskButton(mission: MissionResponse, clickable: Boolean = true): Button {
        Log.d(TAG, "Creating task button for mission: ${mission.task}, clickable: $clickable")
        
        val button = Button(this)
        button.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(R.dimen.task_button_height)
        ).apply {
            bottomMargin = resources.getDimensionPixelSize(R.dimen.task_button_margin)
        }
        
        // 设置按钮文本和样式
        button.text = "${mission.task} (${mission.name})"
        button.textSize = 16f
        button.setBackgroundResource(R.drawable.button_normal) // 使用统一的背景样式
        button.setTextColor(resources.getColor(android.R.color.white)) // 设置文本颜色为白色以适应深色背景
        
        // 确保按钮可见
        button.visibility = View.VISIBLE
        
        if (clickable) {
            button.setOnClickListener {
                // 选中当前任务并显示颜色提示
                Log.d(TAG, "Task button clicked: ${mission.task}")
                selectTask(button, mission)
                
                // 短暂延迟后执行任务，让用户看到选中效果
                button.postDelayed({
                    startSelectedTask()
                }, 300) // 300毫秒延迟，足够看到选中效果
            }
            // 确保按钮可交互
            button.isClickable = true
            button.isFocusable = true
        } else {
            button.isClickable = false
            button.isFocusable = false
        }
        
        Log.d(TAG, "Button created: text=${button.text}, width=${button.layoutParams.width}, height=${button.layoutParams.height}")
        
        return button
    }
    
    private fun selectTask(button: Button, mission: MissionResponse) {
        Log.d(TAG, "Selecting task: ${mission.task} (${mission.name})")
        
        // 重置所有按钮样式
        for (i in 0 until binding.taskButtonContainer.childCount) {
            val b = binding.taskButtonContainer.getChildAt(i) as Button
            b.setBackgroundResource(R.drawable.button_normal)
            b.setTextColor(resources.getColor(android.R.color.white))
            b.textSize = 16f  // 恢复默认字体大小
            b.scaleX = 1.0f
            b.scaleY = 1.0f
            b.elevation = 0f
        }
        
        // 设置选中样式（使用相同的悬停效果，以保持一致性）
        button.setBackgroundResource(R.drawable.button_hover)
        button.setTextColor(resources.getColor(android.R.color.white))
        button.textSize = 20f  // 增大字体到20sp
        button.scaleX = 1.5f  // 放大按钮
        button.scaleY = 1.5f
        button.elevation = 20f  // 增加高度
        Log.d(TAG, "Button style updated: selected=${button.text}")
        
        // 保存选中的任务
        selectedMission = mission
        
        // 显示任务详情
        binding.taskDetailText.text = "已选择：${mission.task}\n执行人：${mission.name}"
        binding.taskDetailTextRight.text = binding.taskDetailText.text
        
        Log.d(TAG, "Task detail updated: ${binding.taskDetailText.text}")
    }
    
    private fun startSelectedTask() {
        val mission = selectedMission ?: run {
            Toast.makeText(this, "请先选择一个任务", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 跳转到二维码扫描界面，并传递任务信息
        val intent = Intent(this, FunctionSelectionActivity::class.java).apply {
            putExtra("MISSION_ID", mission.id)
            putExtra("MISSION_TASK", mission.task)
            putExtra("MISSION_NAME", mission.name)
        }
        startActivity(intent)
    }
    
    private fun showErrorMessage(message: String) {
        binding.loadingView.visibility = View.GONE
        binding.taskContainer.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
        
        val detailedMessage = "错误: $message"
        binding.errorText.text = detailedMessage
        binding.errorTextRight.text = detailedMessage
        
        // 使用Toast显示错误信息
        Toast.makeText(this, detailedMessage, Toast.LENGTH_LONG).show()
        
        // 设置重试按钮点击事件
        binding.retryButton.setOnClickListener {
            fetchUnfinishedMissions()
        }
        
        // 添加测试连接按钮
        if (binding.errorView.findViewById<Button>(R.id.testConnectionButton) == null) {
            val testButton = Button(this).apply {
                id = R.id.testConnectionButton
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.task_button_margin)
                }
                text = "测试服务器连接"
                textSize = 14f
                setOnClickListener {
                    testServerConnection()
                }
            }
            (binding.errorView as LinearLayout).addView(testButton)
            
            // 右侧视图也添加相同按钮（不可点击）
            val testButtonRight = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.task_button_margin)
                }
                text = "测试服务器连接"
                textSize = 14f
                isClickable = false
                isFocusable = false
            }
            (binding.errorViewRight as LinearLayout).addView(testButtonRight)
        }
    }
    
    /**
     * 测试与服务器的连接
     */
    private fun testServerConnection() {
        Toast.makeText(this, "正在测试服务器连接...", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Testing server connection: ${RetrofitClient.BASE_URL}")
        
        lifecycleScope.launch {
            try {
                // 显示正在测试对话框
                val message = StringBuilder("正在测试服务器连接:\n")
                message.append("服务器: ${RetrofitClient.BASE_URL}\n")
                message.append("${checkNetworkConnection()}")
                
                Toast.makeText(this@TaskSelectionActivity, message.toString(), Toast.LENGTH_LONG).show()
                
                // 尝试获取CSRF令牌，这是一个轻量级请求
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getCsrfToken()
                }
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Server connection test successful: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@TaskSelectionActivity,
                            "服务器连接成功！状态码: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Log.e(TAG, "Server connection test failed: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@TaskSelectionActivity,
                            "服务器连接失败: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server connection test exception", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TaskSelectionActivity,
                        "服务器连接异常: ${e.javaClass.simpleName} - ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    /**
     * 检查网络连接是否可用
     * @return 网络是否可用
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }
    
    /**
     * 获取详细的网络连接信息
     * @return 网络连接信息字符串
     */
    private fun checkNetworkConnection(): String {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val result = StringBuilder()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return "No active network"
            val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return "Cannot get network capabilities"
            
            result.append("Network type: ")
            when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> result.append("WiFi")
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> result.append("Cellular")
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> result.append("Ethernet")
                else -> result.append("Other")
            }
            
            result.append(", Network capabilities: ")
            if (actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                result.append("Internet access ")
            }
            if (actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                result.append("Validated ")
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            result.append("Network info: ")
            @Suppress("DEPRECATION")
            if (networkInfo != null) {
                result.append("type=${networkInfo.typeName}, ")
                @Suppress("DEPRECATION")
                result.append("connected=${networkInfo.isConnected}")
            } else {
                result.append("No connection")
            }
        }
        
        return result.toString()
    }
    
    /**
     * 显示空任务列表消息
     */
    private fun showEmptyMissionMessage(message: String) {
        binding.loadingView.visibility = View.GONE
        binding.taskContainer.visibility = View.VISIBLE
        binding.errorView.visibility = View.GONE
        
        // 清空任务列表但保持UI可见
        binding.taskButtonContainer.removeAllViews()
        binding.taskButtonContainerRight.removeAllViews()
        
        // 更新任务详情显示为空消息
        binding.taskDetailText.text = message
        binding.taskDetailTextRight.text = message
        
        // 将开始按钮改为刷新按钮
        binding.startButton.text = "刷新任务"
        binding.startButton.isEnabled = true
        binding.startButton.setOnClickListener {
            fetchUnfinishedMissions() // 刷新任务列表
        }
        
        // 右侧对应按钮也更新
        binding.startButtonRight.text = "刷新任务"
        binding.startButtonRight.isEnabled = false
        
        Log.d(TAG, "Showing empty mission message: $message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
} 