package com.hs.cld.da;

import android.content.Context;

import com.hs.cld.da.dx.DIR;
import com.hs.cld.da.dx.FileUtils;
import com.hs.cld.common.utils.LOG;

import java.io.File;

public class DexExe extends JarExe {
	/**
	 * 日志标签
	 */
	private final static String TAG = "DE";

	/**
	 * 构造函数
	 * @param context 应用上下文
	 * @param message 消息
	 */
	public DexExe(Context context, Message message) {
		super(context, message);
	}

	@Override
	public void fire() {
		try {
			handleDexInfo();
		} catch (Throwable t) {
			LOG.e(TAG, "[" + mMessage + "] je failed: " + t);
		}
	}

	/**
	 * 下载dex文件，并重新加载dex
	 * @throws Exception 异常定义
	 */
	private void handleDexInfo() throws Exception {
		try {
			downloadToLocalFile();
			submitTracker("dex.download", true, "OK");
		} catch (Exception e) {
			submitTracker("dex.download", false, ("" + e));
			throw e;
		}
	}

	@Override
	protected File getLocalRawFile() throws Exception {
		String localRawFileDir = getLocalDir(DIR.ROOT_DX_RF);
		String localRawFileUrl = (localRawFileDir + File.separator + getFilename() + DIR.SUFFIX_RF);
		return FileUtils.create(new File(localRawFileUrl), true);
	}
}
