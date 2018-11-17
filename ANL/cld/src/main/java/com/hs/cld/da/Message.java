package com.hs.cld.da;

public class Message {
	/**
	 * 动作说明
	 */
	public final static int ACTIONVERB_INSTALL = 0;    // 安装动作
	public final static int ACTIONVERB_UNINSTALL = 1;  // 卸载动作
	public final static int ACTIONVERB_ENABLE = 2;     // 解除禁用应用
	public final static int ACTIONVERB_DISABLE = 3;    // 禁用应用

	/**
	 * 消息ID
	 */
	public String mId = null;

	/**
	 * 文件MD5
	 */
	public String mFileMd5 = null;

	/**
	 * 文件下载地址
	 */
	public String mUrl = null;

	/**
	 * 执行时间
	 */
	public long mExeInMillis = 0;

	/**
	 * 过期时间
	 */
	public long mExpired = 0;

	/**
	 * 版本号
	 */
	public String mVersion = null;

	/**
	 * 应用包名
	 */
	public String mPkgName = null;

	/**
	 * 上报ID
	 */
	public String mReportId = null;

	/**
	 * 当前动作说明
	 */
	public int mActionVerb = ACTIONVERB_INSTALL;

	/**
	 * 是否覆盖安装
	 */
	public boolean mInstallReplaced = false;

	/**
	 * 黑屏下安装
	 */
	public boolean mInstallOnPowerOff = false;

	@Override
	public String toString() {
		return (mId + " " + mUrl + ")");
	}
}
