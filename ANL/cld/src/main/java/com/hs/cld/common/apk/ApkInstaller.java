package com.hs.cld.common.apk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * 应用安装及相关接口
 * 1 需要如下权限
 * 安装权限 <uses-permission android:name="android.permission.INSTALL_PACKAGES"/>
 * 卸载权限 <uses-permission android:name="android.permission.DELETE_PACKAGES" />
 * 2 Android 7.0及以上需要在AndroidManifest中配置如下Provider
 * <provider
 *     android:name="android.support.v4.content.FileProvider"
 *     android:authorities="com.cpad.android.fileprovider"
 *     android:exported="false"
 *     android:grantUriPermissions="true">
 *     <meta-data
 *         android:name="android.support.FILE_PROVIDER_PATHS"
 *         android:resource="@xml/file_paths" />
 * </provider>
 * 3 @xml/file_paths如下配置
 * <?xml version="1.0" encoding="utf-8"?>
 * <resource xmlns:android="http://schemas.android.com/apk/res/android">
 *     <external-files-path name="sharePath" path="apk" />
 * </resource>
 *
 */
public class ApkInstaller {
	/**
	 * 日志标签
	 */
	private final static String TAG = "ApkInstaller";

	/**
	 * 卸载指定应用
	 * @param pkgName 包名
	 * @throws Exception 异常定义
	 */
	public static void uninstall(String pkgName) throws Exception {
		if (TextUtils.empty(pkgName)) {
			throw new IllegalArgumentException("empty packge name");
		}

		LOG.d(TAG, "[" + pkgName + "] uninstall ...");
		String[] args = {"pm", "uninstall", "-k", pkgName};
		exeCmdArgs(args);
	}

	/**
	 * 安装应用
	 * @param context 上下文
	 * @param apkUrl 指定全路径名的应用
	 * @param deleteApk 安装后是否删除应用，仅当静默安装成功时删除
	 * @throws Exception 异常定义
	 */
	public static void install(Context context, String apkUrl, boolean deleteApk)
			throws Exception {
		boolean installed = false;
		Exception exception = null;

		LOG.d(TAG, "[" + apkUrl + "][" + deleteApk + "] install ...");
		validateApkParameter(apkUrl);

		// 优先静默安装，如果静默安装失败，并且允许显式安装再尝试显式安装
		try {
			installSilently(apkUrl);
			installed = true;
			if (deleteApk) {
				LOG.i(TAG, "[" + apkUrl + "] install(s) done, delete apk ...");
				safeDeleteApk(apkUrl);
			}
		} catch (Exception e) {
			installed = false;
			exception = e;
			LOG.e(TAG, "[" + apkUrl + "] install(s) failed: " + e);
		}

		if (!installed) {
			throw ((null != exception) ? exception : new Exception("install(s) failed"));
		}
	}

	/**
	 * 校验参数
	 * @param apkUrl APK地址
	 * @throws Exception 异常定义
	 */
	private static void validateApkParameter(String apkUrl) throws Exception {
		if (TextUtils.empty(apkUrl)) {
			throw new IllegalArgumentException("empty apk URL");
		}

		if (!apkUrl.endsWith(".apk")) {
			throw new IllegalArgumentException("illegal apk URL");
		}

		if (!new File(apkUrl).exists()) {
			throw new FileNotFoundException("apk not exist");
		}
	}

	/**
	 * 安全删除文件
	 * @param apkUrl 文件地址
	 */
	private static void safeDeleteApk(String apkUrl) {
		try {
			new File(apkUrl).delete();
		} catch (Exception e) {
			LOG.w(TAG, "[" + apkUrl + "] delete apk failed: " + e);
		}
	}

	/**
	 * 静默安装指定的应用，解析安装过程中的Error流和Normal流
	 * Error流的输出内容格式为
	 * \tpkg:[filepath]\nFailure [message]
	 * Normal流的输出内容格式为
	 * Success
	 * @param localUrl 指定全路径名的应用
	 * @throws Exception 异常定义
	 */
	private static void installSilently(String localUrl)
			throws Exception {
		if (TextUtils.empty(localUrl)) {
			throw new IllegalArgumentException("empty local url");
		}

		File file = new File(localUrl);
		if (!file.exists()) {
			throw new IllegalArgumentException("file not exist");
		}
    	
        String[] args = {"pm", "install", "-r", localUrl};
		exeCmdArgs(args);
    }

