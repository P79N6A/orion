package com.hs.cld.common.dm;

import android.content.Context;

import com.hs.cld.common.http.HTTPBuilder;
import com.hs.cld.common.http.HTTPError;
import com.hs.cld.common.http.HTTPHelper;
import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.SystemUtils;
import com.hs.cld.common.utils.TextUtils;
import com.hs.cld.common.utils.Threadable;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 传输管理器
 *
 */
public class TransferManager {
	/**
	 * 日志标签
	 */
	private final static String TAG = "TM";

	/**
	 * 全局计数器
	 */
	private static AtomicInteger gCounter = new AtomicInteger(1);

	/**
	 * 执行任务列表
	 */
	private final Map<String, AbortableTask> mTasks = new LinkedHashMap<>();

	/**
	 * 任务线程执行器
	 */
	private ExecutorService mTaskWorker = null;

	/**
	 * 回调线程执行器
	 */
	private ExecutorService mListenWorker = null;

	/**
	 * 内部静态类，用于保存静态实例对象
	 * 1）保证线程安全
	 * 2）防止DCL引起的问题
	 * 3）能够实现Lazy Loading
	 *
	 */
	private static class TransferManagerHolder {
		/**
		 * 静态实例对象
		 */
		private static final TransferManager INSTANCE = new TransferManager();
	}

	/**
	 * 构造函数，隐藏
	 */
	private TransferManager() {}
	
	/**
	 * 获取TransferManager实例
	 * @return TransferManager实例对象
	 */
	public static TransferManager get() {
		return TransferManagerHolder.INSTANCE;
	}

	/**
	 * 获取任务线程池
	 * @return 任务线程池
	 */
	private synchronized ExecutorService getTaskWorker() {
		if (null == mTaskWorker) {
			mTaskWorker = Executors.newFixedThreadPool(5);
		}

		return mTaskWorker;
	}

	/**
	 * 获取回调线程池
	 * @return 回调线程池
	 */
	private synchronized ExecutorService getListenWorker() {
		if (null == mListenWorker) {
			mListenWorker = Executors.newSingleThreadExecutor();
		}

		return mListenWorker;
	}
	
	/**
	 * 添加一个下载任务
	 * @param context 上下文对象
	 * @param url 下载文件地址
	 * @param localUrl 本地存储文件地址
	 * @param listener 监听器
	 * @return 传输任务ID
	 */
	public String download(Context context, String url, String localUrl,
			OnTransferListener listener) {
		LOG.d(TAG, "[" + url + "][" + localUrl + "] download ...");
		
		DownloadTask task = new DownloadTask(context, url, localUrl);
		task.setOnTransferListener(listener);
		task.execute(getTaskWorker());

		return addTask(task).ID();
	}
	
	/**
	 * 添加一个上传任务
	 * @param localUrl 上传的本地文件地址
	 * @param listener 监听器
	 * @return 传输任务ID
	 */
    public String upload(String localUrl, OnTransferListener listener) {
    	throw new IllegalStateException("unsupported");
	}
    
    /**
     * 全部任务，如果指定任务ID不存在，则返回false
     * @param taskId 传输任务ID
     * @return 是否成功
     */
    public boolean remove(String taskId) {
    	if (!TextUtils.empty(taskId)) {
			AbortableTask removedTask = removeTask(taskId);

			if (null != removedTask) {
				removedTask.abort();
				return true;
			}
    	}
    	
    	return false;
    }
    
    /**
     * 删除全部任务
     */
    public void removeAll() {
    	Map<String, AbortableTask> removes = removeAllTask();
    	Collection<AbortableTask> removedTasks = removes.values();
    	
    	for (AbortableTask removedTask : removedTasks) {
    		if (null != removedTask) {
				removedTask.abort();
			}
    	}
    }

	/**
	 * 添加一个下载任务
	 * @param task 下载任务
	 * @return 任务对象
	 */
	private AbortableTask addTask(AbortableTask task) {
		synchronized (mTasks) {
			mTasks.put(task.ID(), task);
			return task;
		}
	}

	/**
	 * 移除一个下载任务
	 * @param taskId 下载任务ID
	 * @return 移除的任务对象
	 */
	private AbortableTask removeTask(String taskId) {
		synchronized (mTasks) {
			return mTasks.remove(taskId);
		}
	}

