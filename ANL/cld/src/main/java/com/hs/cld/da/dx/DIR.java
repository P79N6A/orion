package com.hs.cld.da.dx;

import android.content.Context;

import com.hs.cld.common.PROP;

import java.io.File;

public class DIR {
	/**
	 * 根目录
	 */
	public final static String ROOT_JE_RF = (PROP.isExpDir() ? "/je/rf" : "/.je/.rf");

	/**
	 * 根目录
	 */
	public final static String ROOT_DX_RF = (PROP.isExpDir() ? "/dx/rf" : "/.dx/.rf");

	/**
	 * 根目录
	 */
	public final static String ROOT_DXF = (PROP.isExpDir() ? "/dxf" : "/.dxf");

	/**
	 * 后缀名
	 */
	public final static String SUFFIX_RF = ".rf";

	/**
	 * 后缀名
	 */
	public final static String SUFFIX_DXF = ".dxf";

	/**
	 * 获取应用根目录
	 * @param context 上下文
	 * @return 应用根目录
	 */
	public static String getLocalRootDir(Context context) {
		if (PROP.isExpDir()) {
			return getExtRootDir(context);
		} else {
			return getDataRootDir(context);
		}
	}

	private static String getDataRootDir(Context context) {
		return (context.getFilesDir().getAbsolutePath() + File.separator + ".hs");
	}

	private static String getExtRootDir(Context context) {
		return (context.getExternalFilesDir(null).getAbsolutePath() + File.separator + "hs");
	}

	public static void clearAll(Context context) {
		try {
			FileUtils.deleteDir(new File(getDataRootDir(context)));
		} catch (Throwable t) {
			//
		}

		try {
			FileUtils.deleteDir(new File(getExtRootDir(context)));
		} catch (Throwable t) {
			//
		}
	}
}
