package com.hxty.assist.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class AppUtil {

    public static final int REQUEST_IGNORE_BATTERY_OPTIMIZATION_CODE = 10009;
    public static final String TAG = AppUtil.class.getSimpleName();


    /**
     * 获取版本号
     *
     * @param context
     * @return
     */
    public static int getAppVersionCode(Context context) {
        try {
            // ---get the package info---
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取版本名称
     *
     * @param context
     * @return
     */
    public static String getAppVersionName(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (Exception e) {
            return "-";
        }
    }

    /**
     * 判断应用是否在白名单中
     *
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean isIgnoringBatteryOptimizations(Activity activity) {
        boolean isIgnoring = false;
        PowerManager powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        try {
            if (powerManager != null) {
                isIgnoring = powerManager.isIgnoringBatteryOptimizations(activity.getPackageName());
            }
        } catch (Throwable e) {
            return true;
        }
        return isIgnoring;
    }

    /**
     * 申请加入白名单
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void requestIgnoreBatteryOptimizations(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATION_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 方法描述:根据包名判断App是否安装
     *
     * @param context     上下文
     * @param packageName 包名
     * @return true: 已安装<br>false: 未安装
     */
    public static boolean isInstallApp(Context context, String packageName) {
        return getIntentByPackageName(context, packageName) != null;
    }

    /**
     * 方法描述:根据包名获取意图
     *
     * @param context     上下文
     * @param packageName 包名
     * @return 意图
     */
    private static Intent getIntentByPackageName(Context context, String packageName) {
        return context.getPackageManager().getLaunchIntentForPackage(packageName);
    }


    /**
     * 方法描述：根据路径安装App
     *
     * @param context  上下文
     * @param filePath 文件路径
     */
    public static void installApp(Context context, String filePath) {
        installApp(context, new File(filePath));
    }

    /**
     * 方法描述：根据文件安装App
     *
     * @param context 上下文
     * @param file    文件
     */
    public static void installApp(Context context, File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 方法描述:卸载指定包名的App
     *
     * @param context     上下文
     * @param packageName 包名
     */
    public static void uninstallApp(Context context, String packageName) {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 是否有权限读取
     *
     * @param context
     * @return
     */
    public static boolean hasPermissionToReadUsage(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        final AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(),
                context.getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true;
        }

        requestToReadUsage(context);
        return false;
    }

    /**
     * 打开“有权查看使用情况的应用”页面
     *
     * @param context
     */
    private static void requestToReadUsage(Context context) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        context.startActivity(intent);
    }

    /**
     * 获取设备安装应用所有包名
     *
     * @param context
     */
    public static ArrayList<String> getAllPackageName(Context context) {
        //获取PackageManager
        PackageManager packageManager = context.getPackageManager();
        //获取所有已安装程序的包信息
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        ArrayList<String> packageNames = new ArrayList();

        if (packageInfos != null) {
            for (int i = 0; i < packageInfos.size(); i++) {
                String packName = packageInfos.get(i).packageName;
                packageNames.add(packName);
                Log.e(TAG, packName);
            }
        }
        return packageNames;
    }

    /**
     * 是否安装包名
     *
     * @param context
     * @param pkgName
     * @param pkglist
     * @return
     */
    public static boolean getInstallPkg(Context context, String pkgName, ArrayList<String> pkglist) {
        try {
            context.getPackageManager().getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        pkglist.add(pkgName);
        return true;
    }


    /**
     * 获取进程号对应的进程名
     *
     * @param pid 进程号
     * @return 进程名
     */
    public static String getProcessName(int pid) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
            String processName = reader.readLine();
            if (!TextUtils.isEmpty(processName)) {
                processName = processName.trim();
            }
            return processName;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }


    /**
     * 判断Service是否正在运行
     *
     * @param context     上下文
     * @param serviceName Service 类全名
     * @return true 表示正在运行，false 表示没有运行
     */
    public static boolean isServiceRunning(Context context, String serviceName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceInfoList = manager.getRunningServices(100);
        if (serviceInfoList.size() <= 0) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo info : serviceInfoList) {
            if (info.service.getClassName().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取顶层应用
     *
     * @param name
     * @param context
     * @return
     */
    public static boolean isNewTopActivity(String name, Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
            if (appList != null && appList.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    String currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                    //Log.d("TouchOnlineReceiver","currentApp = " + currentApp);
                    return currentApp.contains(name);
                }
            }
            return false;
        } else {
            //android 5.0 以上 获取自生（或者sharedUserId相同）顶层应用包名
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> taskInfos = am.getRunningTasks(1);
            ComponentName cn = null;
            if (taskInfos.size() > 0) {
                cn = taskInfos.get(0).topActivity;
            } else {
                return false;
            }
            Log.e("=====", "classname = " + cn.getClassName());
            return cn.getClassName().contains(name);
        }
    }


    /**
     * 获取栈顶的应用包名
     */
    public static String getForegroundAppPackageName(Context context) {
        String currentName = "";
        long END_TIME = System.currentTimeMillis();
        long TIME_INTERVAL = 60 * 1000L;
        long START_TIME = END_TIME - TIME_INTERVAL;
        UsageStats initStat = getForegroundUsageStats(context, START_TIME, END_TIME);
        if (initStat != null) {
            currentName = initStat.getPackageName();
        }
        Log.e("获取栈顶的应用包名", currentName);
        return currentName;
    }

    /**
     * 判断当前应用是否在前台
     */
    public static boolean isForegroundApp(Context context) {
        return TextUtils.equals(getForegroundAppPackageName(context), context.getPackageName());
    }

    /**
     * 获取时间段内，
     */
//    public static long getTotleForegroundTime(Context context) {
//        UsageStats usageStats = getCurrentUsageStats(context, START_TIME, END_TIME);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            return usageStats != null ? usageStats.getTotalTimeInForeground() : 0;
//        }
//        return 0;
//    }

    /**
     * 获取记录前台应用的UsageStats对象
     */
    private static UsageStats getForegroundUsageStats(Context context, long startTime, long endTime) {
        UsageStats usageStatsResult = null;
        List<UsageStats> usageStatses = getUsageStatsList(context, startTime, endTime);
        if (usageStatses == null || usageStatses.isEmpty()) return null;
        for (UsageStats usageStats : usageStatses) {
            if (usageStatsResult == null || usageStatsResult.getLastTimeUsed() < usageStats.getLastTimeUsed()) {
                usageStatsResult = usageStats;
            }
        }
        return usageStatsResult;
    }

    /**
     * 获取记录当前应用的UsageStats对象
     */
    public static UsageStats getCurrentUsageStats(Context context, long startTime, long endTime) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            List<UsageStats> usageStatses = getUsageStatsList(context, startTime, endTime);
            if (usageStatses == null || usageStatses.isEmpty()) return null;
            for (UsageStats usageStats : usageStatses) {
                if (TextUtils.equals(usageStats.getPackageName(), context.getPackageName())) {
                    return usageStats;
                }
            }
        }
        return null;
    }

    /**
     * 通过UsageStatsManager获取List<UsageStats>集合
     */
    public static List<UsageStats> getUsageStatsList(Context context, long startTime, long endTime) {
        UsageStatsManager manager =
                (UsageStatsManager) context.getApplicationContext().getSystemService(Context.USAGE_STATS_SERVICE);
        //UsageStatsManager.INTERVAL_WEEKLY，UsageStatsManager的参数定义了5个，具体查阅源码
        List<UsageStats> usageStatses = manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime);
        if (usageStatses == null || usageStatses.size() == 0) {// 没有权限，获取不到数据
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.getApplicationContext().startActivity(intent);
            return null;
        }
        return usageStatses;
    }


    /**
     * 判断服务是否在运行
     * with Android O(8.0) this is now deprecated and will only return information about the calling apps services
     *
     * @param context
     * @param serviceName
     * @return
     */
    public boolean isRunService(Context context, String serviceName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            //Log.e(TAG,service.service.getClassName()+" service name");
            if (service.service.getClassName().contains(serviceName)) {
                return true;
            }
        }
        return false;
    }

    public static void openAppByPackageName(Context context, String packageName) {
        PackageInfo pi;
        try {
            pi = context.getPackageManager().getPackageInfo(packageName, 0);
            Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
            resolveIntent.setPackage(pi.packageName);
            PackageManager pManager = context.getPackageManager();
            List<ResolveInfo> apps = pManager.queryIntentActivities(resolveIntent, 0);
            ResolveInfo ri = apps.iterator().next();
            if (ri != null) {
                packageName = ri.activityInfo.packageName;
                String className = ri.activityInfo.name;
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                //重点是加这个
                ComponentName cn = new ComponentName(packageName, className);
                intent.setComponent(cn);
                context.startActivity(intent);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }


    //跳转到指定应用的首页
//    public static void showActivity(Context context, @NonNull String packageName) {
//        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        context.startActivity(intent);
//    }


    public boolean isHuawei() {
        if (Build.BRAND == null) {
            return false;
        } else {
            return Build.BRAND.toLowerCase().equals("huawei") || Build.BRAND.toLowerCase().equals("honor");
        }
    }

    public static boolean isXiaomi() {
        return Build.BRAND != null && Build.BRAND.toLowerCase().equals("xiaomi");
    }

    public static boolean isOPPO() {
        return Build.BRAND != null && Build.BRAND.toLowerCase().equals("oppo");
    }

    public static boolean isVIVO() {
        return Build.BRAND != null && Build.BRAND.toLowerCase().equals("vivo");
    }

    public static boolean isMeizu() {
        return Build.BRAND != null && Build.BRAND.toLowerCase().equals("meizu");
    }

    public static boolean isSamsung() {
        return Build.BRAND != null && Build.BRAND.toLowerCase().equals("samsung");
    }


}
