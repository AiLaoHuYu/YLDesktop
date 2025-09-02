package com.yl.ylappmonitor;

import android.content.Context;
import android.util.Log;

import com.yl.ylappmonitor.util.DeviceUtils;
import com.yl.ylappmonitor.util.HttpUtils;

import java.util.Date;

// CrashHandler.java - 崩溃监控
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String CRASH_DIR = "apm_crashes";
    private static CrashHandler instance;
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final Context context;

    public static void init(Context context) {
        if (instance == null) {
            instance = new CrashHandler(context);
        }
    }

    private CrashHandler(Context context) {
        this.context = context;
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        saveCrashReport(ex);
        defaultHandler.uncaughtException(thread, ex);
    }

    private void saveCrashReport(Throwable ex) {
        String stackTrace = Log.getStackTraceString(ex);
        String deviceInfo = DeviceUtils.getDeviceInfo();
        String log = "Crash Time: " + new Date() + "\n" +
                "Device Info: " + deviceInfo + "\n" +
                "StackTrace:\n" + stackTrace;

        String fileName = "crash_" + System.currentTimeMillis() + ".log";
        FileUtils.writeFile(context.getExternalFilesDir(CRASH_DIR), fileName, log);

        // 立即上报崩溃
//        HttpUtils.postJson("", log);
    }
}
