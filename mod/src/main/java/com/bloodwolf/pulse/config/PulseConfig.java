package com.bloodwolf.pulse.config;

import com.bloodwolf.pulse.Pulse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PulseConfig {
	public String uploadUrl = "";
	public String viewerBaseUrl = "https://bloodwoof.github.io/PULSE-Profiler";
	public String serverName = "";
	public int spikeMs = 50;
	public int errorCap = 200;
	public int seriesCap = 600;
	public int defaultDurationSeconds = 30;
	public int defaultIntervalMs = 10;
	public int metricIntervalMs = 1000;
	public int maxStackDepth = 96;
	public int maxHistogramRows = 80;
	public double pruneBelowPercent = 0.25;
	public boolean sampleOnlyServerThread = false;
	public boolean includeWaitingThreads = true;
	public boolean autoUpload = false;
	public boolean saveLocal = true;
	public boolean includeHeapOnProfilerStop = true;
	public boolean scanWorldSize = false;
	public boolean deepWorldScan = true;
	public boolean anonymizePlayers = false;
	public boolean actionBarHud = false;
	public boolean fancyChat = true;
	public List<String> sampleGroups = new ArrayList<>();

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("pulse.json");
	}

	public static Path reportsDir() {
		Path dir = FabricLoader.getInstance().getConfigDir().resolve("pulse").resolve("reports");
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			Pulse.LOG.warn("Could not create reports folder", e);
		}
		return dir;
	}

	public static PulseConfig load() {
		Path path = file();
		try {
			if (Files.exists(path)) {
				PulseConfig cfg = GSON.fromJson(Files.readString(path), PulseConfig.class);
				if (cfg == null) {
					return new PulseConfig();
				}
				if (cfg.sampleGroups == null) {
					cfg.sampleGroups = new ArrayList<>();
				}
				return cfg;
			}
			PulseConfig cfg = new PulseConfig();
			Files.writeString(path, GSON.toJson(cfg));
			return cfg;
		} catch (Exception e) {
			Pulse.LOG.warn("Falling back to default Pulse config", e);
			return new PulseConfig();
		}
	}

	public boolean groupAllowed(String group) {
		return sampleGroups == null || sampleGroups.isEmpty() || sampleGroups.contains(group);
	}
}