	/**
	 * 移除所有的下载任务
	 * @return 移除的下载对象
	 */
	private Map<String, AbortableTask> removeAllTask() {
		synchronized (mTasks) {
			Map<String, AbortableTask> removes = new LinkedHashMap<>(mTasks);
			mTasks.clear();
			return removes;
		}
	}

	/**
	 * 任务开始回调
	 * @param listener 优先回调监听器
	 * @param taskId 传输任务ID
	 */
	private void notifyBegin(final OnTransferListener listener,
			final String taskId) {
		new Threadable("notifyBegin") {
			@Override
			public void doFire() {
				if (null != listener) {
					listener.onBegin(taskId);
				}
			}
		}.execute(getListenWorker());
	}

	/**
	 * URL重定向回调
	 * @param listener 优先回调监听器
	 * @param taskId 传输任务ID
	 * @param url 重定向URL
	 */
	private void notifyRedirectUrl(final OnTransferListener listener,
			final String taskId, final String url) {
		new Threadable("notifyRedirectUrl") {
			@Override
			public void doFire() {
				if (null != listener) {
					listener.onRedirectUrl(taskId, url);
				}
			}
		}.execute(getListenWorker());
	}
    
    /**
	 * 进度发生变化时回调
	 * @param listener 回调监听器
	 * @param taskId 传输任务ID
	 * @param progress 进度百分比
	 * @param transferred 已传输大小，单位字节
	 * @param total 总大小，单位字节
	 */
    private void notifyProgress(final OnTransferListener listener,
    		final String taskId, final int progress,
    		final long transferred, final long total) {
		new Threadable("notifyProgress") {
			@Override
			public void doFire() {
				if (null != listener) {
					listener.onTransfer(taskId, progress, transferred, total);
				}
			}
		}.execute(getListenWorker());
    }

	/**
	 * 返回结果回调
	 * @param listener 回调监听器
	 * @param taskId 传输任务ID
	 */
	private void notifyFinish(final OnTransferListener listener,
			final String taskId) {
		new Threadable("notifyFinished") {
			@Override
			public void doFire() {
				// 首先清除对应的任务对象
				synchronized (mTasks) {
					mTasks.remove(taskId);
				}

				// 首先通知指定监听器
				if (null != listener) {
					listener.onFinish(taskId);
				}
			}
		}.execute(getListenWorker());
	}
    
    /**
	 * 返回结果回调
	 * @param listener 优先回调监听器
	 * @param taskId 传输任务ID
	 * @param e 错误对象
	 */
    private void notifyException(final OnTransferListener listener,
    		final String taskId, final Exception e) {
    	new Threadable("notifyError") {
			@Override
			public void doFire() {
				// 首先清除对应的任务对象
				synchronized (mTasks) {
					mTasks.remove(taskId);
				}
				
				if (null != listener) {
					listener.onException(taskId, e);
				}
			}
    	}.execute(getListenWorker());
    }

	/**
	 * 取消回调
	 * @param listener 优先回调监听器
	 * @param taskId 传输任务ID
	 */
	private void notifyCancel(final OnTransferListener listener,
			final String taskId) {
		new Threadable("notifyCancel") {
			@Override
			public void doFire() {
				// 首先清除对应的任务对象
				synchronized (mTasks) {
					mTasks.remove(taskId);
				}

				if (null != listener) {
					listener.onCancel(taskId);
				}
			}
		}.execute(getListenWorker());
	}

	/**
	 * 网络错误回调
	 * @param listener 优先回调监听器
	 * @param taskId 传输任务ID
	 */
	private void notifyNetworkError(final OnTransferListener listener,
			final String taskId) {
		new Threadable("notifyNetworkError") {
			@Override
			public void doFire() {
				// 首先清除对应的任务对象
				synchronized (mTasks) {
					mTasks.remove(taskId);
				}

				if (null != listener) {
					listener.onNetworkError(taskId);
				}
			}
		}.execute(getListenWorker());
	}
    
    /**
     * 下载任务
     *
     */
    private class DownloadTask extends AbortableTask implements HTTPHelper.OnBlockListener {
		/**
		 * 上下文对象
		 */
		private final Context mContext;

    	/**
    	 * 下载链接URL
    	 */
    	private final String mRemoteFileUrl;
    	
    	/**
    	 * 本地存储文件URL
    	 */
    	private final String mLocalFileUrl;

