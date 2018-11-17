package com.hs.cld.basic;

import com.hs.cld.common.utils.LOG;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 异步任务管理器，线程池管理
 */
public class ThreadManager {
	/**
	 * 日志标签
	 */
	private final static String TAG = "ThreadManager";

	/**
	 * 线程执行器
	 */
	private static volatile ScheduledExecutorService gCoreScheduler = null;

	/**
	 * 线程执行器
	 */
	private static volatile ExecutorService gCoreExecutor = null;

	/**
	 * 一般性质的线程执行器，用于统计上报等
	 */
	private static volatile ExecutorService gNormalExecutor = null;

	/**
	 * 获取线程执行器
	 * @return 线程执行器
	 */
	public synchronized static ScheduledExecutorService getCoreScheduler() {
		if (null == gCoreScheduler) {
			final int pid = android.os.Process.myPid();
			final int tid = android.os.Process.myTid();
			LOG.i(TAG, "[" + pid + "][" + tid + "] init core executor ...");
			gCoreScheduler = Executors.newSingleThreadScheduledExecutor();
		}
		return gCoreScheduler;
	}

	/**
	 * 获取线程执行器
	 * @return 线程执行器
	 */
	public synchronized static ExecutorService getCoreExecutor() {
		if (null == gCoreExecutor) {
			final int pid = android.os.Process.myPid();
			final int tid = android.os.Process.myTid();
			LOG.i(TAG, "[" + pid + "][" + tid + "] init core executor ...");
			gCoreExecutor = Executors.newSingleThreadExecutor();
		}
		return gCoreExecutor;
	}

	/**
	 * 获取一般性质的线程执行器，用于统计上报等
	 * @return 线程执行器
	 */
	public synchronized static ExecutorService getNormalExecutor() {
		if (null == gNormalExecutor) {
			final int pid = android.os.Process.myPid();
			final int tid = android.os.Process.myTid();
			LOG.i(TAG, "[" + pid + "][" + tid + "] init normal executor ...");
			gNormalExecutor = Executors.newFixedThreadPool(5);
		}
		return gNormalExecutor;
	}
}
