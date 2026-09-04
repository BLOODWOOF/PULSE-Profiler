package com.bloodwolf.pulse.collect;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class WorldProbe {
	public record EntityCount(String type, int count) {}

	public record WorldRow(
		String id,
		double msptMean,
		int chunks,
		int entities,
		int players,
		List<EntityCount> entityTypes,
		int chunkLoads,
		int chunkUnloads,
		int tickingBlockEntities,
		String difficulty,
		long dayTime,
		boolean raining,
		boolean thundering
	) {}

	public record DiskRow(String path, long totalBytes, long freeBytes, long worldBytes) {}

	private WorldProbe() {}

	public static List<WorldRow> worlds(MinecraftServer server, Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, List<Double>> worldMs) {
		List<WorldRow> rows = new ArrayList<>();
		for (ServerLevel level : server.getAllLevels()) {
			Map<EntityType<?>, Integer> counts = new HashMap<>();
			int total = 0;
			for (Entity entity : level.getAllEntities()) {
				total++;
				counts.merge(entity.getType(), 1, Integer::sum);
			}
			List<EntityCount> types = new ArrayList<>();
			for (var entry : counts.entrySet()) {
				types.add(new EntityCount(EntityType.getKey(entry.getKey()).toString(), entry.getValue()));
			}
			types.sort(Comparator.comparingInt(EntityCount::count).reversed());
			if (types.size() > 24) {
				types = new ArrayList<>(types.subList(0, 24));
			}

			List<Double> samples = worldMs.getOrDefault(level.dimension(), List.of());
			double mean = mean(samples);
			int chunks = level.getChunkSource().getLoadedChunksCount();
			int players = level.players().size();
			int tickingBe = tickingBlockEntities(level);
			rows.add(new WorldRow(
				level.dimension().toString(),
				mean,
				chunks,
				total,
				players,
				types,
				ChunkTraffic.loads(level.dimension()),
				ChunkTraffic.unloads(level.dimension()),
				tickingBe,
				level.getDifficulty().getSerializedName(),
				level.getGameTime(),
				level.isRaining(),
				level.isThundering()
			));
		}
		return rows;
	}

	public static DiskRow disk(MinecraftServer server) {
		Path world = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
		long worldBytes = com.bloodwolf.pulse.Pulse.config().scanWorldSize ? folderSize(world, 4_000_000_000L) : -1;
		long total = 0;
		long free = 0;
		try {
			FileStore store = Files.getFileStore(world);
			total = store.getTotalSpace();
			free = store.getUsableSpace();
		} catch (IOException ignored) {
		}
		return new DiskRow(world.toAbsolutePath().toString(), total, free, worldBytes);
	}

	private static long folderSize(Path root, long cap) {
		long size = 0;
		if (!Files.exists(root)) {
			return 0;
		}
		try (Stream<Path> walk = Files.walk(root, 8)) {
			for (Path path : (Iterable<Path>) walk::iterator) {
				if (Files.isRegularFile(path)) {
					size += Files.size(path);
					if (size > cap) {
						return size;
					}
				}
			}
		} catch (IOException ignored) {
		}
		return size;
	}

	private static int tickingBlockEntities(ServerLevel level) {
		if (!com.bloodwolf.pulse.Pulse.config().deepWorldScan) {
			return -1;
		}
		try {
			for (var field : ServerLevel.class.getDeclaredFields()) {
				String n = field.getName().toLowerCase();
				if (n.contains("blockentityticker") || n.contains("blockentitytick")) {
					field.setAccessible(true);
					Object value = field.get(level);
					if (value instanceof java.util.Collection<?> col) {
						return col.size();
					}
				}
			}
		} catch (Exception ignored) {
		}
		return -1;
	}

	public static double mean(List<Double> values) {
		if (values.isEmpty()) {
			return 0;
		}
		double s = 0;
		for (double v : values) {
			s += v;
		}
		return s / values.size();
	}

	public static double percentile(List<Double> values, double p) {
		if (values.isEmpty()) {
			return 0;
		}
		List<Double> copy = new ArrayList<>(values);
		copy.sort(Double::compareTo);
		int idx = (int) Math.min(copy.size() - 1, Math.round((copy.size() - 1) * p));
		return copy.get(idx);
	}

	public static double max(List<Double> values) {
		double m = 0;
		for (double v : values) {
			if (v > m) {
				m = v;
			}
		}
		return m;
	}
}
