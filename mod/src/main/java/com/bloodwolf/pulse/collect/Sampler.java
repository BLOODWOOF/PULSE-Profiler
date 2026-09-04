package com.bloodwolf.pulse.collect;

import com.bloodwolf.pulse.Pulse;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Sampler {
	private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
	private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();
	private static final Object OS = ManagementFactory.getOperatingSystemMXBean();
	private static final Method PROCESS_CPU = cpuMethod("getProcessCpuLoad");
	private static final Method SYSTEM_CPU = cpuMethod("getCpuLoad");
	private static final ConcurrentHashMap<String, String> FRAME_CACHE = new ConcurrentHashMap<>(4096);
	private static volatile boolean running;
	private static volatile Thread worker;
	private static volatile int intervalMs = 10;
	private static volatile int dumps;
	private static volatile long overheadNs;
	private static final Map<String, int[]> states = new ConcurrentHashMap<>();
	private static final Map<String, Group> groups = new ConcurrentHashMap<>();
	private static final List<CpuPoint> cpu = new ArrayList<>();
	private static final List<MemPoint> memory = new ArrayList<>();
	private static final List<NetPoint> network = new ArrayList<>();
	private static final List<GcPoint> gc = new ArrayList<>();
	private static Map<String, Long> lastGcCount = Map.of();
	private static Map<String, Long> lastGcTime = Map.of();
	private static long startedAt;

	public record ThreadSnap(String thread, List<String> frames) {}
	public record CpuPoint(long t, double process, double system, int threads) {}
	public record MemPoint(long t, long heapUsed, long heapCommitted, long heapMax, long nonHeap, long metaspace, long direct, long mapped) {}
	public record NetPoint(long t, long in, long out, long bytesIn, long bytesOut) {}
	public record GcPoint(long t, long pauseMs, String name) {}

	public static final class Node {
		public final String name;
		public final String mod;
		public int samples;
		public int self;
		public final Map<String, Node> children = new HashMap<>();

		Node(String name, String mod) {
			this.name = name;
			this.mod = mod;
		}
	}

	public static final class Group {
		public int samples;
		public final Node root = new Node("root", null);
	}

	private Sampler() {}

	public static synchronized boolean start(int interval) {
		if (running) {
			return false;
		}
		intervalMs = Math.max(1, interval);
		dumps = 0;
		overheadNs = 0;
		groups.clear();
		states.clear();
		FRAME_CACHE.clear();
		cpu.clear();
		memory.clear();
		network.clear();
		gc.clear();
		lastGcCount = gcCounts();
		lastGcTime = gcTimes();
		startedAt = System.currentTimeMillis();
		TickClock.beginWindow();
		ChunkTraffic.reset();
		NetworkCounters.markWindow();
		running = true;
		worker = new Thread(Sampler::loop, "pulse-sampler");
		worker.setDaemon(true);
		worker.start();
		return true;
	}

	public static synchronized void stop() {
		running = false;
		TickClock.endWindow();
		Thread t = worker;
		if (t != null) {
			try {
				t.join(2000);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		}
		worker = null;
	}

	public static boolean running() {
		return running;
	}

	public static int intervalMs() {
		return intervalMs;
	}

	public static int dumps() {
		return dumps;
	}

	public static long startedAt() {
		return startedAt;
	}

	public static Map<String, Group> groups() {
		return groups;
	}

	public static List<CpuPoint> cpu() {
		synchronized (cpu) {
			return new ArrayList<>(cpu);
		}
	}

	public static List<MemPoint> memory() {
		synchronized (memory) {
			return new ArrayList<>(memory);
		}
	}

	public static List<NetPoint> network() {
		synchronized (network) {
			return new ArrayList<>(network);
		}
	}

	public static List<GcPoint> gc() {
		synchronized (gc) {
			return new ArrayList<>(gc);
		}
	}

	public static long overheadNs() {
		return overheadNs;
	}

	public static Map<String, int[]> states() {
		return states;
	}

	public static List<ThreadSnap> snapshotNow() {
		List<ThreadSnap> out = new ArrayList<>();
		for (ThreadInfo info : THREADS.dumpAllThreads(false, false)) {
			if (info == null || info.getStackTrace() == null) {
				continue;
			}
			if (isSampler(info)) {
				continue;
			}
			List<String> frames = new ArrayList<>();
			StackTraceElement[] st = info.getStackTrace();
			int n = Math.min(st.length, 24);
			for (int i = 0; i < n; i++) {
				frames.add(frameName(st[i]));
			}
			out.add(new ThreadSnap(info.getThreadName() + " " + info.getThreadState(), frames));
			if (out.size() >= 8) {
				break;
			}
		}
		return out;
	}

	private static void loop() {
		var cfg = Pulse.config();
		int metricEvery = Math.max(1, cfg.metricIntervalMs);
		long lastMetric = 0;
		while (running) {
			long t0 = System.nanoTime();
			sampleThreads();
			overheadNs += System.nanoTime() - t0;
			long now = System.currentTimeMillis();
			if (now - lastMetric >= metricEvery) {
				lastMetric = now;
				sampleMetrics(now - startedAt);
			}
			long waitNs = intervalMs * 1_000_000L - (System.nanoTime() - t0);
			if (waitNs > 0) {
				try {
					Thread.sleep(waitNs / 1_000_000L, (int) (waitNs % 1_000_000L));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	private static void sampleThreads() {
		dumps++;
		var cfg = Pulse.config();
		ThreadInfo[] infos;
		if (cfg.sampleOnlyServerThread && TickClock.serverThreadId() > 0) {
			ThreadInfo one = THREADS.getThreadInfo(TickClock.serverThreadId(), cfg.maxStackDepth);
			infos = one == null ? new ThreadInfo[0] : new ThreadInfo[] { one };
		} else {
			infos = THREADS.dumpAllThreads(false, false);
		}
		int depth = Math.max(8, cfg.maxStackDepth);
		for (ThreadInfo info : infos) {
			if (info == null || isSampler(info)) {
				continue;
			}
			Thread.State state = info.getThreadState();
			if (!cfg.includeWaitingThreads && (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING)) {
				continue;
			}
			StackTraceElement[] st = info.getStackTrace();
			if (st == null || st.length == 0) {
				continue;
			}
			String groupName = groupName(info.getThreadName());
			if (!cfg.groupAllowed(groupName)) {
				continue;
			}
			int[] bucket = states.computeIfAbsent(groupName, k -> new int[4]);
			if (state == Thread.State.RUNNABLE) {
				bucket[0]++;
			} else if (state == Thread.State.BLOCKED) {
				bucket[1]++;
			} else if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
				bucket[2]++;
			} else {
				bucket[3]++;
			}
			Group group = groups.computeIfAbsent(groupName, k -> new Group());
			group.samples++;
			Node node = group.root;
			node.samples++;
			int from = Math.max(0, st.length - depth);
			for (int i = st.length - 1; i >= from; i--) {
				String name = frameName(st[i]);
				Node child = node.children.get(name);
				if (child == null) {
					child = new Node(name, ModIndex.modFor(st[i].getClassName()));
					node.children.put(name, child);
				}
				child.samples++;
				node = child;
			}
			node.self++;
		}
	}

	public static void captureMetricsNow() {
		if (startedAt == 0) {
			startedAt = System.currentTimeMillis();
		}
		sampleMetrics(System.currentTimeMillis() - startedAt);
	}

	private static void sampleMetrics(long t) {
		double process = cpuLoad(PROCESS_CPU);
		double system = cpuLoad(SYSTEM_CPU);
		int threads = THREADS.getThreadCount();
		synchronized (cpu) {
			trim(cpu, Pulse.config().seriesCap);
			cpu.add(new CpuPoint(t, process, system, threads));
		}

		MemoryUsage heap = MEMORY.getHeapMemoryUsage();
		MemoryUsage non = MEMORY.getNonHeapMemoryUsage();
		long meta = 0;
		for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
			if (pool.getName().toLowerCase(Locale.ROOT).contains("metaspace")) {
				meta = pool.getUsage().getUsed();
			}
		}
		long direct = 0;
		long mapped = 0;
		for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
			if (pool.getName().equals("direct")) {
				direct = pool.getMemoryUsed();
			} else if (pool.getName().equals("mapped")) {
				mapped = pool.getMemoryUsed();
			}
		}
		synchronized (memory) {
			trim(memory, Pulse.config().seriesCap);
			memory.add(new MemPoint(t, heap.getUsed(), heap.getCommitted(), heap.getMax(), non.getUsed(), meta, direct, mapped));
		}

		synchronized (network) {
			trim(network, Pulse.config().seriesCap);
			network.add(new NetPoint(t, NetworkCounters.windowIn(), NetworkCounters.windowOut(), NetworkCounters.windowBytesIn(), NetworkCounters.windowBytesOut()));
		}

		Map<String, Long> counts = gcCounts();
		Map<String, Long> times = gcTimes();
		for (var e : counts.entrySet()) {
			long dc = e.getValue() - lastGcCount.getOrDefault(e.getKey(), 0L);
			long dt = times.getOrDefault(e.getKey(), 0L) - lastGcTime.getOrDefault(e.getKey(), 0L);
			if (dc > 0) {
				synchronized (gc) {
					trim(gc, Pulse.config().seriesCap);
					gc.add(new GcPoint(t, dt, e.getKey()));
				}
			}
		}
		lastGcCount = counts;
		lastGcTime = times;
	}

	private static Map<String, Long> gcCounts() {
		Map<String, Long> map = new LinkedHashMap<>();
		for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			map.put(bean.getName(), bean.getCollectionCount());
		}
		return map;
	}

	private static Map<String, Long> gcTimes() {
		Map<String, Long> map = new LinkedHashMap<>();
		for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			map.put(bean.getName(), bean.getCollectionTime());
		}
		return map;
	}

	private static Method cpuMethod(String name) {
		try {
			Method m = OS.getClass().getMethod(name);
			m.setAccessible(true);
			return m;
		} catch (Exception e) {
			return null;
		}
	}

	private static double cpuLoad(Method method) {
		if (method == null) {
			return 0;
		}
		try {
			double v = ((Number) method.invoke(OS)).doubleValue();
			return v < 0 ? 0 : v;
		} catch (Exception e) {
			return 0;
		}
	}

	private static boolean isSampler(ThreadInfo info) {
		return "pulse-sampler".equals(info.getThreadName());
	}

	private static String groupName(String thread) {
		String n = thread.toLowerCase(Locale.ROOT);
		if (n.contains("server thread") || n.equals("server thread")) {
			return "Server";
		}
		if (n.contains("world") || n.contains("chunk") || n.contains("light")) {
			return "World";
		}
		if (n.contains("netty") || n.contains("nioeventloop") || n.contains("epollev")) {
			return "Netty";
		}
		if (n.contains("g1") || n.contains("gc") || n.contains("zgc") || n.contains("shenandoah")) {
			return "GC";
		}
		if (n.contains("worker") || n.contains("forkjoin") || n.contains("pool-") || n.contains("async")) {
			return "Worker";
		}
		return "Other";
	}

	private static String frameName(StackTraceElement el) {
		String file = el.getFileName() == null ? "" : el.getFileName();
		int line = el.getLineNumber();
		String key = el.getClassName() + "." + el.getMethodName() + "(" + file + (line > 0 ? ":" + line : "") + ")";
		String cached = FRAME_CACHE.putIfAbsent(key, key);
		return cached == null ? key : cached;
	}

	private static <T> void trim(List<T> list, int cap) {
		while (list.size() >= cap) {
			list.removeFirst();
		}
	}

	public static List<Map<String, Object>> gcCollectors() {
		List<Map<String, Object>> out = new ArrayList<>();
		for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("name", bean.getName());
			row.put("count", bean.getCollectionCount());
			row.put("timeMs", bean.getCollectionTime());
			out.add(row);
		}
		out.sort(Comparator.comparing(m -> String.valueOf(m.get("name"))));
		return out;
	}

	public static List<Map<String, Object>> mods() {
		List<Map<String, Object>> out = new ArrayList<>();
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", mod.getMetadata().getId());
			row.put("version", mod.getMetadata().getVersion().toString());
			row.put("name", mod.getMetadata().getName());
			out.add(row);
		}
		out.sort(Comparator.comparing(m -> String.valueOf(m.get("id"))));
		return out;
	}
}
