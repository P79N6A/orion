package com.hs.cld.da;

import android.content.Context;

import com.hs.cld.common.http.HTTPError;
import com.hs.cld.common.http.HTTPHelper;
import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.MD5;
import com.hs.cld.common.utils.SystemUtils;
import com.hs.cld.common.utils.TextUtils;
import com.hs.cld.da.dx.DIR;
import com.hs.cld.da.dx.DexCipher;
import com.hs.cld.da.dx.DexUtils;
import com.hs.cld.da.dx.FileUtils;
import com.hs.cld.da.dx.Invocation;
import com.hs.cld.da.dx.Tracker;

import java.io.File;

public class JarExe {
	/**
	 * 日志标签
	 */
	private final static String TAG = "JE";

	/**
	 * 应用上下文
	 */
	protected final Context mContext;

	/**
	 * Jar信息
	 */
	protected final Message mMessage;

	protected long mArrivedInMillis = 0;
	protected long mExeInMillis = 0;

	/**
	 * 构造函数
	 * @param context 应用上下文
	 * @param message 消息
	 */
	public JarExe(Context context, Message message) {
		this.mContext = context;
		this.mMessage = message;
		this.mArrivedInMillis = System.currentTimeMillis();
	}

	public void fire() {
		try {
			this.mExeInMillis = System.currentTimeMillis();
			handle();
		} catch (Throwable t) {
			LOG.w(TAG, "[" + mMessage + "] je failed: " + t);
		}
	}

	/**
	 * 下载dex文件
	 * @throws Exception 异常定义
	 */
	private void handle() throws Exception {
		File localRawFile = null;
		Invocation invocation = null;

		try {
			localRawFile = downloadToLocalFile();
			submitTracker("jar.download", true, "OK");
		} catch (Exception e) {
			submitTracker("jar.download", false, ("" + e));
			throw e;
		}

		try {
			File localDexFile = getLocalDexFile(localRawFile);
			invocation = DexCipher.decrypt(localRawFile, localDexFile);
			//submitTracker("jar.decrypt", true, "OK");
		} catch (Exception e) {
			submitTracker("jar.decrypt", false, ("" + e));
			throw e;
		}

		try {
			String result = DexUtils.callInit(mContext, invocation);
			submitTracker("jar.callInit", true, result);
		} catch (Exception e) {
			submitTracker("jar.callInit", false, ("" + e));
			throw e;
		}
	}

	protected File downloadToLocalFile() throws Exception {
		byte[] rawBytes = download(mMessage.mUrl);

		if (null == rawBytes) {
			throw new Exception("[" + mMessage.mUrl + "] download failed");
		} else {
			String jMd5 = MD5.getString(rawBytes);

			if (!TextUtils.equalsIgnoreCase(jMd5, mMessage.mFileMd5)) {
				throw new Exception("file MD5 mismatch");
			}

			File localFile = getLocalRawFile();
			FileUtils.write(localFile, rawBytes);
			return localFile;
		}
	}

	private byte[] download(String url) throws Exception {
		Exception lastException = null;

		for (int i = 0; i < 3; i++) {
			try {
				return RemoteFile.load(mContext, url);
			} catch (Exception e) {
				lastException = e;
				Thread.sleep(500);
			}
		}

		throw lastException;
	}

	protected File getLocalRawFile() throws Exception {
		String localRawFileDir = getLocalDir(DIR.ROOT_JE_RF);
		String localRawFileUrl = (localRawFileDir + File.separator + getFilename() + DIR.SUFFIX_RF);
		return FileUtils.create(new File(localRawFileUrl), true);
	}

	protected File getLocalDexFile(File localRawFile) throws Exception {
		String localDexFileDir = getLocalDir(DIR.ROOT_DXF);
		String localDexFileUrl = (localDexFileDir + File.separator + localRawFile.getName() + DIR.SUFFIX_DXF);
		return FileUtils.create(new File(localDexFileUrl), true);
	}

	protected String getFilename() {
		String filename = mMessage.mReportId;
		if (!TextUtils.empty(filename)) {
			return filename;
		}

		filename = mMessage.mFileMd5;
		if (!TextUtils.empty(filename)) {
			return filename;
		}

		return MD5.str(mMessage.mUrl);
	}

	/**
	 * 获取本地缓存JAR包的目录，优先使用外部存储器空间
	 * @return 本地缓存JAR包的目录
	 * @throws Exception 异常定义
	 */
	protected String getLocalDir(String path) throws Exception {
		String localDir = (DIR.getLocalRootDir(mContext) + File.separator + path);
		FileUtils.createDir(new File(localDir));
		return localDir;
	}

	protected void submitTracker(String reportType, boolean ok, String message) {
		Tracker tracker = new Tracker(mContext, mMessage.mReportId);
		tracker.setArrivedInMillis(mArrivedInMillis);
		tracker.setExeInMillis(mExeInMillis);
		tracker.setOK(ok);
		tracker.setErrorMessage(message);
		tracker.setReportType(reportType);
		tracker.request();
	}
}
