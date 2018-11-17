package com.hs.cld.common.apk;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import com.hs.cld.common.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class UrlOpener {
	/**
	 * 打开指定链接
	 * @param context 上下文
	 * @param url URL字符串
	 */
	public static void open(Context context, String url) {
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}

	/**
	 * 在浏览器中打开
	 * @param context 上下文
	 * @param url URL字符串
	 * @param browser 优先的浏览器
	 */
	public static void openInBrowser(Context context, String url, String browser) {
		try {
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(url));

			String defaultBrowser = getDefaultBrowser(context, browser);
			if (!TextUtils.empty(defaultBrowser)) {
				i.setPackage(defaultBrowser);
			}

			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(i);
		} catch (Exception e) {
			openInBrowser(context, url);
		}
	}

	private static void openInBrowser(Context context, String url) {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}

	private static String getDefaultBrowser(Context context, String browser) {
		List<String> packages = getInstalledBrowserPackages(context);
		if (packages.size() > 0) {
			if (!TextUtils.empty(browser)) {
				if (packages.contains(browser)) {
					return browser;
				}
			}

			if (packages.contains("com.android.browser")) {
				return "com.android.browser";
			}

			return packages.get(0);
		} else {
			return null;
		}
	}

	private static List<String> getInstalledBrowserPackages(Context context) {
		List<String> packages = new ArrayList<>();

		try {
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.addCategory(Intent.CATEGORY_BROWSABLE);
			i.setDataAndType(Uri.parse("http://"), null);

			// 找出手机当前安装的所有浏览器程序
			List<ResolveInfo> resolveInfos = context.getPackageManager()
					.queryIntentActivities(i, PackageManager.GET_INTENT_FILTERS);

			if (null != resolveInfos) {
				for (ResolveInfo resolveInfo: resolveInfos) {
					if ((null != resolveInfo.activityInfo) && (null != resolveInfo.activityInfo.applicationInfo)
							&& (!resolveInfo.activityInfo.applicationInfo.enabled)) {
						// 禁用
					} else {
						if (!TextUtils.empty(resolveInfo.activityInfo.packageName)) {
							packages.add(resolveInfo.activityInfo.packageName);
						}
					}
				}
			}
		} catch (Throwable t) {
			//
		}

		return packages;
	}
}
