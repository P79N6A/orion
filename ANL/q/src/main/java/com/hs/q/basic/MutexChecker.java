package com.hs.q.basic;

import android.content.Context;

import com.hs.q.common.utils.LOG;
import com.hs.q.common.utils.SystemUtils;

import java.util.HashSet;
import java.util.List;

public class MutexChecker {
	/**
	 * 日志标签
	 */
	private final static String TAG = "MC";

	/**
	 * 检查系统中是否安装指定的软件
	 * @param context 上下文
	 * @return 系统包含指定的安全软件列表其中之一
	 */
	public static boolean has(Context context) {
		List<String> mutexPkgs = Settings.getMutexPackages(context);

		if ((null != mutexPkgs) && (mutexPkgs.size() > 0)) {
			List<String> pkgs = SystemUtils.getEnabledInstallApps(context);
			HashSet<String> installed = new HashSet<>(pkgs);

			for (String mutexPkg: mutexPkgs) {
				if (installed.contains(mutexPkg)) {
					LOG.i(TAG, "[" + mutexPkg + "] mutex pkg exist ...");
					return true;
				}
			}
		}

		return false;
	}
}
