package com.hs.cld.basic;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import com.hs.cld.common.utils.LOG;

import java.util.HashSet;
import java.util.List;

public class DxRTLocker {
	private final static String TAG = "DRTL";

	public synchronized static boolean enquire(Context context) {
		int runningPid = getRunningDexPid(context);
		int spid = getSelfPid();

		if (runningPid <= 0) {
			int sspid = setRunningDexPid(context, spid);
			LOG.i(TAG, "set running dx process: pid=" + sspid);
			return true;
		} else if (runningPid != spid) {
			HashSet<Integer> runningPids = getAllRunPids(context);

			if (runningPids.contains(runningPid)) {
				LOG.i(TAG, "dx process is running: pid=" + runningPid);
				return false;
			} else {
				int sspid = setRunningDexPid(context, getSelfPid());
				LOG.i(TAG, "replace running dx process: " + runningPid + "->" + sspid);
				return true;
			}
		} else {
			return true;
		}
	}

	public synchronized static void release(Context context) {
		// nothing to do
	}

	private static int getSelfPid() {
		return android.os.Process.myPid();
	}

	private static int getRunningDexPid(Context context) {
		return readInt(context, "debug.hs.rdp", 0);
	}

	private static int setRunningDexPid(Context context, int pid) {
		return (writeInt(context, "debug.hs.rdp", pid) ? pid : 0);
	}

	public static HashSet<Integer> getAllRunPids(Context context) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			return getAllRunPids6(context);
		} else {
			return getAllRunPids5(context);
		}
	}

	private static HashSet<Integer> getAllRunPids5(Context context) {
		HashSet<Integer> pids = new HashSet<>();

		try {
			ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
			List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();

			for(ActivityManager.RunningAppProcessInfo processInfo: processInfos) {
				pids.add(processInfo.pid);
			}
		} catch (Throwable t) {
		}

		return pids;
	}

	public static HashSet<Integer> getAllRunPids6(Context context) {
		HashSet<Integer> pids = new HashSet<>();
		return pids;
	}

	private static boolean isInteger(String s) {
		try {
			if (empty(s)) {
				return false;
			} else {
				Integer.parseInt(s);
				return true;
			}
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 获取系统属性
	 * @param key 对应关键字
	 * @param defaultValue 默认值
	 * @return 系统属性值
	 */
	private static int readInt(Context context, String key, int defaultValue) {
		try {
			return Settings.System.getInt(context.getContentResolver(), key, defaultValue);
		} catch (Throwable t) {
		}
		return defaultValue;
	}

	/**
	 * 设置系统属性
	 * @param key 对应关键字
	 * @param value 默认值
	 * @return 系统属性值
	 */
	private static boolean writeInt(Context context, String key, int value) {
		try {
			return Settings.System.putInt(context.getContentResolver(), key, value);
		} catch (Throwable t) {
		}
		return false;
	}
	/**
	 * 判断字符串是否为空
	 * @param s 字符串
	 * @return true 为空；false 不为空
	 */
	private static boolean empty(String s) {
		return ((null == s) || (s.length() <= 0));
	}
}
