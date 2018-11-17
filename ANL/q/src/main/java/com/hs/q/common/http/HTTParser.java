package com.hs.q.common.http;

import android.util.Base64;

import com.hs.q.common.utils.AES;
import com.hs.q.common.utils.ZipUtils;

public class HTTParser {
	/**
	 * 解析密文，先Base64解码、再解压、再解密
	 * @param b64 密文
	 * @param key 加密固定KEY
	 * @param salt 加密盐码
	 * @return JSON字符串
	 */
	public static String parse(String b64, String key, String salt) {
		try {
			byte[] zData = Base64.decode(b64.getBytes("UTF-8"), Base64.NO_WRAP);
			byte[] cipher = ZipUtils.ungz(zData);
			byte[] plain = AES.decrypt(cipher, key, salt);
			return new String(plain, "UTF-8");
		} catch (Exception e) {
		}
		return "";
	}
}
