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

class FunctionButtonView : LinearLayout, IFocusable {
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
    
    private var scanButton: Button? = null
    private var photoButton: Button? = null
    private var completeButton: Button? = null
    
    // 按钮点击回调
    var onScanButtonClickListener: (() -> Unit)? = null
    var onPhotoButtonClickListener: (() -> Unit)? = null

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

    fun setupButtons(scanBtn: Button, photoBtn: Button) {
        this.scanButton = scanBtn
        this.photoButton = photoBtn
        
        setupFocusTracker()
    }
    
    /**
     * 添加完成任务按钮到焦点控制
     */
    fun addCompleteButton(completeBtn: Button) {
        this.completeButton = completeBtn
        
        // 重新设置焦点跟踪器以包含完成按钮
        setupFocusTracker()
    }
    
    private fun setupFocusTracker() {
        scanButton?.let { scanBtn ->
            photoButton?.let { photoBtn ->
                focusHolder = FocusHolder(false).apply {
                    val scanInfo = FocusInfo(
                        scanBtn,
                        eventHandler = { action ->
                            when (action) {
                                is TempleAction.Click -> {
                                    onScanButtonClickListener?.invoke()
                                }
                                else -> Unit
                            }
                        },
                        focusChangeHandler = { hasFocus ->
                            triggerFocus(hasFocus, scanBtn)
                        }
                    )
                    
                    val photoInfo = FocusInfo(
                        photoBtn,
                        eventHandler = { action ->
                            when (action) {
                                is TempleAction.Click -> {
                                    onPhotoButtonClickListener?.invoke()
                                }
                                else -> Unit
                            }
                        },
                        focusChangeHandler = { hasFocus ->
                            triggerFocus(hasFocus, photoBtn)
                        }
                    )
                    
                    // 添加扫描和拍照按钮焦点目标
                    addFocusTarget(scanInfo, photoInfo)
                    
                    // 如果有完成任务按钮，也添加到焦点控制
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
                    
                    // 设置默认焦点为扫码按钮
                    currentFocus(scanBtn)
                }
                
                focusTracker = FixPosFocusTracker(focusHolder!!, true).apply {
                    focusObj.hasFocus = hasFocus
                }
            }
        }
    }
    
    private fun triggerFocus(hasFocus: Boolean, view: View) {
        android.util.Log.d("FunctionButtonView", "触发焦点效果: view=${view.id}, hasFocus=$hasFocus")
        
        if (hasFocus) {
            // 添加3D效果和高亮
            make3DEffectForSide(view, true, true)
            
            // 设置悬停样式 - 蓝色背景加白边
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
                view.setTextColor(android.graphics.Color.WHITE)  // 保持白色文字
            }
            
            // 恢复统一的背景样式 - 深灰色
            if (view == completeButton) {
                view.setBackgroundResource(R.drawable.bg_complete_button)
            } else {
                view.setBackgroundResource(R.drawable.button_normal)
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
                android.util.Log.d("FunctionButtonView", "收到触控板事件: $action")
                
                // 确保触控板事件得到处理
                try {
                    focusTracker?.handleFocusTargetEvent(action)
                } catch (e: Exception) {
                    android.util.Log.e("FunctionButtonView", "处理焦点事件出错: ${e.message}")
                }
            }
        }
    }
} 