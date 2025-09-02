package com.yl.ylappmonitor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkMonitor {
    private static final String TAG = "NetworkMonitor";
    private static final int DEFAULT_SAMPLING_INTERVAL = 5000; // 5秒
    private static NetworkMonitor instance;

    private final Context context;
    private final HandlerThread monitorThread;
    private final Handler monitorHandler;
    private final ConnectivityManager connectivityManager;
    private final Map<String, NetworkMetric> requestMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> domainDnsCache = new ConcurrentHashMap<>();

    // 网络状态监听器
    private NetworkCallback networkCallback;

    // 流量统计
    private long lastTxBytes;
    private long lastRxBytes;
    private long lastTimestamp;

    // DNS缓存
    private final DnsCache dnsCache = new DnsCache();

    // 网络事件监听
    private boolean isMonitoring = false;

    public static void init(Context context) {
        if (instance == null) {
            instance = new NetworkMonitor(context);
        }
    }

    public static NetworkMonitor getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NetworkMonitor not initialized");
        }
        return instance;
    }

    private NetworkMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // 创建监控线程
        this.monitorThread = new HandlerThread("Network-Monitor");
        this.monitorThread.start();
        this.monitorHandler = new Handler(monitorThread.getLooper());

        // 初始化流量基准值
        lastTxBytes = TrafficStats.getUidTxBytes(Process.myUid());
        lastRxBytes = TrafficStats.getUidRxBytes(Process.myUid());
        lastTimestamp = System.currentTimeMillis();

        // 注册网络状态监听
        registerNetworkCallback();
    }

    /**
     * 开始网络监控
     */
    public void startMonitoring() {
        if (isMonitoring) return;

        isMonitoring = true;

        // 启动流量监控
        monitorHandler.post(monitorRunnable);

        // 启动DNS缓存清理
        monitorHandler.postDelayed(dnsCacheCleaner, TimeUnit.MINUTES.toMillis(5));

        Log.i(TAG, "Network monitoring started");
    }

    /**
     * 停止网络监控
     */
    public void stopMonitoring() {
        if (!isMonitoring) return;

        isMonitoring = false;
        monitorHandler.removeCallbacksAndMessages(null);

        Log.i(TAG, "Network monitoring stopped");
    }

    /**
     * 获取当前网络状态
     */
    public String getNetworkType() {
        if (connectivityManager == null) {
            return "UNKNOWN";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                return "DISCONNECTED";
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (capabilities == null) {
                return "UNKNOWN";
            }

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "WIFI";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return getMobileNetworkType();
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "ETHERNET";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                return "BLUETOOTH";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return "VPN";
            }
        } else {
            // 旧版本兼容
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnected()) {
                return "DISCONNECTED";
            }

            int type = activeNetwork.getType();
            if (type == ConnectivityManager.TYPE_WIFI) {
                return "WIFI";
            } else if (type == ConnectivityManager.TYPE_MOBILE) {
                return getMobileNetworkType();
            } else if (type == ConnectivityManager.TYPE_ETHERNET) {
                return "ETHERNET";
            } else if (type == ConnectivityManager.TYPE_BLUETOOTH) {
                return "BLUETOOTH";
            } else if (type == ConnectivityManager.TYPE_VPN) {
                return "VPN";
            }
        }

        return "UNKNOWN";
    }

    /**
     * 获取移动网络类型
     */
    private String getMobileNetworkType() {
        if (connectivityManager == null) {
            return "MOBILE";
        }

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null) {
            return "MOBILE";
        }

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
                return "MOBILE";
        }
    }

    /**
     * 获取网络信号强度（相对值）
     */
    public int getNetworkSignalStrength() {
        // 实际项目中需要根据网络类型获取具体信号强度
        // 这里返回相对值 (0-100)
        return 75; // 模拟值
    }

    /**
     * 获取网络指标数据
     */
    public Map<String, Object> getNetworkMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // 网络类型
        metrics.put("network_type", getNetworkType());

        // 信号强度
        metrics.put("signal_strength", getNetworkSignalStrength());

        // 流量使用情况
        long currentTxBytes = TrafficStats.getUidTxBytes(Process.myUid());
        long currentRxBytes = TrafficStats.getUidRxBytes(Process.myUid());
        long currentTimestamp = System.currentTimeMillis();

        long timeDiff = currentTimestamp - lastTimestamp;
        if (timeDiff > 0) {
            long txRate = (currentTxBytes - lastTxBytes) * 1000 / timeDiff;
            long rxRate = (currentRxBytes - lastRxBytes) * 1000 / timeDiff;

            metrics.put("tx_bytes", currentTxBytes);
            metrics.put("rx_bytes", currentRxBytes);
            metrics.put("tx_rate", txRate); // B/s
            metrics.put("rx_rate", rxRate); // B/s

            // 更新基准值
            lastTxBytes = currentTxBytes;
            lastRxBytes = currentRxBytes;
            lastTimestamp = currentTimestamp;
        }

        // 连接信息
        metrics.put("dns_cache_size", dnsCache.size());
        metrics.put("active_requests", requestMetrics.size());

        return metrics;
    }

    /**
     * 获取请求指标数据
     */
    public List<NetworkMetric> getRequestMetrics() {
        return new ArrayList<>(requestMetrics.values());
    }

    /**
     * 清除请求指标数据
     */
    public void clearRequestMetrics() {
        requestMetrics.clear();
    }

    /**
     * 执行DNS查询并记录延迟
     */
    public void resolveDnsAsync(String hostname) {
        monitorHandler.post(() -> {
            long startTime = System.nanoTime();
            try {
                InetAddress[] addresses = InetAddress.getAllByName(hostname);
                long latency = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                dnsCache.put(hostname, addresses, latency);
            } catch (UnknownHostException e) {
                Log.w(TAG, "DNS resolution failed for " + hostname, e);
                dnsCache.putFailure(hostname);
            }
        });
    }

    /**
     * 获取域名的平均DNS延迟
     */
    public long getDnsLatency(String domain) {
        DnsCache.Entry entry = dnsCache.get(domain);
        return entry != null ? entry.averageLatency : -1;
    }

    /**
     * 注册网络状态监听
     */
    private void registerNetworkCallback() {
        if (connectivityManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();

            networkCallback = new NetworkCallback();
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    /**
     * 注销网络状态监听
     */
    private void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        }
    }

    /**
     * 网络监控任务
     */
    private final Runnable monitorRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isMonitoring) return;

            try {
                // 收集网络指标
                Map<String, Object> metrics = getNetworkMetrics();

                // 记录日志
                Log.d(TAG, String.format(
                        "Network: %s, TX: %d B/s, RX: %d B/s, DNS Cache: %d",
                        metrics.get("network_type"),
                        metrics.get("tx_rate"),
                        metrics.get("rx_rate"),
                        metrics.get("dns_cache_size")
                ));

                // 检查慢请求
                checkSlowRequests();

                // 检查失败请求
                checkFailedRequests();
            } catch (Exception e) {
                Log.e(TAG, "Network monitoring error", e);
            } finally {
                // 5秒后再次运行
                monitorHandler.postDelayed(this, DEFAULT_SAMPLING_INTERVAL);
            }
        }
    };

    /**
     * DNS缓存清理任务
     */
    private final Runnable dnsCacheCleaner = new Runnable() {
        @Override
        public void run() {
            dnsCache.cleanup();
            monitorHandler.postDelayed(this, TimeUnit.MINUTES.toMillis(5));
        }
    };

    /**
     * 检查慢请求
     */
    private void checkSlowRequests() {
        long currentTime = System.currentTimeMillis();
        for (NetworkMetric metric : requestMetrics.values()) {
            // 检查进行中的慢请求
            if (metric.responseCode == 0) {
                long duration = currentTime - metric.startTime;
                if (duration > 5000) { // 超过5秒
                    Log.w(TAG, "Slow request detected: " + metric.url + " (" + duration + "ms)");
                    // 可以在这里触发警报或记录详细日志
                }
            }
        }
    }

    /**
     * 检查失败请求
     */
    private void checkFailedRequests() {
        // 实际项目中可以检查最近的失败请求
        // 这里只是示例
    }

    /**
     * 网络状态回调 (API 21+)
     */
    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            Log.i(TAG, "Network available: " + network);
        }

        @Override
        public void onLost(Network network) {
            Log.w(TAG, "Network lost: " + network);
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
            Log.d(TAG, "Network capabilities changed: " + capabilities);
        }

        @Override
        public void onLinkPropertiesChanged(Network network, android.net.LinkProperties linkProperties) {
            Log.d(TAG, "Link properties changed: " + linkProperties);
        }
    }

    /**
     * 网络请求指标类
     */
    public static class NetworkMetric {
        public String requestId;
        public String url;
        public String method;
        public long startTime;
        public long dnsStartTime;
        public long dnsEndTime;
        public long connectStartTime;
        public long connectEndTime;
        public long sslHandshakeStartTime;
        public long sslHandshakeEndTime;
        public long requestStartTime;
        public long requestEndTime;
        public long responseStartTime;
        public long responseEndTime;
        public long requestSize;
        public long responseSize;
        public int responseCode;
        public String error;
        public String networkType;

        public long getDnsTime() {
            return dnsEndTime > 0 ? dnsEndTime - dnsStartTime : -1;
        }

        public long getConnectTime() {
            return connectEndTime > 0 ? connectEndTime - connectStartTime : -1;
        }

        public long getSslTime() {
            return sslHandshakeEndTime > 0 ? sslHandshakeEndTime - sslHandshakeStartTime : -1;
        }

        public long getRequestTime() {
            return requestEndTime > 0 ? requestEndTime - requestStartTime : -1;
        }

        public long getResponseTime() {
            return responseEndTime > 0 ? responseEndTime - responseStartTime : -1;
        }

        public long getTotalTime() {
            return responseEndTime > 0 ? responseEndTime - startTime : -1;
        }

        @Override
        public String toString() {
            return String.format(
                    "%s %s - %dms (DNS:%dms, Connect:%dms, SSL:%dms)",
                    method, url, getTotalTime(), getDnsTime(), getConnectTime(), getSslTime()
            );
        }
    }

    /**
     * DNS缓存类
     */
    private static class DnsCache {
        static class Entry {
            InetAddress[] addresses;
            long lastResolved;
            long totalLatency;
            int resolveCount;
            int failureCount;
            long lastFailure;

            long averageLatency;

            void recordSuccess(long latency) {
                totalLatency += latency;
                resolveCount++;
                averageLatency = totalLatency / resolveCount;
                lastResolved = System.currentTimeMillis();
                failureCount = 0;
            }

            void recordFailure() {
                failureCount++;
                lastFailure = System.currentTimeMillis();
            }
        }

        private final Map<String, Entry> cache = new ConcurrentHashMap<>();

        public void put(String hostname, InetAddress[] addresses, long latency) {
            Entry entry = cache.get(hostname);
            if (entry == null) {
                entry = new Entry();
                cache.put(hostname, entry);
            }
            entry.addresses = addresses;
            entry.recordSuccess(latency);
        }

        public void putFailure(String hostname) {
            Entry entry = cache.get(hostname);
            if (entry == null) {
                entry = new Entry();
                cache.put(hostname, entry);
            }
            entry.recordFailure();
        }

        public Entry get(String hostname) {
            return cache.get(hostname);
        }

        public int size() {
            return cache.size();
        }

        public void cleanup() {
            long now = System.currentTimeMillis();
            long threshold = TimeUnit.MINUTES.toMillis(30); // 30分钟未使用

            cache.entrySet().removeIf(entry -> {
                Entry e = entry.getValue();
                // 移除长时间未使用且解析失败的条目
                return (now - Math.max(e.lastResolved, e.lastFailure)) > threshold;
            });
        }
    }

    /**
     * 获取TCP连接统计信息
     */
    public Map<String, Integer> getTcpStats() {
        Map<String, Integer> stats = new HashMap<>();

        try {
            File tcpFile = new File("/proc/net/tcp");
            if (tcpFile.exists()) {
                parseTcpFile(tcpFile, stats);
            }

            File tcp6File = new File("/proc/net/tcp6");
            if (tcp6File.exists()) {
                parseTcpFile(tcp6File, stats);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading TCP stats", e);
        }

        return stats;
    }

    private void parseTcpFile(File file, Map<String, Integer> stats) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            // 跳过标题行
            reader.readLine();

            int totalConnections = 0;
            int established = 0;
            int timeWait = 0;
            int closeWait = 0;

            while ((line = reader.readLine()) != null) {
                totalConnections++;
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 3) {
                    String state = parts[3];
                    switch (state) {
                        case "01": // ESTABLISHED
                            established++;
                            break;
                        case "06": // TIME_WAIT
                            timeWait++;
                            break;
                        case "08": // CLOSE_WAIT
                            closeWait++;
                            break;
                    }
                }
            }

            stats.put("total_connections", totalConnections);
            stats.put("established", established);
            stats.put("time_wait", timeWait);
            stats.put("close_wait", closeWait);
        }
    }

    /**
     * 关闭监控
     */
    public void shutdown() {
        stopMonitoring();
        unregisterNetworkCallback();
        monitorThread.quitSafely();
    }
}
