package com.hs.cld.common.id;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;

import com.hs.cld.common.utils.Device;
import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.MD5;
import com.hs.cld.common.utils.TextUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.UUID;

/**
 * CUID 算法及生成工具
 */
public final class CUID {
	/**
	 * 日志标签
	 */
	private final static String TAG = "CUID";

	/**
	 * 配置文件的相关定义
	 */
	private static final String SF_NAME = "hs.cuid.v1";

	/**
	 * 扩展卡文件存储
	 */
	private static final String CUID_FILENAME0 = "hs/.cuid.v1";

	/**
	 * 扩展卡文件存储
	 */
	private static final String CUID_FILENAME1 = "Android/.hs/.cuid.v1";

	/**
	 * 存储在系统中的关键字
	 */
	private static final String KEY_CUID = "persist.hs.cuid.v1";

	/**
	 * 全局缓存的CUID
	 */
	private static volatile String gCUID = null;

	/**
	 * 获取CUID字符串
	 * @param context 应用上下文
	 * @return CUID字符串
	 */
	public static String str(Context context) {
		if (TextUtils.empty(gCUID)) {
			synchronized (CUID.class) {
				if (TextUtils.empty(gCUID)) {
					gCUID = getAndSyncCUID(context);
				}
			}
		}
		
		return gCUID;
	}

	private static String getAndSyncCUID(Context context) {
		String cuid = getCUID(context);

		if (TextUtils.empty(cuid)) {
			cuid = generateCUID(context);

			if (!TextUtils.empty(cuid)) {
				putCUID(context, cuid);
			}
		}

		return cuid;
	}

	private static String getCUID(Context context) {
		String cuid = getStringFromExtSD(CUID_FILENAME1);
		if (!TextUtils.empty(cuid)) {
			return cuid;
		}

		cuid = getStringFromExtSD(CUID_FILENAME0);
		if (!TextUtils.empty(cuid)) {
			return cuid;
		}

		cuid = getStringFromSystemProperty(KEY_CUID, "");
		if (!TextUtils.empty(cuid)) {
			return cuid;
		}

		return getStringFromSharedPreferences(context, KEY_CUID, "");
	}

	private static void putCUID(Context context, String cuid) {
		putStringToSharedPreferences(context, KEY_CUID, cuid);
		putStringToSystemProperty(KEY_CUID, cuid);
		putStringToExtSD(CUID_FILENAME0, cuid);
		putStringToExtSD(CUID_FILENAME1, cuid);
	}

	private static String generateCUID(Context context) {
		String matrix = getMatrix(context);
		String md5 = getMD5String(matrix);

		if (TextUtils.empty(md5)) {
			md5 = UUID.randomUUID().toString().replaceAll("-", "");
		}

		LOG.d(TAG, "generate CUID: " + md5 + ", matrix: " + matrix);
		return md5;
	}

	private static String getMatrix(Context context) {
		StringBuilder builder = new StringBuilder();

		HashSet<String> IMEIs = Device.getIMEIs(context);
		if (null != IMEIs) {
			for (String IMEI: IMEIs) {
				builder.append(IMEI);
			}
		}

		String cpuid = Device.getCpuid();
		if (!TextUtils.empty(cpuid)) {
			builder.append(cpuid);
		}

		builder.append(UUID.randomUUID().toString().replaceAll("-", ""));
		return builder.toString();
	}

	private static String getMD5String(String matrix) {
		try {
			return MD5.getString(matrix.getBytes("UTF-8"));
		} catch (Exception e) {
			// nothing to do
		}

		return "";
	}

	private static String getStringFromSharedPreferences(Context context,
			String name, String defaultValue) {
		try {
			SharedPreferences sf = context.getSharedPreferences(SF_NAME, Context.MODE_PRIVATE);
			return sf.getString(name, defaultValue);
		} catch (Throwable t) {
			LOG.e(TAG, "[" + name + "][" + defaultValue
					+ "] get string failed(" + t.getClass().getSimpleName()
					+ "): " + t.getMessage());
		}

		return defaultValue;
	}

	private static boolean putStringToSharedPreferences(Context context,
			String name, String value) {
		try {
			String v = ((null == value) ? "" : value);
			SharedPreferences sf = context.getSharedPreferences(SF_NAME, Context.MODE_PRIVATE);
			Editor editor = sf.edit();
			editor.putString(name, v);
			return editor.commit();
		} catch (Throwable t) {
			LOG.e(TAG, "[" + name + "][" + value
					+ "] put string failed(" + t.getClass().getSimpleName()
					+ "): " + t.getMessage());
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
					+ "] put string failed(" + e.getClass().getSimpleName()
					+ "): " + e.getMessage());
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
					+ "] get string failed(" + e.getClass().getSimpleName()
					+ "): " + e.getMessage());
		} finally {
			close(reader);
		}

		return "";
	}

	private static void close(BufferedWriter writer) {
		try {
			if (null != writer) {
				writer.close();
			}
		} catch (Exception e) {
			// nothing to do
		}
	}

	private static void close(BufferedReader reader) {
		try {
			if (null != reader) {
				reader.close();
			}
		} catch (Exception e) {
			// nothing to do
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
			LOG.e(TAG, "[" + key + "][" + defaultValue
					+ "] get system property failed(" + t.getClass().getSimpleName()
					+ "): " + t.getMessage());
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
			LOG.e(TAG, "[" + key + "][" + value
					+ "] put system property failed(" + t.getClass().getSimpleName()
					+ "): " + t.getMessage());
		}

		return false;
	}
}
