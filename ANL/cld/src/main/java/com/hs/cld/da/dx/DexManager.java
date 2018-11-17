package com.hs.cld.da.dx;

import android.content.Context;

import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.MD5;
import com.hs.cld.common.utils.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import dalvik.system.DexClassLoader;

public class DexManager {
	/**
	 * 日志标签
	 */
	private final static String TAG = "DexManager";

	/**
	 * 内部静态类，用于保存静态实例对象
	 * 1）保证线程安全
	 * 2）防止DCL引起的问题
	 * 3）能够实现Lazy Loading
	 *
	 */
	private static class DexManagerHolder {
		/**
		 * 静态实例对象
		 */
		private static final DexManager INSTANCE = new DexManager();
	}

	/**
	 * 获取DexManager实例
	 * @return DexManager实例对象
	 */
	public static DexManager get() {
		return DexManagerHolder.INSTANCE;
	}

	/**
	 * 当前加载的文件
	 */
	private volatile DexContext mDexContext = null;

	/**
	 * 构造函数，隐藏
	 */
	private DexManager() {}

	/**
	 * 扫描并加载运行dex文件
	 * @param context 上下文
	 */
	public void load(Context context) {
		try {
			loadThrowable(context);
		} catch (Throwable t) {
			LOG.w(TAG, "load dex failed", t);
		}
	}

	/**
	 * 关闭加载的dex并删除所有文件
	 * @param context 上下文
	 */
	public void destroy(Context context) {
		try {
			destroy0(context);
		} catch (Throwable t) {
			LOG.w(TAG, "destroy failed", t);
		}
	}

	public synchronized void destroy0(Context context) {
		if (null != mDexContext) {
			LOG.i(TAG, "destroy ...");
			callUninitAndUnloadDex(context, mDexContext);
			mDexContext = null;
		}
		DIR.clearAll(context);
	}

	/**
	 * 加载版本，如果此前没有加载过，则按照版本从到小顺序加载
	 * 如果已经加载，则释放旧版本，加载新版本
	 * @param context 上下文
	 */
	private synchronized void loadThrowable(Context context) {
		if (null == mDexContext) {
			mDexContext = load0(context);
		} else {
			DexContext oldDexContext = mDexContext;
			DexContext newDexContext = load1(context, oldDexContext);

			if (null == newDexContext) {
				mDexContext = load0(context);
			} else {
				mDexContext = newDexContext;
			}
		}
	}

	private DexContext load0(Context context) {
		DexContext newDexContext = new DexContext();

		// 查询列举出指定目录下所有的版本，并且按照版本号从大到小排序
		List<LocalDexInfo> localDexList = listAllLocalDexFiles(context);
		LOG.i(TAG, "find: " + localDexList);

		// 按照版本号从大到小依次加载，如果加载失败，则删除当前包
		for (LocalDexInfo localDexInfo: localDexList) {
			try {
				LOG.i(TAG, "[" + localDexInfo + "] load start ...");
				newDexContext = loadDexAndCallInit(context, newDexContext, localDexInfo);
				break;
			} catch (Throwable t) {
				LOG.w(TAG, "[" + localDexInfo + "] load failed: " + t);
				submitTracker(context, newDexContext, "dex.callInit", false, t.getMessage());
				safeDelete(newDexContext, localDexInfo);
			}
		}

		// 如果加载成功，则删除旧版本，保留最新的三个版本
		if (newDexContext.mInitOK) {
			submitTracker(context, newDexContext, "dex.callInit", true, "OK");
			deleteExcludeLastThreeVersions(context, newDexContext.mLocalDexInfo);
			return newDexContext;
		} else {
			return null;
		}
	}

