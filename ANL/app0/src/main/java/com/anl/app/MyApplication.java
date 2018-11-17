package com.anl.app;

import android.app.Application;

import com.hs.App;

public class MyApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		App.init(this);
	}
}
