package com.bloodwolf.pulse.collect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class NetworkCounters {
	public static final AtomicLong packetsIn = new AtomicLong();
	public static final AtomicLong packetsOut = new AtomicLong();
	public static final AtomicLong bytesIn = new AtomicLong();
	public static final AtomicLong bytesOut = new AtomicLong();
	public static final AtomicInteger connections = new AtomicInteger();

	private static final ConcurrentHashMap<String, AtomicLong> typesIn = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, AtomicLong> typesOut = new ConcurrentHashMap<>();

	private static long baseIn;
	private static long baseOut;
	private static long baseBytesIn;
	private static long baseBytesOut;

	private NetworkCounters() {}

	public static void inbound(String type, int bytes) {
		packetsIn.incrementAndGet();
		bytesIn.addAndGet(bytes);
		typesIn.computeIfAbsent(type, k -> new AtomicLong()).incrementAndGet();
	}

	public static void outbound(String type, int bytes) {
		packetsOut.incrementAndGet();
		bytesOut.addAndGet(bytes);
		typesOut.computeIfAbsent(type, k -> new AtomicLong()).incrementAndGet();
	}

	public static void markWindow() {
		baseIn = packetsIn.get();
		baseOut = packetsOut.get();
		baseBytesIn = bytesIn.get();
		baseBytesOut = bytesOut.get();
		typesIn.clear();
		typesOut.clear();
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

	public static List<Map.Entry<String, Long>> topIn(int cap) {
		return top(typesIn, cap);
	}

	public static List<Map.Entry<String, Long>> topOut(int cap) {
		return top(typesOut, cap);
	}

	private static List<Map.Entry<String, Long>> top(ConcurrentHashMap<String, AtomicLong> map, int cap) {
		List<Map.Entry<String, Long>> rows = new ArrayList<>();
		for (var e : map.entrySet()) {
			rows.add(Map.entry(e.getKey(), e.getValue().get()));
		}
		rows.sort(Comparator.comparingLong(Map.Entry<String, Long>::getValue).reversed());
		if (rows.size() > cap) {
			return new ArrayList<>(rows.subList(0, cap));
		}
		return rows;
	}
}
