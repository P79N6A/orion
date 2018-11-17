package com.hs.ol;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public interface Aslistener {
    void onAccessibilityEvent(AccessibilityService asdAccess, AccessibilityEvent event);
}