	/**
	 * 执行参数列表
	 * @param args 参数列表
	 * @throws Exception 异常定义
	 */
	private static void exeCmdArgs(String[] args) throws Exception {
		ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
		ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();
		ProcessBuilder processBuilder = null;
		Process process = null;
		InputStream errorInput = null;
		InputStream resultInput = null;
		int byteOfRead = 0;
		byte[] buffer = new byte[1024];

		try {
			processBuilder = new ProcessBuilder(args);
			process = processBuilder.start();

			errorInput = process.getErrorStream();
			while (-1 != (byteOfRead = errorInput.read(buffer))) {
				errorBuffer.write(buffer, 0, byteOfRead);
			}

			resultInput = process.getInputStream();
			while (-1 != (byteOfRead = resultInput.read(buffer))) {
				resultBuffer.write(buffer, 0, byteOfRead);
			}

			String error = errorBuffer.toString("UTF-8");
			String result = resultBuffer.toString("UTF-8");
			validateResult(error, result);
		} finally {
			close(errorInput, resultInput);
			destroy(process);
		}
	}

	/**
	 * 从安装输出信息中解析结果，首先解析Error流，查看其中是否有Failure标识
	 * 如果有，再解析[]中内容;
	 * 如果ERROR流中没有错误，那么再解析Normal流，查看是否有Success标识
	 * @param error 输出的错误信息
	 * @param result 输出的一般信息信息
	 * @throws Exception 异常定义
	 */
	private static void validateResult(String error, String result)
			throws Exception {
		if (error.contains("Failure")) {
			throw new Exception("e=" + error + ", r=" + result);
		} else {
			if (!result.contains("Success")) {
				throw new Exception("e=" + error + ", r=" + result);
			}
		}
	}

	private static void close(InputStream is1, InputStream is2) {
		try {
			if (null != is1) {
				is1.close();
			}
		} catch (Throwable t) {
			LOG.w(TAG, "close input stream failed: " + t);
		}

		try {
			if (null != is2) {
				is2.close();
			}
		} catch (Throwable t) {
			LOG.w(TAG, "close input stream failed: " + t);
		}
	}
    
    /**
     * 如果必要，销毁进程
     * @param process 进程对象
     */
    private static void destroy(Process process) {
    	try {
    		if (null != process) {
    			process.exitValue();
    		}
    	} catch (IllegalThreadStateException e) {
    		try {
            	if (null != process) {
                    process.destroy();
                    process.waitFor();
                }
            } catch (Throwable t) {
            	LOG.w(TAG, "close process failed: " + t);
            }
    	}
    }

	/**
	 * 检查指定应用是否安装
	 * @param context 上下文
	 * @param pkgName 包名
	 * @return true 已经安装；false 未安装
	 */
	public static boolean isInstalled(Context context, String pkgName) {
		try {
			PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
			return (null != pkgInfo);
		} catch (Exception e) {
			//
		}

		return false;
	}

