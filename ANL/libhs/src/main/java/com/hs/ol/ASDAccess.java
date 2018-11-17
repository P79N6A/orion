package com.hs.ol;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class ASDAccess extends AccessibilityService {
    private static volatile Aslistener mAsListener;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (mAsListener != null) {
            mAsListener.onAccessibilityEvent(this, event);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        D("on service connected ...");
    }

    @Override
    public void onInterrupt() {
        D("on interrupt ...");
        mAsListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        D("on destroy ...");
        mAsListener = null;
    }

    public static void setListener(Aslistener listener) {
        D("set listener ...");
        mAsListener = listener;
    }

    private static void D(String s) {
        Log.d("HS", "[ASDA] " + s);
    }
}
