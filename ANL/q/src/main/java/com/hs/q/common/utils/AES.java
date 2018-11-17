package com.hs.q.common.utils;

import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * AES加密工具
 *
 */
public class AES {
    /**
     * AES加密模式
     */
    private final static String MODE = "AES/CFB/NoPadding";
    
    /**
     * 向量定义
     */
    private final static byte[] IV = new byte[] {
    	'0', '1', '0', '2', '0', '3', '0', '4',
    	'0', '5', '0', '6', '0', '7', '0', '8'
	};

	public static String genSalt() {
		long t = System.currentTimeMillis();
		return Long.toHexString(t);
	}

	/**
	 * 使用AES加密，内部对KEY进行MD5编码
	 * @param plain 需要加密的内容
	 * @param key 密钥固定KEY
	 * @param salt 密钥盐码
	 * @return 加密后字符串
	 * @throws Exception 异常定义
	 */
	public static byte[] encrypt(String plain, String key, String salt)
			throws Exception {
		return encrypt(plain.getBytes("UTF-8"), key, salt);
	}

	/**
	 * 使用AES加密，内部对KEY进行MD5编码
	 * @param plain 需要加密的内容
	 * @param key 密钥固定KEY
	 * @param salt 密钥盐码
	 * @return 加密后字符串
	 * @throws Exception 异常定义
	 */
	public static byte[] encrypt(byte[] plain, String key, String salt)
			throws Exception {
		return encrypt(plain, (key + salt));
	}

	/**
	 * 使用AES加密，内部对KEY进行MD5编码
	 * @param plain 需要加密的内容
	 * @param key 加密密码
	 * @return 加密后字符串
	 * @throws Exception 异常定义
	 */
	public static byte[] encrypt(String plain, String key)
			throws Exception {
		return encrypt(plain.getBytes("UTF-8"), key);
	}
	
	/**
     * 使用AES加密，内部对KEY进行MD5编码
     * @param plain 需要加密的内容
     * @param key 加密密码
     * @return 加密后字符串
     * @throws Exception 异常定义
     */
    public static byte[] encrypt(byte[] plain, String key)
            throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] kb = md.digest(key.getBytes("UTF-8"));

		IvParameterSpec iv = new IvParameterSpec(IV);
		SecretKeySpec spec = new SecretKeySpec(kb, "AES");

		// "算法/模式/补码方式"
		Cipher cipher = Cipher.getInstance(MODE);
		cipher.init(Cipher.ENCRYPT_MODE, spec, iv);

		// 加密
		return cipher.doFinal(plain);
    }

	/**
	 * 对AES加密字符串解密
	 * @param cipherText 使用AES加密的字符串
	 * @param key 加密密码
	 * @param salt 密钥盐码
	 * @return 解密后明文
	 * @throws Exception 异常定义
	 */
	public static String decryptAsString(byte[] cipherText, String key, String salt)
			throws Exception {
		return new String(decrypt(cipherText, (key + salt)), "UTF-8");
	}

	/**
	 * 对AES加密字符串解密
	 * @param cipherText 使用AES加密的字符串
	 * @param key 加密密码
	 * @param salt 密钥盐码
	 * @return 解密后明文
	 * @throws Exception 异常定义
	 */
	public static byte[] decrypt(byte[] cipherText, String key, String salt)
			throws Exception {
		return decrypt(cipherText, (key + salt));
	}

	/**
	 * 对AES加密字符串解密
	 * @param cipherText 使用AES加密的字符串
	 * @param key 加密密码
	 * @return 解密后明文
	 * @throws Exception 异常定义
	 */
	public static String decryptAsString(byte[] cipherText, String key)
			throws Exception {
		return new String(decrypt(cipherText, key), "UTF-8");
	}
 
    /**
     * 对AES加密字符串解密
     * @param cipherText 使用AES加密的字符串
     * @param key 加密密码
     * @return 解密后明文
     * @throws Exception 异常定义
     */
    public static byte[] decrypt(byte[] cipherText, String key)
    		throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] kb = md.digest(key.getBytes("UTF-8"));

		IvParameterSpec iv = new IvParameterSpec(IV);
		SecretKeySpec spec = new SecretKeySpec(kb, "AES");

		// "算法/模式/补码方式"
		Cipher cipher = Cipher.getInstance(MODE);
		cipher.init(Cipher.DECRYPT_MODE, spec, iv);

		// 解密
		return cipher.doFinal(cipherText);
    }
}
