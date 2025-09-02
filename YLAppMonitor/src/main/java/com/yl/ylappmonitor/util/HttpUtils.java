package com.yl.ylappmonitor.util;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpUtils {
    private static final String TAG = "HttpUtils";
    private static final int CONNECT_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 15000; // 15 seconds

    /**
     * 发送POST请求 (JSON格式)
     */
    public static void postJson(String urlString, String jsonData) {
        new HttpTask(urlString, "POST", jsonData, "application/json", null).execute();
    }

    /**
     * 发送GET请求
     */
    public static void get(String urlString, Map<String, String> headers) {
        new HttpTask(urlString, "GET", null, null, headers).execute();
    }

    /**
     * 发送压缩的POST请求
     */
    public static void postJsonCompressed(String urlString, String jsonData) {
        try {
            byte[] compressedData = compressData(jsonData.getBytes("UTF-8"));
            new HttpTask(urlString, "POST", compressedData, "application/json", null, true).execute();
        } catch (Exception e) {
            Log.e(TAG, "Error compressing data", e);
        }
    }

    /**
     * 异步HTTP任务
     */
    private static class HttpTask extends AsyncTask<Void, Void, String> {
        private final String urlString;
        private final String method;
        private final Object data;
        private final String contentType;
        private final Map<String, String> headers;
        private final boolean compressed;

        HttpTask(String urlString, String method, Object data, String contentType, Map<String, String> headers) {
            this(urlString, method, data, contentType, headers, false);
        }

        HttpTask(String urlString, String method, Object data, String contentType,
                 Map<String, String> headers, boolean compressed) {
            this.urlString = urlString;
            this.method = method;
            this.data = data;
            this.contentType = contentType;
            this.headers = headers;
            this.compressed = compressed;
        }

        @Override
        protected String doInBackground(Void... voids) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();

                // 如果是HTTPS，设置信任所有证书（仅用于调试）
                if (urlString.startsWith("https")) {
                    trustAllCertificates((HttpsURLConnection) connection);
                }

                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setRequestMethod(method);
                connection.setDoInput(true);

                // 设置请求头
                if (contentType != null) {
                    connection.setRequestProperty("Content-Type", contentType);
                }

                if (compressed) {
                    connection.setRequestProperty("Content-Encoding", "gzip");
                }

                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        connection.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }

                // 发送请求数据
                if (data != null && ("POST".equals(method) || "PUT".equals(method))) {
                    connection.setDoOutput(true);
                    try (OutputStream os = connection.getOutputStream()) {
                        if (data instanceof String) {
                            os.write(((String) data).getBytes("UTF-8"));
                        } else if (data instanceof byte[]) {
                            os.write((byte[]) data);
                        }
                    }
                }

                // 获取响应
                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    return readResponse(connection);
                } else {
                    String error = readError(connection);
                    Log.e(TAG, "HTTP error " + responseCode + ": " + error);
                    return "Error: " + responseCode;
                }
            } catch (Exception e) {
                Log.e(TAG, "HTTP request failed: " + urlString, e);
                return "Error: " + e.getMessage();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // 可以在这里处理响应结果
            Log.d(TAG, "HTTP response: " + result);
        }

        private String readResponse(HttpURLConnection connection) throws IOException {
            try (InputStream is = connection.getInputStream()) {
                InputStream input = is;

                // 处理GZIP压缩的响应
                if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
                    input = new GZIPInputStream(is);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        }

        private String readError(HttpURLConnection connection) throws IOException {
            try (InputStream is = connection.getErrorStream()) {
                if (is == null) return "No error stream";

                InputStream input = is;
                if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
                    input = new GZIPInputStream(is);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line);
                }
                return error.toString();
            }
        }
    }

    /**
     * 信任所有证书 (仅用于调试，生产环境使用正式证书)
     */
    private static void trustAllCertificates(HttpsURLConnection connection) {
        try {
            // 创建信任所有证书的TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // 安装信任管理器
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            connection.setSSLSocketFactory(sc.getSocketFactory());

            // 忽略主机名验证
            connection.setHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            Log.e(TAG, "Error trusting all certificates", e);
        }
    }

    /**
     * 压缩数据
     */
    private static byte[] compressData(byte[] data) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(data.length);
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(data);
        gzip.close();
        return bos.toByteArray();
    }

    /**
     * 解压数据
     */
    private static byte[] decompressData(byte[] compressed) throws IOException {
        java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(bis);
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = gis.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        gis.close();
        bos.close();
        return bos.toByteArray();
    }
}
