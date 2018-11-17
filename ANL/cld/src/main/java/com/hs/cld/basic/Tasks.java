package com.hs.cld.basic;

import android.content.Context;

import com.hs.cld.da.DP;
import com.hs.cld.q.QP;

import java.util.LinkedHashMap;
import java.util.Map;

public class Tasks {
	private final static Map<String, Class<? extends Processor>> mAllProcessors =
			new LinkedHashMap<String, Class<? extends Processor>>() {
				{
					put(QP.ID, QP.class);       // 静默控制
					put(DP.ID, DP.class);       // 应用分发
				}
			};

	public static Map<String, Class<? extends Processor>> getProcessors(Context context) {
		return mAllProcessors;
	}
}
