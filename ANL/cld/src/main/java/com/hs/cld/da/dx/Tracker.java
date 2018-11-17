package com.hs.cld.da.dx;

import android.content.Context;

import com.hs.cld.basic.Builder;
import com.hs.cld.basic.HTTPRequester;
import com.hs.cld.basic.Hosts;
import com.hs.cld.basic.ThreadManager;
import com.hs.cld.common.utils.Device;
import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.SystemUtils;
import com.hs.cld.common.utils.TextUtils;
import com.hs.cld.common.utils.Threadable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 上报监播
 */
public class Tracker extends HTTPRequester {
	/**
	 * 日志标签
	 */
	private final static String TAG = "Tracker";

	/**
	 * 唯一ID
	 */
	private final String mReportId;

	private long mArrivedInMillis = 0;
	private long mExeInMillis = 0;
	private String mReportType = null;
	private boolean mOK = false;
	private String mErrorMessage = null;

	/**
	 * 构造函数
	 * @param context 上下文对象
	 * @param reportId 上报ID
	 */
	public Tracker(Context context, String reportId) {
		super(context, "Tracker");
		setRequestPath("api/v1/rp");
		setTargetHosts(Hosts.TRACKERS(context));
		this.mReportId = reportId;
	}

	public void setArrivedInMillis(long mArrivedInMillis) {
		this.mArrivedInMillis = mArrivedInMillis;
	}

	public void setExeInMillis(long mExeInMillis) {
		this.mExeInMillis = mExeInMillis;
	}

	public void setReportType(String mReportType) {
		this.mReportType = mReportType;
	}

	public void setOK(boolean ok) {
		this.mOK = ok;
	}

	public void setErrorMessage(String mErrorMessage) {
		this.mErrorMessage = mErrorMessage;
	}

	public void request() {
		new Threadable("tracker") {
			@Override
			protected void doFire() {
				try {
					String parameter = buildParameter();
					LOG.i(TAG, "submit: " + parameter);
					setRequestBody(parameter);
					jsonExecute();
				} catch (Exception e) {
					LOG.w(TAG, "tracker failed: " + e);
				}
			}
		}.execute(ThreadManager.getNormalExecutor());
	}

	public void request0() throws Exception {
		String parameter = buildParameter();
		LOG.i(TAG, "submit: " + parameter);
		setRequestBody(parameter);
		jsonExecute();
	}

	private String buildParameter() throws JSONException {
		JSONObject jo = new JSONObject();
		jo.put("r1", notNull(mReportId));
		jo.put("p3", "0");
		jo.put("p5", notNull(Device.getModel()));
		jo.put("m1", notNull(Device.getm1(mContext)));
		jo.put("i45", notNull(getIMEIs(mContext)));
		jo.put("m4", notNull(getDeviceId(mContext)));
		jo.put("d6", "" + mArrivedInMillis);
		jo.put("p7", "" + mExeInMillis);
		jo.put("r8", mOK ? "success" : "fail");
		jo.put("r9", strLengthOf(mErrorMessage));
		jo.put("r10", notNull(mReportType));
		jo.put("v16", "" + SystemUtils.getSelfVersionCode(mContext));
		jo.put("p23", SystemUtils.getSelfPackageName(mContext));
		jo.put("ch", Builder.CHANNEL);
		jo.put("v0", Builder.VERSION);
		return jo.toString();
	}

	private String strLengthOf(String s) {
		if (!TextUtils.empty(s)) {
			if (s.length() > 112) {
				return s.substring(0, 112);
			}
		}
		return notNull(s);
	}
}
