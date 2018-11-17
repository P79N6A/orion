package com.hs.q.common.http;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import com.hs.q.common.utils.LOG;
import com.hs.q.common.utils.TextUtils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

public class HTTPHelper {
    /**
     * 日志标签
     */
    private final static String TAG = "HTTPHelper";

	/**
	 * 主要参数定义
	 */
	private final static String P_REQUEST_TIME = "t";           // 发起请求的时间戳，对应一次HTTP原请求
	private final static String P_SYNCNO = "syn";               // 请求同步ID
	private final static String P_SIGNATURE = "sign";           // 签名参数

    /**
     * 请求计数
     */
    private static AtomicLong gCounter = new AtomicLong(1);

    /**
     * 上下文
     */
    private final Context mContext;

	/**
	 * 超时，这里连接超时和套接字超时是同一个值
	 */
	private int mTimeout = 30000;

    /**
     * 标识日志是否打开，默认打开
     */
    private boolean mLogEnabled = true;

    /**
     * HTTP请求目标地址列表
     */
    private String[] mHosts = null;

    /**
     * HTTP请求路径
     */
    private String mPath = null;

    /**
     * HTTP请求查询参数
     */
    private Map<String, String> mQueryParameters =
            new LinkedHashMap<String, String>();

    /**
     * 参数签名所用的混淆字符串，一般是APPKEY和ACESS_TOKEN
     */
    private String mSignKey = null;

    /**
     * 对应HTTP包头
     */
    private Map<String, String> mHeaders = new HashMap<String, String>();

    /**
     * HTTP请求包体，只有POST方法时有效
     */
    private byte[] mRequestBody = null;

    /**
     * 文件下载时的回调
     */
    private OnBlockListener mOnBlockListener = null;

	/**
	 * 请求处理上下文
	 */
	private HCTX mHCTX = null;

    /**
     * 创建HTTP请求实例
     * @param context 上下文对象
     * @return HTTP请求实例
     */
    public static HTTPHelper get(Context context) {
        return new HTTPHelper(context);
    }

	/**
	 * 根据当前网络环境，获取一个HttpClient对象
	 * @param node 代理接入点
	 * @param timeout 超时
	 * @return 返回HttpClient对象
	 * @throws Exception 异常定义
	 */
	private static HttpClient getHttpClient(ProxyManager.ApnNode node, int timeout) throws Exception {
		if (null != node) {
			HttpHost httpHost = new HttpHost(node.getProxy(), node.getPort());
			return HttpClientFactory.create(httpHost, timeout);
		} else {
			return HttpClientFactory.create(null, timeout);
		}
	}

    /**
     * 构造函数，私有化
     *
     * @param context 上下文对象
     */
    private HTTPHelper(Context context) {
        this.mContext = context;
    }

	/**
	 * 设置连接超时和套接字超时
	 * @param millis 超时，单位毫秒
	 */
	public void setTimeout(int millis) {
		this.mTimeout = millis;
	}

    /**
     * 设置日志标志
     *
     * @param enabled 日志开关
     */
    public void setLogEnabled(boolean enabled) {
        mLogEnabled = enabled;
    }

    /**
     * 设置目标地址
     * @param hosts 目标地址
     */
    public synchronized void setHosts(String[] hosts) {
        this.mHosts = hosts;
    }

    /**
     * 设置请求路径，对应HTTP的Path域
     * @param path 请求路径，对应HTTP的Path域
     */
    public synchronized void setPath(String path) {
        this.mPath = path;
    }

    /**
     * 设置URL中的查询参数
     * @param name 查询参数键值对对应的名称
     * @param value 查询参数键值对对应的值
     */
    public synchronized void putQueryParameter(String name, String value) {
        if (TextUtils.empty(name)) {
            W("[" + name + "][" + value
                    + "] put query parameter, but empty name ...");
        } else if (TextUtils.empty(value)) {
            W("[" + name + "][" + value
                    + "] put query parameter, but empty value ...");
        } else {
			this.mQueryParameters.put(name, value);
        }
    }

    /**
     * 设置查询参数列表
     * @param parameters 参数列表
     */
    public synchronized void putQueryParameters(Map<String, String> parameters) {
		this.mQueryParameters.putAll(parameters);
    }

    /**
     * 设置对参数签名所用的混淆串
     * @param key 对参数签名所用的混淆串
     */
    public synchronized void setSignKey(String key) {
        this.mSignKey = key;
    }

    /**
     * 设置HTTP包头信息
     * @param name HTTP包头键值名称
     * @param value HTTP包头键值
     */
    public synchronized void putHeader(String name, String value) {
        if (TextUtils.empty(name)) {
            W("[" + name + "][" + value
                    + "] put header, but empty name ...");
        } else if (TextUtils.empty(value)) {
            W("[" + name + "][" + value
                    + "] put header, but empty value ...");
        } else {
            this.mHeaders.put(name, value);
        }
    }

