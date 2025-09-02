package com.yl.ylappmonitor;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.yl.ylappmonitor.util.AppUtils;
import com.yl.ylappmonitor.util.DeviceUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class APMManager {

    private static final String TAG = "APMMonitor";
    private static APMManager instance;
    private final HandlerThread monitorThread;
    private final Context context;
    private boolean isInitialized = false;

    public static void init(Context context) {
        if (instance == null) {
            instance = new APMManager(context);
        }
    }

    public static APMManager getInstance(){
        if (instance == null) {
            throw new IllegalStateException("APMManager not initialized");
        }
        return instance;
    }

    private APMManager(Context context) {
        this.context = context.getApplicationContext();
        this.monitorThread = new HandlerThread("APM-Monitor");
        monitorThread.start();
    }

    public void startMonitoring() {
        if (isInitialized) return;

        // 初始化各监控模块
        CpuMonitor.start();
        MemoryMonitor.start(context);
        FPSMonitor.start(context);
        NetworkMonitor.init(context);
        NetworkMonitor.getInstance().startMonitoring();
        CrashHandler.init(context);

        // 定时上报任务
        new Handler(monitorThread.getLooper()).postDelayed(this::collectAndReport, 5000);
        isInitialized = true;
    }

    private void collectAndReport() {
        JSONObject metrics = new JSONObject();
        try {
            metrics.put("timestamp", System.currentTimeMillis());
            metrics.put("device", DeviceUtils.getDeviceId(context));
            metrics.put("app_version", AppUtils.getVersionName(context));
            metrics.put("cpu", CpuMonitor.getCpuUsage());
            metrics.put("memory", MemoryMonitor.getMemoryUsage());
            metrics.put("fps", FPSMonitor.getAvgFPS());
            metrics.put("network", NetworkMonitor.getInstance().getNetworkMetrics());

            // 上报到服务器
            new ReportTask().execute(metrics.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Metrics collection error", e);
        }

        // 每5秒采集一次
        new Handler(monitorThread.getLooper()).postDelayed(this::collectAndReport, 5000);
    }

    private static class ReportTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... data) {
            Log.e(TAG, "data: " + Arrays.toString(data));
//            HttpUtils.postJson("https://your-api-domain.com/apm/report", data[0]);
            return null;
        }
    }

}
