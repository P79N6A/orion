package com.hs.cld.common.http;

import android.content.Context;
import android.util.Base64;

import com.hs.cld.common.utils.AES;
import com.hs.cld.common.utils.Device;
import com.hs.cld.common.utils.MD5;
import com.hs.cld.common.utils.SystemUtils;
import com.hs.cld.common.utils.TextUtils;
import com.hs.cld.common.utils.ZipUtils;

public class HTTPBuilder {
	/**
	 * 常用的参数定义
	 */
	public final static String P_M = "m";              // 型号+IMEI的MD5
	public final static String P_N = "n";              // 网络类型
	public final static String P_CHANNEL = "ch";       // 渠道号

	/**
	 * 构造HTTP请求对象
	 * @param context 上下文
	 * @return HTTP请求对象
	 */
	public static HTTPHelper build(Context context) {
		HTTPHelper helper = HTTPHelper.get(context);
		helper.setUserAgent(SystemUtils.getUserAgent(context));
		return helper;
	}

	/**
	 * 构造HTTP请求对象
	 * @param context 上下文
	 * @param hosts 请求地址列表
	 * @param path 请求路径
	 * @return HTTP请求对象
	 */
	public static HTTPHelper build(Context context, String[] hosts, String path) {
		HTTPHelper helper = build(context);

		// 设置HTTP请求方法等基本变量
		helper.setHosts(hosts);
		helper.setPath(path);

		return helper;
	}

	/**
	 * 追加常用的参数
	 * @param context 上下文
	 * @param helper HTTP请求对象
	 * @param channel 渠道号
	 */
	public static void appendCommonParameters(Context context, HTTPHelper helper, String channel) {
		appendCommonParameters(context, helper);
		appendQueryParameter(helper, P_CHANNEL, channel);
	}

	/**
	 * 追加常用的参数
	 * @param context 上下文
	 * @param helper HTTP请求对象
	 */
	public static void appendCommonParameters(Context context, HTTPHelper helper) {
		appendQueryParameter(helper, P_M, getM(context));
		appendQueryParameter(helper, P_N, SystemUtils.getNetworkInfo(context));
	}

	private static String getM(Context context) {
		String model = Device.getModel();
		String IEMI = Device.getIMEI(context);
		return MD5.str(notNull(model) + notNull(IEMI));
	}

	private static String notNull(String s) {
		return ((null == s) ? "" : s);
	}

	/**
	 * 追加请求参数
	 * @param helper HTTP请求对象
	 * @param key 键值定义
	 * @param value 键值
	 * @return HTTP请求对象
	 */
	public static HTTPHelper appendQueryParameter(HTTPHelper helper, String key, String value) {
		if (!TextUtils.empty(value)) {
			helper.putQueryParameter(key, value);
		}

		return helper;
	}

	/**
	 * 构造P参数密文，先加密、再压缩、再Base64编码
	 * @param json JSON字符串
	 * @param key 加密固定KEY
	 * @param salt 加密盐码
	 * @return 密文参数
	 */
	public static String buildCipherBody(String json, String key, String salt) {
		try {
			byte[] cipher = AES.encrypt(json, key, salt);
			byte[] zData = ZipUtils.gz(cipher);
			return new String(Base64.encode(zData, Base64.NO_WRAP), "UTF-8");
		} catch (Exception e) {
		}
		return "";
	}
}