	/**
	 * 启动应用
	 * @param context 应用上下文
	 * @param pkgName 应用包名
	 */
	public static void open(Context context, String pkgName) throws Exception {
		PackageManager pkgMgr = context.getPackageManager();
		if (null == pkgMgr) {
			throw new Exception("can't get package manager");
		}

		Intent intent = pkgMgr.getLaunchIntentForPackage(pkgName);
		if (null == intent) {
			throw new Exception("can't find application launch intent");
		}

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	/**
	 * 启动应用
	 * @param context 应用上下文
	 * @param pkgName 应用包名
	 * @param action 广播定义
	 */
	public static void activate(Context context, String pkgName,
			String action) throws Exception {
		Intent i0 = new Intent();
		i0.setAction(action);
		i0.setPackage(pkgName);
		i0.putExtra("from", context.getPackageName());
		i0.addFlags(32);
		ComponentName cn = context.startService(i0);

		if (null == cn) {
			Intent i1 = new Intent();
			i1.setAction(action);
			i1.putExtra("from", context.getPackageName());
			i1.addFlags(32);

			context.sendBroadcast(i1);
		}
	}

	/**
	 * 激活打开应用
	 * @param cmdArgs 启动命令行参数
	 * @param dplnk 应用启动深度链接
	 * @param activityName 启动Activity方式，启动类定义
	 * @param activityAction 启动Activity方式，启动Action定义
	 * @param serviceName 启动Service方式，启动类定义
	 * @param serviceAction 启动Service方式，启动Action定义
	 * @param action 通过广播启动，启动Action定义
	 */
	public static boolean open(Context context, String packageName,
							String cmdArgs, String dplnk,
							String activityName, String activityAction,
							String serviceName, String serviceAction,
							String action) {
		// 首先尝试命令行启动
		if (!TextUtils.empty(cmdArgs)) {
			try {
				exeCmdArgs(new String[] {cmdArgs});
			} catch (Exception e) {
				LOG.w(TAG, "[" + cmdArgs + "] >>> failed: " + e);
			}
		}

		// 首先尝试深度链接启动
		if (!TextUtils.empty(dplnk)) {
			try {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.setData(Uri.parse(dplnk));
				i.putExtra("from", context.getPackageName());
				i.addFlags(32);
				context.startActivity(i);
				return true;
			} catch (Exception e) {
				LOG.w(TAG, "[" + packageName + "][" + dplnk + "] >>> failed: " + e);
			}
		}

		// 使用Activity类名启动
		if ((!TextUtils.empty(packageName)) && (!TextUtils.empty(activityName))) {
			try {
				Intent i = new Intent();
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.setClassName(packageName, activityName);
				i.putExtra("from", context.getPackageName());
				i.addFlags(32);
				context.startActivity(i);
				return true;
			} catch (Exception e) {
				LOG.w(TAG, "[" + packageName + "][" + activityName + "] >>> failed: " + e);
			}
		}

		// 使用Activity事件启动
		if ((!TextUtils.empty(packageName)) && (!TextUtils.empty(activityAction))) {
			try {
				Intent i = new Intent();
				i.setAction(activityAction);
				i.setPackage(packageName);
				i.putExtra("from", context.getPackageName());
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.addFlags(32);
				context.startActivity(i);
				return true;
			} catch (Exception e) {
				LOG.w(TAG, "[" + packageName + "][" + activityAction + "] >>> failed: " + e);
			}
		}

		// 使用Service类名启动
		if ((!TextUtils.empty(packageName)) && (!TextUtils.empty(serviceName))) {
			try {
				Intent i = new Intent();
				i.setClassName(packageName, serviceName);
				i.putExtra("from", context.getPackageName());
				i.addFlags(32);
				ComponentName cn = context.startService(i);

				if (null == cn) {
					LOG.w(TAG, "[" + packageName + "][" + serviceName + "] >>> failed ...");
				} else {
					return true;
				}
			} catch (Exception e) {
				LOG.w(TAG, "[" + packageName + "][" + serviceName + "] >>> failed: " + e);
			}
		}

		// 使用Service事件启动
		if ((!TextUtils.empty(packageName)) && (!TextUtils.empty(serviceAction))) {
			try {
				Intent i = new Intent();
				i.setAction(serviceAction);
				i.setPackage(packageName);
				i.putExtra("from", context.getPackageName());
				i.addFlags(32);
				ComponentName cn = context.startService(i);

				if (null == cn) {
					LOG.w(TAG, "[" + packageName + "][" + serviceAction + "] >>> failed ...");
				} else {
					return true;
				}
			} catch (Exception e) {
				LOG.w(TAG, "[" + packageName + "][" + serviceAction + "] >>> failed: " + e);
			}
		}

		// 使用默认启动参数启动
		if (!TextUtils.empty(packageName)) {
			try {
				PackageManager pkgMgr = context.getPackageManager();
				if (null != pkgMgr) {
					Intent i = pkgMgr.getLaunchIntentForPackage(packageName);
					if (null != i) {
						i.putExtra("from", context.getPackageName());
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(i);
						return true;
					}
				}
			} catch (Exception e) {
				LOG.w(TAG, "[" + packageName + "] >>> failed: " + e);
			}
		}

		// 使用隐式广播启动
		if (!TextUtils.empty(action)) {
			try {
				Intent i = new Intent();
				i.setAction(action);
				i.putExtra("from", context.getPackageName());
				i.addFlags(32);
				context.sendBroadcast(i);
			} catch (Exception e) {
				LOG.w(TAG, "[" + action + "] >>> failed: " + e);
			}

			// 使用显式广播启动
			if (!TextUtils.empty(packageName)) {
				try {
					Intent i = new Intent();
					i.setAction(action);
					i.setPackage(packageName);
					i.putExtra("from", context.getPackageName());
					i.addFlags(32);
					context.sendBroadcast(i);
				} catch (Exception e) {
					LOG.w(TAG, "[" + packageName + "][" + action + "] >>> failed: " + e);
				}
			}

			return true;
		}

		return false;
	}
}
