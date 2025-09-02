package com.yl.ylappmonitor;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

// CpuMonitor.java - CPU监控
public class CpuMonitor {
    private static long lastCpuTime;
    private static long lastAppCpuTime;

    public static void start() {
        getProcessCpuTime(); // 初始化基准值
    }

    /**
     * 获取当前进程的CPU时间（用户态+内核态）
     *
     * @return 当前进程的CPU时间（单位：jiffies）
     */
    private static long getProcessCpuTime() {
        String path = "LeftBarService/stat";
        BufferedReader reader = null;
        long cpuTime = 0;

        try {
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();

            if (line != null) {
                // 解析进程状态行
                String[] tokens = line.split("\\s+");

                // 字段索引参考: https://man7.org/linux/man-pages/man5/proc.5.html
                // 第14项: utime - 用户态时间 (clock ticks)
                // 第15项: stime - 内核态时间 (clock ticks)
                if (tokens.length > 15) {
                    long utime = Long.parseLong(tokens[13]);
                    long stime = Long.parseLong(tokens[14]);
                    cpuTime = utime + stime;
                }
            }
        } catch (IOException | NumberFormatException e) {
            Log.e("CpuMonitor", "Error reading /proc/self", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e("CpuMonitor", "Error closing reader", e);
                }
            }
        }

        return cpuTime;
    }

    public static double getCpuUsage() {
        long systemTime = System.currentTimeMillis();
        try {
            long cpuTime = getTotalCpuTime();
            long appTime = getAppCpuTime();

            if (lastCpuTime == 0 || lastAppCpuTime == 0) {
                lastCpuTime = cpuTime;
                lastAppCpuTime = appTime;
                return 0;
            }

            long cpuDiff = cpuTime - lastCpuTime;
            long appDiff = appTime - lastAppCpuTime;

            if (cpuDiff <= 0) return 0;

            double usage = (appDiff * 100.0) / cpuDiff;
            lastCpuTime = cpuTime;
            lastAppCpuTime = appTime;
            return Math.min(100, usage);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static long getTotalCpuTime() {
        String[] cpuInfos = FileUtils.readFile("/proc/self/stat").split(" ");
        Log.e("CPUMonitor", "getTotalCpuTime: " + Arrays.toString(cpuInfos));
        long total = 0;
        for (int i = 2; i < 9; i++) {
            total += Long.parseLong(cpuInfos[i]);
        }
        return total;
    }

    private static long getAppCpuTime() {
        String path = "/proc/self/stat";
        String[] stat = FileUtils.readFile(path).split(" ");
        return Long.parseLong(stat[13]) + Long.parseLong(stat[14]);
    }
}