    /**
     * 设置HTTP请求的User-Agent请求头字段，接口会对UserAgent内部封装，格式为
     * [userAgent] (Android [Build.VERSION.RELEASE]; Linux)
     * @param value 所设置的值
     */
    public synchronized void setUserAgent(String value) {
        if (TextUtils.empty(value)) {
            W("[" + value + "] set user agent, but empty ...");
        } else {
            String userAgent = (value + " (Android " + Build.VERSION.RELEASE + "; Linux)");
            this.mHeaders.put("User-Agent", userAgent);
        }
    }

    /**
     * 设置HTTP的POST方法下发送的包体信息
     * @param body 包体信息
     */
    public synchronized void setRequestBody(byte[] body) {
        this.mRequestBody = body;
    }

    /**
     * 设置文件下载过程中的回调接口
     * @param listener 文件下载过程中的回调接口
     */
    public synchronized void setBlockListener(OnBlockListener listener) {
        this.mOnBlockListener = listener;
    }

    /**
     * 获取当前HTTP请求的URL
     * @return 请求的URL
     */
    public String getRequestUrl() {
        return mHCTX.mRequestUrl;
    }

    /**
     * 获取处理当前HTTP请求的目标地址
     * @return 处理当前HTTP请求的目标地址
     */
    public String getTargetHost() {
        return mHCTX.mTargetHost;
    }

    /**
     * 获取当前HTTP请求返回的响应的包体内容，解压并解密之后的内容
     * @return 当前请求返回的HTTP响应的包体内容
     */
    public byte[] getResponseBody() {
        return mHCTX.mResponseBody;
    }

    /**
     * 同步执行一次HTTP请求，使用POST方法
     */
    public HTTPError post() {
		mHCTX = new HCTX();
        I("[NO:" + mHCTX.ID + "] http(POST) hosts: " + TextUtils.toText(mHosts));

        try {
            HttpPost httpPost = new HttpPost();
			httpPost.setHeaders(buildRequestHeaders());

            if (null != mRequestBody) {
				mHCTX.mRequestBodyMd5 = getMD5String(mRequestBody);
                ByteArrayEntity entity = new ByteArrayEntity(mRequestBody);
				httpPost.setEntity(entity);
            }

			mHCTX.mRequest = httpPost;
			requestOnEveryHost(mHCTX);
			return mHCTX.mHttpError;
        } catch (Throwable t) {
            E("[NO:" + mHCTX.ID + "] http(POST) failed", t);
            return new HTTPError(HTTPError.EEXCEPTION, t.getMessage());
        }
    }

	/**
	 * 同步执行一次HTTP请求，使用GET方法
	 */
	public HTTPError get() {
		mHCTX = new HCTX();
		I("[NO:" + mHCTX.ID + "] http(GET) hosts: " + TextUtils.toText(mHosts));

		try {
			HttpGet httpGet = new HttpGet();
			httpGet.setHeaders(buildRequestHeaders());

			mHCTX.mRequest = httpGet;
			requestOnEveryHost(mHCTX);
			return mHCTX.mHttpError;
		} catch (Throwable t) {
			E("[NO:" + mHCTX.ID + "] http(GET) failed", t);
			return new HTTPError(HTTPError.EEXCEPTION, t.getMessage());
		}
	}

	/**
	 * 同步执行一次HTTP请求，使用GET方法
	 */
	public HTTPError get(String url) {
		mHCTX = new HCTX();
		I("[NO:" + mHCTX.ID + "] http(GET) url: " + url);

		try {
			mHCTX.mRequestUrl = url;
			mHCTX.mRealUrl = url;
			mHCTX.mRequest = new HttpGet(url);
			mHCTX.mRequest.setHeaders(buildRequestHeaders());
			requestAndGetResponse(null, mHCTX);

			// 分析执行POST请求返回的结果，如果为HTTP_CONNECT_FAILURE
			// 或者HTTP_TIMEOUT，可能设置了代理节点
			// 重新获取代理节点，再重试请求
			if (canRetryForProxy(mHCTX.mHttpError)) {
				ProxyManager.ApnNode node = ProxyManager.getNode(mContext);

				if (null != node) {
					I("[NO:" + mHCTX.ID + "] http failed: " + mHCTX.mHttpError + ", continue: " + node);
					requestAndGetResponse(node, mHCTX);
				}
			}

			return mHCTX.mHttpError;
		} catch (Throwable t) {
			E("[NO:" + mHCTX.ID + "] http(GET) failed", t);
			return new HTTPError(HTTPError.EEXCEPTION, t.getMessage());
		}
	}

