package com.yl.yldesktop.utils;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

public class BackgroundAppManager {

    // 获取当前后台运行的应用程序列表
    public static List<ActivityManager.RunningAppProcessInfo> getRunningApps(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return activityManager.getRunningAppProcesses();
    }

    public static void scanRunningApps(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            Calendar calendar = Calendar.getInstance();
            long endTime = calendar.getTimeInMillis();
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            long startTime = calendar.getTimeInMillis();
            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
            if (usageStatsList != null && !usageStatsList.isEmpty()) {
                for (UsageStats usageStats : usageStatsList) {
                    String packageName = usageStats.getPackageName();
                    long lastTimeUsed = usageStats.getLastTimeUsed();
                    // 根据使用时间判断前后台状态
                    if (lastTimeUsed > System.currentTimeMillis() - 1000) {
                        Log.e("scanRunningApps", packageName + " is in foreground.");
                    } else {
                        killApp(context, packageName);
                        Log.e("scanRunningApps", packageName + " is in background.");
                    }
                }
            }
        }
    }

    // 根据包名终止指定的后台应用程序
    public static void killApp(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        try {
            activityManager.killBackgroundProcesses(packageName);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
