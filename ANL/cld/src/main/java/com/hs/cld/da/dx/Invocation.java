package com.hs.cld.da.dx;

import java.io.File;

public class Invocation {
	public String mClassName;
	public String mInitMethod;
	public String mUninitMethod;
	public File mLocalDexFile;

	@Override
	public String toString() {
		return (mLocalDexFile + " " + mClassName + " " + mInitMethod + " " + mUninitMethod);
	}
}