    /**
     * 执行一个HTTP请求，使用GET方法执行，指定当前HTTP请求的序列号
     * 如果进行重试，此序列号不变
     * 顺序按照Hosts列表执行HTTP请求，直到一个执行成功为止
	 * @param ctx 上下文对象
     * @throws Exception 异常定义
     */
    private void requestOnEveryHost(HCTX ctx) throws Exception {
        // 根据提供的地址列表，依次执行HTTP请求
        // 如果有成功，则立即返回
        if (empty(mHosts)) {
			ctx.mHttpError = new HTTPError(HTTPError.EILLEGALPARAMETER, "empty hosts");
        } else {
			for (String host : mHosts) {
				ctx.mHttpHost = buildHttpHost(host);
				signAndExecuteRequest(ctx);

				// 如果目标地址不存在，那么将目标地址设置为当前域名
				if (TextUtils.empty(ctx.mTargetHost)) {
					ctx.mTargetHost = host;
				}

				// 如果结果为地址不可达或者ECONNRESET错误，那么更换地址，重新请求
				if (canRetryForHosts(ctx.mHttpError)) {
					I("[NO:" + ctx.ID + "] http failed: " + host + ", " + ctx.mHttpError + ", continue ...");
				} else {
					break;
				}
			}
        }
    }

    /**
     * 对参数签名并执行HTTP请求
     * @param ctx 上下文对象
     * @throws Exception 异常定义
     */
    private void signAndExecuteRequest(HCTX ctx) throws Exception {
		buildRequestURI(ctx);
        requestAndGetResponse(null, ctx);

        // 分析执行POST请求返回的结果，如果为HTTP_CONNECT_FAILURE
        // 或者HTTP_TIMEOUT，可能设置了代理节点
        // 重新获取代理节点，再重试请求
        if (canRetryForProxy(ctx.mHttpError)) {
            ProxyManager.ApnNode node = ProxyManager.getNode(mContext);

            if (null != node) {
                I("[NO:" + ctx.ID + "] http failed: " + ctx.mHttpError + ", continue: " + node);
				buildRequestURI(ctx);
				requestAndGetResponse(node, ctx);
            }
        }
    }

    /**
     * 检查错误码是否可以继续重试，当出现EUNKNOWNHOST或者ECONNRESET
     * 可以继续使用其它host重试
     * @param httpError HTTP错误码
     * @return true 可以重试；false 不需要重试
     */
    private boolean canRetryForHosts(HTTPError httpError) {
        return (httpError.equals(HTTPError.EUNKNOWNHOST)
                || httpError.equals(HTTPError.ECONNRESET)
				|| httpError.equals(HTTPError.ECONNECT));
    }

    /**
     * 判断HTTP错误是否可以使用代理重试
     * @param httpError 错误信息
     * @return true 可以使用代理重试；false 不用重试
     */
    private boolean canRetryForProxy(HTTPError httpError) {
        return (httpError.equals(HTTPError.ECONNECT)
                || httpError.equals(HTTPError.ETIMEOUT));
    }

    /**
     * 同步执行HTTP请求，并返回响应数据
     * @param proxy 代理对象，默认为NULL
     * @param ctx 请求环境
     * @throws Exception 异常定义
     */
	private void requestAndGetResponse(ProxyManager.ApnNode proxy,
			HCTX ctx) throws Exception {
		long start = 0;
		long respMillis = 0;
		long recvMillis = 0;
		int httpCode = 0;
		StatusLine httpStatusLine = null;
		HttpResponse response = null;
		HttpContext httpContext = new BasicHttpContext();
		HttpClient client = null;

        try {
			client = getHttpClient(proxy, mTimeout);
            start = System.currentTimeMillis();
            response = execute(client, ctx, httpContext);
            respMillis = System.currentTimeMillis() - start;
			ctx.mTargetHost = getRemoteAddress(httpContext);
            start = System.currentTimeMillis();
            httpStatusLine = response.getStatusLine();
            httpCode = httpStatusLine.getStatusCode();

            if (SUCCESS(httpCode)) {
                HttpEntity resEntity = response.getEntity();
				ctx.mResponseBody = ((204 == httpCode) ? null : EntityUtils.toByteArray(resEntity));
                recvMillis = System.currentTimeMillis() - start;
				ctx.mHttpError = new HTTPError(HTTPError.OK, "OK");
                I("[NO:" + ctx.ID + "] http done: target=" + ctx.mTargetHost
                        + ", respMillis=" + respMillis + ", recvMillis=" + recvMillis
						+ ", length=" + getContentLength(resEntity)
                        + ", " + httpStatusLine);
            } else {
				ctx.mHttpError = new HTTPError((HTTPError.EHTTPSTATUSOFFSET + httpCode),
                        httpStatusLine.toString());
                E("[NO:" + ctx.ID + "] http failed: target=" + ctx.mTargetHost
                        + ", respMillis=" + respMillis + ", " + httpStatusLine + "\n"
                        + printHTTPHeaders(ctx.mRequest) + "\n"
                        + httpStatusLine + "\n"
                        + printHTTPHeaders(response));
            }
        } catch (Exception e) {
            long millis = System.currentTimeMillis() - start;
			ctx.mHttpError = handleException(e);
            E("[NO:" + ctx.ID + "] http exception: respMillis=" + millis, e);
        } finally {
			safeClose(ctx, ctx.mRequest, response, client);
        }
    }

