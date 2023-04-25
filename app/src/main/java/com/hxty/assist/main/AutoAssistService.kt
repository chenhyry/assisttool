package com.hxty.assist.main

import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.hxty.assist.globle.BaseAccessibilityService
import com.hxty.assist.globle.Constants
import com.hxty.assist.utils.AppUtil
import com.hxty.assist.utils.Utils
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


/**
 * AutoAssistService是一个服务，需要注册在AndroidManifest
 * res/accessibility_service_config.xml里面需要加上包名
 * 需要在AndroidManifest <queries>写上包名申请对其他应用操作权限
 *
 * 每隔一段时间按一次返回，避免有些弹框出现无法滑动
 *
 * 在使用 AccessibilityService 遍历包含 WebView 的 AccessibilityNodeInfo 时会在某些情况下必现 StackOverflowError 的错误，导致应用崩溃
 * 一个是使用递归遍历 AccessibilityNodeInfo 时限制遍历的最大深度，这个深度根据情况大致在 40~70之间即可，既要基本保证能遍历完正常的 NodeTree 内的 Node，
 * 又要不引起 StackOverflowError 导致应用崩溃。
 */
class AutoAssistService : BaseAccessibilityService() {

    //屏幕宽高 用于计算滑动距离
    private var X = 0
    private var Y = 0
    private val executor = Executors.newSingleThreadExecutor()

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        X = intent.getIntExtra("width", 0)
        Y = intent.getIntExtra("height", 0)
        val `val` = "X: $X   Y: $Y"
        Log.d(TAG, `val`)
        return super.onStartCommand(intent, flags, startId)
    }

    /*
     单一线程 会阻塞
     */
    /**
     * TYPE_VIEW_CLICKED | 点击View
     * TYPE_VIEW_TEXT_CHANGED | EditText中文本变化
     * TYPE_WINDOW_STATE_CHANGED | 窗口状态变化（比如切换了Activity）
     * TYPE_NOTIFICATION_STATE_CHANGED | 收到通知
     * TYPE_WINDOW_CONTENT_CHANGED | 窗口内容变化（比如View树发生变化）
     * TYPE_VIEW_SCROLLED | 滑动View
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        super.onAccessibilityEvent(event)
        /* if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
             Log.e(TAG, "event${event.packageName}")
         } else */if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.e(TAG, "event ${event.packageName}")
            Log.e(TAG, "event ${event.className}")
            when (event.packageName) {
                "com.android.packageinstaller" -> {
                    val nodeInfo = findViewByText("安装", true)
                    nodeInfo?.let { performViewClick(it) }
                }
                else -> {
                    if (Constants.appPackageList.contains(event.packageName)) {
                        //延迟一秒，有些手机卡慢
                        delayTime(1)
                        //处理打开app后弹出的提示框
                        closeAppDialog()
                        if (IS_STARTED) return
                        Log.e(TAG, "成功打开${Utils.getAppName(event.packageName.toString())}")
                        val eventNodeInfo = event.source
                        if (eventNodeInfo != null) {
                            Log.d(TAG, "eventNodeInfo:" + eventNodeInfo.className)
                        }
                        val rootInActiveWindow = rootInActiveWindow
                        if (rootInActiveWindow == null) {
                            Log.e(TAG, "rootInActiveWindow=null")
                            return
                        }
//                    if (false) {
//                        //查看是否是首页
//                        TRY_TIMES = 0
//                        while (rootInActiveWindow.findAccessibilityNodeInfosByText("首页").size == 0) {
//                            TRY_TIMES++
//                            if (TRY_TIMES > 10) {
//                                ToastUtils.showLong("${getAppName(event.packageName.toString())}多次尝试无法到达首页，请手动操作")
//                                return
//                            }
//                            //每3秒一次返回以免退出程序
//                            delayTime(5)
//                            Log.e(TAG, "不是首页,回退页面")
//                            goBack()
//                        }
//                        Log.e(TAG, "是主页面")
//
//                        //是首页
//                        val mainPageCheck1 = rootInActiveWindow.findAccessibilityNodeInfosByText("同城")
//                        val mainPageCheck2 = rootInActiveWindow.findAccessibilityNodeInfosByText("发现")
//                        Log.e(TAG, "同城:" + mainPageCheck1.size)
//                        Log.e(TAG, "发现:" + mainPageCheck2.size)
//
//                        if (mainPageCheck1.size > 0 && mainPageCheck2.size > 0) {
//                            //点击发现
//                            delayTime(1)
//                            for (itemNodeInfo in mainPageCheck2) {
//                                if (itemNodeInfo.isClickable) {
//                                    Log.e(TAG, "点击发现")
//                                    performViewClick(itemNodeInfo)
//                                    itemNodeInfo.recycle()
//                                    delayTime(1)
//                                    break
//                                }
//                            }
//                        }
//                    }
                        if (IS_STARTED) return
                        //循环滑动，运行10分钟关闭
                        executor.submit(runnable)
                    } else {
                        //番茄阅读，尝试阶段，没有继续
              /*          if (event.packageName == Constants.FANQIE) {
                            if (IS_STARTED) return
                            Log.e(TAG, "打开${Utils.getAppName(event.packageName.toString())}")
                            delayTime(5)
                            goBackAfterSomeTime()
                            closeAppDialog()
                            delayTime(5)

                            //热门书籍第一个
                            val nodeInfoList2 = findViewByID("com.dragon.read:id/bpn")
                            Log.e("AutoAssistService", "${nodeInfoList2 == null}")
                            mockClickNode(nodeInfoList2)

                            val nodeInfoList3 = findViewByText("加入书架", true)
                            Log.e("加入书架3", "${nodeInfoList3 == null}")

                            val nodeInfoList4 = findViewByText("加入书架", false)
                            Log.e("加入书架4", "${nodeInfoList4 == null}")

                            //加入书架
                            val nodeInfoList5 = findViewByID("com.dragon.read:id/dm")
                            Log.e("AutoAssistService", "${nodeInfoList5 == null}")
                            mockClickNode(nodeInfoList5)

                            val nodeInfo2 = findViewByText("书架", true)
                            nodeInfo2?.let { performViewClick(it) }
                            delayTime(3)

//                    val nodeInfo3=  findViewByText("乡村神医",true)
//                    nodeInfo3?.let { performViewClick(it) }
//                    delayTime(3)
//
//                    if (IS_STARTED) return
//                    //循环滑动，运行10分钟关闭
//                    executor.submit(runnable)
                        }*/
                    }
                }
            }
        }
    }


    /**
     * 关闭所有弹框
     * 把所有的弹框情况列出来，判断并关闭
     */
    private fun closeAppDialog() {
        //广告页跳过
        val nodeInfoTiaoGuo = findViewByText("跳过", true)
        if (nodeInfoTiaoGuo != null) {
            delayTime(1)
            Log.e(TAG, "跳过")
            performViewClick(nodeInfoTiaoGuo)
            nodeInfoTiaoGuo.recycle()
        }
        val nodeInfoTongYi = findViewByText("同意", true)
        if (nodeInfoTongYi != null) {
            delayTime(1)
            Log.e(TAG, "同意")
            performViewClick(nodeInfoTongYi)
            nodeInfoTongYi.recycle()
        }
        val nodeInfoTongYiAndGo = findViewByText("同意并继续", true)
        if (nodeInfoTongYiAndGo != null) {
            delayTime(1)
            Log.e(TAG, "同意并继续")
            performViewClick(nodeInfoTongYiAndGo)
            nodeInfoTongYiAndGo.recycle()
        }

        val nodeInfoJiXuKanShiPing = findViewByText("继续看视频", true)
        if (nodeInfoJiXuKanShiPing != null) {
            delayTime(1)
            Log.e(TAG, "继续看视频")
            performViewClick(nodeInfoJiXuKanShiPing)
            nodeInfoJiXuKanShiPing.recycle()
        }

        val nodeInfoYiHou = findViewByText("以后再说", true)
        if (nodeInfoYiHou != null) {
            delayTime(1)
            Log.e(TAG, "以后再说")
            performViewClick(nodeInfoYiHou)
            nodeInfoYiHou.recycle()
        }

        val nodeInfoZhiDao = findViewByText("我知道了", true)
        if (nodeInfoZhiDao != null) {
            delayTime(1)
            Log.e(TAG, "我知道了")
            nodeInfoZhiDao.recycle()
            goBack()
        }
        val nodeInfoYaoQing = findViewByText("立即邀请", true)
        if (nodeInfoYaoQing != null) {
            delayTime(1)
            Log.e(TAG, "立即邀请")
            nodeInfoYaoQing.recycle()
            goBack()
        }

        //点击翻倍
        val nodeInfoFanBei = findViewByText("点击翻倍", true)
        if (nodeInfoFanBei != null) {
            delayTime(1)
            Log.e(TAG, "点击翻倍")
            nodeInfoFanBei.recycle()
            goBack()
        }

        //继续观看视频
        val nodeInfoJiXu = findViewByText("继续看视频", true)
        if (nodeInfoJiXu != null) {
            delayTime(1)
            Log.e(TAG, "继续看视频")
            nodeInfoJiXu.recycle()
            goBack()
        }
    }

    private fun goBackAfterSomeTime() {
        //每隔一段时间返回一次，防止出现未知弹窗情况无法滑动
        if ((System.currentTimeMillis() - Constants.LAST_CLICK_BACK_Time) / Constants.CLICK_BACK_TIME > Constants.CLICK_BACK_TIME) {
            Constants.LAST_CLICK_BACK_Time = System.currentTimeMillis()
            Log.e(TAG, "每隔一段时间执行返回")
            goBack()
        }
    }

    private val runnable = Runnable {
        IS_STARTED = true
        do {
            //检测应用是否在前台
            val foregroundAppPackageName = AppUtil.getForegroundAppPackageName(this)

            if (Constants.appPackageList.contains(foregroundAppPackageName)) {
                goBackAfterSomeTime()
                delayTime(3)

                delayTimeNoNeedRandom(loopScrollRandomTime)
                Log.e(TAG, "执行滑动")
                mockScrollUp()

                //每一个应用循环到固定时间后自动退出切换下一个应用
                if (System.currentTimeMillis() - Constants.START_TIME > Constants.LOOP_TIME) {
                    Log.e(TAG, "开始切换到下一个应用 回到桌面")
                    goHome()
                    delayTime(3)
                    openNextApp()
                    delayTime(5)//打开后等待5秒，如果还没有在前台，等待5秒后再次打开
                }
            } else {
                delayTime(5)
                openNextApp()
            }
            //番茄阅读，尝试阶段，没有继续
//            else if (Constants.FANQIE == foregroundAppPackageName) {
//                goBackAfterSomeTime()
//                closeAppDialog()
//
//                delayTimeNoNeedRandom(loopClickRandomTime)
//                Log.e(TAG, "执行点击")
//                mockClick()
//
//                //每一个应用循环后自动退出切换下一个应用
//                if (System.currentTimeMillis() - Constants.START_TIME > Constants.LOOP_TIME) {
//                    Log.e(TAG, "开始切换到下一个应用 回到桌面")
//                    goHome()
//                    delayTime(3)
//                    val nextOpenApp = Constants.appPackageList[Constants.curRunIndex++ % Constants.appPackageList.size]
//
//                    if (isAccessibilitySettingsOn()) {
//                        Constants.START_TIME = System.currentTimeMillis()
//                        AppUtil.openAppByPackageName(this, nextOpenApp)
//                        Log.e(TAG, "打开新app $nextOpenApp")
//                        break
//                    } else {
//
//                    }
//                }
//            } else {
//                Log.e(TAG, "循环结束")
//                break
//            }
        } while (IS_STARTED)
        Log.e(TAG, "此次循环停止")
        IS_STARTED = false
    }

    private fun openNextApp() {
        if (isAccessibilitySettingsOn()) {
            Constants.START_TIME = System.currentTimeMillis()
            val nextOpenApp = Constants.appPackageList[Constants.curRunIndex++ % Constants.appPackageList.size]
            AppUtil.openAppByPackageName(this, nextOpenApp)
            Log.e(TAG, "打开新app $nextOpenApp")
        }
    }

    /**
     * 是否处于桌面
     */
    private fun isLauncherHomePage(): Boolean {
        val node = rootInActiveWindow
//        if (node !=null &&  !Constants.launcher_PakeName.equals(node.packageName.toString())) {
//            throw Exception("程序不在初始化启动器页面,抛出异常")
//        }
        return false
    }

    /**
     * 判断是否开启了辅助功能
     * @return
     */
    private fun isAccessibilitySettingsOn(): Boolean {
        var accessibilityEnabled = 0
        // MyService为对应的服务
        val service = packageName + "/" + AutoAssistService::class.java.canonicalName
        Log.i(TAG, "service:$service")
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
            Log.v(TAG, "accessibilityEnabled = $accessibilityEnabled")
        } catch (e: Settings.SettingNotFoundException) {
            Log.e(TAG, "Error finding setting, default accessibility to not found: " + e.message)
        }
        val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            Log.v(TAG, "***ACCESSIBILITY IS ENABLED*** -----------------")
            val settingValue = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    Log.v(TAG, "-------------- > accessibilityService :: $accessibilityService $service")
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        Log.v(TAG, "We've found the correct setting - accessibility is switched on!")
                        return true
                    }
                }
            }
        } else {
            Log.v(TAG, "***ACCESSIBILITY IS DISABLED***")
        }
        return false
    }

    /**
     * 操作延迟 进行随机加减 不固定整数 模拟人为操作
     *
     * @param delayTime 秒
     */
    private fun delayTime(delayTime: Int) {
        var time = delayTime
        try {
            val range = 300
            val random = (Math.random() * range).toInt() //[0,range)
            //对时间进行粗略随机加减
            time = if (random < range / 2) { //小于50做减法
                time * 1000 - random
            } else {
                time * 1000 + random
            }
            Log.e(TAG, "延迟毫秒 $time")
            TimeUnit.MILLISECONDS.sleep(time.toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * 随机时间不需要加减
     *
     * @param time
     */
    private fun delayTimeNoNeedRandom(time: Int) {
        try {
            TimeUnit.SECONDS.sleep(time.toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * random() 0.0=<Math.random></Math.random><1.0
     * 循环滑动随机时间 5s - 20s
     */
    private val loopScrollRandomTime: Int
        private get() {
            var random = (Math.random() * 15).toInt() //[0,14]
            random += 1
            Log.e(TAG, "${random}秒后执行滑动")
            return random
        }

    /**
     * random() 0.0=<Math.random></Math.random><1.0
     * 循环点击随机时间 5s - 12s
     */
    private val loopClickRandomTime: Int
        private get() {
            var random = (Math.random() * 15).toInt() //[0,14]
            random += 5
            Log.e(TAG, "${random}秒后执行点击")
            return random
        }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun mockScrollUp() {
        val path = Path()
        path.moveTo((X / 2).toFloat(), (Y * 2 / 3).toFloat())
//        path.lineTo((X / 2).toFloat(), (Y * 1 / 5).toFloat())
        path.lineTo((X / 2).toFloat(), 0F)//多次尝试，部分低分辨率手机距离需要设置大才能滑动
        val builder = GestureDescription.Builder()
        //参数startTime：时间 (以毫秒为单位)，从手势开始到开始笔划的时间，非负数
        //参数duration：笔划经过路径的持续时间(以毫秒为单位)，非负数
        val gestureDescription = builder.addStroke(StrokeDescription(path, 120, 300)).build()
        dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                Log.e(TAG, "滑动完成")
                path.close()
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                Log.e(TAG, "scroll cancel.")
            }
        }, null)

//        val dragRightPath = Path()
//        dragRightPath.moveTo(200f, 200f)
//        dragRightPath.lineTo(400f, 200f)
//        val rightThenDownDrag = StrokeDescription(dragRightPath, 0L, dragRightDuration, true)
//        rightThenDownDrag.continueStroke(dragDownPath, dragRightDuration,
//            dragDownDuration, false);
    }

    /**
     * 单击
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun mockClickNode(node: AccessibilityNodeInfo?) {
        node?.let {
            val outBounds = Rect()
            it.getBoundsInScreen(outBounds)
            val path = Path()
            Log.e("AutoAssistService", "${(outBounds.left + outBounds.right)}")
            path.moveTo((outBounds.left + outBounds.right) / 2.0f, (outBounds.top + outBounds.bottom) / 2.0f)
            val builder = GestureDescription.Builder()
            val gestureDescription = builder.addStroke(
                StrokeDescription(
                    path, 0,
                    100
                )
            ).build()
            dispatchGesture(gestureDescription, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    super.onCompleted(gestureDescription)
                    Log.d(TAG, "double click finish.")
                    path.close()
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    super.onCancelled(gestureDescription)
                    Log.d(TAG, "scroll cancell.")
                }
            }, null)
        }

    }

    /**
     * 单击
     * 如果要模拟单机事件，直接像这样就好：val path = Path() path.moveTo(x, y)
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun mockClick() {
        val path = Path()
        path.moveTo((X / 2).toFloat(), (Y / 2).toFloat())
        val builder = GestureDescription.Builder()
        val gestureDescription = builder.addStroke(
            StrokeDescription(
                path, 0,
                100
            )
        ).build()
        dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "double click finish.")
                path.close()
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                Log.d(TAG, "scroll cancell.")
            }
        }, null)
    }

    /**
     * 双击
     * 如果要模拟单机事件，直接像这样就好：val path = Path() path.moveTo(x, y)
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun mockDoubleClick() {
        val path = Path()
        path.moveTo((X / 2).toFloat(), (Y / 2).toFloat())
        val builder = GestureDescription.Builder()
        val gestureDescription = builder.addStroke(
            StrokeDescription(
                path, 0,
                100
            )
        ).build()
        dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                val path2 = Path()
                path2.moveTo((X / 2).toFloat(), (Y / 2).toFloat())
                val builder2 = GestureDescription.Builder()
                val gestureDescription2 = builder2.addStroke(StrokeDescription(path2, 0, 100)).build()
                dispatchGesture(gestureDescription2, null, null)
                Log.d(TAG, "double click finish.")
                path.close()
                path2.close()
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                Log.d(TAG, "scroll cancell.")
            }
        }, null)
    }

    companion object {
        val TAG = AutoAssistService::class.java.simpleName
        private var TRY_TIMES = 0 //重试次数
        private var IS_STARTED = false //是否已开始
    }
}