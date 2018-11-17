package com.hs.cld.common.http;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.SystemUtils;
import com.hs.cld.common.utils.TextUtils;

import java.util.Locale;

/**
 * 网络接入点管理，重点需要处理WAP接入点的情况
 * @author yyyyMMdd
 */
public class ProxyManager {
    /**
     * 日志标签
     */
    private final static String TAG = "ProxyManager";

    /**
     * APN查询URI
     */
    private static Uri preferredAPNUrl = Uri.parse("content://telephony/carriers/preferapn");

    /**
     * 获取代理信息，如果为空，表明没有使用代理
     * @param context 上下文对象
     * @return 代理信息
     */
    public static ApnNode getNode(Context context) {
    	try {
    		String netInfo = SystemUtils.getNetworkInfo(context);

            if (!TextUtils.empty(netInfo)) {
                if (netInfo.toLowerCase(Locale.getDefault()).contains("wap")) {
                    return getApnNodeInternal(context);
                }
            }
    	} catch (Exception e) {
    		LOG.e(TAG, "get proxy node failed(Exception): " + e.getMessage());
    	}

        return null;
    }

    /**
     * 移动网络为WAP代理下，获取接入点信息
     * @return 接入点信息
     */
    private static ApnNode getApnNodeInternal(Context context) {
		Cursor cursor = null;

        try {
			ContentResolver resolver = context.getContentResolver();
            cursor = resolver.query(preferredAPNUrl,
                    new String[] {"apn", "proxy", "port"},
                    null, null, null);

            if ((null != cursor) && cursor.moveToFirst()) {
                do {
                    String apn = cursor.getString(cursor.getColumnIndex("apn"));
                    String proxy = cursor.getString(cursor.getColumnIndex("proxy"));
                    String port = cursor.getString(cursor.getColumnIndex("port"));

                    if (!TextUtils.empty(apn)) {
                        if (TextUtils.empty(proxy)) {
                            proxy = getDefaultApnNode(apn);
                        }

                        if (!TextUtils.empty(proxy)) {
                            return new ApnNode(apn, proxy, parsePort(port));
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            LOG.e(TAG, "get apn node failed(" + e.getClass().getSimpleName() + ")", e);
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return null;
    }

    /**
     * 获取默认代理主机地址
     * @param apn 指定APN名称
     * @return 代理服务器地址
     */
    private static String getDefaultApnNode(String apn) {
        if (!TextUtils.empty(apn)) {
            String apnl = apn.toLowerCase(Locale.getDefault());

            if (apnl.contains("cmwap")
                    || apnl.contains("uniwap")
                    || apnl.contains("3gwap")) {
                return "10.0.0.172";
            } else if (apnl.contains("ctwap")) {
                return "10.0.0.200";
            }
        }

        return null;
    }

	/**
	 * 解析端口号
	 * @param portString 端口字符串
	 * @return 解析出的端口号
	 */
	private static int parsePort(String portString) {
		try {
			if (!TextUtils.empty(portString)) {
				return Integer.parseInt(portString);
			}
		} catch (Exception e) {
			// nothing to do
		}

		return 80;
	}

    /**
     * 代理节点对象
     * @author yyyyMMdd
     */
    public static class ApnNode {
        /**
         * 接入点名称
         */
        private String apn = null;

        /**
         * 代理地址
         */
        private String proxy = null;

        /**
         * 代理端口
         */
        private int port = 0;

		/**
		 * 构造函数
		 * @param apn 接入点名称
		 * @param proxy 代理地址
		 * @param port 代理端口
		 */
		private ApnNode(String apn, String proxy, int port) {
			this.apn = apn;
			this.proxy = proxy;
			this.port = port;
		}

        /**
         * 获取接入点名称
         * @return 接入点名称
         */
        public String getApn() {
            return apn;
        }

        /**
         * 获取代理地址
         * @return 代理地址
         */
        public String getProxy() {
            return proxy;
        }

        /**
         * 获取代理端口
         * @return 代理端口
         */
        public int getPort() {
            return port;
        }
        
        @Override
        public String toString() {
        	return (apn + " " + proxy + ":" + port);
        }
    }
}