    /**
     * 使用HTTP协议卸载文件，同步返回请求响应结果
     * 该接口务必在工作线程中调用
     * 该接口不是线程安全的
	 * @param url 下载URL
     * @param offset 下载文件的起始偏移量
     * @return HTTP请求返回码
     */
    public synchronized HTTPError download(String url, long offset) {
		mHCTX = new HCTX();
		I("[NO:" + mHCTX.ID + "] download: url: " + url);
		I("[NO:" + mHCTX.ID + "] download: offset: " + offset);

		try {
			HttpGet httpGet = new HttpGet(url);
			httpGet.setHeaders(buildBrokenHeaders(offset, -1));

			mHCTX.mRequestUrl = url;
			mHCTX.mRealUrl = url;
			mHCTX.mRequest = httpGet;
			downloadAndGetResult(mHCTX, mOnBlockListener);
			return mHCTX.mHttpError;
		} catch (Throwable t) {
			E("[NO:" + mHCTX.ID + "] download failed", t);
			return new HTTPError(HTTPError.EEXCEPTION, t.getMessage());
		}
    }

    /**
     * 下载指定文件，并且检测返回值，看是否需要设置代理重置
     * @param ctx 上下文
     * @param listener 回调接口
     * @throws Exception 异常定义
     */
    private void downloadAndGetResult(HCTX ctx, OnBlockListener listener) throws Exception {
        downloadBreakpoint(null, ctx, listener);

        // 分析执行POST请求返回的结果，如果为HTTP_CONNECT_FAILURE
        // 或者HTTP_TIMEOUT，可能设置了代理节点
        // 重新获取代理节点，再重试请求
        if (canRetryForProxy(ctx.mHttpError)) {
            ProxyManager.ApnNode node = ProxyManager.getNode(mContext);

            if (null != node) {
                I("[NO:" + ctx.ID + "] download failed: " + ctx.mHttpError + ", continue: " + node);
                downloadBreakpoint(node, ctx, listener);
            }
        }
    }

    /**
     * 同步执行文件下载请求，为确保响应及时，分段下载，第一次下载8K，
     * 后面每次下载64K
     * @param proxy 代理对象
     * @param ctx 上下文
     * @param listener 下载过程回调
     * @throws Exception 异常定义
     */
    private void downloadBreakpoint(ProxyManager.ApnNode proxy, HCTX ctx,
             OnBlockListener listener) throws Exception {
        long start = 0;
        long respMillis = 0;
        long recvMillis = 0;
        int httpCode = 0;
		StatusLine httpStatusLine = null;
		HttpResponse response = null;
        HttpContext httpContext = new BasicHttpContext();
		HttpClient client = null;

        try {
            // 执行下载请求
			client = getHttpClient(proxy, mTimeout);
            start = System.currentTimeMillis();
            response = execute(client, ctx, httpContext);
            respMillis = System.currentTimeMillis() - start;
            ctx.mTargetHost = getRemoteAddress(httpContext);
            start = System.currentTimeMillis();

            httpStatusLine = response.getStatusLine();
            httpCode = httpStatusLine.getStatusCode();

            if (SUCCESS(httpCode)) {
				BrokenRange range = parseResponseRange(response);

                // 回调通知下载开始
                if (!listener.onStart(ctx.mRealUrl, range.start, range.length, range.total)) {
                    E("[NO:" + ctx.ID + "] download: target=" + ctx.mTargetHost
							+ ", callback failed");
					ctx.mHttpError = new HTTPError(HTTPError.ECLIENTCALLBACK, "callback error");
                } else {
                    HttpEntity resEntity = response.getEntity();
					ctx.mHttpError = handleResponse(ctx, range.length, resEntity, listener);
                    recvMillis = System.currentTimeMillis() - start;

                    if (!ctx.mHttpError.failed()) {
                        I("[NO:" + ctx.ID + "] download done: target=" + ctx.mTargetHost
                                + ", respMillis=" + respMillis + ", recvMillis=" + recvMillis
                                + ", " + httpStatusLine);
                    } else {
                        E("[NO:" + ctx.ID + "] download failed: target=" + ctx.mTargetHost
                                + ", " + ctx.mHttpError);
                    }
                }
            } else {
                E("[NO:" + ctx.ID + "] download failed: target=" + ctx.mTargetHost
                        + ", respMillis: " + respMillis + ", " + httpStatusLine + "\n"
                        + printHTTPHeaders(ctx.mRequest) + "\n"
                        + httpStatusLine + "\n"
                        + printHTTPHeaders(response));
				ctx.mHttpError = new HTTPError((HTTPError.EHTTPSTATUSOFFSET + httpCode),
                        httpStatusLine.toString());
            }
        } catch (Exception e) {
            long millis = System.currentTimeMillis() - start;
			ctx.mHttpError = handleException(e);
            E("[NO:" + ctx.ID + "] download exception: respMillis=" + millis, e);
        } finally {
			safeClose(ctx, ctx.mRequest, response, client);
        }
    }

