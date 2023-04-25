package com.hxty.assist.globle

import android.accessibilityservice.AccessibilityService
import android.annotation.TargetApi
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 *
    GLOBAL_ACTION_BACK
    GLOBAL_ACTION_HOME
    GLOBAL_ACTION_RECENTS
    GLOBAL_ACTION_NOTIFICATIONS
    GLOBAL_ACTION_QUICK_SETTINGS
    GLOBAL_ACTION_POWER_DIALOG
    GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN
    GLOBAL_ACTION_LOCK_SCREEN
    GLOBAL_ACTION_TAKE_SCREENSHOT
 */
open class BaseAccessibilityService : AccessibilityService(){

    /**
     * 模拟点击事件
     *
     * @param nodeInfo nodeInfo
     */
    fun performViewClick(nodeInfo: AccessibilityNodeInfo?) {
        var clickNode = nodeInfo ?: return
        while (clickNode != null) {
            if (clickNode.isClickable) {
                clickNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                break
            }
            clickNode = clickNode.parent
        }
    }

    /**
     * 模拟返回操作
     */
    fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * 返回桌面
     */
    fun goHome(){
        performGlobalAction(GLOBAL_ACTION_HOME)
//        val intent = Intent(Intent.ACTION_MAIN)
//        //如果是服务里面调用，必须加FLAG_ACTIVITY_NEW_TASK标识
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        intent.addCategory(Intent.CATEGORY_HOME)
//        startActivity(intent)
    }

    /**
     * 查找对应文本的View
     *
     * @param text      text
     * @param clickable 该View是否可以点击
     * @return View
     */
    fun findViewByText(text: String?, clickable: Boolean): AccessibilityNodeInfo? {
        val accessibilityNodeInfo: AccessibilityNodeInfo = rootInActiveWindow ?: return null
        val nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText(text)
        if (nodeInfoList != null && nodeInfoList.isNotEmpty()) {
            for (nodeInfo in nodeInfoList) {
                if (nodeInfo != null && nodeInfo.isClickable == clickable) {
                    return nodeInfo
                }
            }
        }
        return null
    }

    /**
     * 查找对应ID的第一个View
     *
     * @param id id
     * @return View
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun findViewByID(id: String): AccessibilityNodeInfo? {
        val accessibilityNodeInfo: AccessibilityNodeInfo = rootInActiveWindow ?: return null
        val nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(id)
        if (nodeInfoList != null && nodeInfoList.isNotEmpty()) {
            for (nodeInfo in nodeInfoList) {
                if (nodeInfo != null) {
                    return nodeInfo
                }
            }
        }
        return null
    }

    fun clickTextViewByText(text: String?) {
        val accessibilityNodeInfo: AccessibilityNodeInfo = rootInActiveWindow ?: return
        val nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText(text)
        if (nodeInfoList != null && nodeInfoList.isNotEmpty()) {
            for (nodeInfo in nodeInfoList) {
                if (nodeInfo != null) {
                    performViewClick(nodeInfo)
                    break
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun clickTextViewByID(id: String?) {
        val accessibilityNodeInfo: AccessibilityNodeInfo = rootInActiveWindow ?: return
        val nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(id!!)
        if (nodeInfoList != null && nodeInfoList.isNotEmpty()) {
            for (nodeInfo in nodeInfoList) {
                if (nodeInfo != null) {
                    performViewClick(nodeInfo)
                    break
                }
            }
        }
    }

    /**
     * 模拟输入
     *
     * @param nodeInfo nodeInfo
     * @param text     text
     */
    fun inputText(nodeInfo: AccessibilityNodeInfo, text: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", text)
            clipboard.setPrimaryClip(clip)
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {

    }

     override fun onServiceConnected() {
        super.onServiceConnected()

    }

    override fun onInterrupt() {
        Log.d("BaseAccessibilityService", "onInterrupt")
    }

    override fun onDestroy() {
        super.onDestroy()
        // 服务被销毁
    }
}