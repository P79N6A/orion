package com.hs.cld.da.dx;

import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtils {
	private final static String TAG = "FileUtils";

	public static byte[] read(String localFileUrl) throws Exception {
		return read(new File(localFileUrl));
	}

	public static byte[] read(File localFile) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		InputStream input = null;

		try {
			input = new FileInputStream(localFile);
			byte[] buffer = new byte[4096];
			int length = 0;

			while (-1 != (length = input.read(buffer))) {
				if (length > 0) {
					output.write(buffer, 0, length);
				}
			}

			return output.toByteArray();
		} finally {
			close(output, input);
		}
	}

	public static int write(String localFileUrl,
							byte[] data) throws Exception {
		return write(new File(localFileUrl), data);
	}

	public static int write(File localFile,
			byte[] data) throws Exception {
		if (!localFile.getParentFile().exists()) {
			localFile.getParentFile().mkdirs();
		}

		if (!localFile.exists()) {
			localFile.createNewFile();
		}

		if (!localFile.exists()) {
			throw new Exception("create file(" + localFile.toString() + ") failed");
		}

		if (!TextUtils.empty(data)) {
			return writeToFile(localFile, data);
		} else {
			return 0;
		}
	}

	private static int writeToFile(File file, byte[] buffer) throws Exception {
		FileOutputStream fos = null;

		try {
			fos = new FileOutputStream(file);
			fos.write(buffer);
			fos.flush();
			return buffer.length;
		} finally {
			close(fos, null);
		}
	}

	private static void close(Closeable... args) {
		if (null != args) {
			for (Closeable arg: args) {
				try {
					if (null != arg) {
						arg.close();
					}
				} catch (Exception e) {
					// nothing to do
				}
			}
		}
	}

	public static File create(File file, boolean newAlways) {
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}

		if (newAlways) {
			if (file.exists()) {
				file.delete();
			}
		}

		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (Exception e) {
				return null;
			}
		}

		return file;
	}

	public static File createDir(File file) {
		if (file.exists()) {
			if (!file.isDirectory()) {
				FileUtils.deleteFile(file);
			}
		}

		if (!file.exists()) {
			return (file.mkdirs() ? file : null);
		} else {
			return file;
		}
	}

	/**
	 * 安全删除文件
	 * @param file 文件对象
	 */
	public static void deleteFile(File file) {
		try {
			if (file.exists()) {
				//LOG.i(TAG, "delete file: " + file);
				file.delete();
			}
		} catch (Exception e) {
			// nothing to do
		}
	}

	public static boolean deleteDir(File dir) {
		if (!dir.exists()) {
			return true;
		}

		if (dir.isDirectory()) {
			String[] children = dir.list();

			if (null != children) {
				for (int i = 0; i < children.length; i++) {
					deleteDir(new File(dir, children[i]));
				}
			}

			LOG.i(TAG, "delete dir: " + dir);
			return dir.delete();
		} else {
			LOG.i(TAG, "delete file: " + dir);
			return dir.delete();
		}
	}
}
