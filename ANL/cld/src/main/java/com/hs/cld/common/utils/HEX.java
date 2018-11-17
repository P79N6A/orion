package com.hs.cld.common.utils;

import java.util.Locale;

/**
 * 自定义加密工具
 *
 */
public class HEX {
	/**
	 * 将字符串转换成十六进制字符串
	 * @param s 字符串
	 * @return 转化后的十六进制字符串
	 */
	public static String getCipher(String s) {
		return getCipher(s, '0');
	}

	/**
	 * 将字符串转换成十六进制字符串
	 * @param s 字符串
	 * @return 转化后的十六进制字符串
	 */
	public static String getCipher(String s, char salt) {
		try {
			return getCipher(s.getBytes("UTF-8"), salt);
		} catch (Throwable t) {
			return "";
		}
	}

	/**
	 * 将数组转换成十六进制字符串
	 * @param b 数组
	 * @return 转化后的十六进制字符串
	 */
	public static String getCipher(byte[] b, char salt) {
		if ((null != b) && (b.length > 0)) {
			String hex = "";

			for (int i = 0; i < b.length; i++) {
				int bs = (b[i] + salt);
				hex += Integer.toHexString((bs & 0x000000FF) | 0xFFFFFF00).substring(6);
			}

			return hex.toLowerCase(Locale.getDefault());
		} else {
			return "";
		}
	}

	/**
	 * 将十六进制字符串解密成明文字符串
	 * @param cipher 密文
	 * @return 字符串
	 */
	public static String getString(String cipher) {
		return getString(cipher, '0');
	}

	/**
	 * 将十六进制字符串解密成明文字符串
	 * @param cipher 密文
	 * @param salt 盐码
	 * @return 明文字符串
	 */
	public static String getString(String cipher, char salt) {
		try {
			return new String(getByteArray(cipher, salt), "UTF-8");
		} catch (Throwable t) {
			return "";
		}
	}

	/**
	 * 将十六进制字符串数组解密成明文字符串
	 * @param cipher 密文
	 * @param salt 盐码
	 * @return 明文字符串
	 */
	public static byte[] getByteArray(String cipher, char salt) {
		if (null == cipher) {
			return new byte[] {};
		}

		String lower = cipher.toLowerCase(Locale.getDefault());
		int length = lower.length();

		if ((length <= 0) || (0 != (length % 2))) {
			return new byte[] {};
		} else {
			int m = 0;
			byte[] buffer = new byte[length / 2];

			for (int i = 0; i < length; i += 2) {
				int bs = Integer.valueOf(cipher.substring(i, i+2), 16);
				buffer[m] = (byte)(bs - salt);
				m++;
			}

			return buffer;
		}
	}
}
