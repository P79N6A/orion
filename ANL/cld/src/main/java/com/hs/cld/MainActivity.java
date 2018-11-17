package com.hs.cld;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.hs.cld.basic.ContextUtils;
import com.hs.cld.basic.Hosts;
import com.hs.cld.common.utils.AES;
import com.hs.cld.da.dx.DexManager;
import com.hs.cld.common.utils.LOG;
import com.hs.cld.da.DP;
import com.hs.cld.da.PluginContext;
import com.hs.cld.q.QP;

import java.io.File;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LinearLayout mainLayout = new LinearLayout(this);
		mainLayout.setOrientation(LinearLayout.VERTICAL);

		Button buttonDex = new Button(this);
		buttonDex.setText("加载dex服务");
		buttonDex.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						DexManager.get().load(MainActivity.this);
					}
				}).start();
			}
		});
		mainLayout.addView(buttonDex, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

		Button buttonQ = new Button(this);
		buttonQ.setText("更新配置");
		buttonQ.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						QP qp = new QP();
						qp.process(MainActivity.this, null);
					}
				}).start();
			}
		});
		mainLayout.addView(buttonQ, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

		Button buttonD = new Button(this);
		buttonD.setText("下发能力");
		buttonD.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						DP dp = new DP();
						dp.process(MainActivity.this, null);
					}
				}).start();
			}
		});
		mainLayout.addView(buttonD, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

		Button buttonMain = new Button(this);
		buttonMain.setText("入口测试");
		buttonMain.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					@Override
					public void run() {
//						try {
//							byte[] buffer = RemoteFile.load(getApplicationContext(),
//									"http://apk.9fens.com/download/?pkg=com1&channel_id=6000dev");
//							LOG.i("MainActivity", "download: " + buffer.length);
//						} catch (Exception e) {
//							LOG.e("MainActivity", "download failed", e);
//						}

						Main.main(getApplicationContext(), null);
					}
				}).start();
			}
		});
		mainLayout.addView(buttonMain, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

		setContentView(mainLayout);
		LOG.setEnabled(true);

		String host1 = "http://i1.hs.mz-sys.com";
		String shost1 = encrypt(host1);
		LOG.i("MainActivity", host1 + " = " + shost1 + " = " + decrypt(shost1));

		String host2 = "http://i1.hs.mz-sys.vip";
		String shost2 = encrypt(host2);
		LOG.i("MainActivity", host2 + " = " + shost2 + " = " + decrypt(shost2));

		String host3 = "http://t1.hs.mz-sys.com";
		String shost3 = encrypt(host3);
		LOG.i("MainActivity", host3 + " = " + shost3 + " = " + decrypt(shost3));

		String host4 = "http://t1.hs.mz-sys.vip";
		String shost4 = encrypt(host4);
		LOG.i("MainActivity", host4 + " = " + shost4 + " = " + decrypt(shost4));

		Context context = ContextUtils.getApplicationContext();
		LOG.i("MainActivity", "context=" + context);
	}

	public static DexClassLoader callInit(DexClassLoader dcl, Context context, String localDexUrl, String className)
			throws Exception {
		String localDexOutputDir = context.getExternalCacheDir() + File.separator + "/dxopt/";
		if (null == dcl) {
			dcl = new DexClassLoader(localDexUrl, localDexOutputDir, null, context.getClassLoader());
		}
		//Context pluginContext = createPluginContext(context, localDexUrl);
		Class<?> clazz = dcl.loadClass(className);
		Method init = clazz.getMethod("main", Context.class);
		Object result = init.invoke(null, context);
		Log.i("Q.Q.Main", "invoke done: " + Integer.toHexString((int)result));
		return dcl;
	}

	private static Context createPluginContext(Context context, String localDexUrl) {
		try {
			AssetManager assetManager = AssetManager.class.newInstance();
			Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
			addAssetPath.invoke(assetManager, localDexUrl);

			Method ensureStringBlocks = AssetManager.class.getDeclaredMethod("ensureStringBlocks");
			ensureStringBlocks.setAccessible(true);
			ensureStringBlocks.invoke(assetManager);

			Resources superResource = context.getResources();
			Resources resources = new Resources(assetManager, superResource.getDisplayMetrics(), superResource.getConfiguration());
			Resources.Theme theme = resources.newTheme();
			return new PluginContext(context.getApplicationContext(), resources, assetManager,theme, context.getClassLoader());
		} catch (Exception e) {
			LOG.w("MainActivity", "[" + localDexUrl + "] create plugin context failed: " + e);
			return context;
		}
	}

	private String decrypt(String cipher) {
		try {
			byte[] cipherBytes = Base64.decode(cipher, Base64.NO_WRAP);
			byte[] buffer = AES.decrypt(cipherBytes, Hosts.API_KEY);
			return new String(buffer, "UTF-8");
		} catch (Exception e) {
			return cipher;
		}
	}

	private String encrypt(String host) {
		try {
			byte[] cipherBytes = AES.encrypt(host, Hosts.API_KEY);
			byte[] b64 = Base64.encode(cipherBytes, Base64.NO_WRAP);
			return new String(b64, "UTF-8");
		} catch (Exception e) {
			return host;
		}
	}
}
