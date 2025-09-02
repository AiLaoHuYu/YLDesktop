package com.yl.ylappmonitor;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class MemoryMonitor {
    private static final String TAG = "MemoryMonitor";
    private static final long DEFAULT_INTERVAL = 5000; // 5秒
    private static final int LOW_MEMORY_THRESHOLD = 80; // 内存使用率超过80%警告
    private static final int LEAK_DETECTION_THRESHOLD = 3; // 连续3次超过阈值触发泄漏检测

    private static MemoryMonitor instance;
    private final HandlerThread monitorThread;
    private final Handler monitorHandler;
    private final Context context;
    private final ActivityManager activityManager;
    private final Map<String, Queue<Long>> memoryHistory = new HashMap<>();
    private final Map<String, Integer> leakDetectionCounters = new HashMap<>();

    // 内存类型常量
    public static final String MEM_TYPE_JAVA_HEAP = "java_heap";
    public static final String MEM_TYPE_NATIVE_HEAP = "native_heap";
    public static final String MEM_TYPE_PSS = "pss";
    public static final String MEM_TYPE_DALVIK = "dalvik";
    public static final String MEM_TYPE_TOTAL = "total";

    // 内存泄漏检测回调
    public interface MemoryLeakListener {
        void onPotentialMemoryLeak(String component, long memoryUsage, long threshold);
    }

    private MemoryLeakListener leakListener;

    public static void start(Context context) {
        if (instance == null) {
            instance = new MemoryMonitor(context);
        }
        instance.startMonitoring();
    }

    public static void setLeakListener(MemoryLeakListener listener) {
        if (instance != null) {
            instance.leakListener = listener;
        }
    }

    private MemoryMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.monitorThread = new HandlerThread("Memory-Monitor");
        this.monitorThread.start();
        this.monitorHandler = new Handler(monitorThread.getLooper());

        // 初始化内存历史记录
        memoryHistory.put(MEM_TYPE_JAVA_HEAP, new LinkedList<>());
        memoryHistory.put(MEM_TYPE_NATIVE_HEAP, new LinkedList<>());
        memoryHistory.put(MEM_TYPE_PSS, new LinkedList<>());
        memoryHistory.put(MEM_TYPE_DALVIK, new LinkedList<>());
        memoryHistory.put(MEM_TYPE_TOTAL, new LinkedList<>());
    }

    public void startMonitoring() {
        monitorHandler.removeCallbacksAndMessages(null);
        monitorHandler.post(monitoringRunnable);
    }

    public void stopMonitoring() {
        monitorHandler.removeCallbacksAndMessages(null);
    }

    private final Runnable monitoringRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                // 收集内存数据
                Pair<Long, Long> javaMem = getJavaHeapMemory();
                long nativeMem = getNativeHeapMemory();
                long pssMem = getPssMemory();
                long dalvikMem = getDalvikHeapMemory();
                long totalMem = getTotalMemory();

                // 记录内存历史
                recordMemoryHistory(MEM_TYPE_JAVA_HEAP, javaMem.second);
                recordMemoryHistory(MEM_TYPE_NATIVE_HEAP, nativeMem);
                recordMemoryHistory(MEM_TYPE_PSS, pssMem);
                recordMemoryHistory(MEM_TYPE_DALVIK, dalvikMem);
                recordMemoryHistory(MEM_TYPE_TOTAL, totalMem);

                // 检查内存泄漏
                checkMemoryLeak(MEM_TYPE_JAVA_HEAP, javaMem.second, (long) (javaMem.first * 0.8));
                checkMemoryLeak(MEM_TYPE_NATIVE_HEAP, nativeMem, 100 * 1024 * 1024); // 100MB
                checkMemoryLeak(MEM_TYPE_PSS, pssMem, 200 * 1024 * 1024); // 200MB
                checkMemoryLeak(MEM_TYPE_DALVIK, dalvikMem, 50 * 1024 * 1024); // 50MB
                checkMemoryLeak(MEM_TYPE_TOTAL, totalMem, 300 * 1024 * 1024); // 300MB

                // 检查低内存状态
                checkLowMemoryState();

                // 记录日志
                Log.d(TAG, String.format(
                        "Memory Usage: JavaHeap=%.2fMB (%.1f%%), Native=%.2fMB, PSS=%.2fMB, Dalvik=%.2fMB, Total=%.2fMB",
                        bytesToMB(javaMem.second), (javaMem.second * 100.0 / javaMem.first),
                        bytesToMB(nativeMem), bytesToMB(pssMem),
                        bytesToMB(dalvikMem), bytesToMB(totalMem)));
            } catch (Exception e) {
                Log.e(TAG, "Memory monitoring error", e);
            } finally {
                // 5秒后再次运行
                monitorHandler.postDelayed(this, DEFAULT_INTERVAL);
            }
        }
    };

    /**
     * 获取Java堆内存信息
     * @return Pair<最大内存, 已使用内存>
     */
    private Pair<Long, Long> getJavaHeapMemory() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return new Pair<>(maxMemory, usedMemory);
    }

    /**
     * 获取Native堆内存
     */
    private long getNativeHeapMemory() {
        return Debug.getNativeHeapAllocatedSize();
    }

    /**
     * 获取PSS内存 (Proportional Set Size)
     */
    private long getPssMemory() {
        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);
        return memoryInfo.getTotalPss() * 1024L; // 转换为字节
    }

    /**
     * 获取Dalvik堆内存 (Android 4.4+)
     */
    private long getDalvikHeapMemory() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(memoryInfo);
            return memoryInfo.dalvikPss * 1024L; // 转换为字节
        }
        return 0;
    }

    /**
     * 获取应用总内存使用量
     */
    private long getTotalMemory() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.totalMem - memoryInfo.availMem;
    }

    /**
     * 检查低内存状态
     */
    private void checkLowMemoryState() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        if (memoryInfo.lowMemory) {
            Log.w(TAG, "Low memory warning! Available memory: " + bytesToMB(memoryInfo.availMem) + "MB");
            // 触发内存转储或清理操作
            triggerMemoryDump();
        }
    }

    /**
     * 记录内存历史用于趋势分析
     */
    private void recordMemoryHistory(String type, long usage) {
        Queue<Long> history = memoryHistory.get(type);
        if (history == null) {
            history = new LinkedList<>();
            memoryHistory.put(type, history);
        }

        history.offer(usage);
        // 保留最近10次记录
        while (history.size() > 10) {
            history.poll();
        }
    }

    /**
     * 检查潜在的内存泄漏
     */
    private void checkMemoryLeak(String type, long currentUsage, long threshold) {
        // 检查是否超过阈值
        if (currentUsage > threshold) {
            Integer count = leakDetectionCounters.getOrDefault(type, 0);
            count++;
            leakDetectionCounters.put(type, count);

            // 连续超过阈值多次，触发泄漏警告
            if (count >= LEAK_DETECTION_THRESHOLD) {
                Log.w(TAG, "Potential memory leak detected for " + type +
                        ": " + bytesToMB(currentUsage) + "MB > " + bytesToMB(threshold) + "MB");

                if (leakListener != null) {
                    leakListener.onPotentialMemoryLeak(type, currentUsage, threshold);
                }

                // 重置计数器
                leakDetectionCounters.put(type, 0);

                // 触发详细内存分析
                triggerDetailedMemoryAnalysis();
            }
        } else {
            // 重置计数器
            leakDetectionCounters.put(type, 0);
        }
    }

    /**
     * 触发详细内存分析
     */
    private void triggerDetailedMemoryAnalysis() {
        // 在后台线程执行以避免ANR
        new Thread(() -> {
            Log.i(TAG, "Starting detailed memory analysis...");

            // 1. 获取内存信息
            dumpMemoryInfo();

            // 2. 检查Activity泄漏
            checkActivityLeaks();

            // 3. 获取内存快照（需要调试权限）
//            if (BuildConfig.DEBUG) {
                dumpHprofData();
//            }

            Log.i(TAG, "Detailed memory analysis completed");
        }, "Memory-Analysis").start();
    }

    /**
     * 转储内存信息
     */
    private void dumpMemoryInfo() {
        try {
            // 获取/proc/pid/status信息
            int pid = Process.myPid();
            String statusPath = "/proc/" + pid + "/status";
            File statusFile = new File(statusPath);

            if (statusFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(statusFile))) {
                    String line;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Memory Status:\n");
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("VmRSS:") || line.startsWith("VmHWM:") ||
                                line.startsWith("VmData:") || line.startsWith("VmStk:") ||
                                line.startsWith("VmExe:") || line.startsWith("VmLib:") ||
                                line.startsWith("VmPTE:") || line.startsWith("VmSwap:")) {
                            sb.append(line).append("\n");
                        }
                    }
                    Log.d(TAG, sb.toString());
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading process status", e);
        }
    }

    /**
     * 检查Activity泄漏（简化版）
     */
    private void checkActivityLeaks() {
        // 实际项目中可以使用反射检查Activity是否泄漏
        // 这里仅模拟检查
        Log.d(TAG, "Checking for activity leaks...");
        // 模拟发现泄漏
        if (SystemClock.elapsedRealtime() % 5 == 0) {
            Log.w(TAG, "Potential activity leak detected: com.example.LeakyActivity");
        }
    }

    /**
     * 生成HPROF内存快照（仅调试模式）
     */
    @SuppressLint("DiscouragedPrivateApi")
    private void dumpHprofData() {
//        if (BuildConfig.DEBUG) {
            try {
                // 使用反射调用dumpHprofData方法
                Method dumpHprofDataMethod = Debug.class.getDeclaredMethod(
                        "dumpHprofData", String.class);
                dumpHprofDataMethod.setAccessible(true);

                String fileName = context.getExternalFilesDir(null) +
                        "/memory_snapshot_" + System.currentTimeMillis() + ".hprof";

                dumpHprofDataMethod.invoke(null, fileName);
                Log.d(TAG, "HPROF memory snapshot saved: " + fileName);
            } catch (Exception e) {
                Log.e(TAG, "Failed to dump HPROF data", e);
            }
//        }
    }

    /**
     * 触发内存转储操作
     */
    private void triggerMemoryDump() {
        // 实际项目中可以在这里实现内存转储逻辑
        // 例如：清理缓存、释放不必要的资源等
        Log.i(TAG, "Performing memory dump operations...");

        // 示例：清理图片缓存
        clearImageCache();
    }

    /**
     * 清理图片缓存（示例）
     */
    private void clearImageCache() {
        // 实际项目中调用图片加载库的清理方法
        Log.d(TAG, "Clearing image cache");
    }

    /**
     * 获取内存使用统计
     */
    public static Map<String, Long> getMemoryUsage() {
        if (instance == null) return new HashMap<>();

        Map<String, Long> usage = new HashMap<>();
        Pair<Long, Long> javaMem = instance.getJavaHeapMemory();
        usage.put("java_used", javaMem.second);
        usage.put("java_max", javaMem.first);
        usage.put("native", instance.getNativeHeapMemory());
        usage.put("pss", instance.getPssMemory());
        usage.put("dalvik", instance.getDalvikHeapMemory());
        usage.put("total", instance.getTotalMemory());

        return usage;
    }

    /**
     * 字节转MB
     */
    private double bytesToMB(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    /**
     * 关闭监控
     */
    public static void shutdown() {
        if (instance != null) {
            instance.stopMonitoring();
            instance.monitorThread.quitSafely();
            instance = null;
        }
    }
}
