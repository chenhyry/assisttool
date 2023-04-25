package com.hxty.assist.main

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import com.hxty.assist.R
import com.hxty.assist.utils.ViewModleMain
import com.hxty.assist.globle.Constants
import com.hxty.assist.utils.AppUtil
import com.hxty.assist.utils.Utils

/**
 * 新添加包名需要添加的地方
 * 1.Constant加入
 * 2.MainActivity加入列表
 * 3.manifest                          manifest需要写入包名申请权限<queries>
 * 3.accessibility_service_config.xml  res/accessibility_service_config.xml里面需要加上包名
 */
class MainActivity : AppCompatActivity() {

    private var isReceptionShow = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intent2 = Intent(this@MainActivity, AutoAssistService::class.java)
        val wm = this.getSystemService(WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.defaultDisplay.getMetrics(dm)
        //将屏幕宽高传过去
        intent2.putExtra("width", dm.widthPixels)
        intent2.putExtra("height", dm.heightPixels)
        startService(intent2)

        Constants.appPackageList = ArrayList<String>()
        //新开启个线程来做获取，否则可能因为在主线程时间太长导致获取不全

        AppUtil.getInstallPkg(this, Constants.KuaiShouNebulaPackegeName, Constants.appPackageList)
        AppUtil.getInstallPkg(this, Constants.KuaiShouPackegeName, Constants.appPackageList)
        AppUtil.getInstallPkg(this, Constants.DOUYINPackegeName, Constants.appPackageList)
        AppUtil.getInstallPkg(this, Constants.DOUYINPackegeNamelite, Constants.appPackageList)

        //申请后台运行权限
        if (!AppUtil.isIgnoringBatteryOptimizations(this)) {
            AppUtil.requestIgnoreBatteryOptimizations(this)
        }

        AppUtil.hasPermissionToReadUsage(this)

        //打开悬浮框
        startService(Intent(this, SuspendwindowService::class.java))

        Utils.checkSuspendedWindowPermission(this) {
            isReceptionShow = false
            ViewModleMain.isShowSuspendWindow.postValue(true)
        }
    }

    /**
     * 随机打开一个app
     */
    fun goApp(view: View) {
        if (Constants.appPackageList.isEmpty()) return
        if (isAccessibilitySettingsOn()) {
            Constants.START_TIME = System.currentTimeMillis()
            val nextOpenApp = Constants.appPackageList[Constants.curRunIndex++ % Constants.appPackageList.size]
            AppUtil.openAppByPackageName(this, nextOpenApp)
        } else {
            goAccess(null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (resultCode == Utils.REQUEST_FLOAT_CODE) {
                Utils.checkSuspendedWindowPermission(this) {
                    isReceptionShow = false
                    ViewModleMain.isShowSuspendWindow.postValue(true)
                }
            }
        }
    }

    /**
     * Check当前辅助服务是否启用
     *
     * @param serviceName serviceName
     * @return 是否启用
     */
    private fun checkAccessibilityEnabled(serviceName: String): Boolean {
        val mAccessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val accessibilityServices =
            mAccessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (info in accessibilityServices) {
            if (info.id == serviceName) {
                return true
            }
        }
        return false
    }


    /**
     * 代码打开app
     */
    private fun goAccess(view: View?) {
        goAccess(this)
    }

    /**
     * 前往开启辅助服务界面
     */
    fun goAccess(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * 判断是否开启了辅助功能
     * @return
     */
    private fun isAccessibilitySettingsOn(): Boolean {
        var accessibilityEnabled = 0
        // MyService为对应的服务
        val service = packageName + "/" + AutoAssistService::class.java.canonicalName
        Log.i(AutoAssistService.TAG, "service:$service")
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
            Log.v(AutoAssistService.TAG, "accessibilityEnabled = $accessibilityEnabled")
        } catch (e: Settings.SettingNotFoundException) {
            Log.e(AutoAssistService.TAG, "Error finding setting, default accessibility to not found: " + e.message)
        }
        val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            Log.v(AutoAssistService.TAG, "***ACCESSIBILITY IS ENABLED*** -----------------")
            val settingValue = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    Log.v(
                        AutoAssistService.TAG,
                        "-------------- > accessibilityService :: $accessibilityService $service"
                    )
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        Log.v(AutoAssistService.TAG, "We've found the correct setting - accessibility is switched on!")
                        return true
                    }
                }
            }
        } else {
            Log.v(AutoAssistService.TAG, "***ACCESSIBILITY IS DISABLED***")
        }
        return false
    }

}