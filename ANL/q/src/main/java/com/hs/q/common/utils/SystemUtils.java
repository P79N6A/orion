package com.hs.q.common.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.WebSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * 系统接口
 */
public class SystemUtils {
	/**
	 * 日志标签
	 */
	private final static String TAG = "SystemUtils";

	/**
	 * 网络类型定义
	 */
	public static final int NET_NONE = -1;
	public static final int NET_UNKNOWN = 0;
	public static final int NET_WIFI = 1;
	public static final int NET_2G = 2;
	public static final int NET_3G = 3;
	public static final int NET_4G = 4;

	public static String getLang() {
		try {
			return Locale.getDefault().toString();
		} catch (Throwable t) {
			return "";
		}
	}

	/**
	 * 开发者模式是否打开
	 * @param context 上下文
	 * @return true 开启；false 未开启
	 */
	public static boolean isDevModeEnabled(Context context) {
		try {
			return (Secure.getInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 0) > 0);
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * CTS测试是否打开
	 * @param context 上下文
	 * @return true 开启；false 未开启
	 */
	public static boolean isCTSEnabled(Context context) {
		try {
			return readPropertyAsBoolean("persist.sys.cts_state", false);
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * CTA测试是否打开
	 * @param context 上下文
	 * @return true 开启；false 未开启
	 */
	public static boolean isCTAEnabled(Context context) {
		try {
			Class<?> clazz = Class.forName("android.os.BuildExt");
			Field field = clazz.getDeclaredField("IS_CTA");
			field.setAccessible(true);
			return (boolean)field.get(clazz);
		} catch (Throwable t) {
		}
		return false;
	}

	/**
	 * 判断当前是否亮屏
	 * @param context 上下文
	 * @return true 亮屏；false 未亮屏
	 */
	public static boolean isScreenOn(Context context) {
		try {
			PowerManager pwrMgr = (PowerManager)context.getSystemService(
					Context.POWER_SERVICE);

			if (Build.VERSION.SDK_INT >= 20) {
				return pwrMgr.isInteractive();
			} else {
				return pwrMgr.isScreenOn();
			}
		} catch (Throwable t) {
			LOG.e(TAG, "check screen on/off failed: " + t);
		}

		return true;
	}

	/**
	 * 判断网络连接是否可用
	 * @param context 上下文环境
	 * @return true 可用；false 不可用
	 */
	public static boolean isNetworkAvailable(Context context) {
		try {
			ConnectivityManager connMgr = (ConnectivityManager)context.getSystemService(
					Context.CONNECTIVITY_SERVICE);
			NetworkInfo info = connMgr.getActiveNetworkInfo();
			return ((null != info) && info.isAvailable());
		} catch (Throwable t) {
			LOG.e(TAG, "check network available failed: " + t);
		}

		return false;
	}

	/**
	 * 判断WIFI是否连接
	 * @param context 上下文
	 * @return true 连接；false 未连接
	 */
	public static boolean isWIFIAvailable(Context context) {
		try {
			ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(
					Context.CONNECTIVITY_SERVICE);
			NetworkInfo info = connectivityManager.getActiveNetworkInfo();
			return ((null != info) && info.isAvailable()
					&& (info.getType() == ConnectivityManager.TYPE_WIFI));
		} catch (Throwable t) {
			LOG.e(TAG, "check WIFI available failed: " + t);
		}

		return false;
	}

	/**
	 * 获取网络类型
	 * @param context 上下文
	 * @return 当前网络类型
	 */
	public static int getNetworkType(Context context) {
		try {
			ConnectivityManager connMgr = (ConnectivityManager)
					context.getSystemService(Context.CONNECTIVITY_SERVICE);
			if(null != connMgr){
				NetworkInfo info = connMgr.getActiveNetworkInfo();

				if (null == info){
					return NET_NONE;
				} else {
					int type = info.getType();

					if (type == ConnectivityManager.TYPE_WIFI){
						return NET_WIFI;
					} else if (type == ConnectivityManager.TYPE_MOBILE) {
						return typeOf(info.getSubtype());
					} else {
						return NET_UNKNOWN;
					}
				}
			}
		} catch (Throwable t) {
			LOG.e(TAG, "get network type failed: " + t);
		}

		return NET_UNKNOWN;
	}

	/**
	 * 将系统网络类型转为自定义
	 * @param networkType 系统网络类型
	 * @return 自定义网络类型
	 */
	private static int typeOf(int networkType) {
		switch (networkType) {
			case TelephonyManager.NETWORK_TYPE_GPRS:
			case TelephonyManager.NETWORK_TYPE_EDGE:
			case TelephonyManager.NETWORK_TYPE_CDMA:
			case TelephonyManager.NETWORK_TYPE_1xRTT:
			case TelephonyManager.NETWORK_TYPE_IDEN:
				return NET_2G;
			case TelephonyManager.NETWORK_TYPE_UMTS:
			case TelephonyManager.NETWORK_TYPE_EVDO_0:
			case TelephonyManager.NETWORK_TYPE_EVDO_A:
			case TelephonyManager.NETWORK_TYPE_HSDPA:
			case TelephonyManager.NETWORK_TYPE_HSUPA:
			case TelephonyManager.NETWORK_TYPE_HSPA:
			case TelephonyManager.NETWORK_TYPE_EVDO_B:
			case TelephonyManager.NETWORK_TYPE_EHRPD:
			case TelephonyManager.NETWORK_TYPE_HSPAP:
				return NET_3G;
			case TelephonyManager.NETWORK_TYPE_LTE:
				return NET_4G;
			default:
				return NET_UNKNOWN;
		}
	}

	/**
	 * 获取当前网路信息
	 * @param context 上下文对象
	 * @return 当前网路类型
	 */
	public static String getNetworkInfo(Context context) {
		try {
			ConnectivityManager connMgr = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (null != connMgr) {
				NetworkInfo info = connMgr.getActiveNetworkInfo();

				if ((null == info) || (!info.isAvailable())) {
					return "";
				} else {
					return TextUtils.tidy(info.getTypeName() + "(" + info.getType() + ")");
				}
			}
		} catch (Throwable t) {
			LOG.e(TAG, "get network info failed: " + t);
		}

		return "";
	}

	/**
	 * 读取网络描述信息
	 * @param context 上下文环境
	 * @return 网络描述信息
	 */
	public static String getNetworkDescriptor(Context context) {
		try {
			ConnectivityManager connMgr = (ConnectivityManager)context.getSystemService(
					Context.CONNECTIVITY_SERVICE);
			NetworkInfo info = connMgr.getActiveNetworkInfo();

			if (null != info) {
				return ("type=" + info.getType() + "/" + info.getTypeName()
						+ "; subtype=" + info.getSubtype() + "/" + info.getSubtypeName()
						+ "; ext=" + info.getExtraInfo() + "; conn=" + info.isConnected()
						+ "; available=" + info.isAvailable());
			} else {
				return "";
			}
		} catch (Throwable t) {
			LOG.e(TAG, "get network info failed: " + t);
		}

		return "";
	}

	/**
	 * 获取当前设备类型，这里默认为"Android"
	 * @return 当前设备类型
	 */
	public static String getOSType() {
		return "android";
	}

	/**
	 * 获取系统软件版本号
	 * @return 系统软件版本号
	 */
	public static String getROMRelease() {
		return (empty(Build.VERSION.INCREMENTAL) ? "" : Build.VERSION.INCREMENTAL);
	}

	/**
	 * 获取Android系统版本号
	 * @return Android系统版本号
	 */
	public static String getOSRelease() {
		return empty(Build.VERSION.RELEASE) ? "" : Build.VERSION.RELEASE;
	}

	/**
	 * 获取Android的SDK版本号
	 * @return Android的SDK版本号
	 */
	public static int getOSDKInt() {
		return Build.VERSION.SDK_INT;
	}

	/**
	 * 获取手机默认SIM卡的IMSI
	 * @param context 上下文对象
	 * @return 手机默认SIM卡的IMSI
	 */
	public static String getDefaultIMSI(Context context) {
		try {
			TelephonyManager telMgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			String IMSI = telMgr.getSubscriberId();
			return (empty(IMSI) ? "" : IMSI);
		} catch (Exception e) {
			LOG.e(TAG, "get default IMSI failed(" + e.getClass().getSimpleName()
					+ "): " + e.getMessage());
		}

		return "";
	}

	/**
	 * 获取手机运营商名称
	 * @param context 上下文对象
	 * @return 手机运营商名称
	 */
	public static int getDefaultOperatorId(Context context) {
		int operatorId = 0;
		String IMSI = getDefaultIMSI(context);

		// 46000和46002是中国移动，46001是中国联通，46003是中国电信
		if (IMSI.startsWith("46000") || IMSI.startsWith("46002")) {
			operatorId = 1;
		} else if (IMSI.startsWith("46001")) {
			operatorId = 3;
		} else if (IMSI.startsWith("46003")) {
			operatorId = 2;
		}

		return operatorId;
	}

	/**
	 * 获取基站ID
	 * @param context 上下文对象
	 * @return 基站ID
	 */
	public static String getCellId(Context context) {
		try {
			TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

			if (TelephonyManager.SIM_STATE_READY == telMgr.getSimState()) {
				CellLocation location = telMgr.getCellLocation();

				if(null != location){
					if (location instanceof GsmCellLocation) {
						GsmCellLocation gsmLocation = (GsmCellLocation) location;
						return ("" + gsmLocation.getCid());
					} else if (location instanceof CdmaCellLocation) {
						CdmaCellLocation cdmaLocation = (CdmaCellLocation) location;
						return ("" + cdmaLocation.getBaseStationId());
					}
				}
			}
		} catch (Exception e) {
			LOG.e(TAG, "get cellular ID failed(" + e.getClass().getSimpleName()
					+ "): " + e.getMessage());
		}

		return "";
	}

	/**
	 * 获取系统Android ID
	 * @param context 应用上下文
	 * @return 系统Android ID
	 */
	public static String getaid(Context context) {
		try {
			return Secure.getString(context.getContentResolver(),
					Secure.ANDROID_ID);
		} catch (Throwable t) {
			LOG.e(TAG, "get android ID failed(" + t.getClass().getSimpleName()
					+ "): " + t.getMessage());
		}

		return "";
	}

	/**
	 * 获取屏幕密度
	 * @param context 应用上下文
	 * @return 屏幕密度
	 */
	public static float getDensity(Context context) {
		try {
			WindowManager wndMgr = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

			if (null != wndMgr) {
				DisplayMetrics metric = new DisplayMetrics();
				wndMgr.getDefaultDisplay().getMetrics(metric);
				return metric.density;
			}
		} catch (Throwable t) {
			LOG.e(TAG, "get screen density failed(" + t.getClass().getSimpleName()
					+ "): " + t.getMessage());
		}

		return 0.0f;
	}

	public static int getDensityDpi(Context context) {
		try {
			DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
			return displayMetrics.densityDpi;
		} catch (Exception e) {
			LOG.e(TAG, "get screen density DPI failed: " + e);
		}

		return 0;
	}

	/**
	 * 获取屏幕尺寸信息，宽和高
	 * @param context 应用上下文
	 * @return 屏幕尺寸信息，宽和高
	 */
	public static Point getScreenXY(Context context) {
		Point point = new Point();

		try {
			WindowManager wndMgr = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
			if (null != wndMgr) {
				Display display = wndMgr.getDefaultDisplay();
				display.getSize(point);
				return point;
			}
		} catch (Exception e) {
			LOG.e(TAG, "get screen XY failed(" + e.getClass().getSimpleName()
					+ "): " + e.getMessage());
		}

		return point;
	}

	/**
	 * 获取当前IP地址
	 * @param context 上下文对象
	 * @return IP地址
	 */
	public static String getIPAddress(Context context) {
		try {
			ConnectivityManager connMgr = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);

			if (null != connMgr) {
				NetworkInfo info = connMgr.getActiveNetworkInfo();

				if ((null != info) && info.isAvailable()) {
					if (ConnectivityManager.TYPE_WIFI == info.getType()) {
						return getIPAddrOnWIFI(context);
					} else {
						return getIPAddrOnMobile();
					}
				}
			}
		} catch (Throwable t) {
			LOG.e(TAG, "get IP failed(" + t.getClass().getSimpleName()
					+ "): " + t);
		}

		return "";
	}

	/**
	 * 获取移动网络IP地址
	 * @return IP地址
	 * @throws SocketException 异常定义
	 */
	private static String getIPAddrOnMobile() throws SocketException {
		Enumeration<NetworkInterface> elements = NetworkInterface.getNetworkInterfaces();

		while (elements.hasMoreElements()) {
			NetworkInterface element = elements.nextElement();
			Enumeration<InetAddress> addrs = element.getInetAddresses();

			if ((null != addrs)) {
				while (addrs.hasMoreElements()) {
					InetAddress addr = addrs.nextElement();

					if ((null != addr) && (!addr.isLoopbackAddress()) && (addr instanceof Inet4Address)){
						return addr.getHostAddress();
					}
				}
			}
		}

		return "";
	}

	/**
	 * 获取WIFI下的IP地址
	 * @param context 上下文
	 * @return IP地址
	 */
	private static String getIPAddrOnWIFI(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		return ip2s(wifiInfo.getIpAddress());
	}

	/**
	 * IP地址转字符串形式
	 * @param i IP地址格式
	 * @return 字符串形式
	 */
	private static String ip2s(int i) {
		return (i & 0xFF ) + "." + ((i >> 8 ) & 0xFF) + "."
				+ ((i >> 16 ) & 0xFF) + "." + ( i >> 24 & 0xFF) ;
	}

	/**
	 * 获取所在应用的版本号
	 * @param context 应用上下文
	 * @return 应用的版本号
	 */
	public static String getSelfVersionName(Context context) {
		try {
			PackageManager pkgMgr = context.getPackageManager();
			PackageInfo info = pkgMgr.getPackageInfo(context.getPackageName(), 0);
			return info.versionName;
		} catch (Throwable t) {
			LOG.e(TAG, "get version name failed(Throwable): " + t);
		}

		return "";
	}

	/**
	 * 获取指定应用的版本号
	 * @param context 应用上下文
	 * @param pkgName 应用包名
	 * @return 应用的版本号
	 */
	public static String getVersionName(Context context, String pkgName) {
		try {
			PackageManager pkgMgr = context.getPackageManager();
			PackageInfo info = pkgMgr.getPackageInfo(pkgName, 0);
			return info.versionName;
		} catch (Throwable t) {
			LOG.e(TAG, "[" + pkgName + "] get version name failed(Throwable): " + t);
		}

		return "";
	}

	/**
	 * 获取所在应用的版本数
	 * @param context 应用上下文
	 * @return 应用的版本数
	 */
	public static int getSelfVersionCode(Context context) {
		try {
			PackageManager pkgMgr = context.getPackageManager();
			PackageInfo info = pkgMgr.getPackageInfo(context.getPackageName(), 0);
			return info.versionCode;
		} catch (Throwable t) {
			LOG.e(TAG, "get version code failed(Throwable): " + t);
		}

		return 0;
	}

	/**
	 * 获取指定应用的版本数
	 * @param context 应用上下文
	 * @param pkgName 应用包名
	 * @return 应用的版本数
	 */
	public static int getVersionCode(Context context, String pkgName) {
		try {
			PackageManager pkgMgr = context.getPackageManager();
			PackageInfo info = pkgMgr.getPackageInfo(pkgName, 0);
			return info.versionCode;
		} catch (Throwable t) {
			LOG.e(TAG, "[" + pkgName + "] get version code failed: " + t);
		}

		return 0;
	}

	/**
	 * 获取Push客户端所在应用的包名
	 * @return 应用的包名
	 */
	public static String getSelfPackageName(Context context) {
		try {
			return context.getPackageName();
		} catch (Throwable t) {
			LOG.e(TAG, "get package name failed: " + t);
		}

		return "";
	}

	/**
	 * 获取客户端标志，包名+版本号
	 * @param context 上下文
	 * @return 应用的版本号
	 */
	public static String getClientName(Context context) {
		try {
			String pkgName = context.getPackageName();
			PackageInfo info = context.getPackageManager().getPackageInfo(pkgName, 0);
			return (pkgName + "/" + info.versionName);
		} catch (Throwable t) {
			LOG.e(TAG, "get client name failed: " + t);
		}

		return "";
	}

	/**
	 * 获取加密的客户端标志，包名+版本号
	 * @param context 上下文
	 * @return 应用的版本号
	 */
	public static String getCN(Context context) {
		return HEX.getCipher(getClientName(context));
	}

	@SuppressLint("NewApi")
	public static String getUserAgent(Context context) {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
				try {
					return WebSettings.getDefaultUserAgent(context);
				} catch (Exception e) {
					return System.getProperty("http.agent");
				}
			} else {
				return System.getProperty("http.agent");
			}
		} catch (Throwable t) {
			LOG.e(TAG, "get user agent failed: " + t);
		}

		return "";
	}

