package com.hs.cld.da;

import android.content.Context;

import com.hs.cld.common.http.HTTPBuilder;
import com.hs.cld.common.http.HTTPError;
import com.hs.cld.common.http.HTTPHelper;

import java.io.ByteArrayOutputStream;

public class RemoteFile {
	public static byte[] load(Context context, String url) throws Exception {
		HTTPHelper helper = HTTPBuilder.build(context);
		OnBlockHandler handler = new OnBlockHandler();
		helper.setBlockListener(handler);

		// 执行下载请求
		HTTPError he = helper.download(url, 0);

		if (he.failed()) {
			throw new Exception("download failed: " + he);
		} else {
			return handler.getByteArray();
		}
	}

	private static class OnBlockHandler implements HTTPHelper.OnBlockListener {
		private ByteArrayOutputStream mOutput = new ByteArrayOutputStream();

		private byte[] getByteArray() {
			return mOutput.toByteArray();
		}

		@Override
		public boolean onStart(String url, long offset, long length, long total) {
			return true;
		}

		@Override
		public boolean onBlock(byte[] buffer, long length) {
			mOutput.write(buffer, 0, (int)length);
			return true;
		}
	}
}