		/**
		 * 真实下载地址
		 */
		private String mRealUrl = null;

		/**
		 * 下载文件的总大小
		 */
		private long mTotalBytes = 0;

		/**
		 * 已经下载的大小（包括断点续传）
		 */
		private long mTransferred = 0;

		/**
		 * 文件访问器
		 */
		private RandomAccessFile mFileAccessor = null;
    	
    	/**
    	 * 构造函数
    	 * @param url 下载链接URL
    	 * @param localUrl 本地存储文件URL
    	 */
		private DownloadTask(Context context, String url, String localUrl) {
			super("downloadTask");
			this.mContext = context;
    		this.mRemoteFileUrl = url;
			this.mRealUrl = url;
    		this.mLocalFileUrl = localUrl;
    	}
    	
		@Override
		public void doFire() {
			LOG.i(TAG, "[ID:" + mTaskId + "] download: url=" + mRemoteFileUrl + ", dir=" + mLocalFileUrl);

			if (mAborted) {
				LOG.i(TAG, mLogContext + " task abort ...");
				return;
			}

			// 开始回调
			onBegin();

			// 如果下载文件存在，则直接返回
			if ((new File(mLocalFileUrl)).exists()) {
				LOG.i(TAG, mLogContext + " file exist ...");
				onFinish();
				return;
			}

			// 开始执行，如果下载取消，则不回调
			try {
				if (tryDownload()) {
					onFinish();
				}
			} catch (Exception e) {
				if (mAborted) {
					onCancel();
				} else {
					onException(e);
				}
			}
		}

		/**
		 * 尝试下载，并在尽量重试
		 * @return true 下载成功；false 下载失败，但是内部已经做了处理
		 * @throws Exception 下载异常
		 */
		private boolean tryDownload() throws Exception {
			// 判断是否取消
			if (mAborted) {
				LOG.i(TAG, mLogContext + " task abort ...");
				onCancel();
				return false;
			}

			// 判断网络是否可用
			if (!SystemUtils.isNetworkAvailable(mContext)) {
				onNetworkError();
				return false;
			}

			for (int i = 0; i < 3; i++) {
				// 下载，如果成功直接返回
				if (downloadFromRemote()) {
					return true;
				}

				// 判断是否取消
				if (mAborted) {
					LOG.i(TAG, mLogContext + " task abort ...");
					onCancel();
					return false;
				}

				// 下载失败，如果是网络不可用，则直接退出，不用重试
				// 否则，休眠500毫秒重试
				if (!SystemUtils.isNetworkAvailable(mContext)) {
					onNetworkError();
					return false;
				} else {
					sleep(500);
				}
			}

			throw new Exception("download and retry(2) failed");
		}

		/**
		 * 休眠一段时间
		 * @param millis 毫秒数
		 */
		private void sleep(long millis) {
			try {Thread.sleep(millis);} catch (Exception e) {}
		}

		/**
		 * 下载应用，如果不能重试，则抛出异常
		 * @return true 成功；false 失败
		 * @throws Exception 异常定义
		 */
		private boolean downloadFromRemote() throws Exception {
			try {
				// 如果下载最终文件不存在，创建断点续传中间文件
				File brokenFile = createBrokenFile(mLocalFileUrl);
				long offset = brokenFile.length();
				mFileAccessor = new RandomAccessFile(brokenFile, "rw");

				HTTPHelper helper = HTTPBuilder.build(mContext);
				helper.setBlockListener(this);

				// 断点下载
				long start = System.currentTimeMillis();
				HTTPError he = helper.download(mRemoteFileUrl, offset);
				long millis = (System.currentTimeMillis() - start);

				if (he.failed()) {
					if (canRetryOnError(he)) {
						LOG.e(TAG, mLogContext + " download failed(" + he + "), retry ...");
						return false;
					} else {
						throw new Exception("download failed: " + he);
					}
				}

				close();
				File destFile = convertBrokenFileOnCompleted(mLocalFileUrl);
				LOG.i(TAG, mLogContext + " download done: ms=" + millis + ", len=" + destFile.length() + ", file=" + destFile);
				return true;
			} finally {
				close();
			}
		}

