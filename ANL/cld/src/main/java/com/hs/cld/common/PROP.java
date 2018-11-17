package com.hs.cld.common;

import java.lang.reflect.Method;

public class PROP {
	/**
	 * 是否启用测试参数
	 */
	public static boolean isBeta() {
		return readPropertyAsBoolean("debug.hs.beta", false);
	}

	/**
	 * 是否打开日志
	 */
	public static boolean isLogEnabled() {
		return readPropertyAsBoolean("debug.hs.log.enabled", false);
	}

	/**
	 * 是否隐藏目录
	 */
	public static boolean isExpDir() {
		return readPropertyAsBoolean("debug.hs.dir.explicit", false);
	}

	/**
	 * 是否忽略开发者模式
	 */
	public static boolean ignoreCheckMode() {
		return readPropertyAsBoolean("debug.hs.ignore.checkmode", false);
	}

	/**
	 * 获取系统属性
	 * @param key 对应关键字
	 * @param defaultValue 默认值
	 * @return 系统属性值
	 */
	private static boolean readPropertyAsBoolean(String key, boolean defaultValue) {
		try {
			String s = readProperty(key, "");
			if (empty(s)) {
				return defaultValue;
			} else {
				return equalsIgnoreCase(s, "1");
			}
		} catch (Throwable t) {
		}
		return defaultValue;
	}

	/**
	 * 获取系统属性
	 * @param key 对应关键字
	 * @param defaultValue 默认值
	 * @return 系统属性值
	 */
	private static String readProperty(String key, String defaultValue) {
		try {
			Class<?> clazz = Class.forName("android.os.SystemProperties");
			Method method = clazz.getDeclaredMethod("get", String.class);
			method.setAccessible(true);
			return (String) method.invoke(null, key);
		} catch (Throwable t) {
		}
		return defaultValue;
	}

	/**
	 * 判断字符串是否为空
	 * @param s 字符串
	 * @return true 为空；false 不为空
	 */
	private static boolean empty(String s) {
		return ((null == s) || (s.length() <= 0));
	}

	/**
	 * 字符串比较，并且忽略大小写
	 * @param s1 字符串1
	 * @param s2 字符串2
	 * @return true 相同；false 不相同
	 */
	private static boolean equalsIgnoreCase(String s1, String s2) {
		if ((null == s1) && (null == s2)) {
			return true;
		} else if ((null != s1) && (null != s2)) {
			return s1.equalsIgnoreCase(s2);
		} else {
			return false;
		}
	}
}
