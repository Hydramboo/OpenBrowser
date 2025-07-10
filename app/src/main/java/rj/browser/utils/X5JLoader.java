package rj.browser.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * 腾讯X5内核Android客户端
 * 完全基于成功的Python实现同步到Java
 * 可直接集成到Android项目中
 */
public class X5JLoader {
    
    public static final String TAG = "X5JLoader";
    
    // 成功验证的API端点
    public static final String API_ENDPOINT = "https://tbsfile.imtt.qq.com/tbs/x5sdk/getcomponentinfo";
    
    // 连接和读取超时
    public static final int CONNECT_TIMEOUT = 30000;
    public static final int READ_TIMEOUT = 300000;
    
    public Context context;
    public ConfigData configData;
    public DownloadListener downloadListener;
    
    /**
     * 配置数据结构
     */
    public static class ConfigData {
        public String pkg;
        public String sig;
        public String conf_id;
        public String key;
        public String lck;
        
        @Override
        public String toString() {
            return String.format("ConfigData{pkg='%s', conf_id='%s', key='%s'}", 
                pkg, conf_id, key);
        }
    }
    
    /**
     * X5内核信息
     */
    public static class X5CoreInfo {
        public String version;
        public String downloadUrl;
        public String md5;
        public long size;
        public String name;
        public String sign;
        
        @Override
        public String toString() {
            return String.format("X5CoreInfo{version='%s', size=%d, md5='%s'}", 
                version, size, md5);
        }
    }
    
    /**
     * 下载监听器
     */
    public interface DownloadListener {
        void onConfigParsed(ConfigData config);
        void onCoreInfoReceived(X5CoreInfo coreInfo);
        void onDownloadProgress(long downloaded, long total);
        void onDownloadSuccess(File file);
        void onDownloadError(String error);
        void onLog(String message);
    }
    
