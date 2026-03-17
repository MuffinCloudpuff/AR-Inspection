package com.rayneo.arsdk.android.demo.ui.wedget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rayneo.arsdk.android.core.make3DEffectForSide
import com.rayneo.arsdk.android.demo.R
import com.rayneo.arsdk.android.focus.IFocusable
import com.rayneo.arsdk.android.touch.TempleAction
import com.rayneo.arsdk.android.touch.TempleActionViewModel
import com.rayneo.arsdk.android.ui.util.FixPosFocusTracker
import com.rayneo.arsdk.android.ui.util.FocusHolder
import com.rayneo.arsdk.android.ui.util.FocusInfo
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive

class CameraButtonView : LinearLayout, IFocusable {
    override var hasFocus: Boolean = true
        set(value) {
            field = value
            focusTracker?.apply {
                focusObj.hasFocus = field
                val current = focusHolder.currentFocusItem
                focusHolder.currentFocus(current.target)
            }
        }
    
    override var focusParent: IFocusable? = null
    private var focusTracker: FixPosFocusTracker? = null
    private var focusHolder: FocusHolder? = null
    
    private var backButton: Button? = null
    private var captureButton: Button? = null
    private var retakeButton: Button? = null
    private var uploadButton: Button? = null
    private var completeButton: Button? = null
    
