package com.bloodwolf.pulse.hud;

import com.bloodwolf.pulse.Pulse;
import com.bloodwolf.pulse.collect.TickClock;
import com.bloodwolf.pulse.collect.WorldProbe;
import com.bloodwolf.pulse.command.PulseStyle;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class PulseHud {
	private static int tick;

	private PulseHud() {}

	public static void onEndTick(MinecraftServer server) {
		if (!Pulse.config().actionBarHud) {
			return;
		}
		tick++;
		if (tick % 20 != 0) {
			return;
		}
		List<Double> samples = TickClock.liveSamples();
		double mean = WorldProbe.mean(samples);
		double tps = mean <= 0 ? 20 : Math.min(20.0, 1000.0 / mean);
		MutableComponent bar = Component.literal("PULSE ").withStyle(PulseStyle.color(PulseStyle.CYAN).withBold(true))
			.append(Component.literal("TPS ").withStyle(PulseStyle.color(PulseStyle.MUTED)))
			.append(PulseStyle.value(String.format("%.1f", tps), PulseStyle.tpsColor(tps)))
			.append(Component.literal("  MSPT ").withStyle(PulseStyle.color(PulseStyle.MUTED)))
			.append(PulseStyle.value(String.format("%.1f", mean), PulseStyle.msptColor(mean)));
		var allow = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (allow.test(player.createCommandSourceStack())) {
				player.sendSystemMessage(bar, true);
			}
		}
	}
}
