package com.yl.ylappmonitor.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

public class AppUtils {
    private static final String TAG = "AppUtils";

    /**
     * 获取应用版本名称
     */
    public static String getVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
            return "Unknown";
        }
    }

    /**
     * 获取应用版本号
     */
    public static int getVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
            return 0;
        }
    }

    /**
     * 获取应用安装时间
     */
    public static long getInstallTime(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return packageInfo.lastUpdateTime;
            } else {
                return new File(packageInfo.applicationInfo.sourceDir).lastModified();
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
            return 0;
        }
    }

    /**
     * 获取应用大小
     */
    public static long getAppSize(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), 0);

            // 获取APK大小
            File apkFile = new File(appInfo.sourceDir);
            long apkSize = apkFile.length();

            // 获取数据目录大小
            long dataSize = FileUtils.getFileSize(context.getFilesDir());
            dataSize += FileUtils.getFileSize(context.getCacheDir());

            // 获取外部存储大小
            if (context.getExternalFilesDir(null) != null) {
                dataSize += FileUtils.getFileSize(context.getExternalFilesDir(null));
                dataSize += FileUtils.getFileSize(context.getExternalCacheDir());
            }

            return apkSize + dataSize;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
            return 0;
        }
    }

    /**
     * 获取应用安装来源
     */
    public static String getInstallSource(Context context) {
        try {
            @SuppressLint("PrivateApi")
            Class<?> packageManagerClass = Class.forName("android.content.pm.PackageManager");
            PackageManager pm = context.getPackageManager();

            String installerPackageName = pm.getInstallerPackageName(context.getPackageName());
            if (installerPackageName != null) {
                switch (installerPackageName) {
                    case "com.android.vending":
                        return "Google Play";
                    case "com.amazon.venezia":
                        return "Amazon Appstore";
                    case "com.huawei.appmarket":
                        return "Huawei AppGallery";
                    case "com.tencent.android.qqdownloader":
                        return "Tencent App Store";
                    case "com.xiaomi.market":
                        return "Xiaomi App Store";
                    default:
                        return installerPackageName;
                }
            }
            return "Unknown";
        } catch (Exception e) {
            Log.e(TAG, "Error getting install source", e);
            return "Unknown";
        }
    }

    /**
     * 检查应用是否在前台
     */
    public static boolean isAppForeground(Context context) {
        try {
            @SuppressLint("PrivateApi")
            Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
            Object activityManager = context.getSystemService(Context.ACTIVITY_SERVICE);

            Method getRunningTasks = activityManagerClass.getMethod(
                    "getRunningTasks", int.class);
            Object runningTasks = getRunningTasks.invoke(activityManager, 1);

            if (runningTasks instanceof java.util.List) {
                java.util.List<?> tasks = (java.util.List<?>) runningTasks;
                if (!tasks.isEmpty()) {
                    Object task = tasks.get(0);
                    Method getTopActivity = task.getClass().getMethod("getTopActivity");
                    Object componentName = getTopActivity.invoke(task);

                    Method getPackageName = componentName.getClass().getMethod("getPackageName");
                    String packageName = (String) getPackageName.invoke(componentName);

                    return context.getPackageName().equals(packageName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking app foreground", e);
        }
        return false;
    }

    /**
     * 获取应用ABI信息
     */
    public static String getAppAbi() {
        StringBuilder sb = new StringBuilder();
        sb.append("Supported ABIs: ").append(TextUtils.join(", ", Build.SUPPORTED_ABIS)).append("\n");

        try {
            // 获取原生库目录
            String nativeLibraryDir = new File(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD ?
                            Build.SUPPORTED_ABIS[0] : Build.CPU_ABI
            ).getName();

            sb.append("Native library dir: ").append(nativeLibraryDir);
        } catch (Exception e) {
            Log.e(TAG, "Error getting ABI info", e);
        }

        return sb.toString();
    }

    /**
     * 获取应用签名信息
     */
    public static String getSignatures(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);

            if (packageInfo.signatures == null || packageInfo.signatures.length == 0) {
                return "No signatures";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < packageInfo.signatures.length; i++) {
                byte[] signature = packageInfo.signatures[i].toByteArray();
                sb.append("Signature ").append(i + 1).append(": ");
                sb.append(bytesToHex(signature));
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error getting signatures", e);
            return "Error: " + e.getMessage();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
