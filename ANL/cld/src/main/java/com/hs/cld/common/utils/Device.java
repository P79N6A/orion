package com.hs.cld.common.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

/**
 * 该文件定义了获取Android系统设备主要信息的接口，这些信息接口如下
 * * 获取设备类型，目前默认为"Android"
 * * 获取设备品牌标识
 * * 获取设备型号
 * * 获取设备ID，设备的唯一标识
 * 
 * 其中，设备ID的读取方案如下
 * 1 反射调用TelephonyManager的getImei(0)，如果结果不合法下一步
 * 2 反射调用TelephonyManager的getImei(1)，如果结果不合法下一步
 * 3 反射调用TelephonyManager的getDeviceId(0)，如果结果不合法下一步
 * 4 反射调用TelephonyManager的getDeviceId(1)，如果结果不合法下一步
 * 5 如果以上都不合法，再调用旧的原生接口getDeviceId()
 * 
 * @version 1.0.0 初次创建
 * @version 1.1.0 存储关键修改为向前兼容，尽量保证和原用户管理帐户统一
 *          改进设备ID的生成规则，增加MAC地址的获取等
 * @version 2.0.0 不再向前兼容，并且修改读取设备ID的方案
 * @version 2.0.1 去掉缓存和持久化，兼容Android6.0
 * @version 2.0.2 调整设备ID的读取规则，增加M1和M2读取
 */
public class Device {
    /**
     * 日志标签
     */
    private final static String TAG = "Device";
    
    /**
     * 全局缓存的设备类型
     */
    private static volatile String devmodel = null;

	/**
	 * 全局缓存的设备ID
	 */
	private static volatile String id = null;

	/**
	 * 全局缓存的M1，IMEI号的MD5
	 */
	private static volatile String m1 = null;

