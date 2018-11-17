package com.hs;

import android.app.Application;


public class AppCrashHandler implements Thread.UncaughtExceptionHandler {
	private final static String TAG = "AppExceptionHandler";
	private final Application mApplication;
	private Thread.UncaughtExceptionHandler mDefaultExceptionHandler = null;

	public AppCrashHandler(Application application) {
		mApplication = application;

		try {
			mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
			Thread.setDefaultUncaughtExceptionHandler(this);
		} catch (Throwable t) {
			LOG.e(TAG, "init crash handler failed: " + t);
		}
	}

	@Override
	public void uncaughtException(Thread thread, Throwable t) {
		LOG.e(TAG, "thread(" + thread + ") uncaught exception", t);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (null != mDefaultExceptionHandler) {
			mDefaultExceptionHandler.uncaughtException(thread, t);
		}
	}
}
