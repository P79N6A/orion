package com.hs.cld.da.dx;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * DSA签名工具
 *
 */
public class DSAUtils {
	/**
	 * 定义加密算法
	 */
	public final static String ALGORITHM = "DSA";
	
	/**
	 * 定义KEY大小
	 */
	private final static int KS1024 = 1024;
	
	/**
	 * 默认种子
	 * "This's seed for generating 360OS/DSA key pair on 2016/08/11"
	 */
	private static final byte[] SEED = new byte[] {
		'T', 'h', 'i', 's', '\'', 's', ' ', 's', 'e', 'e', 'd', ' ', 'f', 'o', 'r',
		' ', 'g', 'e', 'n', 'e', 'r', 'a', 't', 'i', 'n', 'g', ' ', '3', '6', '0',
		'O', 'S', '/', 'D', 'S', 'A', ' ', 'k', 'e', 'y', ' ', 'p', 'a', 'i', 'r',
		' ', 'o', 'n', ' ', '2', '0', '1', '6', '/', '0', '8', '/', '1', '1'
	};
	
	/**
	 * 生成一对DSA密钥
	 * @return DSA密钥对
	 * @throws NoSuchAlgorithmException 异常定义
	 */
	public static KeyPair generate() throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
		SecureRandom random = new SecureRandom();
		random.setSeed(SEED);
		keyPairGenerator.initialize(KS1024, random);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		return keyPair;
	}
	
	/**
	 * 将数组类型的KEY转换为字符串
	 * @param keyEncoded 编码后的KEY
	 * @return 字符串格式
	 * @throws UnsupportedEncodingException 异常定义
	 */
	public static String getKeyString(byte[] keyEncoded) throws UnsupportedEncodingException {
		byte[] keyBase64 = Base64.encode(keyEncoded, Base64.NO_WRAP);
		return new String(keyBase64, "UTF-8");
	}
	
	/**
	 * 将字符串格式KEY转换为数组类型
	 * @param keyBase64 字符串格式的KEY，Base64编码
	 * @return 数组格式KEY
	 * @throws UnsupportedEncodingException 异常定义
	 */
	public static byte[] getKeyEncoded(String keyBase64) throws UnsupportedEncodingException {
		byte[] keyEncoded = Base64.decode(keyBase64.getBytes("UTF-8"), Base64.NO_WRAP);
		return keyEncoded;
	}
	
	/**
	 * 根据编码KEY生成私钥
	 * @param privateKeyEncoded 数组类型的编码私钥KEY
	 * @return 私钥对象
	 * @throws InvalidKeySpecException 异常定义
	 * @throws NoSuchAlgorithmException 异常定义
	 */
	public static PrivateKey getPrivateKey(byte[] privateKeyEncoded)
	        throws InvalidKeySpecException, NoSuchAlgorithmException {
		// 构造签名用的私钥对象
		PKCS8EncodedKeySpec pKCS8 = new PKCS8EncodedKeySpec(privateKeyEncoded);
		KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
		return keyFactory.generatePrivate(pKCS8);
	}
	
	/**
	 * 根据编码KEY生成公钥对象
	 * @param publicKeyEncoded 数组类型的编码公钥KEY
	 * @return 公钥对象
	 * @throws NoSuchAlgorithmException 异常定义
	 * @throws InvalidKeySpecException 异常定义
	 */
	public static PublicKey getPublicKey(byte[] publicKeyEncoded)
	        throws NoSuchAlgorithmException, InvalidKeySpecException {
		// 构造校验签名用的公钥对象
		X509EncodedKeySpec x509 = new X509EncodedKeySpec(publicKeyEncoded);
		KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
		return keyFactory.generatePublic(x509);
	}
	
	/**
	 * 使用DSA算法对一段文本进行签名，生成签名文本
	 * @param plain 数组类型的普通文本
	 * @param privateKey 签名私钥
	 * @return 签名文本，以数组的形式
	 * @throws InvalidKeyException 异常定义
	 * @throws NoSuchAlgorithmException 异常定义
	 * @throws SignatureException 异常定义
	 */
	public static byte[] sign(byte[] plain, PrivateKey privateKey)
	        throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature signer = Signature.getInstance(ALGORITHM);
		signer.initSign(privateKey);
		signer.update(plain);
		return signer.sign();
	}
	
	/**
	 * 使用DSA算法对一段文本进行签名，生成签名文本
	 * @param plain 数组类型的普通文本
	 * @param privateKeyEncoded 签名私钥
	 * @return 签名文本，以数组的形式
	 * @throws InvalidKeyException 异常定义
	 * @throws NoSuchAlgorithmException 异常定义
	 * @throws SignatureException 异常定义
	 * @throws InvalidKeySpecException 异常定义
	 */
	public static byte[] sign(byte[] plain, byte[] privateKeyEncoded)
	        throws NoSuchAlgorithmException, InvalidKeyException,
	               SignatureException, InvalidKeySpecException {
		return sign(plain, getPrivateKey(privateKeyEncoded));
	}
	
	/**
	 * 使用DSA算法对一段文本进行签名，生成签名文本
	 * @param plain 数组类型的普通文本
	 * @param privateKeyBase64 签名私钥，Base64编码格式
	 * @return 签名文本，以数组的形式
	 * @throws InvalidKeyException 异常定义
	 * @throws NoSuchAlgorithmException 异常定义
	 * @throws SignatureException 异常定义
	 * @throws InvalidKeySpecException 异常定义
	 * @throws UnsupportedEncodingException 异常定义
	 */
	public static byte[] sign(byte[] plain, String privateKeyBase64)
	        throws NoSuchAlgorithmException, InvalidKeyException,
	               SignatureException, InvalidKeySpecException,
	               UnsupportedEncodingException {
		return sign(plain, getPrivateKey(getKeyEncoded(privateKeyBase64)));
	}
	
	/**
	 * 使用DSA算法对一段文本进行签名校验
	 * @param buffer 数据缓存区
	 * @param offset 数据偏移量
	 * @param length 数据长度
	 * @param sign 给定签名
	 * @param publicKey 校验签名公钥
	 * @return true 匹配；false 不匹配
	 * @throws NoSuchAlgorithmException 异常定义
	 * @throws InvalidKeyException 异常定义
	 * @throws SignatureException 异常定义
	 */
	public static boolean verify(byte[] buffer, int offset, int length, byte[] sign, PublicKey publicKey)
	        throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature signer = Signature.getInstance(ALGORITHM);
		signer.initVerify(publicKey);
		signer.update(buffer, offset, length);
		return signer.verify(sign);
	}
	
	/**
	 * 使用DSA算法对一段文本进行签名校验
	 * @param buffer 数据缓存区
	 * @param offset 数据偏移量
	 * @param length 数据长度
	 * @param sign 给定签名
	 * @param publicKeyEncoded 校验签名公钥
	 * @return true 匹配；false 不匹配
	 * @throws NoSuchAlgorithmException 异常定义
	 * @throws InvalidKeyException 异常定义
	 * @throws SignatureException 异常定义
	 * @throws InvalidKeySpecException 异常定义
	 */
	public static boolean verify(byte[] buffer, int offset, int length, byte[] sign, byte[] publicKeyEncoded)
	        throws NoSuchAlgorithmException, InvalidKeyException,
	               SignatureException, InvalidKeySpecException {
		return verify(buffer, offset, length, sign, getPublicKey(publicKeyEncoded));
	}
	
	/**
	 * 使用DSA算法对一段文本进行签名校验
	 * @param buffer 数据缓存区
	 * @param offset 数据偏移量
	 * @param length 数据长度
	 * @param sign 给定签名
	 * @param publicKeyBase64 校验签名公钥，Base64编码格式
	 * @return true 匹配；false 不匹配
	 * @throws NoSuchAlgorithmException 异常定义
	 * @throws InvalidKeyException 异常定义
	 * @throws SignatureException 异常定义
	 * @throws InvalidKeySpecException 异常定义
	 * @throws UnsupportedEncodingException 异常定义
	 */
	public static boolean verify(byte[] buffer, int offset, int length, byte[] sign, String publicKeyBase64)
	        throws NoSuchAlgorithmException, InvalidKeyException,
	               SignatureException, InvalidKeySpecException,
	               UnsupportedEncodingException {
		return verify(buffer, offset, length, sign, getPublicKey(getKeyEncoded(publicKeyBase64)));
	}
}
