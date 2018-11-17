package com.hs.q.common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * 字符串压缩解压接口
 * 
 */
public class ZipUtils {
    /**
     * 使用GZIP压缩方式，对字节数组数据压缩
     * @param plain 普通数据
     * @return 压缩后数据
     * @throws Exception 异常定义
     */
    public static byte[] gz(byte[] plain) throws Exception {
        ByteArrayInputStream input = null;
        ByteArrayOutputStream output = null;
        GZIPOutputStream gzOutput = null;

        try {
        	input = new ByteArrayInputStream(plain);
        	output = new ByteArrayOutputStream();
        	gzOutput = new GZIPOutputStream(output);
        	gzOutput.write(plain);
        	gzOutput.close();
            return output.toByteArray();
        } catch (IOException e) {
            throw new Exception("compress(gzip) failed(IOException)", e);
        } catch (NullPointerException e) {
        	throw new Exception("compress(gzip) failed(NullPointerException)", e);
        } finally {
            close(gzOutput);
            close(input);
            close(output);
        }
    }

    private static void close(Closeable closeable) {
        try {
            if (null != closeable) {
                closeable.close();
            }
        } catch (Throwable t) {
        }
    }

    /**
     * 使用GZIP解压方式，对字节数组数据解压
     * @param cipher 压缩数据
     * @return 解压后数据
     * @throws Exception 异常定义
     */
    public static byte[] ungz(byte[] cipher) throws Exception {
        try {
            return ungz0(cipher);
        } catch (Exception e) {
            return doWhileUNGZFailed(e, cipher);
        }
    }

    /**
     * 当解压失败时，试图打印出解压失败的字符串，可能是普通文本
     * @param cipher 解压失败的字符串
     * @throws Exception 异常定义
     */
    private static byte[] doWhileUNGZFailed(Exception e, byte[] cipher)
            throws Exception {
        try {
            if ((e instanceof IOException) && TextUtils.contains(e.getMessage(), "unknown format")) {
                if ((null != cipher) && (cipher.length > 0)) {
                    return cipher;
                }
            }
        } catch (Exception ex) {
            // nothing do do
        }

        throw e;
    }

    /**
     * 使用GZIP解压方式，对字节数组数据解压
     * @param cipher 压缩数据
     * @return 解压后数据
     * @throws Exception 异常定义
     */
    private static byte[] ungz0(byte[] cipher) throws Exception {
        ByteArrayInputStream input = null;
        ByteArrayOutputStream output = null;
        GZIPInputStream gzInput = null;
        int length = 0;
        byte[] buffer = new byte[1024];

        try {
        	output = new ByteArrayOutputStream();
        	input = new ByteArrayInputStream(cipher);
        	gzInput = new GZIPInputStream(input);

            while ((length = gzInput.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            return output.toByteArray();
        } finally {
            close(gzInput);
            close(input);
            close(output);
        }
    }
}
