package com.hxty.assist.globle;

import java.util.ArrayList;
import java.util.List;

public class Constants {

    //快手极速版
    public static final String KuaiShouNebulaPackegeName = "com.kuaishou.nebula";
    public static final String KuaiShouPackegeName = "com.smile.gifmaker";
    //抖音
    public static final String DOUYINPackegeName = "com.ss.android.ugc.aweme";
    public static final String DOUYINPackegeNamelite = "com.ss.android.ugc.aweme.lite";
    //番茄阅读
    public static final String FANQIE = "com.dragon.read";

    public static final String JinRiTouTiao = "com.ss.android.article.news";
    public static final String JinRiTouTiaoJiSu = "com.ss.android.article.lite";
    public static ArrayList<String> appPackageList;
    //当前正运行app的下标
    public static int curRunIndex = 0;
    //每一个app执行时间 即退出到下一个
    public static long START_TIME = 0;
    public static final long LOOP_TIME = 60 * 1000L;
    //每隔20分钟返回一次 以免有未知弹框出现无法滑动
    public static final long CLICK_BACK_TIME = 2 * 60 * 1000L;
    public static long LAST_CLICK_BACK_Time = 0;
}