	/**
	 * 获取系统当前的MAC地址
	 * @param context 上下文环境
	 * @return 系统当前的MAC地址
	 */
	public static String getMACAddress(Context context) {
		String addr = "";

		try {
			WifiManager manager = (WifiManager) context.getSystemService(
					Context.WIFI_SERVICE);

			if (null != manager) {
				WifiInfo info = manager.getConnectionInfo();
				String mac = info.getMacAddress();

				if ((null != mac) && (!mac.equals("02:00:00:00:00:00"))) {
					addr = replace(mac, ":", "");
				}
			}

			if (!valid(addr)) {
				NetworkInterface wlan0 = NetworkInterface.getByName("wlan0");
				if (null != wlan0) {
					addr = ConvertMacByte2String(wlan0.getHardwareAddress());
				}
			}
		} catch (Throwable t) {
			LOG.e(TAG, "get mac address failed: " + t);
		}

		return addr;
	}

	/**
	 * 获取已安装的应用列表
	 * @param context 上下文
	 * @return 已安装的应用列表
	 */
	public static List<String> getInstallApps(Context context) {
		List<String> pkgNames = new ArrayList<String>();

		try {
			List<PackageInfo> pkgInfos = context.getPackageManager().getInstalledPackages(0);

			if ((null != pkgInfos) && (pkgInfos.size() > 0)) {
				for (PackageInfo pkgInfo: pkgInfos) {
					pkgNames.add(pkgInfo.packageName);
				}
			}
		} catch (Throwable t) {
			LOG.w(TAG, "get install apps failed: " + t);
		}

		return pkgNames;
	}

