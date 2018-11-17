package com.hs.q.common.http;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class HttpClientFactory {
	/**
	 * 连接池参数
	 */
	private static final int POOL_TIMEOUT = (5 * 1000);      // 从连接池中取出连接的等待时间：5秒
	private static final int POOL_MAX_CONNS_PERROUTE = 2;    // 每个目标地址上最大连接数：4
	private static final int POOL_MAX_CONNS = 3;             // 连接池总最大连接数：8

	/**
	 * 使用压缩的HTTP请求头
	 */
	private static final String ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";

	public static HttpClient create(HttpHost proxy, int timeout) throws Exception {
		SchemeRegistry registry = new SchemeRegistry();

		// 注册HTTP工厂
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

		// 注册HTTPS工厂
//		SSLSocketFactory sFactory = SSLSocketFactory.getSocketFactory();
//		sFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//		registry.register(new Scheme("https", sFactory, 443));
		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(null, null);
		SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
		sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		registry.register(new Scheme("https", sf, 443));

		HttpParams params = createHttpParams(timeout);
		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(params, registry);
		DefaultHttpClient httpClient = new DefaultHttpClient(cm, params);
		initHttpClient(httpClient);

		// 设置代理对象
		if (null != proxy) {
			httpClient.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY, proxy);
		}

		return httpClient;
	}

	private static HttpParams createHttpParams(int timeout) {
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setStaleCheckingEnabled(params, false);

		// 设置连接超时
		HttpConnectionParams.setConnectionTimeout(params, timeout);

		// 设置套接字超时
		HttpConnectionParams.setSoTimeout(params, timeout);

		HttpConnectionParams.setTcpNoDelay(params, true);
		HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
		HttpClientParams.setRedirecting(params, false);

		// 设置连接池参数
		ConnManagerParams.setTimeout(params, POOL_TIMEOUT);
		ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(POOL_MAX_CONNS_PERROUTE));
		ConnManagerParams.setMaxTotalConnections(params, POOL_MAX_CONNS);
		return params;
	}

	private static void initHttpClient(DefaultHttpClient httpClient) {
		// 添加请求拦截器
		httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
			@Override
			public void process(HttpRequest request, HttpContext context) {
				if (!request.containsHeader(ACCEPT_ENCODING)) {
					request.addHeader(ACCEPT_ENCODING, ENCODING_GZIP);
				}
			}
		});

		// 添加响应拦截器
		httpClient.addResponseInterceptor(new HttpResponseInterceptor() {
			@Override
			public void process(HttpResponse response, HttpContext context) {
				HttpEntity entity = response.getEntity();

				if (null != entity) {
					Header encodingHeader = entity.getContentEncoding();

					if (null != encodingHeader) {
						for (HeaderElement element: encodingHeader.getElements()) {
							if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
								response.setEntity(new InflatingEntity(response.getEntity()));
								break;
							}
						}
					}
				}
			}
		});
	}

	/**
	 * 解压包体
	 */
	private static class InflatingEntity extends HttpEntityWrapper {
		public InflatingEntity(HttpEntity wrapped) {
			super(wrapped);
		}

		@Override
		public InputStream getContent() throws IOException {
			return new GZIPInputStream(wrappedEntity.getContent());
		}

		@Override
		public long getContentLength() {
			return -1;
		}
	}

	/**
	 * 重载的套接字工厂类型
	 */
	private static class MySSLSocketFactory extends SSLSocketFactory {
		SSLContext mSSLCtx = SSLContext.getInstance("TLS");

		public MySSLSocketFactory(KeyStore truststore)
				throws NoSuchAlgorithmException, KeyManagementException,
				       KeyStoreException, UnrecoverableKeyException {
			super(truststore);

			TrustManager tm = new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws java.security.cert.CertificateException {
				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws java.security.cert.CertificateException {
				}
			};

			mSSLCtx.init(null, new TrustManager[]{tm}, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
				throws IOException {
			return mSSLCtx.getSocketFactory().createSocket(socket, host, port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return mSSLCtx.getSocketFactory().createSocket();
		}
	}
}
