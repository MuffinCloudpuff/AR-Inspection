package com.rayneo.arsdk.android.demo.ui.wedget

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.lifecycle.LifecycleOwner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rayneo.arsdk.android.core.make3DEffectForSide
import com.rayneo.arsdk.android.demo.R
import com.rayneo.arsdk.android.focus.reqFocus
import com.rayneo.arsdk.android.touch.TempleAction
import com.rayneo.arsdk.android.ui.util.FocusHolder
import com.rayneo.arsdk.android.ui.util.FocusInfo
import com.rayneo.arsdk.android.ui.util.FixPosFocusTracker
import com.rayneo.arsdk.android.touch.TempleActionViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive

/**
 * 任务按钮控制类
 */
class TaskButtonView(private val context: Context) {
    private val focusHolder = FocusHolder(true)
    private var fixPosFocusTracker: FixPosFocusTracker? = null
    private val focusTargets = mutableListOf<Button>()
    
    /**
     * 添加焦点控制目标按钮
     */
    fun addFocusTarget(button: Button) {
        if (!focusTargets.contains(button)) {
            focusTargets.add(button)
            
            val focusInfo = FocusInfo(
                button,
                eventHandler = { action ->
                    when (action) {
                        is TempleAction.Click -> {
                            button.callOnClick()
                        }
                        else -> Unit
                    }
                },
                focusChangeHandler = { hasFocus ->
                    triggerFocus(button, hasFocus)
                }
            )
            
            focusHolder.addFocusTarget(focusInfo)
            
            // 如果是第一个按钮，设置为默认焦点
            if (focusTargets.size == 1) {
                focusHolder.currentFocus(button)
                fixPosFocusTracker = FixPosFocusTracker(focusHolder).apply {
                    focusObj.reqFocus()
                }
            }
        }
    }
    
    /**
     * 监听触控板事件
     */
    fun watchAction(owner: LifecycleOwner, viewModel: TempleActionViewModel) {
        if (owner is AppCompatActivity) {
            val lifecycleScope = owner.lifecycleScope
            lifecycleScope.launchWhenResumed {
                viewModel.state.filter { !it.consumed }.collect { action ->
                    if (!this.isActive) {
                        return@collect
                    }
                    
                    // 输出日志，帮助调试
                    android.util.Log.d("TaskButtonView", "收到触控板事件: $action")
                    
                    // 确保触控板事件得到处理
                    try {
                        fixPosFocusTracker?.handleFocusTargetEvent(action)
                    } catch (e: Exception) {
                        android.util.Log.e("TaskButtonView", "处理焦点事件出错: ${e.message}")
                    }
                }
            }
        }
    }
    
    /**
     * 触发焦点变化效果
     */
    private fun triggerFocus(view: View, hasFocus: Boolean) {
        android.util.Log.d("TaskButtonView", "触发焦点效果: view=${view.id}, hasFocus=$hasFocus")
        
        if (hasFocus) {
            // 确保3D效果显示
            make3DEffectForSide(view, true, hasFocus)
            // 设置悬停背景 - 蓝色背景加白边
            view.setBackgroundResource(R.drawable.button_hover)
            
            // 添加更明显的视觉提示
            view.elevation = 20f  // 增加高度
            view.scaleX = 1.5f   // 稍微放大
            view.scaleY = 1.5f
            
            // 增大字体
            if (view is Button) {
                view.textSize = 20f  // 增大字体到20sp
                view.setTextColor(android.graphics.Color.WHITE)  // 确保文字为白色
            }
        } else {
            // 取消3D效果
            make3DEffectForSide(view, true, hasFocus)
            // 恢复统一的背景样式 - 深灰色
            view.setBackgroundResource(R.drawable.button_normal)
            
            // 恢复原始大小和高度
            view.elevation = 0f
            view.scaleX = 1.0f
            view.scaleY = 1.0f
            
            // 恢复原始字体大小
            if (view is Button) {
                view.textSize = 16f  // 恢复默认字体大小
                view.setTextColor(android.graphics.Color.WHITE)  // 保持白色文字
            }
        }
    }
} 