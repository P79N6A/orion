package com.hs.cld.common.id;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;

import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.TextUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * IMEI 算法及生成工具
 */
public final class IMEI {
	/**
	 * 日志标签
	 */
	private final static String TAG = "IMEI";

	/**
	 * 配置文件的相关定义
	 */
	private static final String SF_NAME = "hs.imei.v1";

	/**
	 * 扩展卡文件存储
	 */
	private static final String FILENAME0 = "hs/.imei.v1";

	/**
	 * 扩展卡文件存储
	 */
	private static final String FILENAME1 = "Android/.hs/.imei.v1";

	/**
	 * 存储在系统中的关键字
	 */
	private static final String KEY = "persist.hs.imei.v1";

	/**
	 * 全局缓存的IMEI
	 */
	private static volatile String gIMEI = null;

	/**
	 * 获取IMEI字符串
	 * @param context 应用上下文
	 * @return IMEI字符串
	 */
	public static String str(Context context) {
		if (TextUtils.empty(gIMEI)) {
			synchronized (IMEI.class) {
				if (TextUtils.empty(gIMEI)) {
					gIMEI = getAndSync(context);
				}
			}
		}
		
		return gIMEI;
	}

	private static String getAndSync(Context context) {
		String imei = getLocalImei(context);

		if (TextUtils.empty(imei)) {
			imei = generateImei();

			if (!TextUtils.empty(imei)) {
				putImei(context, imei);
			}
		}

		return imei;
	}

	private static String getLocalImei(Context context) {
		String cuid = getStringFromExtSD(FILENAME1);
		if (!TextUtils.empty(cuid)) {
			return cuid;
		}

		cuid = getStringFromExtSD(FILENAME0);
		if (!TextUtils.empty(cuid)) {
			return cuid;
		}

		cuid = getStringFromSystemProperty(KEY, "");
		if (!TextUtils.empty(cuid)) {
			return cuid;
		}

		return getStringFromPrefs(context, KEY, "");
	}

	private static void putImei(Context context, String imei) {
		putStringToPrefs(context, KEY, imei);
		putStringToSystemProperty(KEY, imei);
		putStringToExtSD(FILENAME0, imei);
		putStringToExtSD(FILENAME1, imei);
	}

	private static String generateImei() {
		try {
			long m8 = (System.currentTimeMillis() % 100000000L);
			return String.format(Locale.getDefault(), "8667000%08d", m8);
		} catch (Exception e) {
			return "866700037463799";
		}
	}

	private static String getStringFromPrefs(Context context,
			String name, String defaultValue) {
		try {
			SharedPreferences sf = context.getSharedPreferences(SF_NAME, Context.MODE_PRIVATE);
			return sf.getString(name, defaultValue);
		} catch (Throwable t) {
			LOG.e(TAG, "[" + name + "][" + defaultValue
					+ "] get prefs string failed: " + t);
		}

		return defaultValue;
	}

	private static boolean putStringToPrefs(Context context,
			String name, String value) {
		try {
			String v = ((null == value) ? "" : value);
			SharedPreferences sf = context.getSharedPreferences(SF_NAME, Context.MODE_PRIVATE);
			Editor editor = sf.edit();
			editor.putString(name, v);
			return editor.commit();
		} catch (Throwable t) {
			LOG.e(TAG, "[" + name + "][" + value
					+ "] put prefs string failed: " + t);
		}

		return false;
	}

	private static boolean putStringToExtSD(String filename, String value) {
		File extFile = new File(Environment.getExternalStorageDirectory(), filename);
		BufferedWriter writer = null;

		try {
			ensureFileExist(extFile);
			writer = new BufferedWriter(new FileWriter(extFile));
			writer.write(value);
			writer.write("\n");
			writer.flush();
			return true;
		} catch (Exception e) {
			LOG.e(TAG, "[" + filename + "][" + value
					+ "] put extsd string failed: " + e);
		} finally {
			close(writer);
		}

		return false;
	}

	private static void ensureFileExist(File file) throws IOException {
		if (!file.exists()) {
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
		} else {
			file.delete();
		}

		file.createNewFile();
	}

	private static String getStringFromExtSD(String filename) {
		File extFile = new File(Environment.getExternalStorageDirectory(), filename);
		BufferedReader reader = null;

		if (!extFile.exists()) {
			return "";
		}

		try {
			reader = new BufferedReader(new FileReader(extFile));

			String line = null;
			StringBuilder builder = new StringBuilder();

			while (null != (line = reader.readLine())) {
				builder.append(line);
			}

			return builder.toString();
		} catch (Exception e) {
			LOG.e(TAG, "[" + filename
					+ "] get extsd string failed: " + e);
		} finally {
			close(reader);
		}

		return "";
	}

	private static void close(Closeable... args) {
		if (null != args) {
			for (Closeable arg: args) {
				try {
					if (null != arg) {
						arg.close();
					}
				} catch (Exception e) {
					// nothing to do
				}
			}
		}
	}

	/**
	 * 获取系统属性
	 * @param key 对应关键字
	 * @param defaultValue 默认值
	 * @return 系统属性值
	 */
	private static String getStringFromSystemProperty(String key, String defaultValue) {
		try {
			Class<?> clazz = Class.forName("android.os.SystemProperties");
			Method method = clazz.getDeclaredMethod("get", String.class);
			method.setAccessible(true);
			return (String) method.invoke(null, key);
		} catch (Throwable t) {
			//
		}

		return defaultValue;
	}

	/**
	 * 设置键值到系统属性
	 * @param key 对应关键字
	 * @param value 值
	 * @return true 成功；false 失败
	 */
	private static boolean putStringToSystemProperty(String key, String value) {
		try {
			Class<?> clazz = Class.forName("android.os.SystemProperties");
			Method method = clazz.getDeclaredMethod("set", String.class, String.class);
			method.setAccessible(true);
			method.invoke(null, key, value);
			return true;
		} catch (Throwable t) {
			//
		}

		return false;
	}
}
