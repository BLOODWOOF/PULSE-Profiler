package com.bloodwolf.pulse.report;

import com.bloodwolf.pulse.Pulse;
import com.bloodwolf.pulse.collect.ErrorSink;
import com.bloodwolf.pulse.collect.HeapHistogram;
import com.bloodwolf.pulse.collect.NetworkCounters;
import com.bloodwolf.pulse.collect.Sampler;
import com.bloodwolf.pulse.collect.TickClock;
import com.bloodwolf.pulse.collect.WorldProbe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ReportBuilder {
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	private ReportBuilder() {}

	public static JsonObject build(String kind, boolean includeSampler, boolean includeHeap) {
		MinecraftServer server = Pulse.server();
		JsonObject root = new JsonObject();
		root.addProperty("schemaVersion", 1);
		root.addProperty("kind", kind);
		root.addProperty("createdAt", System.currentTimeMillis());
		root.add("platform", platform(server));
		long duration = Sampler.running() ? 0 : Math.max(0, System.currentTimeMillis() - Sampler.startedAt());
		if (Sampler.startedAt() > 0) {
			root.addProperty("durationMs", duration);
		}

		if (Sampler.cpu().isEmpty()) {
			Sampler.captureMetricsNow();
		}

		List<Double> ticks = TickClock.serverSamples();
		if (ticks.isEmpty()) {
			ticks = TickClock.liveSamples();
		}
		root.add("ticks", ticksJson(ticks));
		root.add("cpu", cpuJson());
		root.add("memory", memoryJson());
		root.add("gc", gcJson());
		root.add("network", networkJson(server));
		root.add("errors", errorsJson());

		if (server != null) {
			var worldMs = TickClock.worldSamples();
			if (worldMs.isEmpty()) {
				worldMs = TickClock.worldLiveSamples();
			}
			root.add("worlds", worldsJson(WorldProbe.worlds(server, worldMs)));
			root.add("players", playersJson(server));
			root.addProperty("viewDistance", server.getPlayerList().getViewDistance());
			root.addProperty("simulationDistance", server.getPlayerList().getSimulationDistance());
			WorldProbe.DiskRow disk = WorldProbe.disk(server);
			JsonObject d = new JsonObject();
			d.addProperty("path", disk.path());
			d.addProperty("totalBytes", disk.totalBytes());
			d.addProperty("freeBytes", disk.freeBytes());
			d.addProperty("worldBytes", disk.worldBytes());
			root.add("disk", d);
		}

		if (includeHeap) {
			root.add("heapHistogram", heapJson());
		}
		if (includeSampler) {
			root.add("sampler", samplerJson());
		}
		return root;
	}

	public static String toJson(JsonObject obj) {
		return GSON.toJson(obj);
	}

	private static JsonObject platform(MinecraftServer server) {
		JsonObject p = new JsonObject();
		p.addProperty("minecraft", FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().toString()).orElse("unknown"));
		p.addProperty("loader", "fabric");
		p.addProperty("loaderVersion", FabricLoader.getInstance().getModContainer("fabricloader").map(c -> c.getMetadata().getVersion().toString()).orElse("unknown"));
		p.addProperty("java", Runtime.version().toString());
		p.addProperty("jvm", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));
		p.addProperty("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
		p.addProperty("arch", System.getProperty("os.arch"));
		p.addProperty("cpus", Runtime.getRuntime().availableProcessors());
		p.addProperty("maxHeap", Runtime.getRuntime().maxMemory());
		JsonArray args = new JsonArray();
		for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
			String lower = arg.toLowerCase(Locale.ROOT);
			if (lower.contains("pass") || lower.contains("token") || lower.contains("secret")) {
				continue;
			}
			args.add(arg);
		}
		p.add("jvmArgs", args);
		JsonArray mods = new JsonArray();
		for (var row : Sampler.mods()) {
			JsonObject m = new JsonObject();
			m.addProperty("id", String.valueOf(row.get("id")));
			m.addProperty("version", String.valueOf(row.get("version")));
			m.addProperty("name", String.valueOf(row.get("name")));
			mods.add(m);
		}
		p.add("mods", mods);
		if (server != null) {
			p.addProperty("brand", server.getServerModName());
			p.addProperty("playerCount", server.getPlayerList().getPlayerCount());
			p.addProperty("uptimeMs", server.getTickCount() * 50L);
			if (!Pulse.config().serverName.isBlank()) {
				p.addProperty("serverName", Pulse.config().serverName);
			}
		}
		return p;
	}

	private static JsonObject ticksJson(List<Double> ticks) {
		JsonObject o = new JsonObject();
		double mean = WorldProbe.mean(ticks);
		double tps = mean <= 0 ? 20 : Math.min(20.0, 1000.0 / mean);
		o.addProperty("count", ticks.size());
		o.addProperty("tps", tps);
		o.addProperty("msptMean", mean);
		o.addProperty("msptP95", WorldProbe.percentile(ticks, 0.95));
		o.addProperty("msptP99", WorldProbe.percentile(ticks, 0.99));
		o.addProperty("msptMax", WorldProbe.max(ticks));
		JsonArray windows = new JsonArray();
		for (TickClock.Window w : TickClock.windows()) {
			if (w.ticks() == 0) {
				continue;
			}
			JsonObject win = new JsonObject();
			win.addProperty("label", w.label());
			win.addProperty("ticks", w.ticks());
			win.addProperty("tps", w.tps());
			win.addProperty("mspt", w.mspt());
			windows.add(win);
		}
		o.add("windows", windows);
		JsonArray series = new JsonArray();
		int step = Math.max(1, ticks.size() / 400);
		for (int i = 0; i < ticks.size(); i += step) {
			JsonObject p = new JsonObject();
			p.addProperty("t", i * 50L);
			p.addProperty("ms", ticks.get(i));
			series.add(p);
		}
		o.add("series", series);
		JsonArray spikes = new JsonArray();
		for (TickClock.Spike spike : TickClock.spikes()) {
			JsonObject s = new JsonObject();
			s.addProperty("t", spike.tMs());
			s.addProperty("ms", spike.ms());
			JsonArray stacks = new JsonArray();
			for (Sampler.ThreadSnap snap : spike.stacks()) {
				JsonObject st = new JsonObject();
				st.addProperty("thread", snap.thread());
				JsonArray frames = new JsonArray();
				for (String f : snap.frames()) {
					frames.add(f);
				}
				st.add("frames", frames);
				stacks.add(st);
			}
			s.add("stacks", stacks);
			spikes.add(s);
		}
		o.add("spikes", spikes);
		return o;
	}

	private static JsonObject cpuJson() {
		JsonObject o = new JsonObject();
		JsonArray samples = new JsonArray();
		for (Sampler.CpuPoint p : Sampler.cpu()) {
			JsonObject n = new JsonObject();
			n.addProperty("t", p.t());
			n.addProperty("process", p.process());
			n.addProperty("system", p.system());
			n.addProperty("threads", p.threads());
			samples.add(n);
		}
		o.add("samples", samples);
		return o;
	}

	private static JsonObject memoryJson() {
		JsonObject o = new JsonObject();
		JsonArray series = new JsonArray();
		for (Sampler.MemPoint p : Sampler.memory()) {
			JsonObject n = new JsonObject();
			n.addProperty("t", p.t());
			n.addProperty("heapUsed", p.heapUsed());
			n.addProperty("heapCommitted", p.heapCommitted());
			n.addProperty("heapMax", p.heapMax());
			n.addProperty("nonHeap", p.nonHeap());
			n.addProperty("metaspace", p.metaspace());
			n.addProperty("direct", p.direct());
			n.addProperty("mapped", p.mapped());
			series.add(n);
		}
		o.add("series", series);
		return o;
	}

	private static JsonObject gcJson() {
		JsonObject o = new JsonObject();
		JsonArray collectors = new JsonArray();
		for (var row : Sampler.gcCollectors()) {
			JsonObject c = new JsonObject();
			c.addProperty("name", String.valueOf(row.get("name")));
			c.addProperty("count", ((Number) row.get("count")).longValue());
			c.addProperty("timeMs", ((Number) row.get("timeMs")).longValue());
			collectors.add(c);
		}
		o.add("collectors", collectors);
		JsonArray series = new JsonArray();
		for (Sampler.GcPoint p : Sampler.gc()) {
			JsonObject n = new JsonObject();
			n.addProperty("t", p.t());
			n.addProperty("pauseMs", p.pauseMs());
			n.addProperty("name", p.name());
			series.add(n);
		}
		o.add("series", series);
		return o;
	}

	private static JsonObject networkJson(MinecraftServer server) {
		JsonObject o = new JsonObject();
		o.addProperty("packetsIn", NetworkCounters.windowIn());
		o.addProperty("packetsOut", NetworkCounters.windowOut());
		o.addProperty("bytesIn", NetworkCounters.windowBytesIn());
		o.addProperty("bytesOut", NetworkCounters.windowBytesOut());
		int connections = server == null ? 0 : server.getPlayerList().getPlayerCount();
		o.addProperty("connections", Math.max(connections, NetworkCounters.connections.get()));
		o.add("topIn", packetTop(NetworkCounters.topIn(16)));
		o.add("topOut", packetTop(NetworkCounters.topOut(16)));
		JsonArray series = new JsonArray();
		for (Sampler.NetPoint p : Sampler.network()) {
			JsonObject n = new JsonObject();
			n.addProperty("t", p.t());
			n.addProperty("in", p.in());
			n.addProperty("out", p.out());
			n.addProperty("bytesIn", p.bytesIn());
			n.addProperty("bytesOut", p.bytesOut());
			series.add(n);
		}
		o.add("series", series);
		return o;
	}

	private static JsonArray packetTop(List<java.util.Map.Entry<String, Long>> rows) {
		JsonArray arr = new JsonArray();
		for (var e : rows) {
			JsonObject o = new JsonObject();
			o.addProperty("type", e.getKey());
			o.addProperty("count", e.getValue());
			arr.add(o);
		}
		return arr;
	}

	private static JsonArray worldsJson(List<WorldProbe.WorldRow> worlds) {
		JsonArray arr = new JsonArray();
		for (WorldProbe.WorldRow w : worlds) {
			JsonObject o = new JsonObject();
			o.addProperty("id", w.id());
			o.addProperty("msptMean", w.msptMean());
			o.addProperty("chunks", w.chunks());
			o.addProperty("entities", w.entities());
			o.addProperty("players", w.players());
			o.addProperty("chunkLoads", w.chunkLoads());
			o.addProperty("chunkUnloads", w.chunkUnloads());
			o.addProperty("tickingBlockEntities", w.tickingBlockEntities());
			o.addProperty("difficulty", w.difficulty());
			o.addProperty("dayTime", w.dayTime());
			o.addProperty("raining", w.raining());
			o.addProperty("thundering", w.thundering());
			JsonArray types = new JsonArray();
			for (WorldProbe.EntityCount e : w.entityTypes()) {
				JsonObject t = new JsonObject();
				t.addProperty("type", e.type());
				t.addProperty("count", e.count());
				types.add(t);
			}
			o.add("entityTypes", types);
			arr.add(o);
		}
		return arr;
	}

	private static JsonArray playersJson(MinecraftServer server) {
		JsonArray arr = new JsonArray();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			JsonObject o = new JsonObject();
			o.addProperty("name", Pulse.config().anonymizePlayers ? "player" : player.getGameProfile().name());
			o.addProperty("ping", player.connection.latency());
			o.addProperty("world", player.level().dimension().toString());
			o.addProperty("gameMode", player.gameMode.getGameModeForPlayer().getSerializedName());
			arr.add(o);
		}
		return arr;
	}

	private static JsonArray errorsJson() {
		List<ErrorSink.Entry> entries = new ArrayList<>(ErrorSink.snapshot());
		entries.sort(Comparator.comparingInt(ErrorSink.Entry::count).reversed());
		JsonArray arr = new JsonArray();
		for (ErrorSink.Entry e : entries) {
			JsonObject o = new JsonObject();
			o.addProperty("at", e.at());
			o.addProperty("level", e.level());
			o.addProperty("logger", e.logger());
			o.addProperty("thread", e.thread());
			o.addProperty("message", e.message());
			o.addProperty("stack", e.stack());
			o.addProperty("count", e.count());
			o.addProperty("fingerprint", e.fingerprint());
			arr.add(o);
		}
		return arr;
	}

	private static JsonArray heapJson() {
		JsonArray arr = new JsonArray();
		for (HeapHistogram.Row row : HeapHistogram.capture(Pulse.config().maxHistogramRows)) {
			JsonObject o = new JsonObject();
			o.addProperty("className", row.className());
			o.addProperty("instances", row.instances());
			o.addProperty("bytes", row.bytes());
			arr.add(o);
		}
		return arr;
	}

	private static JsonObject samplerJson() {
		JsonObject o = new JsonObject();
		o.addProperty("intervalMs", Sampler.intervalMs());
		o.addProperty("threadDumps", Sampler.dumps());
		o.addProperty("overheadMs", Sampler.overheadNs() / 1_000_000.0);
		JsonObject states = new JsonObject();
		Sampler.states().forEach((name, bucket) -> {
			JsonObject s = new JsonObject();
			s.addProperty("runnable", bucket[0]);
			s.addProperty("blocked", bucket[1]);
			s.addProperty("waiting", bucket[2]);
			s.addProperty("other", bucket[3]);
			states.add(name, s);
		});
		o.add("threadStates", states);
		JsonObject locks = new JsonObject();
		Sampler.lockWait().entrySet().stream()
			.sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
			.limit(16)
			.forEach(e -> locks.addProperty(e.getKey(), e.getValue()));
		o.add("lockWait", locks);
		JsonObject groups = new JsonObject();
		Sampler.groups().forEach((name, group) -> {
			JsonObject g = new JsonObject();
			g.addProperty("samples", group.samples);
			g.add("root", nodeJson(group.root, Math.max(1, group.samples)));
			groups.add(name, g);
		});
		o.add("groups", groups);
		return o;
	}

	private static JsonObject nodeJson(Sampler.Node node, int rootSamples) {
		JsonObject o = new JsonObject();
		o.addProperty("name", node.name);
		o.addProperty("samples", node.samples);
		o.addProperty("self", node.self);
		if (node.mod != null) {
			o.addProperty("mod", node.mod);
		}
		JsonArray children = new JsonArray();
		List<Sampler.Node> kids = new ArrayList<>(node.children.values());
		kids.sort((a, b) -> Integer.compare(b.samples, a.samples));
		double prune = Pulse.config().pruneBelowPercent;
		for (Sampler.Node child : kids) {
			if (rootSamples > 0 && (child.samples * 100.0 / rootSamples) < prune) {
				continue;
			}
			children.add(nodeJson(child, rootSamples));
		}
		o.add("children", children);
		return o;
	}
}
