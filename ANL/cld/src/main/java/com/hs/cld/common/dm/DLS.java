package com.hs.cld.common.dm;

import android.content.Context;
import android.content.Intent;

import com.hs.cld.common.apk.ApkInstaller;
import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.MD5;
import com.hs.cld.common.utils.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 下载管理服务，支持如下接口
 * * 下载应用
 * * 下载进度条通知被点击或者删除时的处理
 * * 网络变化时的处理，WIFI重新可用后可能要继续下载此前暂停的任务
 * * 应用已安装后的处理，可能有的应用下载后需要继续使用deep link等命令启动激活
 *
 *
 */
public class DLS {
	/**
	 * 日志标签
	 */
	private final static String TAG = "DLS";

	/**
	 * 下载任务列表，以下三种情况，任务列表将被清除
	 * 1 下载完成
	 * 2 用户划掉通知栏取消任务
	 * 3 程序退出，内存清空
	 */
	private List<InnerDownloadTask> mDownloadTasks =
			Collections.synchronizedList(new ArrayList<InnerDownloadTask>());

	/**
	 * 内部静态类，用于保存静态实例对象
	 * 1）保证线程安全
	 * 2）防止DCL引起的问题
	 * 3）能够实现Lazy Loading
	 *
	 */
	private static class DLSHolder {
		/**
		 * 静态实例对象
		 */
		private static final DLS INSTANCE = new DLS();
	}

	/**
	 * 构造函数，隐藏
	 */
	private DLS() {}
	
	/**
	 * 获取TransferManager实例
	 * @return TransferManager实例对象
	 */
	public static DLS get() {
		return DLSHolder.INSTANCE;
	}
	
	/**
	 * 需要下载应用调用该接口
	 * @param context 上下文对象
	 * @param apkInfo 参数对象
	 */
	public synchronized void download(Context context, ApkInfo apkInfo, OnDownloadListener listener) {
		TrackAppInfo trackAppInfo = new TrackAppInfo(apkInfo, listener);
		downloadApk(context, trackAppInfo);
	}

	/**
	 * 获取Intent对象中的参数
	 * @param i Intent对象
	 * @param name 参数定义
	 * @param defaultValue 参数默认值
	 * @return 指定参数值
	 */
	private String getIntentString(Intent i, String name, String defaultValue) {
		if (null != i) {
			if (i.hasExtra(name)) {
				return i.getStringExtra(name);
			}
		}
		return defaultValue;
	}

	/**
	 * 下载应用并安装激活，如果该应用（下载链接）已经处于下载队列中，直接返回
	 * 否则开启下载任务
	 * @param context 上下文
	 * @param trackAppInfo 应用信息
	 * @throws Exception 异常定义
	 */
	private void downloadApk(Context context, TrackAppInfo trackAppInfo) {
		// 校验参数
		validateParameter(trackAppInfo);

		// 判断是否应在下载
		if (isApkDownloading(trackAppInfo)) {
			LOG.i(TAG, "[" + trackAppInfo.mApkInfo.mApkUrl + "] apk downloading, ignore ...");
			return;
		}

		// 构造本地存储地址
		String localApkUrl = getApkLocalUrl(context, trackAppInfo);

		// 如果下载文件存在，必须在有MD5的情况下校验MD5，否则先删除在下载
		if (new File(localApkUrl).exists()) {
			if (TextUtils.empty(trackAppInfo.mApkInfo.mApkMd5)) {
				LOG.i(TAG, "[" + trackAppInfo.mApkInfo.mApkUrl + "] apk(" + localApkUrl + ") exist, delete ...");
				safeDelete(localApkUrl);
			} else {
				if (verifyApkFile(new File(localApkUrl), trackAppInfo.mApkInfo.mApkMd5)) {
					LOG.i(TAG, "[" + trackAppInfo.mApkInfo.mApkUrl + "] apk(" + localApkUrl + ") exist, install ...");
					installAndActivate(context, trackAppInfo, localApkUrl);
					return;
				} else {
					LOG.i(TAG, "[" + trackAppInfo.mApkInfo.mApkUrl
							+ "] apk(" + localApkUrl + ") exist, but mismatch, delete ...");
					safeDelete(localApkUrl);
				}
			}
		}

		// 下载应用并安装
		onStartDownload(trackAppInfo);
		InnerDownloadTask task = new InnerDownloadTask(context, trackAppInfo, localApkUrl);
		task.start();

		// 添加到队列中
		mDownloadTasks.add(task);
	}

