package com.bloodwolf.pulse;

import com.bloodwolf.pulse.collect.ChunkTraffic;
import com.bloodwolf.pulse.collect.ErrorSink;
import com.bloodwolf.pulse.collect.TickClock;
import com.bloodwolf.pulse.command.PulseCommands;
import com.bloodwolf.pulse.config.PulseConfig;
import com.bloodwolf.pulse.hud.PulseHud;
import com.bloodwolf.pulse.log.PulseLogAppender;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Pulse implements ModInitializer {
	public static final String ID = "pulse";
	public static final Logger LOG = LoggerFactory.getLogger("Pulse");

	private static volatile MinecraftServer server;
	private static PulseConfig config;

	@Override
	public void onInitialize() {
		config = PulseConfig.load();
		PulseLogAppender.install();
		ErrorSink.installUncaught();
		PulseCommands.register();

		ServerLifecycleEvents.SERVER_STARTED.register(started -> server = started);
		ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> {
			if (server == stopped) {
				server = null;
			}
		});

		ServerTickEvents.START_SERVER_TICK.register(TickClock::onServerTickStart);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			TickClock.onServerTickEnd(server);
			PulseHud.onEndTick(server);
		});
		ServerTickEvents.START_LEVEL_TICK.register(TickClock::onWorldTickStart);
		ServerTickEvents.END_LEVEL_TICK.register(TickClock::onWorldTickEnd);

		ServerChunkEvents.CHUNK_LOAD.register((world, chunk, generated) -> ChunkTraffic.loaded(world));
		ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> ChunkTraffic.unloaded(world));

		LOG.info("Pulse profiler ready");
	}

	public static MinecraftServer server() {
		return server;
	}

	public static PulseConfig config() {
		return config;
	}

	public static void reloadConfig() {
		config = PulseConfig.load();
	}
}
