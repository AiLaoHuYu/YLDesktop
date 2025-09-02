package com.yl.ylappmonitor.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

public class DeviceUtils {
    private static final String TAG = "DeviceUtils";

    /**
     * 获取设备唯一ID (SHA256加密)
     */
    @SuppressLint("HardwareIds")
    public static String getDeviceId(Context context) {
        String androidId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID
        );

        // 如果ANDROID_ID不可用，使用其他标识符
        if (TextUtils.isEmpty(androidId) || "9774d56d682e549c".equals(androidId)) {
            String serial = Build.SERIAL;
            String pseudoId = "35" + // 生成一个伪ID
                    Build.BOARD.length() % 10 +
                    Build.BRAND.length() % 10 +
                    Build.DEVICE.length() % 10 +
                    Build.DISPLAY.length() % 10 +
                    Build.HOST.length() % 10 +
                    Build.ID.length() % 10 +
                    Build.MANUFACTURER.length() % 10 +
                    Build.MODEL.length() % 10 +
                    Build.PRODUCT.length() % 10 +
                    Build.TAGS.length() % 10 +
                    Build.TYPE.length() % 10 +
                    Build.USER.length() % 10;

            return hashString(pseudoId + serial);
        }
        return hashString(androidId);
    }

    private static String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 not available", e);
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    /**
     * 获取设备信息
     */
    public static String getDeviceInfo() {
        return String.format(Locale.US,
                "Model: %s\n" +
                        "Manufacturer: %s\n" +
                        "Brand: %s\n" +
                        "Device: %s\n" +
                        "Product: %s\n" +
                        "Hardware: %s\n" +
                        "OS Version: %s (API %d)\n" +
                        "ABIs: %s",
                Build.MODEL,
                Build.MANUFACTURER,
                Build.BRAND,
                Build.DEVICE,
                Build.PRODUCT,
                Build.HARDWARE,
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT,
                TextUtils.join(", ", Build.SUPPORTED_ABIS)
        );
    }

    /**
     * 获取屏幕信息
     */
    public static String getScreenInfo(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        int densityDpi = metrics.densityDpi;
        float density = metrics.density;
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;

        // 获取屏幕方向
        String orientation;
        int configOrientation = context.getResources().getConfiguration().orientation;
        if (configOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            orientation = "Landscape";
        } else if (configOrientation == Configuration.ORIENTATION_PORTRAIT) {
            orientation = "Portrait";
        } else {
            orientation = "Undefined";
        }

        return String.format(Locale.US,
                "Resolution: %dx%d\n" +
                        "Density: %.2f (%d dpi)\n" +
                        "Orientation: %s",
                widthPixels, heightPixels,
                density, densityDpi,
                orientation
        );
    }

    /**
     * 获取网络类型
     */
    public static String getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return "Unknown";

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()) {
            return "Disconnected";
        }

        int type = activeNetwork.getType();
        if (type == ConnectivityManager.TYPE_WIFI) {
            return "WiFi";
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            int subtype = activeNetwork.getSubtype();
            switch (subtype) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return "2G";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return "4G";
                case TelephonyManager.NETWORK_TYPE_NR:
                    return "5G";
                default:
                    return "Mobile";
            }
        }
        return "Other";
    }

    /**
     * 获取电池信息
     */
    @SuppressLint("PrivateApi")
    public static String getBatteryInfo(Context context) {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (bm == null) return "Battery info unavailable";

        int level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        int status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
//        int health = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

        String statusStr;
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                statusStr = "Charging";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                statusStr = "Discharging";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                statusStr = "Full";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                statusStr = "Not charging";
                break;
            default:
                statusStr = "Unknown";
        }

//        String healthStr;
//        switch (health) {
//            case BatteryManager.BATTERY_HEALTH_GOOD:
//                healthStr = "Good";
//                break;
//            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
//                healthStr = "Overheat";
//                break;
//            case BatteryManager.BATTERY_HEALTH_DEAD:
//                healthStr = "Dead";
//                break;
//            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
//                healthStr = "Over voltage";
//                break;
//            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
//                healthStr = "Failure";
//                break;
//            case BatteryManager.BATTERY_HEALTH_COLD:
//                healthStr = "Cold";
//                break;
//            default:
//                healthStr = "Unknown";
//        }

        return String.format(Locale.US,
                "Level: %d%%\n" +
                        "Status: %s\n",
                level, statusStr
        );
    }

    /**
     * 获取CPU信息
     */
    public static String getCpuInfo() {
        StringBuilder sb = new StringBuilder();
        try {
            File cpuInfoFile = new File("/proc/cpuinfo");
            if (cpuInfoFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(cpuInfoFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("Hardware") || line.contains("Processor") ||
                                line.contains("model name") || line.contains("Features") ||
                                line.contains("BogoMIPS")) {
                            sb.append(line).append("\n");
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading CPU info", e);
        }

        if (sb.length() == 0) {
            return "Architecture: " + System.getProperty("os.arch") +
                    "\nCores: " + Runtime.getRuntime().availableProcessors();
        }
        return sb.toString();
    }

    /**
     * 获取内存信息
     */
    public static String getMemoryInfo() {
        StringBuilder sb = new StringBuilder();
        try {
            File memInfoFile = new File("/proc/meminfo");
            if (memInfoFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(memInfoFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("MemTotal") || line.startsWith("MemFree") ||
                                line.startsWith("MemAvailable") || line.startsWith("SwapTotal") ||
                                line.startsWith("SwapFree")) {
                            sb.append(line).append("\n");
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading memory info", e);
        }
        return sb.toString();
    }

    /**
     * 检查设备是否root
     */
    public static boolean isRooted() {
        // 检查常见root文件
        String[] paths = {
                "/sbin/su", "/system/bin/su", "/system/xbin/su",
                "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su"
        };

        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }

        // 检查SU命令是否可用
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "/system/xbin/which", "su" });
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return in.readLine() != null;
        } catch (Exception e) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
