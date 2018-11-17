package com.hs.cld.basic;

import android.content.Context;

import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.Threadable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TaskManager {
	/**
	 * 日志标识
	 */
	private final static String TAG = "TaskManager";

	/**
	 * 当前正在运行的主任务
	 */
	private static volatile HashSet<Threadable> gMainTasks = new HashSet<Threadable>();

	/**
	 * 任务完成的回调
	 */
	public interface OnTaskListener {
		/**
		 * 任务完成的回调方法
		 */
		void onFinished();
	}

	public static void process(Context context, OnTaskListener listener) {
		synchronized (gMainTasks) {
			if (gMainTasks.isEmpty()) {
				new TasksProcessor(context, listener)
						.execute(ThreadManager.getCoreExecutor());
			} else {
				LOG.i(TAG, "there's main task running, ignore ...");
			}
		}
	}

	private static void onMainTaskBegin(Threadable t) {
		if (null != t) {
			synchronized (gMainTasks) {
				gMainTasks.add(t);
			}
		}
	}

	private static void onMainTaskFinish(Threadable t, OnTaskListener listener) {
		if (null != t) {
			synchronized (gMainTasks) {
				gMainTasks.remove(t);
			}
		}

		callbackFinished(listener);
	}

	private static void callbackFinished(OnTaskListener listener) {
		try {
			if (null != listener) {
				listener.onFinished();
			}
		} catch (Throwable t) {
			LOG.e(TAG, "callback finished failed", t);
		}
	}

	private static class TasksProcessor extends Threadable {
		private final Context mContext;
		private final OnTaskListener mOnTaskListener;

		private TasksProcessor(Context context, OnTaskListener listener) {
			super("tasks processor");
			this.mContext = context.getApplicationContext();
			this.mOnTaskListener = listener;
		}

		@Override
		protected void doFire() {
			try {
				onMainTaskBegin(this);
				long start = System.currentTimeMillis();
				processAllTasks();
				long millis = (System.currentTimeMillis() - start);
				LOG.i(TAG, "process all tasks done: " + millis + "MS");
			} catch (Throwable t) {
				LOG.e(TAG, "process all tasks failed", t);
			} finally {
				onMainTaskFinish(this, mOnTaskListener);
			}
		}

		private void processAllTasks() {
			Map<String, Class<? extends Processor>> processors = Tasks.getProcessors(mContext);

			if (null != processors) {
				LOG.i(TAG, "start processors: " + processors.keySet());
				Set<Map.Entry<String, Class<? extends Processor>>> entries = processors.entrySet();

				for (Map.Entry<String, Class<? extends Processor>> entry: entries) {
					processTask(entry.getKey(), entry.getValue());
				}
			}
		}

		private void processTask(String id, Class<? extends Processor> clazz) {
			try {
				Processor processor = clazz.newInstance();
				long start = System.currentTimeMillis();
				processor.process(mContext, null);
				long millis = (System.currentTimeMillis() - start);
				LOG.i(TAG, "[" + id + "][" + clazz + "] process done: " + millis + "MS");
			} catch (Throwable t) {
				LOG.e(TAG, "[" + id + "][" + clazz + "] process failed", t);
			}
		}
	}
}