	/**
	 * 获取当前设备类型，手机或者平板，代码来自 Google I/O App for Android
	 * @return 当前设备类型
	 */
	public static int getType(Context context) {
		try {
			Resources res = context.getResources();
			Configuration config = res.getConfiguration();
			return (((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE) ? 5 : 4);
		} catch (Throwable t) {
			LOG.e(TAG, "get device type failed: " + t);
		}

		return 4;
	}

	/**
	 * 获取手机设备名称：厂商_品牌商_机型
	 * @return 手机设备名称
	 */
	public static String getName() {
		String manufacturer = tidy(Build.MANUFACTURER);
		String brand = tidy(Build.BRAND);
		String model = tidy(Build.MODEL);

		manufacturer = ((empty(manufacturer)) ? "NONE" : manufacturer);
		brand = ((empty(brand)) ? "NONE" : brand);
		model = ((empty(model)) ? "NONE" : model);

		return (manufacturer + "_" + brand + "_" + model);
	}
    
    /**
     * 获取当前设备品牌标识
     * @return 当前设备品牌标识
     */
    public static String getBrand() {
        return Build.BRAND;
    }

	/**
	 * 获取设备制造厂商
	 * @return 制造厂商
	 */
	public static String getManufacturer() {
		return Build.MANUFACTURER;
	}

    /**
     * 获取当前设备型号，不同品牌设备型号有重复，为了更好区分，
     * 实际上格式为【品牌+设备型号】
     * @return 当前设备型号
     */
    public static String getModel() {
    	if (null == devmodel) {
    		synchronized (Device.class) {
    			if (null == devmodel) {
    				devmodel = getRealModel();
    			}
    		}
    	}
    	
    	return devmodel;
    }

	/**
	 * 加密的机型
	 * @return 加密的机型
	 */
	public static String getMd() {
		String model = getModel();
		return HEX.getCipher(model);
	}
    
    /**
     * 获取当前设备型号，不同品牌设备型号有重复，为了更好区分，
     * 实际上格式为【品牌+设备型号】
     * @return 当前设备型号
     */
    private static String getRealModel() {
		String deviceModel = null;
		String model = execCmd("getprop ro.product.model");

		if (empty(model)) {
			model = Build.MODEL;
		}

		if (!empty(Build.BRAND)) {
			if (model.startsWith(Build.BRAND)) {
				deviceModel = model;
			} else {
				deviceModel = (Build.BRAND + model);
			}
		} else {
			deviceModel = model;
		}

		if (!empty(deviceModel)) {
			deviceModel = deviceModel.replaceAll(" ", "");
		}

		return TextUtils.tidy(deviceModel);
	}

	/**
	 * 执行一个shell命令
	 * @param cmd 指定shell 命令
	 * @return 返回的结果
	 */
	private static String execCmd(String cmd) {
		Process process = null;
		BufferedReader reader = null;
		
		try {
			process = Runtime.getRuntime().exec(cmd);
			reader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			
			String line = null;
			StringBuilder buffer = new StringBuilder();
			
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
			}
			
			return buffer.toString().trim();
		} catch (Throwable t) {
			LOG.e(TAG, "[" + cmd + "] exec cmd failed: " + t);
		} finally {
			// 结束进程
            destroy(process);
            
            // 关闭流对象
            try {
                if (null != reader) {
                	reader.close();
                }
            } catch (Throwable t) {
            	LOG.e(TAG, "close reader failed: " + t);
            }
        }
		
		return "";
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
                    process = null;
                }
            } catch (Throwable t) {
            	LOG.e(TAG, "destroy process failed: " + t);
            }
    	} catch (Throwable t) {
    		LOG.e(TAG, "exit process failed: " + t);
    	}
    }

	/**
	 * 获得设备唯一标识ID，支持缓存，只有第一次是从系统读取
	 * @param context 上下文对象
	 * @return 设备唯一标识ID，获取失败返回null或者""
	 */
	public static String getId(Context context) {
		if (null == id) {
			synchronized (Device.class) {
				if (null == id) {
					id = getRealId(context);
				}
			}
		}

		return id;
	}
    
    /**
     * 获得设备唯一标识ID，读取的方案如下
     * 1 获取eMMCid，如果结果不合法下一步
     * 2 获取IMEI号，如果结果不合法下一步
     * 3 获取MAC地址
     * @param context 上下文对象
     * @return 设备唯一标识ID，获取失败返回null或者""
     */
    private static String getRealId(Context context) {
    	try {
    		String matrix = getIdMatrix(context);
    		
    		if (!empty(matrix)) {
    			String id = MD5.getString(matrix);
            	LOG.d(TAG, "get id{matrix=" + matrix + ", id=" + id + "} done");
            	return id;
    		}
    	} catch (Throwable t) {
			LOG.e(TAG, "get id failed: " + t);
		}
    	
    	return getDefaultId();
    }
    
    /**
     * 获得设备唯一标识ID的母串，读取的方案如下
     * 1 获取eMMCid，如果结果不合法下一步
     * 2 获取IMEI号，如果结果不合法下一步
     * 3 获取MAC地址
     * @param context 上下文对象
     * @return 设备唯一标识ID，获取失败返回null或者""
     */
    private static String getIdMatrix(Context context) {
    	StringBuilder buffer = new StringBuilder();
    	String id = null;
    	
//    	if (valid(id = getEMMCid())) {
//    		buffer.append(id);
//    	}
    	
    	if (valid(id = getCpuid())) {
    		buffer.append(id);
    	}
    	
    	if (valid(id = getIMEI(context))) {
    		buffer.append(id);
    	}
    	
    	return buffer.toString();
    }
    
    /**
     * 获取系统当前的MAC地址
     * @param context 上下文环境
     * @return 系统当前的MAC地址
     */
	private static String getMACAddress(Context context) {
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
     * 使用UUID作为默认设备ID
     * @return 默认设备ID
     */
    private static String getDefaultId() {
    	return UUID.randomUUID().toString().replaceAll("-", "")
                .toUpperCase(Locale.getDefault());
    }

	/**
	 * 获取系统M1，MD5(IMEI)
	 * @param context 应用上下文
	 * @return 系统M1
	 */
	public static String getm1(Context context) {
		if (empty(m1)) {
			synchronized (Device.class) {
				if (empty(m1)) {
					m1 = getRealM1(context);
				}
			}
		}

		return m1;
	}

	/**
	 * 获取系统M1，MD5(IMEI)
	 * @param context 应用上下文
	 * @return 系统M1
	 */
	public static String getRealM1(Context context) {
		try {
			String IMEI = getIMEI(context);

			if (valid(IMEI)) {
				return MD5.getString(IMEI);
			}
		} catch (Throwable t) {
			LOG.e(TAG, "get real m1 failed: " + t);
		}

		return "";
	}
    
    /**
     * 获取系统M2，MD5(IMEI + AndroidId + SN) 
     * @param context 应用上下文
     * @return 系统M2
     */
    public static String getm2(Context context) {
    	try {
    		String matrix = getMatrix2(context);
    		
    		if (!empty(matrix)) {
    			String m2 = MD5.getString(matrix);
            	//LOG.d(TAG, "get m2{matrix=" + matrix + ", m2=" + m2 + "} done");
            	return m2;
    		}
    	} catch (Throwable t) {
			LOG.e(TAG, "get m2 failed: " + t);
		}
    	
    	return "";
    }
    
    /**
     * 获取系统M2所需要的参数，(IMEI + AndroidId + SN) 
     * @param context 上下文对象
     * @return M2所需要的参数
     */
    private static String getMatrix2(Context context) {
    	String deviceId = getIMEI(context);
    	String aid = getaid(context);
    	String sn = getSerial();
    	return (deviceId + aid + sn);
    }
    
    /**
     * 读取系统的序列号
     * @return 系统的序列号
     */
	private static String getSerial() {
    	try {
    		return Build.SERIAL;
    	} catch (Throwable t) {
    		LOG.e(TAG, "get serial failed: " + t);
    	}
    	
    	return "";
    }
    
    /**
     * 获取系统Android ID
     * @param context 应用上下文
     * @return 系统Android ID
     */
    private static String getaid(Context context) {
    	try {
    		return Secure.getString(context.getContentResolver(),
    				Secure.ANDROID_ID);
    	} catch (Throwable t) {
			LOG.e(TAG, "get android ID failed: " + t);
		}
    	
    	return "";
    }

	/**
	 * 获取手机的M1列表，以分号分隔
	 * @param context 上下文
	 * @return 手机的M1列表
	 */
	public static HashSet<String> getms(Context context) {
		HashSet<String> mlist = new HashSet<String>();

		try {
			if (null != context) {
				TelephonyManager telMgr = (TelephonyManager) context
						.getSystemService(Context.TELEPHONY_SERVICE);

				if (null != telMgr) {
					// 添加IMEI
					String imei0 = invoke(telMgr, "getImei", 0);
					if (valid(imei0)) {
						mlist.add(MD5.getString(imei0));
					}

					String imei1 = invoke(telMgr, "getImei", 1);
					if (valid(imei1)) {
						mlist.add(MD5.getString(imei1));
					}

					// 添加MEID
					String meid0 = invoke(telMgr, "getDeviceId", 0);
					if (valid(meid0)) {
						mlist.add(MD5.getString(meid0));
					}

					String meid1 = invoke(telMgr, "getDeviceId", 1);
					if (valid(meid1)) {
						mlist.add(MD5.getString(meid1));
					}

					// 最后兼容旧版本，获取默认设备ID
					String imei = getDeviceId(telMgr);
					if (valid(imei)) {
						mlist.add(MD5.getString(imei));
					}
				}
			}
		} catch (Throwable t) {
			LOG.e(TAG, "get M1 list failed: " + t);
		}

		return mlist;
	}
    
    /**
     * 获得设备唯一标识IMEI号，读取的方案如下
     * 1 反射调用TelephonyManager的getImei(0)，如果结果不合法下一步
     * 2 反射调用TelephonyManager的getImei(1)，如果结果不合法下一步
     * 3 反射调用TelephonyManager的getDeviceId(0)，如果结果不合法下一步
     * 4 反射调用TelephonyManager的getDeviceId(1)，如果结果不合法下一步
     * 5 如果以上都不合法，再调用旧的原生接口getDeviceId()
     * 
     * @param context 应用上下文
     * @return 默认的IMEI号
     */
    public static String getIMEI(Context context) {
    	String id = null;
    	
    	try {
    		TelephonyManager tm = (TelephonyManager)context
    				.getSystemService(Context.TELEPHONY_SERVICE);
    		if (null != tm) {
    			id = invoke(tm, "getImei", 0);
    			if (valid(id)) {
    				return id;
    			}
    			
    			id = invoke(tm, "getImei", 1);
    			if (valid(id)) {
    				return id;
    			}
    			
    			id = invoke(tm, "getDeviceId", 0);
    			if (valid(id)) {
    				return id;
    			}
    			
    			id = invoke(tm, "getDeviceId", 1);
    			if (valid(id)) {
    				return id;
    			}
    			
    			return getDeviceId(tm);
    		}
    	} catch (Throwable t) {
    		LOG.e(TAG, "get IMEI failed: " + t);
    	}
    	
    	return "";
    }
    
    /**
     * 获取手机的所有的设备号，以分号分隔
     * 目前有存储芯片ID、IMEI号等
     * @param context 上下文
     * @return 手机的所有的设备号
     */
	public static HashSet<String> getIMEIs(Context context) {
    	HashSet<String> imeis = new HashSet<String>();

		try {
			if (null != context) {
				TelephonyManager telMgr = (TelephonyManager) context
						.getSystemService(Context.TELEPHONY_SERVICE);

				if (null != telMgr) {
					// 添加IMEI
					String imei0 = invoke(telMgr, "getImei", 0);
					if (valid(imei0)) {
						imeis.add(imei0);
					}

					String imei1 = invoke(telMgr, "getImei", 1);
					if (valid(imei1)) {
						imeis.add(imei1);
					}

					// 添加MEID
					String meid0 = invoke(telMgr, "getDeviceId", 0);
					if (valid(meid0)) {
						imeis.add(meid0);
					}

					String meid1 = invoke(telMgr, "getDeviceId", 1);
					if (valid(meid1)) {
						imeis.add(meid1);
					}

					// 最后兼容旧版本，获取默认设备ID
					String devid = getDeviceId(telMgr);
					if (valid(devid)) {
						imeis.add(devid);
					}
				}
			}
		} catch (Throwable t) {
			LOG.e(TAG, "get IMEIs failed: " + t);
		}

    	return imeis;
    }

	/**
	 * 安全读取系统默认的设备ID
	 * @param tm 通信管理对象
	 * @return 系统默认的设备ID
	 */
	private static String getDeviceId(TelephonyManager tm) {
		try {
			if (null != tm) {
				return tm.getDeviceId();
			}
		} catch (Throwable t) {
			//LOG.w(TAG, "get device ID failed: " + t);
		}

		return "";
	}
    
    /**
     * 反射调用TelephonyManager中的方法，方法类型为
     * String [method](int parameter)
     * @param tm 通信管理对象
     * @param method 反射调用的方法
     * @param slotId 卡槽ID，分别为1或者1
     * @return 返回值
     */
    private static String invoke(TelephonyManager tm, String method, int slotId) {
    	String value = "";
        Class<?> clazz = null;

        try {
        	if (null != tm) {
        		clazz = Class.forName("android.telephony.TelephonyManager");
                Method getDeviceId = clazz.getMethod(method, int.class);
                value = (String) getDeviceId.invoke(tm, slotId);
        	}
        } catch (Throwable t) {
//            LOG.w(TAG, "invoke " + method
//					+ " failed(" + t.getClass().getSimpleName()
//					+ "): " + t.getMessage());
        }

        return value;
    }
    
    /**
     * 读取手机的eMMCid，存储芯片的ID
     * @return 存储芯片的ID
     */
//    public static String getEMMCid() {
//    	return readFile("/sys/block/mmcblk0/device/cid");
//    }
    
    /**
     * 从文件中读取简单内容
     * @param fileUrl 文件地址
     * @return 读取到的字符串
     */
    private static String readFile(String fileUrl) {
    	FileInputStream input = null;
    	
		if (empty(fileUrl)) {
			return "";
		}
		
		try {
			File file = new File(fileUrl);
			
			if (!file.exists()) {
				LOG.w(TAG, "[" + fileUrl + "] file not found ...");
				return "";
			}
			
			input = new FileInputStream(file);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int read = 0;
			
			while (-1 != (read = input.read(buffer))) {
				output.write(buffer, 0, read);
			}
			
			return output.toString("UTF-8").trim();
		} catch (Throwable t) {
			LOG.e(TAG, "[" + fileUrl + "] read failed: " + t);
		} finally {
			try {
				if (null != input) {
					input.close();
				}
			} catch (Exception e) {
				LOG.e(TAG, "[" + fileUrl + "] close stream failed: " + e);
			}
		}

		return "";
	}
    
    /**
     * 读取手机的处理器ID
     * @return 处理器ID
     */
    public static String getCpuid() {
    	return readProperty("ro.boot.cpuid", "");
    }
    
	/**
	 * 获取系统属性
	 * @param key 对应关键字
	 * @param defaultValue 默认值
	 * @return 系统属性值
	 */
	private static String readProperty(String key, String defaultValue) {
		try {
			Class<?> clazz = Class.forName("android.os.SystemProperties");
			Method method = clazz.getDeclaredMethod("get", String.class);
			method.setAccessible(true);
			return (String) method.invoke(null, key);
		} catch (Throwable t) {
//			String className = t.getClass().getSimpleName();
//			LOG.w(TAG, "invoke get(" + key + ") failed(" + className
//					+ "): " + t.getMessage());
		}
		
		return defaultValue;
	}

    /**
     * 检查ID是否有效，非空并且不是全0字符串
	 * @param id ID字符串
     * @return true 有效的；false 无效的
     */
    public static boolean valid(String id) {
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
	 * 对应字符串机型整理
	 * @param s 字符串
	 * @return 整理后的字符串
	 */
	private static String tidy(String s) {
		StringBuilder builder = new StringBuilder();

		try {
			char[] charArray = s.toCharArray();

			for (char c: charArray) {
				if ((31 < c) && (c < 127)) {
					builder.append(c);
				}
			}
		} catch (Throwable t) {
			// nothing to do
		}

		return builder.toString();
	}

    /**
     * 判断字符串是否为空
     * @param s 字符串
     * @return true 空；false 非空
     */
    private static boolean empty(String s) {
        return ((null == s) || (s.length() <= 0));
    }
}