		/**
		 * 判断错误码可以重试
		 * @param he HTTP错误码
		 * @return true 可以重试；false 不可以重试
		 */
		private boolean canRetryOnError(HTTPError he) {
			if (null != he) {
				int errorCode = he.getCode();

				return ((HTTPError.EIOEXCEPTION == errorCode)
						|| (HTTPError.EUNKNOWNHOST == errorCode)
						|| (HTTPError.ECONNRESET == errorCode)
						|| (HTTPError.ECONNECT == errorCode)
						|| (HTTPError.ETIMEOUT == errorCode)
						|| (HTTPError.EBROKENPROTOCOL == errorCode)
						|| (HTTPError.EDATARECEIVED == errorCode));
			}

			return false;
		}
		
		/**
		 * 关闭文件
		 */
		private void close() {
			try {
				if (null != mFileAccessor) {
					mFileAccessor.close();
				}
			} catch (Exception e) {
				LOG.e(TAG, "close file failed(Exception)", e);
			} finally {
				mFileAccessor = null;
			}
		}
		
		/**
		 * 创建断点续传中间文件，格式为
		 * [localUrl].part
		 * 下载完毕后，将中间文件修改为最终文件，即localUrl
		 * @param localUrl 指定下载文件
		 * @return 断点续传中间文件，如果失败为NULL
		 * @throws Exception 异常定义
		 */
		private File createBrokenFile(String localUrl) throws Exception {
			String brokenFileUrl = (localUrl + ".part");
			File brokenFile = new File(brokenFileUrl);
			
			// 如果文件不存在，那么继续判断
			// 如果父目录不存在，首先创建父目录，然后再创建文件
			// 如果父目录存在，直接创建文件
			if (!brokenFile.exists()) {
	        	if(!brokenFile.getParentFile().exists()) {
	        		if (!brokenFile.getParentFile().mkdirs()) {
						throw new Exception("mkdirs failed");
	        		}
	        	}

				if (!brokenFile.createNewFile()) {
					throw new Exception("create file failed");
				}
			}

			return brokenFile;
		}
		
		/**
		 * 成功完成文件下载后的处理过程
		 * 把.part的中间下载文件重命名为正式指定的下载文件名
		 * @param localUrl 下载文件的全路径名
		 * Exception 异常定义
		 */
		private File convertBrokenFileOnCompleted(String localUrl) throws Exception {
			String brokenFileUrl = (localUrl + ".part");
			File brokenFile = new File(brokenFileUrl);
			File destFile = new File(localUrl);

			if (!brokenFile.exists()) {
				throw new Exception("broken file not exist");
			}
			
			// 删除掉原来文件
			if (destFile.exists()) {
				destFile.delete();
			}
			
			if (!brokenFile.renameTo(destFile)) {
				throw new Exception("broken file rename failed");
			}

			return destFile;
		}

		@Override
		public boolean onStart(String url, long offset, long length, long total) {
			this.mRealUrl = url;
			this.mTotalBytes = total;
			this.mTransferred = offset;

			if (mAborted) {
				LOG.i(TAG, mLogContext + " on start abort...");
				return false;
			} else {
				LOG.d(TAG, mLogContext + " on start: range=["
						+ offset + " " + total + " " + length + "], url=" + url);
			}

			// 检查是否存在重定向
			if (!TextUtils.equals(mRealUrl, mRemoteFileUrl)) {
				onRedirectUrl(mRealUrl);
			}

			try {
				mFileAccessor.seek(offset);
				return true;
			} catch (IOException e) {
				LOG.e(TAG, mLogContext + " seek failed(IOException): " + e.getMessage());
			}

			return false;
		}

		@Override
		public boolean onBlock(byte[] buffer, long length) {
			if (mAborted) {
				LOG.i(TAG, mLogContext + " on block abort...");
				return false;
			}

			try {
				mFileAccessor.write(buffer, 0, (int)length);
				mTransferred += length;
				onProgress(mTransferred, mTotalBytes);
				return true;
			} catch (IOException e) {
				LOG.e(TAG, mLogContext + " write file failed: " + e);
			}

			return false;
		}
    }
    
    /**
     * 任务定义
     *
     */
    private abstract class AbortableTask extends Threadable {
    	/**
    	 * 传输任务ID
    	 */
    	protected final String mTaskId;

		/**
		 * 上下文日志
		 */
		protected final String mLogContext;
    	
    	/**
    	 * 标识是否终止
    	 */
		protected boolean mAborted = false;
    	