    /**
     * 构造函数
     */
    public void X5AndroidClient(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * 设置下载监听器
     */
    public void setDownloadListener(DownloadListener listener) {
        this.downloadListener = listener;
    }
    
    /**
     * 解析config.tbs文件 - 基于Python成功实现
     */
    public ConfigData parseConfigFile(File configFile) throws Exception {
        log("开始解析配置文件: " + configFile.getAbsolutePath());
        
        // 读取文件数据
        byte[] fileData = readFileBytes(configFile);
        log("配置文件大小: " + fileData.length + " 字节");
        
        // 解析文件结构 - 完全按照Python实现
        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        
        // 1. RSA签名 (216字节)
        byte[] rsaSignature = new byte[216];
        buffer.get(rsaSignature);
        
        // 2. SHA256哈希 (32字节) - 用作AES密钥
        byte[] sha256Hash = new byte[32];
        buffer.get(sha256Hash);
        
        // 3. 第一个数据块长度和内容
        int firstBlockLen = buffer.getInt();
        byte[] firstBlock = new byte[firstBlockLen];
        buffer.get(firstBlock);
        
        // 4. 第二个数据块长度和内容
        int secondBlockLen = buffer.getInt();
        byte[] secondBlock = new byte[secondBlockLen];
        buffer.get(secondBlock);
        
        // 解密第一个数据块 - 完全按照Python实现
        ConfigData config = decryptConfigBlock(firstBlock, sha256Hash);
        
        if (config != null) {
            this.configData = config;
            log("成功解析配置: " + config.toString());
            
            if (downloadListener != null) {
                downloadListener.onConfigParsed(config);
            }
            
            return config;
        } else {
            throw new Exception("配置文件解密失败");
        }
    }
    
    /**
     * 解密配置数据块 - 完全按照Python成功实现
     */
    public ConfigData decryptConfigBlock(byte[] encryptedData, byte[] keyMaterial) {
        try {
            // Base64解码 - 按照Python实现
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            
            // 使用SHA256哈希作为AES密钥 - 按照Python实现
            SecretKeySpec keySpec = new SecretKeySpec(keyMaterial, "AES");
            
            // AES-ECB解密 - 按照Python实现
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decryptedData = cipher.doFinal(decodedData);
            
            // 移除padding并转换为字符串
            String jsonStr = new String(decryptedData, StandardCharsets.UTF_8).trim();
            // 移除null字符
            jsonStr = jsonStr.replaceAll("\\u0000", "");
            
            // 解析JSON
            JSONObject jsonObj = new JSONObject(jsonStr);
            
            ConfigData config = new ConfigData();
            config.pkg = jsonObj.getString("pkg");
            config.sig = jsonObj.getString("sig");
            config.conf_id = String.valueOf(jsonObj.getInt("conf_id"));
            config.key = jsonObj.getString("key");
            config.lck = jsonObj.getString("lck");
            
            return config;
            
        } catch (Exception e) {
            log("解密失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 生成MD5签名 - 完全按照Python成功实现
     */
    public String generateMD5Signature(String packageName, String appVersion, 
                                     String appSignature, String configId, long timestamp) {
        try {
            // 签名格式: {包名}{版本名}{应用签名}{配置ID}{时间戳} - 按照Python实现
            String signString = packageName + appVersion + appSignature + configId + timestamp;
            
            log("MD5签名字符串: " + signString);
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(signString.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            String md5Hash = sb.toString();
            log("MD5签名结果: " + md5Hash);
            return md5Hash;
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }
    
    /**
     * 构建请求JSON - 完全按照Python成功实现
     */
    public String buildRequestJSON(String packageName, String appVersion, int appVersionCode) {
        try {
            // 获取当前时间戳 (13位毫秒) - 按照Python实现
            long timestamp = System.currentTimeMillis();
            
            // 生成MD5签名
            String signature = generateMD5Signature(
                packageName, appVersion, configData.sig, configData.conf_id, timestamp
            );
            
            // 构造JSON对象 - 完全按照Python成功实现，所有字段转为字符串
            JSONObject jsonData = new JSONObject();
            jsonData.put("appn", packageName);
            jsonData.put("appvn", appVersion);
            jsonData.put("appvc", String.valueOf(appVersionCode));  // 转换为字符串
            jsonData.put("sig", configData.sig);
            jsonData.put("cid", configData.conf_id);  // 已经是字符串
            jsonData.put("ckey", configData.key);
            jsonData.put("sign", signature);
            jsonData.put("source", "X5SDK");
            jsonData.put("timestamp", String.valueOf(timestamp));  // 转换为字符串
            
            String jsonString = jsonData.toString();
            log("请求JSON: " + jsonString);
            
            return jsonString;
            
        } catch (JSONException e) {
            throw new RuntimeException("构建JSON失败", e);
        }
    }
    
    /**
     * 获取X5内核信息 - 完全按照Python成功实现
     */
    public X5CoreInfo getX5CoreInfo(String packageName, String appVersion, int appVersionCode) throws Exception {
        log("请求X5内核信息: " + API_ENDPOINT);
        
        // 构建请求JSON
        String jsonString = buildRequestJSON(packageName, appVersion, appVersionCode);
        byte[] requestData = jsonString.getBytes(StandardCharsets.UTF_8);
        
        // 创建HTTP连接
        URL url = new URL(API_ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            // 设置请求属性 - 完全按照Python成功实现
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(requestData.length));
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setDoOutput(true);
            
            // 发送请求数据
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestData);
            }
            
            int responseCode = conn.getResponseCode();
            log("响应状态码: " + responseCode);
            
            if (responseCode == 200) {
                // 读取响应
                String responseText = readResponseText(conn);
                log("响应内容: " + responseText);
                
                // 解析响应JSON
                JSONObject responseObj = new JSONObject(responseText);
                JSONObject header = responseObj.getJSONObject("header");
                
                if (header.getInt("ret_code") == 0) {
                    X5CoreInfo coreInfo = new X5CoreInfo();
                    coreInfo.version = responseObj.getString("version");
                    coreInfo.downloadUrl = responseObj.getString("url");
                    coreInfo.md5 = responseObj.getString("md5");
                    coreInfo.size = responseObj.getLong("size");
                    coreInfo.name = responseObj.getString("name");
                    coreInfo.sign = responseObj.getString("sign");
                    
                    log("获取内核信息成功: " + coreInfo.toString());
                    
                    if (downloadListener != null) {
                        downloadListener.onCoreInfoReceived(coreInfo);
                    }
                    
                    return coreInfo;
                } else {
                    String reason = header.getString("reason");
                    throw new Exception("服务器返回错误: " + reason);
                }
            } else {
                String errorText = readErrorText(conn);
                throw new Exception("请求失败: " + responseCode + " - " + errorText);
            }
            
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * 下载X5内核文件 - 完全按照Python成功实现
     */
    public File downloadX5Core(X5CoreInfo coreInfo, File outputDir) throws Exception {
        String filename = "x5_core_v" + coreInfo.version + ".tbs";
        File outputFile = new File(outputDir, filename);
        
        log("开始下载X5内核: " + coreInfo.downloadUrl);
        log("保存到: " + outputFile.getAbsolutePath());
        log("预期大小: " + String.format("%,d", coreInfo.size) + " 字节");
        log("预期MD5: " + coreInfo.md5);
        
        // 确保输出目录存在
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // 创建HTTP连接
        URL url = new URL(coreInfo.downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestProperty("User-Agent", "X5SDK/1.0 (Android; TBS)");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("下载失败，状态码: " + responseCode);
            }
            
            long totalSize = conn.getContentLengthLong();
            long downloadedSize = 0;
            
            // 流式下载 - 按照Python实现
            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(outputFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    downloadedSize += bytesRead;
                    
                    // 回调进度
                    if (downloadListener != null) {
                        downloadListener.onDownloadProgress(downloadedSize, totalSize);
                    }
                }
            }
            
            log("下载完成，实际大小: " + String.format("%,d", downloadedSize) + " 字节");
            
            // 校验文件大小
            if (Math.abs(downloadedSize - coreInfo.size) > 1024) {
                log("警告: 文件大小不匹配! 预期: " + coreInfo.size + ", 实际: " + downloadedSize);
            }
            
            // 校验MD5 - 按照Python实现
            log("正在计算文件MD5...");
            String actualMd5 = calculateFileMD5(outputFile);
            
            if (actualMd5.equalsIgnoreCase(coreInfo.md5)) {
                log(" MD5校验通过!");
                
                if (downloadListener != null) {
                    downloadListener.onDownloadSuccess(outputFile);
                }
                
                return outputFile;
            } else {
                String error = String.format(" MD5校验失败! 预期: %s, 实际: %s", 
                    coreInfo.md5, actualMd5);
                log(error);
                
                if (downloadListener != null) {
                    downloadListener.onDownloadError(error);
                }
                
                throw new Exception(error);
            }
            
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * 运行完整流程 - 完全按照Python成功实现
     */
    public void getCore(File configFile, String packageName, String appVersion, 
                               int appVersionCode, File outputDir) {
        
        // 使用AsyncTask在后台执行，避免阻塞UI线程
        new AsyncTask<Void, String, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    publishProgress(" 开始X5内核下载流程");
                    
                    // 1. 解析配置文件
                    publishProgress(" 步骤1: 解析config.tbs配置文件");
                    ConfigData config = parseConfigFile(configFile);
                    publishProgress(" 包名: " + config.pkg);
                    publishProgress(" 配置ID: " + config.conf_id);
                    
                    // 2. 获取X5内核信息 - 使用配置文件中的包名
                    publishProgress(" 步骤2: 获取X5内核下载信息");
                    X5CoreInfo coreInfo = getX5CoreInfo(config.pkg, appVersion, appVersionCode);
                    publishProgress(" X5内核版本: " + coreInfo.version);
                    publishProgress(" 文件大小: " + String.format("%,d", coreInfo.size) + 
                        " 字节 (" + String.format("%.1f", coreInfo.size/1024.0/1024.0) + "MB)");
                    
                    // 3. 下载X5内核
                    publishProgress("⬇️ 步骤3: 下载X5内核文件");
                    File outputFile = downloadX5Core(coreInfo, outputDir);
                    
                    // 4. 完成总结
                    publishProgress(" 步骤4: 下载完成总结");
                    publishProgress(" X5内核下载成功!");
                    publishProgress(" 文件位置: " + outputFile.getAbsolutePath());
                    publishProgress(" 内核版本: " + coreInfo.version);
                    publishProgress(" MD5校验: 通过");
                    
                } catch (Exception e) {
                    publishProgress(" 下载失败: " + e.getMessage());
                    if (downloadListener != null) {
                        downloadListener.onDownloadError(e.getMessage());
                    }
                    Log.e(TAG, "下载失败", e);
                }
                
                return null;
            }
            
            @Override
            protected void onProgressUpdate(String... messages) {
                for (String message : messages) {
                    log(message);
                }
            }
        }.execute();
    }
    
    /**
     * 工具方法
     */
    
    public byte[] readFileBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }
    
    public String readResponseText(HttpURLConnection conn) throws IOException {
        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        }
    }
    
    public String readErrorText(HttpURLConnection conn) throws IOException {
        try (InputStream is = conn.getErrorStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            if (is == null) return "";
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        }
    }
    
    public String calculateFileMD5(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    public void log(String message) {
        Log.i(TAG, message);
        if (downloadListener != null) {
            downloadListener.onLog(message);
        }
    }
}
