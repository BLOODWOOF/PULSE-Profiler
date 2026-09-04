package com.bloodwolf.pulse.collect;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChunkTraffic {
	private static final Map<ResourceKey<Level>, AtomicInteger> loads = new ConcurrentHashMap<>();
	private static final Map<ResourceKey<Level>, AtomicInteger> unloads = new ConcurrentHashMap<>();

	private ChunkTraffic() {}

	public static void reset() {
		loads.clear();
		unloads.clear();
	}

	public static void loaded(ServerLevel world) {
		loads.computeIfAbsent(world.dimension(), k -> new AtomicInteger()).incrementAndGet();
	}

	public static void unloaded(ServerLevel world) {
		unloads.computeIfAbsent(world.dimension(), k -> new AtomicInteger()).incrementAndGet();
	}

	public static int loads(ResourceKey<Level> world) {
		AtomicInteger n = loads.get(world);
		return n == null ? 0 : n.get();
	}

	public static int unloads(ResourceKey<Level> world) {
		AtomicInteger n = unloads.get(world);
		return n == null ? 0 : n.get();
	}
}
