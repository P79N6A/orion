package com.hs.q.common.utils;

import android.util.Log;

import java.lang.reflect.Method;

/**
 * 日志
 * 
 */
public class LOG {
    /**
	 * 日志等级
	 */
	public final static int D = Log.DEBUG;    // DEBUG 级别
	public final static int I = Log.INFO;     // INFO 级别
	public final static int W = Log.WARN;     // WARN 级别
	public final static int E = Log.ERROR;    // ERROR 级别

	/**
	 * 日志打开或者关闭
	 */
	private static volatile boolean mEnabled = false;

	/**
	 * 最小日志级别，如果
	 */
	private static volatile int mLevel = 0;

	/**
	 * 日志标签
	 */
	private static volatile String stag = "ANL";

	/**
	 * 设置日志开关
	 * @param enabled true 开启日志；false 关闭日志
	 */
	public static void setEnabled(boolean enabled) {
		mEnabled = enabled;
		if (enabled) {
			Log.i("LOG", "log enabled ...");
		}
	}

	/**
	 * 判断当前系统是否输出debug级别日志
	 * @return true 输出；false 未输出
	 */
	public static boolean isLogD() {
		return equalsIgnoreCase("0", getPropertyString("persist.sys.log_reject_level", ""));
	}

	/**
	 * 获取系统属性
	 * @param key 对应关键字
	 * @param defaultValue 默认值
	 * @return 系统属性值
	 */
	private static boolean getPropertyBoolean(String key, boolean defaultValue) {
		try {
			String s = getPropertyString(key, "");
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
	private static String getPropertyString(String key, String defaultValue) {
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
	
	/**
	 * 设置日志等级
	 * @param level 日志等级
	 */
	public static void setLevel(int level) {
		mLevel = level;
	}
	
	/**
	 * 设置日志标签
	 * @param tag 日志标签
	 */
	public static void setTag(String tag) {
		Log.i("LOG", "[" + stag + "] set log tag to " + tag);
        stag = tag;
	}

    /**
     * 打印日志，DEBUG级别
     * 
     * @param tag 日志标签
     * @param msg 日志消息
     */
    public static void d(String tag, String msg) {
		logLevel(Log.DEBUG, tag, msg);
    }

    /**
     * 打印日志，DEBUG级别
     * 
     * @param tag 日志标签
     * @param msg 日志消息
     * @param t 异常对象
     */
    public static void d(String tag, String msg, Throwable t) {
		logLevel(Log.DEBUG, tag, msg, t);
    }

    /**
     * 打印日志，INFO级别
     * 
     * @param tag 日志标签
     * @param msg 日志消息
     */
    public static void i(String tag, String msg) {
		logLevel(Log.INFO, tag, msg);
    }

    /**
     * 打印日志，INFO级别
     * 
     * @param tag 日志标签
     * @param msg 日志消息
     * @param t 异常对象
     */
    public static void i(String tag, String msg, Throwable t) {
		logLevel(Log.INFO, tag, msg, t);
    }

    /**
     * 打印日志，WARN级别
     * 
     * @param tag 日志标签
     * @param msg 日志消息
     */
    public static void w(String tag, String msg) {
		logLevel(Log.WARN, tag, msg);
    }

    /**
     * 打印日志，WARN级别
     * 
     * @param tag 日志标签
     * @param msg 日志消息
     * @param t 异常对象
     */
    public static void w(String tag, String msg, Throwable t) {
		logLevel(Log.WARN, tag, msg, t);
    }

    /**
     * 打印日志，ERROR级别
     * 
     * @param tag 日志标签
     * @param msg 日志消息
     */
    public static void e(String tag, String msg) {
		logLevel(Log.ERROR, tag, msg);
    }

    /**
     * 打印日志，ERROR级别
     * 
     * @param tag 日志标签
     * @param msg 日志消息
     * @param t 异常对象
     */
    public static void e(String tag, String msg, Throwable t) {
		logLevel(Log.ERROR, tag, msg, t);
    }

	/**
	 * 打印日志
	 * @param level 指定日志级别
	 * @param tag 日志标签
	 * @param msg 消息
 	 * @param t 异常对象
	 */
	private static void logLevel(int level, String tag, String msg, Throwable t) {
		if (mEnabled) {
			int tmpLevel = ((level < mLevel) ? mLevel : level);

			if (Log.VERBOSE == tmpLevel) {
				Log.v(stag, "[" + tag + "] " + msg, t);
			} else if (Log.DEBUG == tmpLevel) {
				Log.d(stag, "[" + tag + "] " + msg, t);
			} else if (Log.INFO == tmpLevel) {
				Log.i(stag, "[" + tag + "] " + msg, t);
			} else if (Log.WARN == tmpLevel) {
				Log.w(stag, "[" + tag + "] " + msg, t);
			} else if (Log.ERROR == tmpLevel) {
				Log.e(stag, "[" + tag + "] " + msg, t);
			} else if (Log.ASSERT == tmpLevel) {
				Log.wtf(stag, "[" + tag + "] " + msg, t);
			} else {
				Log.e(stag, "[" + tag + "] " + msg, t);
			}
		}
	}

	/**
	 * 打印日志
	 * @param level 指定日志级别
	 * @param tag 日志标签
	 * @param msg 消息
	 */
	private static void logLevel(int level, String tag, String msg) {
		if (mEnabled) {
			int tmpLevel = ((level < mLevel) ? mLevel : level);

			if (Log.VERBOSE == tmpLevel) {
				Log.v(stag, "[" + tag + "] " + msg);
			} else if (Log.DEBUG == tmpLevel) {
				Log.d(stag, "[" + tag + "] " + msg);
			} else if (Log.INFO == tmpLevel) {
				Log.i(stag, "[" + tag + "] " + msg);
			} else if (Log.WARN == tmpLevel) {
				Log.w(stag, "[" + tag + "] " + msg);
			} else if (Log.ERROR == tmpLevel) {
				Log.e(stag, "[" + tag + "] " + msg);
			} else if (Log.ASSERT == tmpLevel) {
				Log.wtf(stag, "[" + tag + "] " + msg);
			} else {
				Log.e(stag, "[" + tag + "] " + msg);
			}
		}
	}
}
