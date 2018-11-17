package com.hs.cld.common.dm;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.hs.cld.common.http.HTTPBuilder;
import com.hs.cld.common.http.HTTPError;
import com.hs.cld.common.http.HTTPHelper;
import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.SystemUtils;
import com.hs.cld.common.utils.TextUtils;
import com.hs.cld.common.utils.Threadable;

import java.util.ArrayList;
import java.util.List;

/**
 * 统计上报，主要有以下场景
 * 1 动态路由业务：网络正常的情况下，访问服务器失败
 * 2 XMPP业务：网络正常的情况下，连接失败、上线失败、ACK0失败
 * 3 轮询业务：网络正常的情况下，访问消息服务器失败、ACK失败
 * 4 消息服务注册业务：网络正常的情况下，注册失败、绑定应用失败、设置标签失败
 * 5 消息业务：重复收到消息
 */
public class Tracker {
	/**
	 * 日志标签
	 */
	private final static String TAG = "Tracker";

	/**
	 * 异步工作线程句柄
	 */
	private static Handler sAsyncHandler = null;

	/**
	 * 初始化异步工作线程句柄
	 */
	static {
		HandlerThread worker = new HandlerThread("Tracker");
		worker.start();
		sAsyncHandler = new Handler(worker.getLooper());
	}

	/**
	 * 提交监播数据
	 * @param context 上下文
	 * @param trackerUrls 监播
	 * @param trackerType 监播类型，用户自己定义
	 */
	public static void post(Context context, String[] trackerUrls, String trackerType) {
		if (!TextUtils.empty(trackerUrls)) {
			sAsyncHandler.post(new TrackerTask(context, trackerUrls, trackerType));
		}
	}

	private static class TrackerTask extends Threadable {
		private final Context mContext;
		private final String[] mTrackerUrls;
		private final String mTrackerType;

		private TrackerTask(Context context, String[] trackerUrls, String trackerType) {
			super("trackerTask");
			this.mContext = context;
			this.mTrackerUrls = trackerUrls;
			this.mTrackerType = trackerType;
		}

		@Override
		protected void doFire() {
			LOG.d(TAG, "submit, type=" + mTrackerType + ", urls=" + TextUtils.toText(mTrackerUrls));
			String[] failTrackerUrls = submitTrackers(mTrackerUrls);

			// 如果有失败的，2秒后重试一次
			if (!TextUtils.empty(failTrackerUrls)) {
				LOG.d(TAG, "submit fails 2s delayed, type=" + mTrackerType + ", urls=" + TextUtils.toText(failTrackerUrls));
				sleep(2000);
				submitTrackers(failTrackerUrls);
			}
		}

		private void sleep(long millis) {
			try { Thread.sleep(millis); } catch (Exception e) { }
		}

		private String[] submitTrackers(String[] trackerUrls) {
			List<String> fails = new ArrayList<>();

			if (!TextUtils.empty(trackerUrls)) {
				for (String trackerUrl : trackerUrls) {
					if (!submitTracker(trackerUrl)) {
						fails.add(trackerUrl);
					}
				}
			}

			return fails.toArray(new String[] {});
		}

		/**
		 * 发送监播信息
		 * @param trackerUrl 监播URL
		 * @return true 成功；false 失败
		 */
		private boolean submitTracker(String trackerUrl) {
			HTTPHelper helper = HTTPBuilder.build(mContext);
			helper.setTimeout(10000);
			HTTPError he = helper.get(trackerUrl);
			if (he.failed()) {
				LOG.e(TAG, "[" + trackerUrl + "] submit tracker failed: " + he);
				return false;
			} else {
				return true;
			}
		}
	}
}
