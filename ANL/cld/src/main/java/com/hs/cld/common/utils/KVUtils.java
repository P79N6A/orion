package com.hs.cld.common.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class KVUtils {
	/**
	 * 日志标签
	 */
	private final static String TAG = "KVUtils";

	/**
	 * 安全获取JSON对象中的Boolean类型
	 * @param jo JSON对象
	 * @param name 名字
	 * @param defaultValue 默认值
	 * @return 指定名字对应的Boolean类型
	 */
	public static boolean getBoolean(JSONObject jo, String name, boolean defaultValue) {
		try {
			if ((null != jo) && (jo.has(name))) {
				return TextUtils.equals(jo.getString(name), "1");
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name + "][" + defaultValue
					+ "] get boolean failed: " + e);
		}

		return defaultValue;
	}

	/**
	 * 安全获取JSON对象中的整型
	 * @param jo JSON对象
	 * @param name 名字
	 * @param defaultValue 默认值
	 * @return 指定名字对应的整型
	 */
	public static int getInt(JSONObject jo, String name, int defaultValue) {
		try {
			if ((null != jo) && (jo.has(name))) {
				return Integer.parseInt(jo.getString(name));
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name + "][" + defaultValue
					+ "] get int failed: " + e);
		}

		return defaultValue;
	}

	/**
	 * 安全获取JSON对象中的长整型
	 * @param jo JSON对象
	 * @param name 名字
	 * @param defaultValue 默认值
	 * @return 指定名字对应的长整型
	 */
	public static long getLong(JSONObject jo, String name, long defaultValue) {
		try {
			if ((null != jo) && (jo.has(name))) {
				return Long.parseLong(jo.getString(name));
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name + "][" + defaultValue
					+ "] get long failed: " + e);
		}

		return defaultValue;
	}

	/**
	 * 安全获取JSON对象中的float型
	 * @param jo JSON对象
	 * @param name 名字
	 * @param defaultValue 默认值
	 * @return 指定名字对应的float型
	 */
	public static float getFloat(JSONObject jo, String name, float defaultValue) {
		try {
			if ((null != jo) && (jo.has(name))) {
				return Float.parseFloat(jo.getString(name));
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name + "][" + defaultValue
					+ "] get float failed: " + e);
		}

		return defaultValue;
	}

	/**
	 * 安全获取JSON对象中的字符串类型
	 * @param jo JSON对象
	 * @param name 名字
	 * @param defaultValue 默认值
	 * @return 指定名字对应的字符串类型
	 */
	public static String getString(JSONObject jo, String name, String defaultValue) {
		try {
			if ((null != jo) && (jo.has(name))) {
				return jo.getString(name);
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name + "][" + defaultValue
					+ "] get string failed: " + e);
		}

		return defaultValue;
	}

	/**
	 * 安全获取JSON对象中的字符串类型
	 * @param jo JSON对象
	 * @param name 名字
	 * @return 指定名字对应的字符串类型
	 */
	public static String[] getStringArray(JSONObject jo, String name) {
		try {
			if ((null != jo) && (jo.has(name))) {
				List<String> l = new ArrayList<>();
				JSONArray ja = jo.getJSONArray(name);
				for (int i = 0; i < ja.length(); i++) {
					l.add(ja.getString(i));
				}
				return l.toArray(new String[] {});
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name + "] get string array failed: " + e);
		}

		return new String[] {};
	}

	/**
	 * 安全获取JSON对象中的字符串类型
	 * @param jo JSON对象
	 * @param name 名字
	 * @return 指定名字对应的字符串类型
	 */
	public static List<String> getStringList(JSONObject jo, String name) {
		List<String> l = new ArrayList<>();

		try {
			if ((null != jo) && (jo.has(name))) {
				JSONArray ja = jo.getJSONArray(name);
				for (int i = 0; i < ja.length(); i++) {
					l.add(ja.getString(i));
				}
				return l;
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name + "] get string list failed: " + e);
		}

		return l;
	}

	/**
	 * 将Boolean型值写入JSON对象
	 * @param jo JSON对象
	 * @param name 名字
	 * @param value 对应的Boolean型值
	 */
	public static void putBoolean(JSONObject jo, String name, boolean value) {
		try {
			if (null != jo) {
				jo.put(name, (value ? "1" : "0"));
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name + "][" + value
					+ "] put boolean failed: " + e);
		}
	}

	/**
	 * 将整型值写入JSON对象
	 * @param jo JSON对象
	 * @param name 名字
	 * @param value 对应的整型值
	 */
	public static void putInt(JSONObject jo, String name, int value) {
		try {
			if (null != jo) {
				jo.put(name, ("" + value));
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name + "][" + value
					+ "] put int failed: " + e);
		}
	}

	/**
	 * 将长整型值写入JSON对象
	 * @param jo JSON对象
	 * @param name 名字
	 * @param value 对应的长整型值
	 */
	public static void putLong(JSONObject jo, String name, long value) {
		try {
			if (null != jo) {
				jo.put(name, ("" + value));
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name + "][" + value
					+ "] put long failed: " + e);
		}
	}

	/**
	 * 将浮点型值写入JSON对象
	 * @param jo JSON对象
	 * @param name 名字
	 * @param value 对应的浮点型值
	 */
	public static void putFloat(JSONObject jo, String name, float value) {
		try {
			if (null != jo) {
				jo.put(name, String.valueOf(value));
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name + "][" + value
					+ "] put float failed: " + e);
		}
	}

	/**
	 * 将字符串写入JSON对象
	 * @param jo JSON对象
	 * @param name 名字
	 * @param value 对应的字符串
	 */
	public static void putString(JSONObject jo, String name, String value) {
		try {
			if ((null != jo) && (null != value) && (value.length() > 0)) {
				jo.put(name, value);
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name + "][" + value
					+ "] put string failed: " + e);
		}
	}

	/**
	 * 将字符串写入JSON对象
	 * @param jo JSON对象
	 * @param name 名字
	 * @param array 对应的字符串
	 */
	public static void putStringArray(JSONObject jo, String name, String[] array) {
		try {
			if ((null != jo) && (null != array) && (array.length > 0)) {
				jo.put(name, new JSONArray(array));
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name
					+ "] put string array failed: " + e);
		}
	}

	/**
	 * 将字符串列表写入JSON对象
	 * @param jo JSON对象
	 * @param name 名字
	 * @param l 对应的字符串列表
	 */
	public static void putStringList(JSONObject jo, String name, List<String> l) {
		try {
			if ((null != l) && (!l.isEmpty())) {
				JSONArray ja = new JSONArray();

				for (String s: l) {
					ja.put(s);
				}

				jo.put(name, ja);
			}
		} catch (Exception e) {
			LOG.w(TAG, "[" + name
					+ "] put string array failed: " + e);
		}
	}
}
