package com.hs.cld.common.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 错误码定义
 * 
 */
public class HTTPError {
    /**
     * 所有操作成功
     */
    public final static int OK = 0;

    /**
     * 未知错误，默认错误码
     */
    public final static int EUNKNOWN = 100;

    /**
     * 程序执行过程出现异常
     */
    public final static int EEXCEPTION = 101;
    
    /**
     * HTTP方法不支持
     */
    public final static int EMETHODUNSUPPORTED = 102;
    
    /**
     * 存在不合法的参数
     */
    public final static int EILLEGALPARAMETER = 103;

    /**
     * HTTP接收数据错误，比如要接收的数据没有读取完整等
     */
    public final static int EDATARECEIVED = 104;
    
    /**
     * 客户端协议错误，对应ClientProtocolException错误
     */
    public final static int ECLIENTPROTOCOL = 105;
    
    /**
     * HTTP请求时发生IO错误，对应IOException错误
     */
    public final static int EIOEXCEPTION = 106;
    
    /**
     * 域名解析错误，对应UnknownHostException错误，需要切换地址重试
     */
    public final static int EUNKNOWNHOST = 107;
    
    /**
     * ECONNRESET错误，对应IOException中的ECONNRESET错误信息，需要切换地址重试
     */
    public final static int ECONNRESET = 108;
    
    /**
     * 连接错误，如果存在网络代理，需要设置APN重试
     */
    public final static int ECONNECT = 109;
    
    /**
     * 套接字超时，如果存在网络代理，需要设置APN重试
     */
    public final static int ETIMEOUT = 110;
    
    /**
     * 数据压缩中出现错误或者异常
     */
    public final static int ECOMPRESS = 111;
    
    /**
     * 数据解压中出现错误或者异常
     */
    public final static int EDECOMPRESS = 112;
    
    /**
     * 数据加密中出现错误或者异常
     */
    public final static int EENCRYPT = 113;
    
    /**
     * 数据解密中出现错误或者异常
     */
    public final static int EDECRYPT = 114;
    
    /**
     * 断点续传时，协议错误或者解析协议失败
     */
    public final static int EBROKENPROTOCOL = 115;
    
    /**
     * 客户端回调错误导致接口行为中止，例如客户端取消或者处理数据错误等
     */
    public final static int ECLIENTCALLBACK = 116;
    
    /**
     * HTTP错误码起始偏移量，例如HTTP错误码404时返回1404
     */
    public final static int EHTTPSTATUSOFFSET = 1000;
    
    /**
	 * 默认错误码和错误信息对应表
	 */
	private static Map<Integer, String> tables =
			new ConcurrentHashMap<Integer, String>();
	
	/**
	 * 静态加载内容
	 */
	static {
		do {
			tables.put(HTTPError.OK, "OK");
			tables.put(HTTPError.EUNKNOWN, "unknown error");
			tables.put(HTTPError.EEXCEPTION, "exception");
			tables.put(HTTPError.EMETHODUNSUPPORTED, "method unsupported");
			tables.put(HTTPError.EILLEGALPARAMETER, "illegel parameter");
			tables.put(HTTPError.EDATARECEIVED, "http receiving error");
			tables.put(HTTPError.ECLIENTPROTOCOL, "client protocol error");
			tables.put(HTTPError.EIOEXCEPTION, "I/O exception");
			tables.put(HTTPError.EUNKNOWNHOST, "unknown host");
			tables.put(HTTPError.ECONNRESET, "I/O exception, econnreset");
			tables.put(HTTPError.ECONNECT, "connection error");
			tables.put(HTTPError.ETIMEOUT, "socket timeout");
			tables.put(HTTPError.ECOMPRESS, "compression error");
			tables.put(HTTPError.EDECOMPRESS, "decompression error");
			tables.put(HTTPError.EENCRYPT, "encryption error");
			tables.put(HTTPError.EDECRYPT, "decryption error");
			tables.put(HTTPError.EBROKENPROTOCOL, "broken protocol error");
			tables.put(HTTPError.ECLIENTCALLBACK, "client callback error");
		} while (false);
	}
	
	/**
	 * 错误码
	 */
	final private int code;
	
	/**
	 * 错误码描述信息
	 */
	final private String message;
	
	/**
	 * 构造函数
	 */
	public HTTPError() {
		this(EUNKNOWN);
	}
	
	/**
	 * 构造函数
	 * @param code 错误码
	 */
	public HTTPError(int code) {
		this(code, valueOf(code));
	}
	
	/**
	 * 构造函数
	 * @param code 错误码
	 * @param message 错误码对应的描述信息
	 */
	public HTTPError(int code, String message) {
		this.code = code;
		this.message = message;
	}
	
	/**
	 * 获取错误码对应的描述信息
	 * @param code 错误码
	 * @return 错误码对应的描述信息
	 */
    public static String valueOf(int code) {
    	if (tables.containsKey(code)) {
    		return tables.get(code);
    	} else {
    		return "" + code;
    	}
    }
    
	/**
	 * 是否包含指定错误码
	 * @param code 错误码
	 * @return true 包含；false 不包含
	 */
    public static boolean has(int code) {
    	return tables.containsKey(code);
    }
    
    /**
     * 当前错误码是否表示程序执行错误
     * @return true 错误；false 执行正确
     */
    public boolean failed() {
    	return (OK != code);
    }
    
    /**
     * 错误码是否与指定错误码相同
     * @param code 指定错误码
     * @return true 相同；false 不相同
     */
    public boolean equals(int code) {
    	return (this.code == code);
    }
    
    /**
     * 获取错误码
     * @return 错误码
     */
    public int getCode() {
    	return code;
    }
    
    /**
     * 获取错误码对应的描述信息
     * @return 错误码对应的描述信息
     */
    public String getMessage() {
    	return message;
    }
    
    @Override
	public String toString() {
		return (code + " " + message);
	}
}