	/**
	 * 执行HTTP请求
	 * @param client HttpClient对象
	 * @param ctx 上下文
	 * @param httpContext HttpClient请求上下文
	 * @return HTTP响应对象
	 * @throws IOException 异常定义
	 */
	private HttpResponse execute(HttpClient client, HCTX ctx,
			HttpContext httpContext) throws IOException {
		HttpResponse response = null;

		if (null == ctx.mHttpHost) {
			I("[NO:" + ctx.ID + "] http request: url=" + ctx.mRequest.getURI());
			response = client.execute(ctx.mRequest, httpContext);
		} else {
			I("[NO:" + ctx.ID + "] http request: host=" + ctx.mHttpHost + ", url=" + ctx.mRequest.getURI());
			response = client.execute(ctx.mHttpHost, ctx.mRequest, httpContext);
		}

		// 检查如果重定向，则更新URL后重新请求
		for (int i = 0; i < 5; i++) {
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();

			if (REDIRECT(statusCode)) {
				String location = getLocation(response);

				if (!TextUtils.empty(location)) {
					I("[NO:" + ctx.ID + "] http redirect: " + statusLine + ", location=" + location);
					ctx.mRequest.setURI(URI.create(location));
					ctx.mRealUrl = location;

					// 注意：一定要关闭之前发生重定向的连接，再重新请求，否则连接池会消耗光
					safeClose(ctx, ctx.mRequest, response, null);
					response = client.execute(ctx.mRequest, httpContext);
				}
			} else {
				break;
			}
		}

		return response;
	}

	private HTTPError handleException(Exception e) {
		if (e instanceof ConnectException) {
			return new HTTPError(HTTPError.ECONNECT, e.getMessage());
		} else if (e instanceof SocketTimeoutException) {
			return new HTTPError(HTTPError.ETIMEOUT, e.getMessage());
		} else if (e instanceof UnknownHostException) {
			return new HTTPError(HTTPError.EUNKNOWNHOST, e.getMessage());
		} else if (e instanceof ClientProtocolException) {
			return new HTTPError(HTTPError.ECLIENTPROTOCOL, e.getMessage());
		} else if (e instanceof IOException) {
			return new HTTPError(getErrorCode((IOException)e), e.getMessage());
		} else {
			return new HTTPError(HTTPError.EEXCEPTION, e.getMessage());
		}
	}

	/**
	 * 获取响应数据长度
	 * @param entity 响应数据对象
	 * @return 响应数据长度
	 */
	private long getContentLength(HttpEntity entity) {
		return ((null != entity) ? entity.getContentLength() : 0);
	}

	/**
	 * 获取重定向地址
	 * @param response HTTP响应
	 * @return 重定向地址
	 */
	public String getLocation(HttpResponse response) {
		Header locationHeader = response.getFirstHeader("Location");
		return ((null != locationHeader) ? locationHeader.getValue() : "");
	}

	/**
	 * 获取请求时远端地址，必须要在请求完成后立即获取
	 * @param context 请求上下文
	 * @return 请求时远端地址
	 */
	private String getRemoteAddress(HttpContext context) {
		try {
			Object object = context.getAttribute(ExecutionContext.HTTP_CONNECTION);

			if ((null != object) && (object instanceof HttpInetConnection)) {
				HttpInetConnection hconn = (HttpInetConnection) object;
				String remoteAddr = hconn.getRemoteAddress().toString();
				return remoteAddr.replace("/", "");
			}
		} catch (Exception e) {
//			E("get remote address failed(" + e.getClass().getSimpleName()
//					+ "): " + e.getMessage());
		}

		return "";
	}

