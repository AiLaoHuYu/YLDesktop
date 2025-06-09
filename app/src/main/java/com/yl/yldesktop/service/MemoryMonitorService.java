package com.yl.yldesktop.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Binder;
import android.util.Log;

import com.yl.yldesktop.utils.BackgroundAppManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class MemoryMonitorService extends Service {
    private static final String TAG = "MemoryMonitorService";
    private static final long MONITOR_INTERVAL = 5000; // 每 5 秒监控一次
    private Handler handler;
    private Runnable monitorRunnable;
    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        startMemoryMonitoring();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void startMemoryMonitoring() {
        handler = new Handler();
        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                monitorSystem();
                handler.postDelayed(this, MONITOR_INTERVAL);
            }
        };
        handler.post(monitorRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(monitorRunnable);
    }

    private void monitorSystem() {
        monitorMemory();
        monitorCPU();
        BackgroundAppManager.scanRunningApps(this);
    }

    private void monitorMemory() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        long availableMemory = memoryInfo.availMem;
        long totalMemory = memoryInfo.totalMem;
        Log.d(TAG, "Available Memory: " + availableMemory + " bytes, Total Memory: " + totalMemory + " bytes");
    }

    private void monitorCPU() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));
            String line = reader.readLine();
            if (line != null) {
                String[] tokens = line.split("\\s+");
                long user = Long.parseLong(tokens[1]);
                long nice = Long.parseLong(tokens[2]);
                long system = Long.parseLong(tokens[3]);
                long idle = Long.parseLong(tokens[4]);
                long iowait = Long.parseLong(tokens[5]);
                long irq = Long.parseLong(tokens[6]);
                long softirq = Long.parseLong(tokens[7]);
                long steal = Long.parseLong(tokens[8]);
                long totalCpuTime = user + nice + system + idle + iowait + irq + softirq + steal;
                long cpuUsage = (totalCpuTime - idle) * 100 / totalCpuTime;
                Log.d(TAG, "CPU Usage: " + cpuUsage + "%");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkAndKillApps() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        if (memoryInfo.lowMemory) {
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
        if (runningProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
//                if (processInfo.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    activityManager.killBackgroundProcesses(processInfo.processName);
                    Log.d(TAG, "Killed app: " + processInfo.processName);
//                }
            }
        }
//        }
    }

    public class LocalBinder extends Binder {
        MemoryMonitorService getService() {
            return MemoryMonitorService.this;
        }
    }
}