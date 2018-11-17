package com.hs.cld;

import android.content.Context;

import com.hs.cld.basic.Builder;
import com.hs.cld.basic.Settings;
import com.hs.cld.common.PROP;
import com.hs.cld.common.utils.LOG;

import java.util.Arrays;

public class Main {
    public static int main(Context context, String[] args) {
        long st = System.currentTimeMillis();
        if (PROP.isLogEnabled()) {
            LOG.setEnabled(true);
        } else {
            LOG.setEnabled(Settings.isLogEnabled(context, false));
        }

        LOG.i("CLD.Main", "main in: v=" + Builder.VERSION + " ctx=" + context + " args=" + Arrays.toString(args));
        MainWorker.get().start(context);
        LOG.i("CLD.Main", "main out: ms=" + (System.currentTimeMillis() - st));
        return 1;
    }
}
