package com.anl.app;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 字符串常用函数
 *
 */
public class TextUtils {
	/**
	 * 判断字符串是否相等
	 * @param s1 字符串1
	 * @param s2 字符串2
	 * @return true 是；false 不是
	 */
	public static boolean equals(String s1, String s2) {
		if (s1 == s2) {
			return true;
		} else {
			String ss1 = ((null == s1) ? "" : s1);
			String ss2 = ((null == s2) ? "" : s2);
			return ss1.equals(ss2);
		}
	}
	
	/**
	 * 判断字符串是否相等，忽略大小写
	 * @param s1 字符串1
	 * @param s2 字符串2
	 * @return true 是；false 不是
	 */
	public static boolean equalsIgnoreCase(String s1, String s2) {
	    if ((null == s1) && (null == s2)) {
	    	return true;
	    } else if ((null != s1) && (null != s2)) {
	    	return s1.equalsIgnoreCase(s2);
	    } else {
	    	return false;
	    }
	}
	
	/**
	 * 判断字符串s是否以字符串start开头
	 * @param s 字符串1
	 * @param start 字符串2
	 * @return true 是；false 不是
	 */
	public static boolean startsWith(String s, String start) {
	    if ((null == s) && (null == start)) {
	    	return true;
	    } else if ((null != s) && (null != start)) {
	    	return s.startsWith(start);
	    } else {
	    	return false;
	    }
	}
    
    /**
     * 判断字符串是否为空
     * @param s 字符串
     * @return true 空；false 非空
     */
    public static boolean empty(String s) {
    	return ((null == s) || (s.length() <= 0));
    }

	/**
	 * 判断数组是否为空
	 * @param a 数组
	 * @return true 空；false 非空
	 */
	public static boolean empty(byte[] a) {
		return ((null == a) || (a.length <= 0));
	}

	/**
	 * 判断字符串是否为空
	 * @param s 字符串
	 * @return true 空；false 非空
	 */
	public static <T> boolean empty(T[] s) {
		return ((null == s) || (s.length <= 0));
	}
    
    /**
	 * 获取当前的时间，字符串格式为"yyyyMMddHHmmss"
	 * @return 字符串格式的时间
	 */
	public static String NOW() {
		try {
			final String pattern = "yyyyMMddHHmmss";
			SimpleDateFormat formater = new SimpleDateFormat(pattern, Locale.getDefault());
			return formater.format(new Date());
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 格式化指定时间，字符串格式为"yyyyMMddHHmmss"
	 * @param time 指定时间，长整型
	 * @return 字符串格式的时间
	 */
	public static String TSTR(long time) {
		try {
			final String pattern = "yyyyMMddHHmmss";
			SimpleDateFormat formater = new SimpleDateFormat(pattern, Locale.getDefault());
			return formater.format(time);
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 格式化指定时间
	 * @param time 指定时间，长整型
	 * @return 字符串格式的时间
	 */
	public static String TSTR(String pattern, long time) {
		try {
			SimpleDateFormat formater = new SimpleDateFormat(pattern, Locale.getDefault());
			return formater.format(time);
		} catch (Exception e) {
			return "";
		}
	}
	
	/**
	 * 判断字符串s是否包含字符串sub
	 * @param s 字符串s
	 * @param sub 字符串sub
	 * @return true 包含；false 不包含
	 */
	public static boolean contains(String s, String sub) {
		if ((null == s) && (null == sub)) {
			return true;
		} else if ((null != s) && (null != sub)) {
			return s.contains(sub);
		} else {
			return false;
		}
	}
	
	/**
	 * 比较两个字符串的大小，忽略为null的情况，按照空字符串处理
	 * @param s1 其中的一个字符串
	 * @param s2 另一个字符串
	 * @return 如果s1小于s2返回负整数；相等返回0；s1大于s2返回正整数
	 */
	public static int compare(String s1, String s2) {
		s1 = (null == s1) ? "" : s1;
		s2 = (null == s2) ? "" : s2;
		return s1.compareTo(s2);
	}

	/**
	 * 将字符串按照分隔符分隔成数组
	 * @param s 字符串
	 * @param separator 分隔符
	 * @return 数组
	 */
	public static String[] toArray(String s, String separator) {
		List<String> l = new ArrayList<>();

		if (!empty(s)) {
			String[] array = s.split(separator);

			for (String element: array) {
				if (!empty(element)) {
					l.add(element.trim());
				}
			}
		}

		return l.toArray(new String[] {});
	}
	
	/**
	 * 对应字符串的trim方法
	 * @param s 字符串
	 * @return 执行trim后的返回字符串
	 */
	public static String trim(String s) {
		return ((!empty(s)) ? s.trim() : s);
	}
	
	/**
	 * 将字符串转化为整型
	 * @param s 字符串
	 * @return 整型
	 */
	public static int parseInt(String s) {
		int i = 0;
		
		try {
			if (!TextUtils.empty(s)) {
				i = Integer.parseInt(s);
			}
		} catch (Exception e) {
			i = 0;
		}
		
		return i;
	}
	
	/**
	 * 判断一个数组是是否包含指定元素
	 * @param array 指定数据
	 * @param v 指定元素
	 * @return true 包含；false 不包含
	 */
	public static <T> boolean contains(final T[] array, final T v) {
		if ((null != array) && (array.length > 0)) {
			for (final T e : array) {
		    	if ((e == v) || ((null != v) && (null != e) && v.equals(e))) {
		        	return true;
		        }
		    }
		}

	    return false;
	}
	
	/**
	 * 将文本数组序列化为字符串
	 * @param array 文本数组
	 * @return 序列化后字符串
	 */
	public static <T> String toText(T[] array) {
		if ((null != array) && (array.length > 0)) {
			boolean first = true;
			StringBuffer buffer = new StringBuffer();
			
			for (T e : array) {
				if (first) {
					first = false;
				} else {
					buffer.append(";");
				}
				
				buffer.append(e);
			}
			
			return buffer.toString();
		} else {
			return "";
		}
	}
	
	/**
	 * 将文本数组序列化为字符串
	 * @param array 文本数组
	 * @return 序列化后字符串
	 */
	public static <T> String toText(Set<T> array) {
		if ((null != array) && (array.size() > 0)) {
			boolean first = true;
			StringBuffer buffer = new StringBuffer();
			
			for (T e : array) {
				if (first) {
					first = false;
				} else {
					buffer.append(";");
				}
				
				buffer.append(e);
			}
			
			return buffer.toString();
		} else {
			return "";
		}
	}
	
	/**
	 * 对应StringBuilder的append方法，忽略空字符串
	 * @param builder StringBuilder对象
	 * @param value 字符串，为空则忽略
	 * @return StringBuilder对象
	 */
	public static StringBuilder append(StringBuilder builder, String value) {
		if ((null != builder) && (!empty(value))) {
			builder.append(value);
		}
		
		return builder;
	}

	/**
	 * 对应字符串机型整理
	 * @param s 字符串
	 * @return 整理后的字符串
	 */
	public static String tidy(String s) {
		StringBuffer buffer = new StringBuffer();

		try {
			char[] a = s.toCharArray();

			for (int i = 0; i < a.length; i++) {
				if ((31 < a[i]) && (a[i] < 127)) {
					buffer.append(a[i]);
				}
			}
		} catch (Throwable t) {
		}

		return buffer.toString();
	}
}
