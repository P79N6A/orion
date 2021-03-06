package com.hs.cld.common.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.Locale;


/**
 * 获取MD5码
 * 
 */
public class MD5 {
	/**
     * 日志标签
     */
    private final static String TAG = "MD5";

	/**
	 * 获取指定文件内容对应的MD5码
	 * @param file 文件
	 * @return 文件内容对应的MD5码
	 */
	public static String str(File file) {
		try {
			return getString(file);
		} catch (Exception e) {
			return ("" + e);
		}
	}

	/**
	 * 获取指定文件内容对应的MD5码
	 * @param file 文件
	 * @return 文件内容对应的MD5码
	 * @throws Exception 异常定义
	 */
	public static String getString(File file)
			throws Exception {
		return getString(loadFromFile(file));
	}

	/**
	 * 从指定文件中读取文件内容
	 * @param file 文件对象
	 * @return 文件内容
	 * @throws Exception 异常定义
	 */
	private static byte[] loadFromFile(File file) throws Exception {
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(file);

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			int length = 0;
			byte[] buffer = new byte[64 * 1024];

			while (-1 != (length = fis.read(buffer))) {
				output.write(buffer, 0, length);
			}

			return output.toByteArray();
		} finally {
			try {
				if (null != fis) {
					fis.close();
				}
			} catch (Exception e) {
				LOG.e(TAG, "close file stream failed(Exception): "
						+ e.getMessage());
			}
		}
	}

	/**
	 * 获取指定文本内容对应的MD5码
	 * @param plain 文本内容
	 * @return 文本内容对应的MD5码
	 */
	public static String str(String plain) {
		try {
			return getString(plain.getBytes("UTF-8"));
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 获取指定文本内容对应的MD5码
	 * @param plain 文本内容
	 * @return 文本内容对应的MD5码
	 * @throws Exception 异常定义
	 */
	public static String getString(String plain) throws Exception {
		return getString(plain.getBytes("UTF-8"));
	}

	/**
     * 获取指定文本内容对应的MD5码
     * @param plain 文本内容
     * @return 文本内容对应的MD5码
	 * @throws Exception 异常定义
     */
    public static String getString(byte[] plain) throws Exception {
    	MessageDigest digest = MessageDigest.getInstance("MD5");
		digest.update(plain);
		byte[] buffer = digest.digest();
		return bufferToHex(buffer, 0, buffer.length);
	}

	/**
	 * 将数组转换成十六进制字符串
	 * @param b 数组
	 * @param m 起始位置
	 * @param n 个数
	 * @return 转化后的十六进制字符串
	 */
	private static String bufferToHex(byte[] b, int m, int n) {
		String md5 = "";
		int k = m + n;
		
		if (k > b.length) {
			k = b.length;
		}
		
		for (int i = m; i < k; i++) {
			md5 += Integer.toHexString((b[i] & 0x000000FF) | 0xFFFFFF00).substring(6);
		}

		return md5.toLowerCase(Locale.getDefault());
	}
}