	/**
	 * 获取已安装的应用列表
	 * @param context 上下文
	 * @return 已安装的应用列表
	 */
	public static List<String> getEnabledInstallApps(Context context) {
		List<String> pkgNames = new ArrayList<String>();

		try {
			List<PackageInfo> pkgInfos = context.getPackageManager().getInstalledPackages(0);

			if ((null != pkgInfos) && (pkgInfos.size() > 0)) {
				for (PackageInfo pkgInfo: pkgInfos) {
					if (pkgInfo.applicationInfo.enabled) {
						pkgNames.add(pkgInfo.packageName);
					}
				}
			}
		} catch (Throwable t) {
			LOG.w(TAG, "get enabled install apps failed: " + t);
		}

		return pkgNames;
	}

	/**
	 * 获取已安装并且未禁用的应用列表
	 * @param context 上下文
	 * @return 已安装并且未禁用的应用列表
	 */
	public static List<String> getEnabledApps(Context context) {
		List<String> pkgNames = new ArrayList<String>();

		try {
			List<PackageInfo> pkgInfos = context.getPackageManager().getInstalledPackages(0);

			if ((null != pkgInfos) && (!pkgInfos.isEmpty())) {
				for (PackageInfo pkgInfo: pkgInfos) {
					if (pkgInfo.applicationInfo.enabled) {
						pkgNames.add(pkgInfo.packageName);
					}
				}
			}
		} catch (Throwable t) {
			LOG.e(TAG, "get enabled apps failed: " + t);
		}

		return pkgNames;
	}