    // 按钮点击回调
    var onBackButtonClickListener: (() -> Unit)? = null
    var onCaptureButtonClickListener: (() -> Unit)? = null
    var onRetakeButtonClickListener: (() -> Unit)? = null
    var onUploadButtonClickListener: (() -> Unit)? = null
    var onCompleteButtonClickListener: (() -> Unit)? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        orientation = VERTICAL
    }

    fun setupButtons(backBtn: Button, captureBtn: Button, retakeBtn: Button, uploadBtn: Button) {
        this.backButton = backBtn
        this.captureButton = captureBtn
        this.retakeButton = retakeBtn
        this.uploadButton = uploadBtn
        
        setupFocusTracker()
    }
    
    /**
     * 添加任务完成按钮
     */
    fun addCompleteButton(completeBtn: Button) {
        this.completeButton = completeBtn
        
        // 将完成按钮的点击事件交给按钮自己处理
        completeBtn.setOnClickListener {
            onCompleteButtonClickListener?.invoke()
        }
        
        // 重新设置焦点控制器，包含完成按钮
        focusHolder?.let { holder ->
            val completeInfo = FocusInfo(
                completeBtn,
                eventHandler = { action ->
                    when (action) {
                        is TempleAction.Click -> {
                            completeBtn.callOnClick()
                        }
                        else -> Unit
                    }
                },
                focusChangeHandler = { hasFocus ->
                    triggerFocus(hasFocus, completeBtn)
                }
            )
            
            // 添加到焦点目标中
            holder.addFocusTarget(completeInfo)
        }
    }
    
    private fun setupFocusTracker() {
        backButton?.let { backBtn ->
            captureButton?.let { captureBtn ->
                retakeButton?.let { retakeBtn ->
                    uploadButton?.let { uploadBtn ->
                        focusHolder = FocusHolder(false).apply {
                            val backInfo = FocusInfo(
                                backBtn,
                                eventHandler = { action ->
                                    when (action) {
                                        is TempleAction.Click -> {
                                            onBackButtonClickListener?.invoke()
                                        }
                                        else -> Unit
                                    }
                                },
                                focusChangeHandler = { hasFocus ->
                                    triggerFocus(hasFocus, backBtn)
                                }
                            )
                            
                            val captureInfo = FocusInfo(
                                captureBtn,
                                eventHandler = { action ->
                                    when (action) {
                                        is TempleAction.Click -> {
                                            onCaptureButtonClickListener?.invoke()
                                        }
                                        else -> Unit
                                    }
                                },
                                focusChangeHandler = { hasFocus ->
                                    triggerFocus(hasFocus, captureBtn)
                                }
                            )
                            
                            val retakeInfo = FocusInfo(
                                retakeBtn,
                                eventHandler = { action ->
                                    when (action) {
                                        is TempleAction.Click -> {
                                            onRetakeButtonClickListener?.invoke()
                                        }
                                        else -> Unit
                                    }
                                },
                                focusChangeHandler = { hasFocus ->
                                    triggerFocus(hasFocus, retakeBtn)
                                }
                            )
                            
                            val uploadInfo = FocusInfo(
                                uploadBtn,
                                eventHandler = { action ->
                                    when (action) {
                                        is TempleAction.Click -> {
                                            onUploadButtonClickListener?.invoke()
                                        }
                                        else -> Unit
                                    }
                                },
                                focusChangeHandler = { hasFocus ->
                                    triggerFocus(hasFocus, uploadBtn)
                                }
                            )
                            
                            // 添加焦点目标，设置左右方向的焦点移动
                            addFocusTarget(backInfo, captureInfo)
                            
                            // 如果有完成按钮，也添加进来
                            completeButton?.let { completeBtn ->
                                val completeInfo = FocusInfo(
                                    completeBtn,
                                    eventHandler = { action ->
                                        when (action) {
                                            is TempleAction.Click -> {
                                                completeBtn.callOnClick()
                                            }
                                            else -> Unit
                                        }
                                    },
                                    focusChangeHandler = { hasFocus ->
                                        triggerFocus(hasFocus, completeBtn)
                                    }
                                )
                                
                                addFocusTarget(completeInfo)
                            }
                            
                            // 设置默认焦点为拍照按钮
                            currentFocus(captureBtn)
                        }
                        
                        focusTracker = FixPosFocusTracker(focusHolder!!, true).apply {
                            focusObj.hasFocus = hasFocus
                        }
                    }
                }
            }
        }
    }
    
    // 切换到拍照模式
    fun switchToCaptureMode() {
        captureButton?.visibility = View.VISIBLE
        retakeButton?.visibility = View.GONE
        uploadButton?.visibility = View.GONE
        
        focusHolder?.let { holder ->
            // 如果当前焦点在重拍按钮或上传按钮上，移动到拍照按钮
            if (holder.currentFocusItem.target == retakeButton || 
                holder.currentFocusItem.target == uploadButton) {
                captureButton?.let { holder.currentFocus(it) }
            }
            
            // 重新配置焦点流向 - 重新创建焦点轨迹
            setupFocusTracker()
        }
    }
    
    // 切换到重拍模式（显示重拍和上传按钮）
    fun switchToRetakeMode() {
        captureButton?.visibility = View.GONE
        retakeButton?.visibility = View.VISIBLE
        uploadButton?.visibility = View.VISIBLE
        
        // 完全重新创建焦点处理系统，避免使用不存在的clearFocusTarget
        backButton?.let { backBtn ->
            retakeButton?.let { retakeBtn ->
                uploadButton?.let { uploadBtn ->
                    // 创建全新的焦点控制器
                    val newFocusHolder = FocusHolder(false).apply {
                        // 创建焦点信息对象
                        val backInfo = FocusInfo(
                            backBtn,
                            eventHandler = { action ->
                                when (action) {
                                    is TempleAction.Click -> {
                                        onBackButtonClickListener?.invoke()
                                    }
                                    else -> Unit
                                }
                            },
                            focusChangeHandler = { hasFocus ->
                                triggerFocus(hasFocus, backBtn)
                            }
                        )
                        
                        val retakeInfo = FocusInfo(
                            retakeBtn,
                            eventHandler = { action ->
                                when (action) {
                                    is TempleAction.Click -> {
                                        onRetakeButtonClickListener?.invoke()
                                    }
                                    else -> Unit
                                }
                            },
                            focusChangeHandler = { hasFocus ->
                                triggerFocus(hasFocus, retakeBtn)
                            }
                        )
                        
                        val uploadInfo = FocusInfo(
                            uploadBtn,
                            eventHandler = { action ->
                                when (action) {
                                    is TempleAction.Click -> {
                                        onUploadButtonClickListener?.invoke()
                                    }
                                    else -> Unit
                                }
                            },
                            focusChangeHandler = { hasFocus ->
                                triggerFocus(hasFocus, uploadBtn)
                            }
                        )
                        
                        // 配置焦点流向
                        addFocusTarget(backInfo, retakeInfo, uploadInfo)
                        
                        // 如果有完成按钮，也添加进来
                        completeButton?.let { completeBtn ->
                            val completeInfo = FocusInfo(
                                completeBtn,
                                eventHandler = { action ->
                                    when (action) {
                                        is TempleAction.Click -> {
                                            completeBtn.callOnClick()
                                        }
                                        else -> Unit
                                    }
                                },
                                focusChangeHandler = { hasFocus ->
                                    triggerFocus(hasFocus, completeBtn)
                                }
                            )
                            
                            addFocusTarget(completeInfo)
                        }
                        
                        // 默认焦点放在上传按钮上
                        currentFocus(uploadBtn)
                    }
                    
                    // 替换旧的焦点控制器
                    focusHolder = newFocusHolder
                    
                    // 更新焦点追踪器
                    focusTracker = FixPosFocusTracker(newFocusHolder, true).apply {
                        focusObj.hasFocus = hasFocus
                    }
                }
            }
        }
    }
    
    private fun triggerFocus(hasFocus: Boolean, view: View) {
        android.util.Log.d("CameraButtonView", "触发焦点效果: view=${view.id}, hasFocus=$hasFocus")
        
        if (hasFocus) {
            // 添加3D效果和高亮
            make3DEffectForSide(view, true, true)
            
            // 所有按钮使用相同的悬停样式 - 蓝色背景加白边
            view.setBackgroundResource(R.drawable.button_hover)
            
            // 添加更明显的视觉提示
            view.elevation = 20f  // 增加高度
            view.scaleX = 1.5f   // 放大
            view.scaleY = 1.5f
            
            // 增大字体
            if (view is Button) {
                view.textSize = 20f  // 增大字体到20sp
                view.setTextColor(android.graphics.Color.WHITE)  // 确保文字为白色
            }
        } else {
            // 取消3D效果
            make3DEffectForSide(view, true, false)
            
            // 恢复原始大小和高度
            view.elevation = 0f
            view.scaleX = 1.0f
            view.scaleY = 1.0f
            
            // 恢复原始字体大小
            if (view is Button) {
                view.textSize = 16f  // 恢复默认字体大小
            }
            
            // 按钮使用统一的背景样式
            if (view == completeButton) {
                // 完成任务按钮保持绿色背景以便区分
                view.setBackgroundResource(R.drawable.bg_complete_button)
                if (view is Button) {
                    view.setTextColor(android.graphics.Color.WHITE)  // 保持白色文字
                }
            } else {
                // 其他按钮使用统一的深灰色背景
                view.setBackgroundResource(R.drawable.button_normal)
                if (view is Button) {
                    view.setTextColor(android.graphics.Color.WHITE)  // 保持白色文字
                }
            }
        }
    }
    
    fun watchAction(
        act: AppCompatActivity,
        templeActionViewModel: TempleActionViewModel
    ) {
        val lifecycleScope = act.lifecycleScope
        lifecycleScope.launchWhenResumed {
            templeActionViewModel.state.filter { !it.consumed }.collect { action ->
                if (!hasFocus || !this.isActive) {
                    return@collect
                }
                
                // 输出日志，帮助调试
                android.util.Log.d("CameraButtonView", "收到触控板事件: $action")
                
                // 确保触控板事件得到处理
                try {
                    focusTracker?.handleFocusTargetEvent(action)
                } catch (e: Exception) {
                    android.util.Log.e("CameraButtonView", "处理焦点事件出错: ${e.message}")
                }
            }
        }
    }
} 