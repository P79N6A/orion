package com.hs.q.basic;

import android.content.Context;

import com.hs.q.common.utils.LOG;
import com.hs.q.common.utils.TextUtils;

/**
 * 程序控制器
 *
 */
public class SLT {
	/**
	 * 日志标签
	 */
	private final static String TAG = "SLT";

	/**
	 * 程序需要静默到什么时候
	 */
	private static long mSilentToInMillis = -1;

	/**
	 * 是否需要静默
	 * @param context 上下文
	 * @return true 需要静默；false 不需要静默
	 */
	public static boolean silent(Context context) {
		long millis = millis(context);
		return ((millis > 0) && (System.currentTimeMillis() <= millis));
	}

	/**
	 * 获取静默截止时间
	 * @param context 上下文
	 * @return 静默截止时间
	 */
	public static long millis(Context context) {
		if (mSilentToInMillis < 0) {
			synchronized (SLT.class) {
				if (mSilentToInMillis < 0) {
					mSilentToInMillis = Settings.getSilentToInMillis(context, 0);
				}
			}
		}
		return mSilentToInMillis;
	}

	/**
	 * 设置静默截止时间
	 * @param context 上下文
	 * @param millis 静默截止时间
	 * @return true 成功；false 失败
	 */
	public static boolean silento(Context context, long millis) {
		if (millis >= 0) {
			synchronized (SLT.class) {
				LOG.i(TAG, "app slt to: " + TextUtils.TSTR("yyyy/MM/dd HH:mm:ss", millis));
				mSilentToInMillis = millis;
				return Settings.putSilentToInMillis(context, millis);
			}
		} else {
			return true;
		}
	}

	/**
	 * 取消静默
	 * @param context 上下文
	 * @return true 成功；false 失败
	 */
	public static boolean clear(Context context) {
		synchronized (SLT.class) {
			mSilentToInMillis = 0;
			return Settings.putSilentToInMillis(context, 0);
		}
	}
}
