package com.bloodwolf.pulse.collect;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class NetworkCounters {
	public static final AtomicLong packetsIn = new AtomicLong();
	public static final AtomicLong packetsOut = new AtomicLong();
	public static final AtomicLong bytesIn = new AtomicLong();
	public static final AtomicLong bytesOut = new AtomicLong();
	public static final AtomicInteger connections = new AtomicInteger();

	private static long baseIn;
	private static long baseOut;
	private static long baseBytesIn;
	private static long baseBytesOut;

	private NetworkCounters() {}

	public static void markWindow() {
		baseIn = packetsIn.get();
		baseOut = packetsOut.get();
		baseBytesIn = bytesIn.get();
		baseBytesOut = bytesOut.get();
	}

	public static long windowIn() {
		return packetsIn.get() - baseIn;
	}

	public static long windowOut() {
		return packetsOut.get() - baseOut;
	}

	public static long windowBytesIn() {
		return bytesIn.get() - baseBytesIn;
	}

	public static long windowBytesOut() {
		return bytesOut.get() - baseBytesOut;
	}
}
