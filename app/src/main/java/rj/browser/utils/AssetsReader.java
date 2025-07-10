package rj.browser.utils;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

public class AssetsReader {

    /**
     * 从 assets 文件夹中读取文本文件的内容。
     * @param context 应用的上下文。
     * @param fileName assets 文件夹中文件的名称（例如 "dark.js"）。
     * @return 文件的全部内容字符串，如果发生错误则返回 null。
     */
    public static String readAssetFile(Context context, String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n"); // 重新添加换行符，保持原始文件结构
            }
            reader.close();
            inputStream.close();
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}