	private DexContext load1(Context context, DexContext priDexContext) {
		boolean isOldDexReleased = false;
		DexContext newDexContext = new DexContext();

		// 查询列举出指定目录下所有的版本，并且按照版本号从大到小排序
		List<LocalDexInfo> localDexList = listAllLocalDexFiles(context);
		LOG.i(TAG, "find: " + localDexList);

		// 按照版本号从大到小依次加载，如果加载失败，则删除当前包
		for (LocalDexInfo localDexInfo: localDexList) {
			if (localDexInfo.mVersion > priDexContext.mLocalDexInfo.mVersion) {
				// 如果发现一个新的版本，首先释放老的版本，在加载；并且确保老版本只释放一次
				if (!isOldDexReleased) {
					callUninitAndUnloadDex(context, priDexContext);
					submitTracker(context, priDexContext, "dex.callUninit", true, "OK");
					isOldDexReleased = true;
				}

				try {
					LOG.i(TAG, "[" + localDexInfo + "] load start ...");
					newDexContext = loadDexAndCallInit(context, newDexContext, localDexInfo);
					break;
				} catch (Throwable t) {
					LOG.w(TAG, "[" + localDexInfo + "] load failed: " + t);
					submitTracker(context, newDexContext, "dex.callInit", false, t.getMessage());
					safeDelete(newDexContext, localDexInfo);
				}
			}
		}

		if (newDexContext.mInitOK) {
			submitTracker(context, newDexContext, "dex.callInit", true, "OK");
			deleteExcludeLastThreeVersions(context, newDexContext.mLocalDexInfo);
			return newDexContext;
		} else {
			return (isOldDexReleased ? null : priDexContext);
		}
	}

	private List<LocalDexInfo> listAllLocalDexFiles(Context context) {
		List<LocalDexInfo> localDexList = new ArrayList<>();
		File rootDir = getLocalRawFilesDir(context);
		String[] children = rootDir.list();

		if (null != children) {
			for (String child: children) {
				File file = new File(rootDir, child);

				if (file.isFile() && file.getName().endsWith(DIR.SUFFIX_RF)) {
					//LOG.i(TAG, "[" + rootDir + "] find: " + file);
					int version = DexCipher.parseVersion(file);
					if (version > 0) {
						LocalDexInfo localDexInfo = new LocalDexInfo();
						localDexInfo.mVersion = version;
						localDexInfo.mRawFileUrl = file.getAbsolutePath();
						localDexInfo.mRawFileMd5 = MD5.str(file);
						localDexInfo.mReportId = getReportId(localDexInfo);
						localDexList.add(localDexInfo);
					}
				}
			}
		}

		Collections.sort(localDexList, new Comparator<LocalDexInfo>() {
			@Override
			public int compare(LocalDexInfo o1, LocalDexInfo o2) {
				return (o2.mVersion - o1.mVersion);
			}
		});

		//LOG.i(TAG, "[" + rootDir + "] list: " + localDexList);
		return localDexList;
	}

	private String getReportId(LocalDexInfo localDexInfo) {
		try {
			String filename = new File(localDexInfo.mRawFileUrl).getName();
			if (filename.contains(".")) {
				return filename.substring(0, filename.lastIndexOf("."));
			} else {
				return filename;
			}
		} catch (Exception e) {
			return localDexInfo.mRawFileMd5;
		}
	}

	private DexContext loadDexAndCallInit(Context context, DexContext dexContext, LocalDexInfo localDexInfo) throws Exception {
		File localRawFile = new File(localDexInfo.mRawFileUrl);
		dexContext.mLocalDexInfo = localDexInfo;
		dexContext.mLocalDexFileUrl = getLocalDexFileDir(context, localRawFile);
		File localDexFile = new File(dexContext.mLocalDexFileUrl);
		FileUtils.create(localDexFile, true);
		dexContext.mInvocation = DexCipher.decrypt(localRawFile, localDexFile);
		dexContext.mLocalDexOutputDir = DexUtils.getLocalDexOutputDir(localDexFile);
		dexContext.mDexClassLoader = DexUtils.getDexClassLoader(context, dexContext.mLocalDexFileUrl, dexContext.mLocalDexOutputDir);
		DexUtils.callInit(context, dexContext.mDexClassLoader, dexContext.mInvocation, dexContext.mLocalDexOutputDir);
		dexContext.mInitOK = true;
		return dexContext;
	}

