package com.hs.cld.da.dx;

import com.hs.cld.common.utils.LOG;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

public class DexCipher {
	private final static String TAG = "DexCipher";

	/**
	 * 最大版本号
	 */
	private final static int MAX_VERSION = 200000;
	
	private final static String AES_KEY = "e664c7cb4f8fd84f8033b71c25cfd084";
	
	public final static String DSA_PUBLICKKEY =
		"MIIBuDCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9Kn" +
		"C7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MP" +
		"bLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv" +
		"4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iID" +
		"GZ3RSAHHAhUAl2BQjxUjC8yykrmCouuEC/BYHPUCgYEA9+Ggh" +
		"dabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+" +
		"ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiu" +
		"zpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKL" +
		"Zl6Ae1UlZAFMO/7PSSoDgYUAAoGBAIMnSPEeii7YHCLoakU0h" +
		"DP6znyYKEyEd330NaCBmb6HHn+ygjKFxrClFUoGH4B23wBy5E" +
		"Jo0iq6CvKatSqSt65iIs3us3iDkBzJROq1X5CW0bHozqnYvM/" +
		"TJaDxzg6NJoLydxLDPrNX7AvtH7QWOS1sUX3CYg4E4zeogbf0" +
		"an/q";
	
	public static int parseVersion(File file) {
		try {
			byte[] buffer = FileUtils.read(file);
			CipherBean cipherBean = readCipherBean(buffer);
			return cipherBean.mVersion;
		} catch (Exception e) {
			LOG.w(TAG, "parse " + file + " version failed: " + e);
			return -1;
		}
	}
	
	/**
	 * 解密
	 * @throws Exception 异常定义
	 */
	public static Invocation decrypt(File srcFile, File destFile) throws Exception {
		byte[] buffer = FileUtils.read(srcFile);
		return decrypt(buffer, destFile);
	}

	/**
	 * 解密
	 * @throws Exception 异常定义
	 */
	public static Invocation decrypt(byte[] cipher, File destFile) throws Exception {
		Invocation invocation = new Invocation();
		CipherBean cipherBean = readCipherBean(cipher);
		byte[] invocationBytes = AES.decrypt(cipherBean.mInvocationCipher, 0, cipherBean.mInvocationCipher.length,
				AES_KEY, cipherBean.mSalt);
		parseInvocation(new String(invocationBytes, "UTF-8"), invocation);
		byte[] dexBytes = AES.decrypt(cipherBean.mDexCipher, 0, cipherBean.mDexCipher.length,
				AES_KEY, cipherBean.mSalt);
		FileUtils.write(destFile, dexBytes);
		invocation.mLocalDexFile = destFile;
		return invocation;
	}

	private static void parseInvocation(String json, Invocation invocation) throws JSONException {
		JSONObject jo = new JSONObject(json);
		if (jo.has("cn")) {
			invocation.mClassName = jo.getString("cn");
		}
		if (jo.has("m_init")) {
			invocation.mInitMethod = jo.getString("m_init");
		}
		if (jo.has("m_uninit")) {
			invocation.mUninitMethod = jo.getString("m_uninit");
		}
	}
	
	private static CipherBean readCipherBean(byte[] buffer) throws Exception {
		// 读取4个字节版本号
		int offset = 0;
		int version = readInt(buffer, 0);
		offset += 4;
		if (version > MAX_VERSION) {
			throw new Exception("version(" + version + ") unsupported");
		}
		
		// 过滤4096字节随机数据
		offset += 4096;
		
		// 读取32字节盐码
		String salt = readString(buffer, offset, 32);
		offset += 32;
		
		// 读取调用参数数据长度和调用参数数据
		byte[] invocationCipherBytes = readIntBytes(buffer, offset);
		offset += (4 + invocationCipherBytes.length);
		
		// 读取dex数据体长度和dex数据
		byte[] dexCipherBytes = readIntBytes(buffer, offset);
		offset += (4 + dexCipherBytes.length);
		int signDataLength = offset;
		
		// 读取签名数据长度和签名数据
		byte[] signBytes = readIntBytes(buffer, offset);

		// 对签名之前的文件数据（版本号、随机数据、盐码和有效数据），校验签名
		verifySignature(buffer, signDataLength, signBytes);
		return new CipherBean(version, salt, invocationCipherBytes, dexCipherBytes);
	}

	private static int readInt(byte[] data, int offset) throws Exception {
		if (data.length < (offset + 4)) {
			throw new Exception("illegal data size");
		}

		byte[] buffer = new byte[4];
		System.arraycopy(data, offset, buffer, 0, 4);
		return NumUtils.bytes2int(buffer);
	}

	private static String readString(byte[] data, int offset, int length) throws Exception {
		if (data.length < (offset + length)) {
			throw new Exception("illegal data size");
		}

		byte[] buffer = new byte[length];
		System.arraycopy(data, offset, buffer, 0, length);
		return new String(buffer, "UTF-8");
	}

	private static byte[] readIntBytes(byte[] data, int offset) throws Exception {
		if (data.length < (offset + 4)) {
			throw new Exception("illegal data size");
		}

		byte[] intBytes = new byte[4];
		System.arraycopy(data, offset, intBytes, 0, 4);
		int bufferSize = NumUtils.bytes2int(intBytes);

		if (data.length < (offset + 4 + bufferSize)) {
			throw new Exception("illegal data size");
		}

		if (bufferSize > (10 * 1024 * 1024)) {
			throw new Exception("data too large");
		}

		byte[] buffer = new byte[bufferSize];
		System.arraycopy(data, (offset + 4), buffer, 0, bufferSize);
		return buffer;
	}
	
	private static void verifySignature(byte[] data, int length, byte[] signBytes) throws Exception {
		if (!DSAUtils.verify(data, 0, length, signBytes, DSA_PUBLICKKEY)) {
			throw new Exception("signature verify failed");
		}
	}
	
	private static class CipherBean {
		private final int mVersion;
		private final String mSalt;
		private final byte[] mInvocationCipher;
		private final byte[] mDexCipher;
		
		public CipherBean(int version, String salt, byte[] invocationCipher, byte[] dexCipher) {
			this.mVersion = version;
			this.mSalt = salt;
			this.mInvocationCipher = invocationCipher;
			this.mDexCipher = dexCipher;
		}
	}
}
