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
		List<Double> samples = TickClock.lastTicks(100);
		double mean = WorldProbe.mean(samples);
		double tps = TickClock.tps(samples);
		double tps1m = TickClock.tps(TickClock.lastTicks(1200));
		MutableComponent bar = Component.literal("PULSE ").withStyle(PulseStyle.color(PulseStyle.CYAN).withBold(true))
			.append(Component.literal("5s ").withStyle(PulseStyle.color(PulseStyle.MUTED)))
			.append(PulseStyle.value(String.format("%.1f", tps), PulseStyle.tpsColor(tps)))
			.append(Component.literal("  1m ").withStyle(PulseStyle.color(PulseStyle.MUTED)))
			.append(PulseStyle.value(String.format("%.1f", tps1m), PulseStyle.tpsColor(tps1m)))
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