	/**
	 * 参数校验
	 * @param trackAppInfo 应用信息对象
	 */
	private static void validateParameter(TrackAppInfo trackAppInfo) {
		if (TextUtils.empty(trackAppInfo.mApkInfo.mApkUrl)) {
			throw new IllegalArgumentException("empty apk URL");
		}
	}

	/**
	 * 检查当前应用是否正在下载，根据下载链接检查
	 * @param trackAppInfo 应用信息对象
	 * @return true 正在下载或者处于下载列表中；false 不处于下载列表中
	 */
	private boolean isApkDownloading(TrackAppInfo trackAppInfo) {
		for (InnerDownloadTask task: mDownloadTasks) {
			if (TextUtils.equals(task.mTrackAppInfo.mApkInfo.mApkUrl, trackAppInfo.mApkInfo.mApkUrl)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 安装并激活应用，如果静默安装成功，则直接根据启动参数启动应用
	 * 如果是调用安装器安装，需要监听应用安装事件，等到安装器安装应用成功后启动应用
	 * @param context 上下文
	 * @param trackAppInfo 应用信息
	 * @return true 成功；false 失败
	 */
	private void installAndActivate(Context context, TrackAppInfo trackAppInfo, String localApkUrl) {
		boolean installed = false;

		try {
			submitTrackers(context, trackAppInfo, trackAppInfo.mApkInfo.mStartInstallTrackers, "startinstall");
			onStartInstall(trackAppInfo);
			ApkInstaller.install(context, localApkUrl, true);

			// 上报安装完成
			submitTrackers(context, trackAppInfo, trackAppInfo.mApkInfo.mInstallTrackers, "install");

			// 是否删除安装包
			safeDelete(localApkUrl);
			onInstall(trackAppInfo, true, null);
			installed = true;
		} catch (Exception e) {
			onInstall(trackAppInfo, false, e);
			LOG.e(TAG, "[" + localApkUrl + "] install failed: " + e);
		}

		// 直接激活打开
		if (installed) {
			activate(context, trackAppInfo);
		}
	}

	/**
	 * 启动（激活）应用
	 * @param context 上下文
	 * @param trackAppInfo 应用信息
	 */
	private static void activate(Context context, TrackAppInfo trackAppInfo) {
		try {
			boolean activated = ApkInstaller.open(context, trackAppInfo.mApkInfo.mPackageName,
					trackAppInfo.mApkInfo.mCmdArgs, trackAppInfo.mApkInfo.mDplnk,
					trackAppInfo.mApkInfo.mActClazzName, trackAppInfo.mApkInfo.mActAction,
					trackAppInfo.mApkInfo.mSrvClazzName, trackAppInfo.mApkInfo.mSrvAction,
					trackAppInfo.mApkInfo.mBcAction);
			if (activated) {
				LOG.i(TAG, "[" + trackAppInfo.mApkInfo.mPackageName + "] >>>>>>");
				submitTrackers(context, trackAppInfo, trackAppInfo.mApkInfo.mActiveTrackers, "activate");
				onActivate(trackAppInfo, true, null);
			} else {
				onActivate(trackAppInfo, false, new Exception("activate failed"));
			}
		} catch (Exception e) {
			onActivate(trackAppInfo, false, e);
			LOG.e(TAG, "[" + trackAppInfo.mApkInfo.mPackageName + "] >>>>>> failed: " + e);
		}
	}

	/**
	 * 校验APK文件MD5
	 * @param file 文件对象
	 * @param apkMd5 应用文件MD5
	 * @return true 文件和MD5匹配；false 不匹配
	 */
	private static boolean verifyApkFile(File file, String apkMd5) {
		String dApkMd5 = MD5.str(file);
		return TextUtils.equalsIgnoreCase(dApkMd5, apkMd5);
	}

	/**
	 * 安全删除文件
	 * @param fileUrl 文件地址
	 */
	private static void safeDelete(String fileUrl) {
		safeDelete(new File(fileUrl));
	}

	/**
	 * 安全删除文件
	 * @param file 文件对象
	 */
	private static void safeDelete(File file) {
		try {
			if (file.exists()) {
				file.delete();
			}
		} catch (Exception e) {
			// nothing to do
		}
	}

	/**
	 * 获取APK文件显示名称，从URL中截取的
	 * @return APK文件显示名称
	 */
	private static String getApkDisplayName(String url) {
		if (!TextUtils.empty(url)) {
			int st = url.lastIndexOf("/");
			int et = url.indexOf("?");

			if (et > 0) {
				if (st < et) {
					return url.substring(st + 1, et);
				}
			} else {
				return url.substring(st + 1);
			}
		}

		return url;
	}

	/**
	 * 构建APK本地路径
	 * @param trackAppInfo 应用信息
	 * @return APK本地路径
	 */
	private static String getApkLocalUrl(Context context, TrackAppInfo trackAppInfo) {
		String apkFilename = getApkFilename(trackAppInfo.mApkInfo.mPackageName,
				trackAppInfo.mApkInfo.mApkMd5, trackAppInfo.mApkInfo.mApkUrl);
		return getLocalApkUrl(context, trackAppInfo, apkFilename);
	}

	/**
	 * 获取APK文件名
	 * @param pkgName 包名
	 * @param apkMd5 应用MD5
	 * @param apkUrl 应用下载URL
	 * @return APK文件名
	 */
	private static String getApkFilename(String pkgName, String apkMd5, String apkUrl) {
		String sPkgName = (TextUtils.empty(pkgName) ? "0" : pkgName);
		//String sAppendName = (TextUtils.empty(apkMd5) ? getMD5String(apkUrl) : apkMd5);
		String sAppendName = getMD5String(apkUrl);
		return (sPkgName + "_" + sAppendName + ".apk");
	}

	/**
	 * 获取字符串MD5
	 * @param s 字符串
	 * @return 字符串MD5
	 */
	private static String getMD5String(String s) {
		try {
			return MD5.getString(s);
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 获取APK本地存储路径地址
	 * @param context 上下文
	 * @param trackAppInfo 应用信息
	 * @param apkFilename APK文件名
	 * @return APK本地存储路径地址
	 */
	private static String getLocalApkUrl(Context context, TrackAppInfo trackAppInfo, String apkFilename) {
		final String path;

		if (!TextUtils.empty(trackAppInfo.mApkInfo.mLocalPath)) {
			path = trackAppInfo.mApkInfo.mLocalPath;
		} else {
			File apkDir = context.getExternalFilesDir("apk");
			if (!apkDir.exists()) {
				apkDir = new File(context.getFilesDir(), "apk");
			}
			path = apkDir.getAbsolutePath();
		}

		return (path + File.separator + apkFilename);
	}

	/**
	 * 上报监播
	 * @param context 上下文
	 * @param trackAppInfo 应用信息
	 * @param trackerUrls 监播URL列表
	 * @param trackerType 监播类型，调用者定义
	 */
	private static void submitTrackers(Context context, TrackAppInfo trackAppInfo, String[] trackerUrls, String trackerType) {
		synchronized (trackAppInfo) {
			if (trackAppInfo.mTrackers.contains(trackerUrls)) {
				Tracker.post(context, trackerUrls, trackerType);
				trackAppInfo.mTrackers.remove(trackerUrls);
			}
		}
	}

	private static void onStartDownload(TrackAppInfo trackAppInfo) {
		try {
			if ((null != trackAppInfo) && (null != trackAppInfo.mOnDownloadListener)) {
				trackAppInfo.mOnDownloadListener.onStartDownload(trackAppInfo.mApkInfo);
			}
		} catch (Throwable t) {
			//
		}
	}

	private static void onDownload(TrackAppInfo trackAppInfo, boolean ok, Exception e) {
		try {
			if ((null != trackAppInfo) && (null != trackAppInfo.mOnDownloadListener)) {
				trackAppInfo.mOnDownloadListener.onDownload(trackAppInfo.mApkInfo, ok, e);
			}
		} catch (Throwable t) {
			//
		}
	}

	private static void onStartInstall(TrackAppInfo trackAppInfo) {
		try {
			if ((null != trackAppInfo) && (null != trackAppInfo.mOnDownloadListener)) {
				trackAppInfo.mOnDownloadListener.onStartInstall(trackAppInfo.mApkInfo);
			}
		} catch (Throwable t) {
			//
		}
	}

	private static void onInstall(TrackAppInfo trackAppInfo, boolean ok, Exception e) {
		try {
			if ((null != trackAppInfo) && (null != trackAppInfo.mOnDownloadListener)) {
				trackAppInfo.mOnDownloadListener.onInstall(trackAppInfo.mApkInfo, ok, e);
			}
		} catch (Throwable t) {
			//
		}
	}

	private static void onStartActivate(TrackAppInfo trackAppInfo) {
		try {
			if ((null != trackAppInfo) && (null != trackAppInfo.mOnDownloadListener)) {
				trackAppInfo.mOnDownloadListener.onStartActivate(trackAppInfo.mApkInfo);
			}
		} catch (Throwable t) {
			//
		}
	}

	private static void onActivate(TrackAppInfo trackAppInfo, boolean ok, Exception e) {
		try {
			if ((null != trackAppInfo) && (null != trackAppInfo.mOnDownloadListener)) {
				trackAppInfo.mOnDownloadListener.onActivate(trackAppInfo.mApkInfo, ok, e);
			}
		} catch (Throwable t) {
			//
		}
	}

	public interface OnDownloadListener {
		void onStartDownload(ApkInfo apkInfo);
		void onDownload(ApkInfo apkInfo, boolean ok, Exception e);
		void onStartInstall(ApkInfo apkInfo);
		void onInstall(ApkInfo apkInfo, boolean ok, Exception e);
		void onStartActivate(ApkInfo apkInfo);
		void onActivate(ApkInfo apkInfo, boolean ok, Exception e);
	}

	/**
	 * 下载任务对象
	 */
	private class InnerDownloadTask implements TransferManager.OnTransferListener {
		/**
		 * 上下文
		 */
		private final Context mContext;

		/**
		 * 应用信息
		 */
		private final TrackAppInfo mTrackAppInfo;

		/**
		 * 本地存储地址
		 */
		private final String mLocalApkUrl;

		/**
		 * 任务开始时间
		 */
		private final long mStartInMillis;

		/**
		 * 下载的真实URL
		 */
		private String mApkRealUrl = null;


		/**
		 * 下载任务ID
		 */
		private String mTaskId = null;

		/**
		 * 进度条
		 */
		private int mProgressMilestone = 0;  // 里程碑进度条点

		/**
		 * 构造函数
		 * @param context 上下文
		 * @param trackAppInfo 应用信息
		 * @param localApkUrl 本地存储地址
		 */
		public InnerDownloadTask(Context context, TrackAppInfo trackAppInfo, String localApkUrl) {
			this.mContext = context;
			this.mTrackAppInfo = trackAppInfo;
			this.mLocalApkUrl = localApkUrl;
			this.mStartInMillis = System.currentTimeMillis();
			this.mApkRealUrl = mTrackAppInfo.mApkInfo.mApkUrl;
		}

		public void start() {
			LOG.i(TAG, "start: pkg=" + mTrackAppInfo.mApkInfo.mPackageName
					+ ", url=" + mTrackAppInfo.mApkInfo.mApkUrl + ", local=" + mLocalApkUrl);
			mTaskId = TransferManager.get().download(mContext, mTrackAppInfo.mApkInfo.mApkUrl, mLocalApkUrl, this);
			submitTrackers(mContext, mTrackAppInfo, mTrackAppInfo.mApkInfo.mStartDownTrackers, "startdown");
		}

		public void pause() {
			LOG.i(TAG, "pause: pkg=" + mTrackAppInfo.mApkInfo.mPackageName
					+ ", url=" + mTrackAppInfo.mApkInfo.mApkUrl + ", local=" + mLocalApkUrl);
			TransferManager.get().remove(mTaskId);
		}

		public void cancel() {
			LOG.i(TAG, "cancel: pkg=" + mTrackAppInfo.mApkInfo.mPackageName
					+ ", url=" + mTrackAppInfo.mApkInfo.mApkUrl + ", local=" + mLocalApkUrl);
			TransferManager.get().remove(mTaskId);
		}

		@Override
		public void onBegin(String id) {
			LOG.i(TAG, "[ID:" + id + "] on begin ...");
			this.mTaskId = id;
		}

		@Override
		public void onRedirectUrl(String id, String url) {
			LOG.i(TAG, "[ID:" + id + "] on redirect url: " + url);
			if (!TextUtils.equals(mApkRealUrl, url)) {
				mApkRealUrl = url;
			}
		}

		@Override
		public void onTransfer(String id, int progress, long transferred, long total) {
			// 有节制的打印日志
			if ((progress - mProgressMilestone) >= 10) {
				LOG.i(TAG, "[ID:" + id + "] on transfer: " + progress + "% " + transferred + "/" + total);
				mProgressMilestone = progress;
			}
		}

		@Override
		public void onFinish(String id) {
			LOG.i(TAG, "[ID:" + id + "] on finish ...");
			// 下载完成
			onDownload(mTrackAppInfo, true, null);

			// 删除下载任务
			mDownloadTasks.remove(this);
			submitTrackers(mContext, mTrackAppInfo, mTrackAppInfo.mApkInfo.mDownTrackers, "down");

			// 校验MD5
			if (!TextUtils.empty(mTrackAppInfo.mApkInfo.mApkMd5)) {
				if (!verifyApkFile(new File(mLocalApkUrl), mTrackAppInfo.mApkInfo.mApkMd5)) {
					LOG.i(TAG, "[" + mTrackAppInfo.mApkInfo.mApkUrl
							+ "] apk(" + mLocalApkUrl + ") MD5 mismatch(" + mTrackAppInfo.mApkInfo.mApkMd5 + "), delete ...");
					safeDelete(mLocalApkUrl);
					return;
				}
			}

			// 安装激活应用
			installAndActivate(mContext, mTrackAppInfo, mLocalApkUrl);
		}

		@Override
		public void onException(String id, Exception e) {
			LOG.w(TAG, "[ID:" + id + "] on exception: " + e);
			onDownload(mTrackAppInfo, false, e);

			// 删除下载任务
			mDownloadTasks.remove(this);
		}

		@Override
		public void onCancel(String id) {
			LOG.i(TAG, "[ID:" + id + "] on cancel ...");
			onDownload(mTrackAppInfo, false, new Exception("cancel"));

			// 删除下载任务
			mDownloadTasks.remove(this);
		}

		@Override
		public void onNetworkError(String id) {
			LOG.w(TAG, "[ID:" + id + "] on network error ...");
			onDownload(mTrackAppInfo, false, new Exception("network error"));

			// 删除下载任务
			mDownloadTasks.remove(this);
		}
	}

	/**
	 * 应用信息，包含需要上报的监播列表
	 */
	private static class TrackAppInfo {
		/**
		 * 应用信息
		 */
		private final ApkInfo mApkInfo;

		/**
		 * 监听器
		 */
		private final OnDownloadListener mOnDownloadListener;

		/**
		 * 监播列表
		 */
		private final List<String[]> mTrackers;

		/**
		 * 构造方法
		 * @param apkInfo 应用信息
		 */
		public TrackAppInfo(ApkInfo apkInfo, OnDownloadListener listener) {
			this.mApkInfo = apkInfo;
			this.mOnDownloadListener = listener;
			this.mTrackers = new ArrayList<>();
			safeAddTrackers(mApkInfo.mStartDownTrackers);
			safeAddTrackers(mApkInfo.mDownTrackers);
			safeAddTrackers(mApkInfo.mStartInstallTrackers);
			safeAddTrackers(mApkInfo.mInstallTrackers);
			safeAddTrackers(mApkInfo.mActiveTrackers);
		}

		/**
		 * 构造转换监播列表
		 * @param trackers 监播列表
		 */
		private void safeAddTrackers(String[] trackers) {
			if (null != trackers) {
				mTrackers.add(trackers);
			}
		}
	}


}
