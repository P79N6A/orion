package com.hs.cld.common.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * 捕获异常的执行类
 *
 */
public abstract class Threadable implements Runnable {
	/**
	 * 日志标签
	 */
	private final static String TAG = "Threadable";
	
	/**
	 * 该执行对象名称
	 */
	private final String name;
	
	/**
	 * 构造函数
	 * @param name 该执行对象名称
	 */
	public Threadable(String name) {
		this(name, false);
	}

	/**
	 * 构造函数
	 * @param name 该执行对象名称
	 * @param threadNameAppended 是否追加线程的对象名
	 */
	public Threadable(String name, boolean threadNameAppended) {
		this.name = (threadNameAppended ? (name + "@" + Thread.currentThread().getName()) : name);
	}
	
	/**
	 * 获取该执行对象名称
	 * @return 该执行对象名称
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * 执行当前对象
	 */
	public final void execute() {
		new Thread(this).start();
	}
	
	/**
	 * 使用线程执行器执行当前对象
	 * @param executor 线程执行器
	 */
	public final void execute(ExecutorService executor) {
		try {
			if (null == executor) {
				LOG.e(TAG, "[" + name + "] execute failed(uninitialized)");
			} else if (executor.isShutdown() || executor.isTerminated()) {
				LOG.e(TAG, "[" + name + "][" + executor
						+ "] execute failed(shutdowned)");
			} else {
				executor.execute(this);
			}
		} catch (Throwable t) {
			LOG.e(TAG, "[" + name + "][" + executor
					+ "] execute failed(" + t.getClass().getSimpleName()
					+ "): " + t.getMessage());
		}
	}
	
	/**
	 * 使用线程执行器执行当前对象
	 * @param executor 线程执行器
	 * @param delays 延迟执行的时间，单位秒
	 */
	public final ScheduledFuture<?> schedule(ScheduledExecutorService executor, long delays) {
		try {
			if (null == executor) {
				LOG.e(TAG, "[" + name + "] schedule failed(uninitialized)");
			} else if (executor.isShutdown() || executor.isTerminated()) {
				LOG.e(TAG, "[" + name + "][" + executor.hashCode()
						+ "] schedule failed(shutdowned)");
			} else {
				return executor.schedule(this, delays, TimeUnit.SECONDS);
			}
		} catch (Throwable t) {
			LOG.e(TAG, "[" + name + "][" + executor
					+ "] schedule failed(" + t.getClass().getSimpleName()
					+ "): " + t.getMessage());
		}

		return null;
	}
	
	@Override
	public final void run() {
		// 设置线程名称，并保留之前的线程名称
		String preThreadName = setThreadName(name);

		try {
			doFire();
		} catch (Throwable e) {
			LOG.e(TAG, "[" + name + "] run failed(Throwable)", e);
		} finally {
			setThreadName(preThreadName);
		}
	}

	@Override
	public String toString() {
		return "T(" + name + ")";
	}
	
	/**
	 * 设置线程名称
	 * @param tName 指定的线程名称
	 * @return 设置之前的线程名称
	 */
	private String setThreadName(String tName) {
		String preThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(tName);
		return preThreadName;
	}
	
	/**
	 * 主执行对象，需要重载，执行过程中抛出的任何异常都会被捕获
	 */
	protected abstract void doFire();
}
