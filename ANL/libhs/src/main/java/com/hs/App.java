package com.hs;

import android.app.Application;
import android.content.Context;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 自定义App对象
 */
public class App extends Application {
	/**
	 * 日志标签
	 */
	private final static String TAG = "App";

	/**
	 * 工作线程
	 */
	private final static ScheduledExecutorService SCHEDULER =
			Executors.newSingleThreadScheduledExecutor();

	/**
	 * lib加载
	 */
	static {
		System.loadLibrary("anl");
	}

	/**
	 * 异常处理对象
	 */
	private AppCrashHandler mAppCrashHandler = null;

	@Override
	public void onCreate() {
		super.onCreate();

		try {
			mAppCrashHandler = new AppCrashHandler(this);
			init(this);
		} catch (Throwable t) {
			LOG.w(TAG, "init failed: " + t);
		}
	}

	/**
	 * 初始化，默认延迟10秒
	 * @param context Application对象
	 */
	public static void init(final Context context) {
		init(context, 5);
	}

	/**
	 * 初始化，支持延迟多少秒后初始化
	 * @param context 应用上下文
	 * @param delays 延迟时间，单位秒
	 */
	public static void init(final Context context, final long delays) {
		SCHEDULER.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					long st = System.currentTimeMillis();
					nativeInit();
					LOG.i(TAG, "init done, ms=" + (System.currentTimeMillis() - st));
				} catch (Throwable t) {
					LOG.w(TAG, "init failed: " + t);
				}
			}
		}, delays, TimeUnit.SECONDS);
	}

	public static String getChannel() {
		return nativeChannel();
	}

	public static native void nativeInit();
	public static native String nativeChannel();
}