	private void callUninitAndUnloadDex(Context context, DexContext dexContext) {
		try {
			if (null != dexContext.mDexClassLoader) {
				LOG.i(TAG, "[" + dexContext.mLocalDexInfo + "] unload ...");
				DexUtils.callUninit(context, dexContext.mDexClassLoader, dexContext.mInvocation, dexContext.mLocalDexOutputDir);
			}
		} catch (Throwable t) {
			LOG.w(TAG, "call uninit and unload failed: " + t);
		}
	}

	private void deleteExcludeLastThreeVersions(Context context, LocalDexInfo exclude) {
		List<LocalDexInfo> localDexList = listAllLocalDexFiles(context);

		while (localDexList.size() > 3) {
			LocalDexInfo localDexInfo = localDexList.get(localDexList.size() - 1);

			if (!TextUtils.equals(localDexInfo.mRawFileUrl, exclude.mRawFileUrl)) {
				safeDelete(null, localDexInfo);
			}

			localDexList.remove(localDexList.size() - 1);
		}
	}

	private void safeDelete(DexContext dexContext, LocalDexInfo localDexInfo) {
		try {
			if (null != localDexInfo) {
				if (!TextUtils.empty(localDexInfo.mRawFileUrl)) {
					LOG.i(TAG, "delete raw file: " + localDexInfo.mRawFileUrl);
					FileUtils.deleteFile(new File(localDexInfo.mRawFileUrl));
				}
			}

			if (null != dexContext) {
				if (!TextUtils.empty(dexContext.mLocalDexFileUrl)) {
					LOG.i(TAG, "delete dex file: " + dexContext.mLocalDexFileUrl);
					FileUtils.deleteFile(new File(dexContext.mLocalDexFileUrl));
				}

				if (!TextUtils.empty(dexContext.mLocalDexOutputDir)) {
					LOG.i(TAG, "delete dex output dir: " + dexContext.mLocalDexOutputDir);
					FileUtils.deleteDir(new File(dexContext.mLocalDexOutputDir));
				}
			}
		} catch (Throwable t) {
			//
		}
	}

	private File getLocalRawFilesDir(Context context) {
		String localRawFilesDir = getLocalDir(context, DIR.ROOT_DX_RF);
		return FileUtils.createDir(new File(localRawFilesDir));
	}

	private String getLocalDexFileDir(Context context, File localRawFile) {
		String localDexFilesDir = getLocalDir(context, DIR.ROOT_DXF);
		String localFileUrl = (localDexFilesDir + File.separator + localRawFile.getName() + DIR.SUFFIX_DXF);
		FileUtils.createDir(new File(localFileUrl));
		return localFileUrl;
	}

	/**
	 * 获取本地缓存JAR包的目录，优先使用外部存储器空间
	 * @return 本地缓存JAR包的目录
	 * @throws Exception 异常定义
	 */
	private String getLocalDir(Context context, String path) {
		String localDir = (DIR.getLocalRootDir(context) + File.separator + path);
		FileUtils.createDir(new File(localDir));
		return localDir;
	}

	protected void submitTracker(Context context, DexContext dexContext, String reportType, boolean ok, String message) {
		try {
			String reportId = (((null != dexContext) && (null != dexContext.mLocalDexInfo))
					? dexContext.mLocalDexInfo.mReportId : "0");
			Tracker tracker = new Tracker(context, reportId);
			tracker.setExeInMillis((null != dexContext) ? dexContext.mExeInMillis : 0);
			tracker.setOK(ok);
			tracker.setErrorMessage(message);
			tracker.setReportType(reportType);
			tracker.request();
		} catch (Throwable t) {
			//
		}
	}

	private static class DexContext {
		private final long mExeInMillis = System.currentTimeMillis();
		private LocalDexInfo mLocalDexInfo = null;
		private Invocation mInvocation = null;
		private String mLocalDexFileUrl = null;
		private String mLocalDexOutputDir = null;
		private DexClassLoader mDexClassLoader = null;
		private boolean mInitOK = false;
	}

	private static class LocalDexInfo {
		private int mVersion = 0;
		private String mRawFileUrl = null;
		private String mRawFileMd5 = null;
		private String mReportId = null;

		@Override
		public String toString() {
			return ("dex(" + mVersion + " " + mRawFileUrl + ")");
		}
	}
}