	/**
	 * MAC地址从二进制数组转换为字符串
	 * @param bytes MAC地址二进制数组
	 * @return 字符串形式
	 */
	private static String ConvertMacByte2String(byte[] bytes) {
		StringBuffer buffer = new StringBuffer();

		if (null != bytes) {
			for (int i = 0; i < bytes.length; i++) {
				buffer.append(String.format("%02x", (0xFF & bytes[i])));
			}
		}

		return buffer.toString();
	}

	/**
	 * 检查ID是否有效，非空并且不是全0字符串
	 * @param id ID字符串
	 * @return true 有效的；false 无效的
	 */
	private static boolean valid(String id) {
		return ((!empty(id)) && (!id.matches("[0]+")));
	}

	/**
	 * 将字符串中所有指定子字符串替换成指定字符串
	 * @param s 字符串
	 * @param regular 指定子字符串
	 * @param replaced 指定字符串
	 * @return 替换后的字符串
	 */
	private static String replace(String s, String regular, String replaced) {
		if (!empty(s)) {
			return s.replaceAll(regular, replaced);
		} else {
			return s;
		}
	}

	/**
	 * 判断字符串是否为空
	 * @param s 字符串
	 * @return true 空；false 非空
	 */
	private static boolean empty(String s) {
		return ((null == s) || (s.length() <= 0));
	}

	/**
	 * 获取系统属性
	 * @param key 对应关键字
	 * @param defaultValue 默认值
	 * @return 系统属性值
	 */
	private static boolean readPropertyAsBoolean(String key, boolean defaultValue) {
		try {
			Class<?> clazz = Class.forName("android.os.SystemProperties");
			Method method = clazz.getDeclaredMethod("getBoolean", String.class, boolean.class);
			method.setAccessible(true);
			return (boolean) method.invoke(null, key, defaultValue);
		} catch (Throwable t) {
		}
		return defaultValue;
	}

	/**
	 * 获取系统属性
	 * @param key 对应关键字
	 * @param defaultValue 默认值
	 * @return 系统属性值
	 */
	private static String readPropertyAsString(String key, String defaultValue) {
		try {
			Class<?> clazz = Class.forName("android.os.SystemProperties");
			Method method = clazz.getDeclaredMethod("get", String.class);
			method.setAccessible(true);
			return (String) method.invoke(null, key);
		} catch (Throwable t) {
		}
		return defaultValue;
	}
}
