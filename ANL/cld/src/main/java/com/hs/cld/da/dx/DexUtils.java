package com.hs.cld.da.dx;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.TextUtils;
import com.hs.cld.da.PluginContext;

import java.io.File;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class DexUtils {
	private final static String TAG = "DexUtils";

	/**
	 * 执行指定dex文件中init接口
	 * @param context 上下文
	 * @param invocation 本地dex文件及调用参数
	 * @return 调用返回值
	 * @throws Exception 异常定义
	 */
	public static String callInit(Context context, Invocation invocation)
			throws Exception {
		if (TextUtils.empty(invocation.mClassName)) {
			throw new Exception("empty class name");
		}

		if (TextUtils.empty(invocation.mInitMethod)) {
			throw new Exception("empty init method");
		}

		String localDexUrl = invocation.mLocalDexFile.getAbsolutePath();
		String localDexOutputDir = getLocalDexOutputDir(invocation.mLocalDexFile);
		DexClassLoader dexClassLoader = new DexClassLoader(localDexUrl, localDexOutputDir, null, context.getClassLoader());
		Context pluginContext = createPluginContext(context, localDexUrl);
		Class<?> clazz = dexClassLoader.loadClass(invocation.mClassName);
		Method init = clazz.getMethod(invocation.mInitMethod, Context.class, String.class, String.class, Context.class);
		String result = stringOf(init.invoke(null, context, localDexUrl, localDexOutputDir, pluginContext));
		LOG.i(TAG, "[" + invocation + "] invoke init(" + invocation.mInitMethod + ") done: " + result);
		return result;
	}

	public static String getLocalDexOutputDir(File localDexFile) throws Exception {
		String localDexOutputDir = (localDexFile.getParent() + "/.opt/" + System.currentTimeMillis());
		File localDexOutputDirFile = new File(localDexOutputDir);

		if (!localDexOutputDirFile.exists()) {
			localDexOutputDirFile.mkdirs();
		}

		return localDexOutputDir;
	}

	public static DexClassLoader getDexClassLoader(Context context, String localDexFileUrl, String localDexOutputDir) {
		return new DexClassLoader(localDexFileUrl, localDexOutputDir, null, context.getClassLoader());
	}

	/**
	 * 执行指定dex文件中init接口
	 * @param context 上下文
	 * @param invocation 本地dex文件及调用参数
	 * @return 调用返回值
	 * @throws Exception 异常定义
	 */
	public static String callInit(Context context, DexClassLoader dsl, Invocation invocation, String localDexOutputDir)
			throws Exception {
		if (TextUtils.empty(invocation.mClassName)) {
			throw new Exception("empty class name");
		}

		if (TextUtils.empty(invocation.mInitMethod)) {
			throw new Exception("empty init method");
		}

		String localDexUrl = invocation.mLocalDexFile.getAbsolutePath();
		Context pluginContext = createPluginContext(context, localDexUrl);

		Class<?> clazz = dsl.loadClass(invocation.mClassName);
		Method init = clazz.getDeclaredMethod(invocation.mInitMethod, Context.class, String.class, String.class, Context.class);
		String result = stringOf(init.invoke(null, context, localDexUrl, localDexOutputDir, pluginContext));
		LOG.i(TAG, "[" + invocation + "] invoke init(" + invocation.mInitMethod + ") done: " + result);
		return result;
	}

	/**
	 * 执行指定dex文件中uninit接口
	 * @param context 上下文
	 * @param invocation 本地dex文件及调用参数
	 * @return 调用返回值
	 * @throws Exception 异常定义
	 */
	public static String callUninit(Context context, DexClassLoader dsl, Invocation invocation, String localDexOutputDir)
			throws Exception {
		try {
			if ((!TextUtils.empty(invocation.mClassName)) && (!TextUtils.empty(invocation.mUninitMethod))) {
				String localDexUrl = invocation.mLocalDexFile.getAbsolutePath();
				Context pluginContext = createPluginContext(context, localDexUrl);
				Class<?> clazz = dsl.loadClass(invocation.mClassName);
				Method uninit = clazz.getDeclaredMethod(invocation.mUninitMethod, Context.class, String.class, String.class, Context.class);
				String result = stringOf(uninit.invoke(null, context, localDexUrl, localDexOutputDir, pluginContext));
				LOG.i(TAG, "[" + invocation + "] invoke uninit(" + invocation.mUninitMethod + ") done: " + result);
				return result;
			} else {
				LOG.i(TAG, "[" + invocation + "] invoke uninit ignore ...");
				return "ignore";
			}
		} catch (Throwable t) {
			throw new Exception("invoke uninit(" + invocation.mUninitMethod + ") failed", t);
		}
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
			return new PluginContext(context.getApplicationContext(), resources, assetManager, theme, context.getClassLoader());
		} catch (Exception e) {
			LOG.w(TAG, "[" + localDexUrl + "] create plugin context failed: " + e);
			return context;
		}
	}

	/**
	 * 将结果转换为字符串
	 * @param o 结果对象
	 * @return 字符串
	 */
	private static String stringOf(Object o) {
		try {
			if (null != o) {
				return String.valueOf(o);
			}
		} catch (Exception e) {
			// nothing to do
		}

		return "";
	}
}
