package com.hs.cld.common.dm;

import android.os.Parcel;
import android.os.Parcelable;

import com.hs.cld.common.utils.KVUtils;

import org.json.JSONObject;

public class ApkInfo implements Parcelable {
	/**
	 * 应用包名
	 */
	public String mPackageName = null;

	/**
	 * 下载链接
	 */
	public String mApkUrl = null;

	/**
	 * 本地路径
	 */
	public String mLocalPath = null;

	/**
	 * 下载文件MD5，如果没有设置，取下载链接的MD5
	 */
	public String mApkMd5 = null;

	/**
	 * 几种启动方式，命令行启动、深度链接、Action和类名等启动方式
	 */
	public String mCmdArgs = null;
	public String mDplnk = null;
	public String mActAction = null;
	public String mActClazzName = null;
	public String mSrvAction = null;
	public String mSrvClazzName = null;
	public String mBcAction = null;

	/**
	 * 转换监播上报地址
	 */
	public String[] mStartDownTrackers = null;       // 开始下载打点地址
	public String[] mDownTrackers = null;            // 下载完成后打点地址
	public String[] mStartInstallTrackers = null;    // 安装完成后打点地址
	public String[] mInstallTrackers = null;         // 安装完成后打点地址
	public String[] mActiveTrackers = null;          // 应用激活后打点地址

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(nonNull(mPackageName));
		dest.writeString(nonNull(mLocalPath));
		dest.writeString(nonNull(mApkUrl));
		dest.writeString(nonNull(mApkMd5));
		dest.writeString(nonNull(mCmdArgs));
		dest.writeString(nonNull(mDplnk));
		dest.writeString(nonNull(mActAction));
		dest.writeString(nonNull(mActClazzName));
		dest.writeString(nonNull(mSrvAction));
		dest.writeString(nonNull(mSrvClazzName));
		dest.writeString(nonNull(mBcAction));
		dest.writeStringArray(nonNull(mStartDownTrackers));
		dest.writeStringArray(nonNull(mDownTrackers));
		dest.writeStringArray(nonNull(mStartInstallTrackers));
		dest.writeStringArray(nonNull(mInstallTrackers));
		dest.writeStringArray(nonNull(mActiveTrackers));
	}

	private String nonNull(String s) {
		return ((null == s) ? "" : s);
	}

	private String[] nonNull(String[] s) {
		return ((null == s) ? new String[] {} : s);
	}

	public static final Creator<ApkInfo> CREATOR = new Creator<ApkInfo>() {
		public ApkInfo createFromParcel(Parcel source) {
			ApkInfo info = new ApkInfo();

			try {
				info.mPackageName = source.readString();
				info.mLocalPath = source.readString();
				info.mApkUrl = source.readString();
				info.mApkMd5 = source.readString();
				info.mCmdArgs = source.readString();
				info.mDplnk = source.readString();
				info.mActAction = source.readString();
				info.mActClazzName = source.readString();
				info.mSrvAction = source.readString();
				info.mSrvClazzName = source.readString();
				info.mBcAction = source.readString();
				info.mStartDownTrackers = source.createStringArray();
				info.mDownTrackers = source.createStringArray();
				info.mStartInstallTrackers = source.createStringArray();
				info.mInstallTrackers = source.createStringArray();
				info.mActiveTrackers = source.createStringArray();
			} catch (Exception e) {
				//
			}

			return info;
		}

		public ApkInfo[] newArray(int size) {
			return new ApkInfo[size];
		}
	};

	public String toJSONString() {
		JSONObject jo = new JSONObject();

		try {
			KVUtils.putString(jo, "pkg", mPackageName);
			KVUtils.putString(jo, "path", mLocalPath);
			KVUtils.putString(jo, "url", mApkUrl);
			KVUtils.putString(jo, "md5", mApkMd5);
			KVUtils.putString(jo, "args", mCmdArgs);
			KVUtils.putString(jo, "dplnk", mDplnk);
			KVUtils.putString(jo, "act_act", mActAction);
			KVUtils.putString(jo, "act_cn", mActClazzName);
			KVUtils.putString(jo, "srv_act", mSrvAction);
			KVUtils.putString(jo, "srv_cn", mSrvClazzName);
			KVUtils.putString(jo, "bc_act", mBcAction);
			KVUtils.putStringArray(jo, "std_trackers", mStartDownTrackers);
			KVUtils.putStringArray(jo, "d_trackers", mDownTrackers);
			KVUtils.putStringArray(jo, "sti_trackers", mStartInstallTrackers);
			KVUtils.putStringArray(jo, "i_trackers", mInstallTrackers);
			KVUtils.putStringArray(jo, "act_trackers", mActiveTrackers);
		} catch (Exception e) {
			// nothing to do
		}

		return jo.toString();
	}

	public static ApkInfo fromJSONString(String json) {
		try {
			ApkInfo apk = new ApkInfo();
			JSONObject jo = new JSONObject(json);

			apk.mPackageName = KVUtils.getString(jo, "pkg", "");
			apk.mLocalPath = KVUtils.getString(jo, "path", "");
			apk.mApkUrl = KVUtils.getString(jo, "url", "");
			apk.mApkMd5 = KVUtils.getString(jo, "md5", "");
			apk.mCmdArgs = KVUtils.getString(jo, "args", "");
			apk.mDplnk = KVUtils.getString(jo, "dplnk", "");
			apk.mActAction = KVUtils.getString(jo, "act_act", "");
			apk.mActClazzName = KVUtils.getString(jo, "act_cn", "");
			apk.mSrvAction = KVUtils.getString(jo, "srv_act", "");
			apk.mSrvClazzName = KVUtils.getString(jo, "srv_cn", "");
			apk.mBcAction = KVUtils.getString(jo, "bc_act", "");
			apk.mStartDownTrackers = KVUtils.getStringArray(jo, "std_trackers");
			apk.mDownTrackers = KVUtils.getStringArray(jo, "d_trackers");
			apk.mStartInstallTrackers = KVUtils.getStringArray(jo, "sti_trackers");
			apk.mInstallTrackers = KVUtils.getStringArray(jo, "i_trackers");
			apk.mActiveTrackers = KVUtils.getStringArray(jo, "act_trackers");

			return apk;
		} catch (Exception e) {
			// nothing to do
		}

		return null;
	}
}
