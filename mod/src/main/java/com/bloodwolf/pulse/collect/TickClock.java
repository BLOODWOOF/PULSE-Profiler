package com.bloodwolf.pulse.collect;

import com.bloodwolf.pulse.Pulse;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TickClock {
	private static final int MAX_SAMPLES = 4000;

	private static long serverStartNs;
	private static final int LIVE_CAP = 256;
	private static final double[] liveRing = new double[LIVE_CAP];
	private static int liveHead;
	private static int liveCount;
	private static final List<Double> serverMs = new ArrayList<>();
	private static volatile long serverThreadId = -1;
	private static final Map<ResourceKey<Level>, Long> worldStartNs = new ConcurrentHashMap<>();
	private static final Map<ResourceKey<Level>, List<Double>> worldMs = new ConcurrentHashMap<>();
	private static final List<Spike> spikes = new ArrayList<>();
	private static volatile boolean recording;

	public record Spike(long tMs, double ms, List<Sampler.ThreadSnap> stacks) {}

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
		liveRing[liveHead] = ms;
		liveHead = (liveHead + 1) & (LIVE_CAP - 1);
		if (liveCount < LIVE_CAP) {
			liveCount++;
		}
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
		if (!recording) {
			return;
		}
		Long start = worldStartNs.get(level.dimension());
		if (start == null) {
			return;
		}
		double ms = (System.nanoTime() - start) / 1_000_000.0;
		worldMs.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
		List<Double> list = worldMs.get(level.dimension());
		synchronized (list) {
			if (list.size() < MAX_SAMPLES) {
				list.add(ms);
			}
		}
	}

	public static List<Double> liveSamples() {
		List<Double> out = new ArrayList<>(liveCount);
		int start = liveCount == LIVE_CAP ? liveHead : 0;
		for (int i = 0; i < liveCount; i++) {
			out.add(liveRing[(start + i) & (LIVE_CAP - 1)]);
		}
		return out;
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

	public static List<Spike> spikes() {
		synchronized (spikes) {
			return new ArrayList<>(spikes);
		}
	}

	private static long windowMs(int ticks) {
		return ticks * 50L;
	}
}
