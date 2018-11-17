package com.hs.cld.basic;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Method;

public class ContextUtils {
	public static Context getApplicationContext() {
		try {
			Class<?> clazz = Class.forName("android.app.ActivityThread");
			Method currentApplication = clazz.getDeclaredMethod("currentApplication");
			Application application = (Application)currentApplication.invoke(null);
			return application.getApplicationContext();
		} catch (Throwable t) {
			return null;
		}
	}
}
