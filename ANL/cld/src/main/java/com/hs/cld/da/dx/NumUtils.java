package com.hs.cld.da.dx;

public class NumUtils {
	public static byte[] int2bytes(int i) {
		return new byte[] {
				(byte) (i & 0xFF),
				(byte) ((i >> 8) & 0xFF),
				(byte) ((i >> 16) & 0xFF),
				(byte) ((i >> 24) & 0xFF)
		};
	}
	
	public static int bytes2int(byte[] b) {
		int i = 0;
		i |= (((int)b[0]) & 0xff);
		i |= ((((int)b[1]) << 8) & 0xff00);
		i |= ((((int)b[2]) << 16) & 0xff0000);
		i |= ((((int)b[3]) << 24) & 0xff000000);
		return i;
	}
	
	public static byte[] long2bytes(long l) {
		return new byte[] {
				(byte) (l & 0xFF),
				(byte) ((l >> 8) & 0xFF),
				(byte) ((l >> 16) & 0xFF),
				(byte) ((l >> 24) & 0xFF),
				(byte) ((l >> 32) & 0xFF),
				(byte) ((l >> 40) & 0xFF),
				(byte) ((l >> 48) & 0xFF),
				(byte) ((l >> 56) & 0xFF)
		};
	}
	
	public static long bytes2long(byte[] b) {
		long l = 0;
		l |= (((long)b[0]) & 0xffL);
		l |= ((((long)b[1]) << 8) & 0xff00L);
		l |= ((((long)b[2]) << 16) & 0xff0000L);
		l |= ((((long)b[3]) << 24) & 0xff000000L);
		l |= ((((long)b[4]) << 32) & 0xff00000000L);
		l |= ((((long)b[5]) << 40) & 0xff0000000000L);
		l |= ((((long)b[6]) << 48) & 0xff000000000000L);
		l |= ((((long)b[7]) << 56) & 0xff00000000000000L);
		return l;
	}
	
	public static byte[] short2bytes(short s) {
		return new byte[] {
				(byte) (s & 0xFF),
				(byte) ((s >> 8) & 0xFF)
		};
	}
	
	public static short bytes2short(byte[] b) {
		short s = 0;
		s |= (short)(((short)b[0]) & 0xff);
		s |= (short)((((short)b[1]) << 8) & 0xff00);
		return s;
	}
}
