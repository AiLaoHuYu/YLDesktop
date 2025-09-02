package com.yl.ylappmonitor.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {
    private static final String TAG = "FileUtils";

    /**
     * 读取文件内容
     */
    public static String readFile(String filePath) {
        return readFile(new File(filePath));
    }

    public static String readFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            Log.e(TAG, "File not found: " + (file != null ? file.getAbsolutePath() : "null"));
            return "";
        }

        StringBuilder content = new StringBuilder();
        try (InputStream inputStream = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + file.getAbsolutePath(), e);
        }
        return content.toString();
    }

    /**
     * 写入文件
     */
    public static boolean writeFile(String filePath, String content) {
        return writeFile(new File(filePath), content, false);
    }

    public static boolean writeFile(File file, String content) {
        return writeFile(file, content, false);
    }

    public static boolean writeFile(File file, String content, boolean append) {
        if (file == null) return false;

        // 确保目录存在
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            Log.e(TAG, "Failed to create directory: " + parentDir.getAbsolutePath());
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(file, append), 8192)) {
            writer.write(content);
            writer.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing file: " + file.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 复制文件
     */
    public static boolean copyFile(File src, File dest) {
        if (src == null || !src.exists() || !src.isFile()) {
            Log.e(TAG, "Source file not found: " + (src != null ? src.getAbsolutePath() : "null"));
            return false;
        }

        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error copying file: " + src.getAbsolutePath() + " to " + dest.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 删除文件或目录
     */
    public static boolean deleteFile(File file) {
        if (file == null || !file.exists()) {
            return true;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteFile(child);
                }
            }
        }
        return file.delete();
    }

    /**
     * 获取文件大小
     */
    public static long getFileSize(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }

        if (file.isFile()) {
            return file.length();
        }

        long size = 0;
        File[] files = file.listFiles();
        if (files != null) {
            for (File child : files) {
                size += getFileSize(child);
            }
        }
        return size;
    }

    /**
     * 创建时间戳文件名
     */
    public static String createTimestampFilename(String prefix, String extension) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US);
        String timestamp = sdf.format(new Date());
        return prefix + "_" + timestamp + "." + extension;
    }

    /**
     * 获取应用缓存目录
     */
    public static File getAppCacheDir(Context context) {
        File cacheDir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            cacheDir = context.getExternalCacheDir();
        } else {
            cacheDir = context.getCacheDir();
        }

        if (cacheDir == null) {
            cacheDir = new File("/data/data/" + context.getPackageName() + "/cache");
        }

        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            Log.w(TAG, "Failed to create cache directory: " + cacheDir.getAbsolutePath());
        }
        return cacheDir;
    }

    /**
     * 获取应用文件目录
     */
    public static File getAppFilesDir(Context context) {
        File filesDir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            filesDir = context.getExternalFilesDir(null);
        } else {
            filesDir = context.getFilesDir();
        }

        if (filesDir == null) {
            filesDir = new File("/data/data/" + context.getPackageName() + "/files");
        }

        if (!filesDir.exists() && !filesDir.mkdirs()) {
            Log.w(TAG, "Failed to create files directory: " + filesDir.getAbsolutePath());
        }
        return filesDir;
    }

    /**
     * 清理过期文件
     */
    public static void cleanOldFiles(File directory, long maxAgeMillis) {
        if (directory == null || !directory.isDirectory()) return;

        long currentTime = System.currentTimeMillis();
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile() && (currentTime - file.lastModified()) > maxAgeMillis) {
                if (!file.delete()) {
                    Log.w(TAG, "Failed to delete old file: " + file.getAbsolutePath());
                }
            }
        }
    }
}