    /**
     * 序列化HTTP请求或者响应的包头信息
     * @param httpMessage HTTP请求或者响应对象
     * @return 序列化后的包头信息
     */
    private String printHTTPHeaders(HttpMessage httpMessage) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        Header[] headers = httpMessage.getAllHeaders();
        String prefix = ((httpMessage instanceof HttpResponse) ? "<<<<<<<< " : ">>>>>>>> ");

        if (null != headers) {
            for (Header header : headers) {
                if (first) {
                    first = false;
                } else {
					builder.append("\n");
                }

				builder.append(prefix).append(header);
            }
        }

        return builder.toString();
    }

    /**
     * 关闭资源，注意这个调用很重要，不然会消耗HttpClient的连接池
	 * @param ctx 上下文对象
	 * @param request HTTP请求对象
     * @param response HTTP响应对象
	 * @param client HTTP对象
     */
    private void safeClose(HCTX ctx, HttpRequestBase request, HttpResponse response, HttpClient client) {
        try {
            if (null != response) {
                HttpEntity httpEntity = response.getEntity();
                if (null != httpEntity) {
                    httpEntity.consumeContent();
                }
            }
        } catch (Exception e) {
            //E("[NO:" + ctx.ID + "] close http stream failed: " + e);
        }

		try {
			if (null != client) {
				client.getConnectionManager().shutdown();
			}
		} catch (Exception e) {
			//E("[NO:" + ctx.ID + "] close http client failed: " + e);
		}
    }

    /**
     * 创建HttpHost对象
	 * @param host 目标地址
     * @return 创建的HttpHost对象
     * @throws Exception 异常定义
     */
    private HttpHost buildHttpHost(String host) throws Exception {
		final URL hostURL;

        if (!host.startsWith("http")) {
			hostURL = new URL("http://" + host);
        } else {
			hostURL = new URL(host);
        }

		return new HttpHost(hostURL.getHost(), hostURL.getPort(),
				hostURL.getProtocol());
    }

    /**
     * 添加请求时间参数，签名并且构造请求URL字符串
     * @param ctx 上下文对象
     * @throws Exception 异常定义
     */
    private void buildRequestURI(HCTX ctx) throws Exception {
		LinkedHashMap<String, String> queries = new LinkedHashMap<>(mQueryParameters);
		queries.put(P_REQUEST_TIME, "" + System.currentTimeMillis());
		queries.put(P_SYNCNO, ctx.ID);

        // 根据签名参数和混淆字符串生成签名
        // 并将签名添加到签名参数中
        String signature = getSignature(queries, ctx.mRequestBodyMd5, mSignKey);
		queries.put(P_SIGNATURE, signature);

        // 构建URI对象
		ctx.mRequestUrl = buildRequestUrl(mPath, queries);
		ctx.mRealUrl = ctx.mRequestUrl;

        //I("[NO:" + ctx.ID + "] http url: " + ctx.mRequestUrl);
		ctx.mRequest.setURI(URI.create(ctx.mRequestUrl));
    }

    /**
     * 构造请求URL，不包括目标地址
     * @param path 请求路径
     * @param queries 请求查询参数
     * @return 请求URL
     */
    private String buildRequestUrl(String path, Map<String, String> queries) {
        Uri.Builder ub = new Uri.Builder();

        // 添加路径
        ub.path("").appendEncodedPath(path);

        // 添加参数
		Set<Map.Entry<String, String>> entries = queries.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            ub.appendQueryParameter(entry.getKey(), entry.getValue());
        }

        return ub.build().toString();
    }

    /**
     * 创建断点续传的请求头信息
     * @return Header数组
     */
    private Header[] buildRequestHeaders() {
        Map<String, String> m = new HashMap<String, String>();

        // 添加默认的定义
        m.put("Content-Type", "binary/octet-stream");
        m.put("Charset", "UTF-8");

        // 添加用户定义
        m.putAll(mHeaders);
        return map2Headers(m);
    }

    /**
     * 创建断点续传下载文件的请求头信息
     *
     * @param offset 断点续传偏移量
     * @param length 需要下载的长度
     * @return Header数组
     */
    private Header[] buildBrokenHeaders(long offset, long length) {
        Map<String, String> m = new HashMap<String, String>();
        m.putAll(mHeaders);

        if (offset > 0) {
            if (length > 0) {
                long end = (offset + length - 1);
                m.put("Range", ("bytes=" + offset + "-" + end));
            } else {
                m.put("Range", ("bytes=" + offset + "-"));
            }
        }

        return map2Headers(m);
    }

    /**
     * 将Map对象转换为Header对象数组
     * @param m Map对象
     * @return Header对象数组
     */
    private Header[] map2Headers(Map<String, String> m) {
        int num = 0;
        Set<Map.Entry<String, String>> entries = m.entrySet();
        Header[] headers = new Header[m.size()];

        for (Map.Entry<String, String> entry : entries) {
            headers[num++] = new BasicHeader(entry.getKey(), entry.getValue());
        }

        return headers;
    }

    /**
     * 获取HTTP响应头中的Range信息
     * @param response HTTP响应头
     * @return 存储Range信息的对象
     * @throws Exception 异常定义
     */
    private BrokenRange parseResponseRange(HttpResponse response)
            throws Exception {
        if (response.containsHeader("Content-Range")) {
            Header rangeHeader = response.getFirstHeader("Content-Range");

            try {
                String value = rangeHeader.getValue().replaceAll(" ", "");
                long start = Long.parseLong(value.substring(value.indexOf("bytes") + 5,
                        value.indexOf("-")));
                long end = Long.parseLong(value.substring(value.indexOf("-") + 1,
                        value.indexOf("/")));
                long total = Long.parseLong(value.substring(value.indexOf("/") + 1));
                return new BrokenRange(start, (end - start + 1), total);
            } catch (NumberFormatException e) {
                throw new Exception("parse range(" + rangeHeader
                        + ") failed(NumberFormatException)", e);
            } catch (Exception e) {
                throw new Exception("parse range(" + rangeHeader
                        + ") failed(Exception)", e);
            }
        } else {
            HttpEntity resEntity = response.getEntity();
            long start = 0;
            long length = resEntity.getContentLength();
            long total = resEntity.getContentLength();
            return new BrokenRange(start, length, total);
        }
    }

    /**
     * 处理响应包，如果客户端回调错误，直接返回
     * @param length 数据包总长度，用于校验
     * @param entity 响应包体对象
     * @param listener 回调监听器
     * @throws IllegalStateException 异常定义
     * @throws IOException 异常定义
     * @return 错误信息
     */
    private HTTPError handleResponse(HCTX ctx, long length, HttpEntity entity,
									 OnBlockListener listener)
            throws IllegalStateException, IOException {
        long totalRead = 0;
        int read = 0;
        byte[] buffer = new byte[64 * 1024];
        InputStream is = entity.getContent();

        try {
            while (-1 != (read = is.read(buffer))) {
                totalRead += read;
                if (!listener.onBlock(buffer, read)) {
					safeAbortRequest(ctx, ctx.mRequest);
                    return new HTTPError(HTTPError.ECLIENTCALLBACK, "callback error");
                }
            }
        } finally {
            close(is);
        }

        return ((totalRead != length)
                ? new HTTPError(HTTPError.EDATARECEIVED)
                : new HTTPError(HTTPError.OK));
    }

	/**
	 * 取消时必须先终止请求，否则关闭流对象很慢
	 * @param ctx 上下文
	 * @param request 请求对象
	 */
	private void safeAbortRequest(HCTX ctx, HttpRequestBase request) {
		try {
			if (null != request) {
				I("[NO:" + ctx.ID + "] abort request ...");
				request.abort();
			}
		} catch (Exception e) {
			//E("[NO:" + ctx.ID + "] abort request failed: " + e);
		}
	}

	private void close(Closeable... args) {
		if (null != args) {
			for (Closeable arg: args) {
				try {
					if (null != arg) {
						arg.close();
					}
				} catch (Throwable t) {
				}
			}
		}
	}

    /**
     * 获取参数签名
     * @param signs 需要签名的参数
	 * @param bodyMd5 包体MD5
     * @param signKey 签名KEY
     * @return 签名
     * @throws Exception 异常定义
     */
    private String getSignature(Map<String, String> signs, String bodyMd5,
			String signKey) throws Exception {
        StringBuilder builder = new StringBuilder();
        TreeMap<String, String> t = new TreeMap<String, String>(signs);
        Set<Map.Entry<String, String>> entries = t.entrySet();

        // 按照TreeMap默认便利算法序列化签名参数
        for (Map.Entry<String, String> entry : entries) {
            builder.append(entry.getKey()).append("=").append(entry.getValue());
        }

		// 如果存在包体，追加包体摘要
//		if (!TextUtils.empty(bodyMd5)) {
//			builder.append(bodyMd5);
//		}

        // 如果存在混淆字符串，追加混淆字符串
        if (!TextUtils.empty(signKey)) {
            builder.append(signKey);
        }

		String matrix = builder.toString();
        return getMD5String(matrix.getBytes("UTF-8"));
    }

    /**
     * 根据HTTP错误码，判断是否是执行成功
     * [200, 300)认为执行成功
     * @param httpCode HTTP错误码
     * @return true 执行成功；false 失败
     */
    private boolean SUCCESS(int httpCode) {
        return ((httpCode < 300) && (httpCode >= 200));
    }

	/**
	 * 是否重定向
	 * @param httpCode HTTP错误码
	 * @return true 重定向；false 不需要重定向
	 */
	private boolean REDIRECT(int httpCode) {
		return ((301 == httpCode) || (302 == httpCode));
	}

    /**
     * 根据指定异常定义错误码
     * @param e 指定异常
     * @return 错误码
     */
    private int getErrorCode(IOException e) {
        if (TextUtils.contains(e.getMessage(), "ECONNRESET")) {
            return HTTPError.ECONNRESET;
        } else {
            return HTTPError.EIOEXCEPTION;
        }
    }

    /**
     * 获取指定文本内容对应的MD5码
     * @param plain 文本内容
     * @return 文本内容对应的MD5码
     * @throws Exception 异常定义
     */
    private String getMD5String(byte[] plain) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("MD5");
		byte[] buffer = digest.digest(plain);
		return byteToHex(buffer, 0, buffer.length);
    }

    /**
     * 将数组转换成十六进制字符串
     * @param b 数组
     * @param m 起始位置
     * @param n 个数
     * @return 转化后的十六进制字符串
     */
    private String byteToHex(byte[] b, int m, int n) {
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

	private boolean empty(String[] b) {
		return ((null == b) || (b.length <= 0));
	}

    /**
     * 打印日志，DEBUG级别
     * @param msg 日志消息
     */
    private void D(String msg) {
        if (mLogEnabled) {
            LOG.d(TAG, msg);
        }
    }

    /**
     * 打印日志，INFO级别
     * @param msg 日志消息
     */
    private void I(String msg) {
        if (mLogEnabled) {
            LOG.i(TAG, msg);
        }
    }

    /**
     * 打印日志，INFO级别
     * @param msg 日志消息
     */
    private void W(String msg) {
        if (mLogEnabled) {
            LOG.w(TAG, msg);
        }
    }

    /**
     * 打印日志，ERROR级别
     * @param msg 日志消息
     */
    private void E(String msg) {
        if (mLogEnabled) {
            LOG.e(TAG, msg);
        }
    }

    /**
     * 打印日志，ERROR级别
     * @param msg 日志消息
     * @param t 异常对象
     */
    private void E(String msg, Throwable t) {
        if (mLogEnabled) {
            LOG.e(TAG, msg, t);
        }
    }

    /**
     * 指定HTTP请求的上下文，HTTP Execute Context缩写
     */
    private static class HCTX {
		/**
		 * 当前ID
		 */
		private String ID = ("" + gCounter.getAndIncrement());

		/**
		 * 请求目标服务器地址，非必选
		 */
		private HttpHost mHttpHost = null;

		/**
		 * 请求对象，必选
		 */
		private HttpRequestBase mRequest = null;

		/**
		 * 请求包体MD5
		 */
		private String mRequestBodyMd5 = null;

		/**
		 * 当前请求URL
		 */
		private String mRequestUrl = null;

		/**
		 * 重定向后真正的请求URL
		 */
		private String mRealUrl = null;

        /**
		 * 错误信息
		 */
		private HTTPError mHttpError = new HTTPError();

		/**
		 * HTTP请求的响应数据
		 */
		private byte[] mResponseBody = null;

		/**
		 * 返回响应的目标服务器IP地址
		 */
		private String mTargetHost = null;

		@Override
		public String toString() {
			return ("HCTX(" + ID + ")");
		}
    }

    /**
     * Content Range定义
     *
     */
    private static class BrokenRange {
        /**
         * 起始位置
         */
        private final long start;

        /**
         * 本次下载的数据长度
         */
        private final long length;

        /**
         * 文件总长度
         */
        private final long total;

        /**
         * 构造函数
         * @param start 起始位置
         * @param length 本次下载的数据长度
         * @param total 文件总长度
         */
        private BrokenRange(long start, long length, long total) {
            this.start = start;
            this.length = length;
            this.total = total;
        }
    }

    /**
     * 文件下载过程回调接口定义
     */
    public interface OnBlockListener {
        /**
         * 开始下载
		 * @param url 下载链接，有时候需要重定向到真正的下载链接
         * @param offset 当前下载的起始位置
         * @param length 本次数据包的长度
         * @param total 文件总大小
         * @return 0 处理成功；其它，处理失败
         */
        boolean onStart(String url, long offset, long length, long total);

        /**
         * 下载的内容，如果文件较大，每下载部分数据块就回调通知调用者
         * @param buffer 内容缓存区
         * @param length 实际数据大小
         * @return 0 处理成功；其它，处理失败
         */
        boolean onBlock(byte[] buffer, long length);
    }
}
