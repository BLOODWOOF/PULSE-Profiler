package com.bloodwolf.pulse.collect;

import com.bloodwolf.pulse.Pulse;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TickClock {
	private static final int LIVE_CAP = 32768;
	private static final int WORLD_CAP = 4096;
	private static final int MAX_SAMPLES = 4000;

	private static long serverStartNs;
	private static final TickRing live = new TickRing(LIVE_CAP);
	private static final List<Double> serverMs = new ArrayList<>();
	private static volatile long serverThreadId = -1;
	private static final Map<ResourceKey<Level>, Long> worldStartNs = new ConcurrentHashMap<>();
	private static final Map<ResourceKey<Level>, TickRing> worldLive = new ConcurrentHashMap<>();
	private static final Map<ResourceKey<Level>, List<Double>> worldMs = new ConcurrentHashMap<>();
	private static final List<Spike> spikes = new ArrayList<>();
	private static volatile boolean recording;

	public record Spike(long tMs, double ms, List<Sampler.ThreadSnap> stacks) {}

	public record Window(String label, int ticks, double tps, double mspt) {}

	private TickClock() {}

	public static void beginWindow() {
		recording = true;
		serverMs.clear();
		worldMs.clear();
		spikes.clear();
	}

	public static void endWindow() {
		recording = false;
	}

	public static void onServerTickStart(MinecraftServer server) {
		if (serverThreadId < 0) {
			serverThreadId = Thread.currentThread().threadId();
		}
		serverStartNs = System.nanoTime();
	}

	public static long serverThreadId() {
		return serverThreadId;
	}

	public static void onServerTickEnd(MinecraftServer server) {
		double ms = (System.nanoTime() - serverStartNs) / 1_000_000.0;
		live.add(ms);
		if (!recording) {
			return;
		}
		synchronized (serverMs) {
			if (serverMs.size() < MAX_SAMPLES) {
				serverMs.add(ms);
			}
		}
		if (ms >= Pulse.config().spikeMs) {
			List<Sampler.ThreadSnap> stacks = Sampler.snapshotNow();
			synchronized (spikes) {
				if (spikes.size() < 40) {
					spikes.add(new Spike(windowMs(serverMs.size()), ms, stacks));
				}
			}
		}
	}

	public static void onWorldTickStart(ServerLevel level) {
		worldStartNs.put(level.dimension(), System.nanoTime());
	}

	public static void onWorldTickEnd(ServerLevel level) {
		Long start = worldStartNs.get(level.dimension());
		if (start == null) {
			return;
		}
		double ms = (System.nanoTime() - start) / 1_000_000.0;
		worldLive.computeIfAbsent(level.dimension(), k -> new TickRing(WORLD_CAP)).add(ms);
		if (!recording) {
			return;
		}
		List<Double> list = worldMs.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
		synchronized (list) {
			if (list.size() < MAX_SAMPLES) {
				list.add(ms);
			}
		}
	}

	public static List<Double> liveSamples() {
		return live.last(256);
	}

	public static List<Window> windows() {
		int[] ticks = {100, 200, 1200, 6000, 18000};
		String[] labels = {"5s", "10s", "1m", "5m", "15m"};
		List<Window> out = new ArrayList<>(labels.length);
		for (int i = 0; i < labels.length; i++) {
			List<Double> slice = live.last(ticks[i]);
			out.add(new Window(labels[i], slice.size(), tps(slice), WorldProbe.mean(slice)));
		}
		return out;
	}

	public static List<Double> lastTicks(int n) {
		return live.last(n);
	}

	public static List<Double> serverSamples() {
		synchronized (serverMs) {
			return new ArrayList<>(serverMs);
		}
	}

	public static Map<ResourceKey<Level>, List<Double>> worldSamples() {
		Map<ResourceKey<Level>, List<Double>> copy = new ConcurrentHashMap<>();
		for (var e : worldMs.entrySet()) {
			synchronized (e.getValue()) {
				copy.put(e.getKey(), new ArrayList<>(e.getValue()));
			}
		}
		return copy;
	}

	public static Map<ResourceKey<Level>, List<Double>> worldLiveSamples() {
		Map<ResourceKey<Level>, List<Double>> copy = new LinkedHashMap<>();
		for (var e : worldLive.entrySet()) {
			copy.put(e.getKey(), e.getValue().last(200));
		}
		return copy;
	}

	public static List<Spike> spikes() {
		synchronized (spikes) {
			return new ArrayList<>(spikes);
		}
	}

	public static double tps(List<Double> samples) {
		double mean = WorldProbe.mean(samples);
		if (mean <= 0) {
			return 20;
		}
		return Math.min(20.0, 1000.0 / mean);
	}

	private static long windowMs(int ticks) {
		return ticks * 50L;
	}

	private static final class TickRing {
		private final double[] buf;
		private int head;
		private int count;

		TickRing(int cap) {
			this.buf = new double[cap];
		}

		synchronized void add(double ms) {
			buf[head] = ms;
			head = (head + 1) % buf.length;
			if (count < buf.length) {
				count++;
			}
		}

		synchronized List<Double> last(int n) {
			int take = Math.min(n, count);
			List<Double> out = new ArrayList<>(take);
			int start = (head - take + buf.length) % buf.length;
			for (int i = 0; i < take; i++) {
				out.add(buf[(start + i) % buf.length]);
			}
			return out;
		}
	}
}
