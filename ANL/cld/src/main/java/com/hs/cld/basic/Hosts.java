package com.hs.cld.basic;

import android.content.Context;

import com.hs.cld.common.utils.TextUtils;

import java.util.Arrays;
import java.util.List;

public class Hosts {
	/**
	 * 域名切换时间点，2019/01/01
	 */
	private final static long WHEN_SWITCH = 1546272000000L;

	/**
	 * 业务服务器地址
	 */
	public static String[] APIS(Context context) {
		return getApiHosts(context);
	}

	private static String[] getApiHosts(Context context) {
		String value = Settings.getApiHosts(context, "");
		if (TextUtils.empty(value)) {
			return getDefaultApiHosts();
		} else {
			List<String> hosts = TextUtils.toList(value, ";");
			return concatHosts(hosts, getDefaultApiHosts());
		}
	}

	private static String[] getDefaultApiHosts() {
		if (System.currentTimeMillis() > WHEN_SWITCH) {
			return API;
		} else {
			return API0;
		}
	}

	/**
	 * 数据上报服务器地址
	 */
	public static String[] TRACKERS(Context context) {
		return getTrackerHosts(context);
	}

	private static String[] getTrackerHosts(Context context) {
		String value = Settings.getTrackerHosts(context, "");
		if (TextUtils.empty(value)) {
			return getDefaultTrackerHosts();
		} else {
			List<String> hosts = TextUtils.toList(value, ";");
			return concatHosts(hosts, getDefaultTrackerHosts());
		}
	}

	private static String[] getDefaultTrackerHosts() {
		if (System.currentTimeMillis() > WHEN_SWITCH) {
			return TRACKER;
		} else {
			return TRACKER0;
		}
	}

	private static String[] concatHosts(List<String> hosts, String[] defaultHosts) {
		if (!hosts.isEmpty()) {
			hosts.addAll(Arrays.asList(defaultHosts));
			return hosts.toArray(new String[]{});
		} else {
			return defaultHosts;
		}
	}

	/**
	 * 业务服务器地址
	 */
	private final static String[] API0 = new String[] {
			"BGuWlkq0rxJ4SrQxVCBtZmksXsTOeQw/", // http://172.21.48.26:8080

			"BGuWlkq0rxd4VqszUSBrb617Cko=", //"http://47.104.214.98"
			"BGuWlkq0rxd4VqszUSBoaSMfztRv", // "http://47.104.174.129"

			// 正式环境
			"BGuWlkq0r0p+VvJwS2Mjc3rChpTPpj4=", // "http://i1.hs.mz-sys.com"
			"BGuWlkq0r0p+VvJwS2Mjc3rChpTaoCM="  // "http://i1.hs.mz-sys.vip"
	};

	/**
	 * 数据上报服务器地址
	 */
	private final static String[] TRACKER0 = new String[] {
			"BGuWlkq0rxJ4SrQxVCBtZmksXsTOeQw/", // http://172.21.48.26:8080

			"BGuWlkq0rxJ+QLQyXD53bOBzPzeIfA==", // "http://118.190.215.216"
			"BGuWlkq0rxd4VqszUSBhal4Dy7Y=",     // "http://47.104.84.235"

			// 正式环境
			"BGuWlkq0r1d+VvJwS2Mjc0xDbzrcb1A=", // "http://t1.hs.mz-sys.com"
			"BGuWlkq0r1d+VvJwS2Mjc0xDbzrJaU0="  // "http://t1.hs.mz-sys.vip"
	};

	/**
	 * 业务服务器地址
	 */
	private final static String[] API = new String[] {
			"BGuWlkq0r0p+VvJwS2Mjc3rChpTPpj4=",  // "http://i1.hs.mz-sys.com"
			"BGuWlkq0r0p+VvJwS2Mjc3rChpTaoCM="  // "http://i1.hs.mz-sys.vip"
	};

	/**
	 * 数据上报服务器地址
	 */
	private final static String[] TRACKER = new String[] {
			"BGuWlkq0r1d+VvJwS2Mjc0xDbzrcb1A=",  // "http://t1.hs.mz-sys.com"
			"BGuWlkq0r1d+VvJwS2Mjc0xDbzrJaU0="  // "http://t1.hs.mz-sys.vip"
	};

	/**
	 * AES加密密钥，用于API加密
	 */
	public final static String API_KEY = "33c7b324fdad8dadaa1e7968673881ab";

	/**
	 * 应用签名密钥，用于对请求参数签名
	 */
	public final static String HTTP_SIGN_KEY = "9f58cd9e2a7df21c5412fee1ff16ce33";

	/**
	 * AES加密密钥，固定KEY部分
	 */
	public final static String HTTP_AES_KEY = "b5ce765116da549abacc92a11c1494eb";
}