    	/**
    	 * 监听器
    	 */
    	private OnTransferListener mOnTransferListener = null;
    	
    	/**
    	 * 记录当前进度，调整进度回调的频率，避免过渡回调刷新
    	 */
    	private int mCurProgress = 0;

		/**
		 * 是否通知取消
		 */
		private boolean mCancelNotified = false;

		/**
		 * 构造函数
		 * @param name 线程名
		 */
		public AbortableTask(String name) {
			super(name);
			this.mTaskId = ("" + gCounter.getAndIncrement());
			this.mLogContext = ("[ID:" + mTaskId + "]");
		}

		/**
		 * 获取当前任务的ID
		 * @return 当前任务的ID
		 */
		public String ID() {
			return mTaskId;
		}

		/**
		 * 设置回调接口
		 * @param listener 回调接口
		 */
		public void setOnTransferListener(OnTransferListener listener) {
			this.mOnTransferListener = listener;
		}
    	
    	/**
    	 * 终止任务
    	 */
    	public synchronized void abort() {
			mAborted = true;
    	}

		/**
		 * 任务开始回调
		 */
		public synchronized void onBegin() {
			if (!mAborted) {
				notifyBegin(mOnTransferListener, mTaskId);
			}
		}

		/**
		 * 重定向时回调
		 * @param url 真实URL
		 */
		public synchronized void onRedirectUrl(String url) {
			if (!mAborted) {
				notifyRedirectUrl(mOnTransferListener, mTaskId, url);
			}
		}

		/**
		 * 进度状态回调处理，记录前一次的进度，为了避免过渡频繁回调，这里
		 * 做一个频率控制
		 * 1）进度比之前至少增加一个单位，
		 * 2）进度条百分比小于等于100
		 * @param transferred 当前传输大小
		 * @param total 总大小
		 */
		public synchronized void onProgress(long transferred, long total) {
			if (!mAborted) {
				if (total > 0) {
					int percent = (int)((transferred * 100) / total);
					percent = Math.max(0, percent);
					percent = Math.min(100, percent);

					if ((percent - mCurProgress) >= 1) {
						mCurProgress = percent;
						notifyProgress(mOnTransferListener, mTaskId, mCurProgress, transferred, total);
					}
				}
			}
		}

		/**
		 * 完成任务回调
		 */
		public synchronized void onFinish() {
			if (!mAborted) {
				notifyFinish(mOnTransferListener, mTaskId);
			}
		}
    	
    	/**
    	 * 发生错误回调处理
    	 * @param e 错误信息
    	 */
    	public synchronized void onException(Exception e) {
			if (!mAborted) {
				notifyException(mOnTransferListener, mTaskId, e);
			}
    	}

		/**
		 * 发生错误回调处理
		 */
		public synchronized void onCancel() {
			if (!mCancelNotified) {
				notifyCancel(mOnTransferListener, mTaskId);
				mCancelNotified = true;
			}
		}

		/**
		 * 发生错误回调处理
		 */
		public synchronized void onNetworkError() {
			if (!mAborted) {
				notifyNetworkError(mOnTransferListener, mTaskId);
			}
		}
    }
    
    /**
     * 结果监听器
     *
     */
    public interface OnTransferListener {
		/**
		 * 传输开始
		 * @param id 传输任务ID
		 */
		void onBegin(String id);

		/**
		 * 发生了URL重定向，用于客户截取文件名显示
		 * @param id 任务ID
		 * @param url 真实URL
		 */
		void onRedirectUrl(String id, String url);

    	/**
    	 * 进度发生变化时回调
    	 * @param id 传输任务ID
    	 * @param progress 进度百分比
    	 * @param transferred 已传输大小
		 * @param total 总大小
    	 */
    	void onTransfer(String id, int progress, long transferred, long total);

		/**
		 * 下载完成
		 * @param id 传输任务ID
		 */
		void onFinish(String id);
    	
    	/**
    	 * 下载失败
    	 * @param id 传输任务ID
    	 * @param e 错误对象
    	 */
    	void onException(String id, Exception e);

		/**
		 * 下载任务被取消
		 * @param id 传输任务ID
		 */
		void onCancel(String id);

		/**
		 * 下载过程中网络断开
		 * @param id 传输任务ID
		 */
		void onNetworkError(String id);
    }
}
