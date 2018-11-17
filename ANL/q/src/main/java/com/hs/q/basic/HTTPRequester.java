package com.hs.q.basic;

import android.content.Context;
import android.util.Base64;

import com.hs.q.common.http.HTTPBuilder;
import com.hs.q.common.http.HTTPError;
import com.hs.q.common.http.HTTPHelper;
import com.hs.q.common.http.HTTParser;
import com.hs.q.common.id.CUID;
import com.hs.q.common.utils.AES;
import com.hs.q.common.utils.Device;
import com.hs.q.common.utils.KVUtils;
import com.hs.q.common.utils.LOG;
import com.hs.q.common.utils.SystemUtils;
import com.hs.q.common.utils.TextUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * HTTP请求
 *
 */
public class HTTPRequester {
	/**
	 * 日志标签
	 */
	public final String TAG;

    /**
	 * 上下文对象
	 */
    public final Context mContext;
    
    /**
     * 远程服务器地址
     */
	public String[] mTargetHosts = null;
    
    /**
     * 请求路径
     */
	public String mRequestPath = null;
    
    /**
     * 请求包体
     */
    private String mRequestBody = null;
    
    /**
     * 请求结果
     */
    private String mResponseBody = null;

	/**
	 * Data节点
	 */
	private String mResponseData = null;
    
    /**
     * 构造函数
     * @param context 上下文对象
     */
    public HTTPRequester(Context context, String tag) {
    	this.mContext = context;
		this.TAG = tag;
    }

	/**
	 * 设置请求地址列表
	 * @param hosts 请求地址列表
	 */
	public void setTargetHosts(String[] hosts) {
		if (TextUtils.empty(hosts)) {
			throw new IllegalArgumentException("empty target hosts");
		} else {
			this.mTargetHosts = parseHostArray(hosts);
		}
	}

	private String[] parseHostArray(String[] cipherHosts) {
		List<String> realHosts = new ArrayList<>();

		if (null != cipherHosts) {
			for (String cipherHost: cipherHosts) {
				String host = decrypt(cipherHost);
				if (validate(host)) {
					realHosts.add(host);
				}
			}
		}

		return realHosts.toArray(new String[] {});
	}

	private String decrypt(String cipher) {
		try {
			byte[] cipherBytes = Base64.decode(cipher, Base64.NO_WRAP);
			byte[] buffer = AES.decrypt(cipherBytes, Hosts.API_KEY);
			return new String(buffer, "UTF-8");
		} catch (Exception e) {
			return cipher;
		}
	}

	private boolean validate(String host) {
		return ((!TextUtils.empty(host)) && (!host.equalsIgnoreCase("0")));
	}
    
    /**
     * 设置请求路径
     * @param path 请求路径
     */
    public void setRequestPath(String path) {
    	this.mRequestPath = path;
    }

	/**
	 * 设置请求包体
	 * @param requestBody 请求包体
	 */
	public void setRequestBody(String requestBody) {
		mRequestBody = requestBody;
	}

	/**
	 * 获取响应包体
	 * @return 响应包体
	 */
	public String getResponseBody() {
		return mResponseBody;
	}

	/**
	 * 获取响应包体
	 * @return 响应包体
	 */
	public String getResponseData() {
		return mResponseData;
	}

	/**
	 * 执行HTTP请求，处理返回的JSON对象
	 * @throws Exception 异常定义
	 */
	public void jsonExecute() throws Exception {
		if (TextUtils.empty(mTargetHosts)) {
			throw new Exception("empty hosts");
		}

		if (!SystemUtils.isNetworkAvailable(mContext)) {
			throw new Exception("network unavailable");
		}

		String salt = AES.genSalt();
		HTTPHelper helper = createHelper(salt);

		long start = System.currentTimeMillis();
		HTTPError he = helper.post();
		long millis = (System.currentTimeMillis() - start);

		if (he.failed()) {
			throw new Exception("http(POST JSON) failed: " + he);
		} else {
			String targetHost = helper.getTargetHost();
			byte[] responseBody = helper.getResponseBody();

			if (!TextUtils.empty(responseBody)) {
				parseResponseBody(responseBody, salt);
				LOG.d(TAG, "[" + mRequestPath + "] http(POST JSON) done, host=" + targetHost
						+ ", ms=" + millis + ", resp="
						+ printResponse(mResponseBody));
			} else {
				LOG.d(TAG, "[" + mRequestPath + "] http(POST JSON) done, host=" + targetHost
						+ ", ms=" + millis + ", resp=none");
			}
		}
	}

	/**
	 * 数据包不大的情况下打印结果
	 * @param response 数据包
	 * @return 打印结果
	 */
	private String printResponse(String response) {
		if (response.length() > 4096) {
			return "too large(" + response.length() + ")";
		} else {
			return response;
		}
	}
    
    /**
	 * 构造HTTP传输对象
	 * @psalt salt 盐码
	 * @return HTTP传输对象
	 * @throws Exception 异常定义
	 */
    private HTTPHelper createHelper(String salt) throws Exception {
		// 构造对象
		String path = (mRequestPath + "/" + salt);
		HTTPHelper helper = HTTPBuilder.build(mContext, mTargetHosts, path);

        // 添加公共参数
		HTTPBuilder.appendCommonParameters(mContext, helper);

		// 添加包体
		String body = buildRequestBody(salt);
		helper.setRequestBody(body.getBytes("utf-8"));

        // 设置签名KEY
        helper.setSignKey(Hosts.HTTP_SIGN_KEY);
        return helper;
	}

	private String buildRequestBody(String salt) throws Exception {
		return HTTPBuilder.buildCipherBody(mRequestBody, Hosts.HTTP_AES_KEY, salt);
	}

	private void parseResponseBody(byte[] cipher, String salt) throws Exception {
		mResponseBody = new String(cipher, "UTF-8");
		if (TextUtils.empty(mResponseBody)) {
			throw new Exception("no response from server");
		}

		JSONObject root = new JSONObject(mResponseBody);
		int code = KVUtils.getInt(root, "r1", -1);
		String message = KVUtils.getString(root, "m2", "-1");

		if (0 != code) {
			throw new Exception("remote server error(" + code + message + ")");
		}

		if (root.has("r3")) {
			String dataB64 = root.getString("r3");

			if (!TextUtils.empty(dataB64)) {
				mResponseData = parseDataNode(dataB64, salt);
			}
		}
	}

	private String parseDataNode(String b64, String salt) throws Exception {
		try {
			return HTTParser.parse(b64, Hosts.HTTP_AES_KEY, salt);
		} catch (Exception e) {
			throw new Exception("parse response body failed", e);
		}
	}

	protected void alertEmpty(String s, String message) throws Exception {
		if (TextUtils.empty(s)) {
			throw new Exception(message);
		}
	}

	protected boolean isValueValid(String value) {
		return (!(TextUtils.empty(value) || value.equals("null")));
	}

	protected String notNull(String s) {
		return ((null == s) ? "" : s);
	}

	protected String getDeviceId(Context context) {
		String m2 = Device.getm2(context);
		if (TextUtils.empty(m2)) {
			m2 = CUID.str(context);
		}
		return m2;
	}

	protected String getIMEIs(Context context) {
		HashSet<String> imeis = Device.getIMEIs(context);
		return TextUtils.toText(imeis);
	}
}
