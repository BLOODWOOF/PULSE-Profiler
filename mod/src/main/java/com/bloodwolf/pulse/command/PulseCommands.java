package com.bloodwolf.pulse.command;

import com.bloodwolf.pulse.Pulse;
import com.bloodwolf.pulse.collect.Sampler;
import com.bloodwolf.pulse.collect.TickClock;
import com.bloodwolf.pulse.collect.WorldProbe;
import com.bloodwolf.pulse.report.ReportBuilder;
import com.bloodwolf.pulse.report.ReportIO;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class PulseCommands {
	private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "pulse-timer");
		t.setDaemon(true);
		return t;
	});
	private static volatile ScheduledFuture<?> autoStop;

	private PulseCommands() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, selection) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("pulse")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> help(ctx.getSource()))
				.then(Commands.literal("profiler")
					.then(Commands.literal("start")
						.executes(ctx -> start(ctx.getSource(), Pulse.config().defaultDurationSeconds, Pulse.config().defaultIntervalMs))
						.then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
							.executes(ctx -> start(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "seconds"), Pulse.config().defaultIntervalMs))
							.then(Commands.argument("intervalMs", IntegerArgumentType.integer(1, 200))
								.executes(ctx -> start(
									ctx.getSource(),
									IntegerArgumentType.getInteger(ctx, "seconds"),
									IntegerArgumentType.getInteger(ctx, "intervalMs")
								)))))
					.then(Commands.literal("stop").executes(ctx -> stop(ctx.getSource(), "profiler", true, Pulse.config().includeHeapOnProfilerStop))))
				.then(Commands.literal("health").executes(ctx -> snapshot(ctx.getSource(), "health", false, false)))
				.then(Commands.literal("heap").executes(ctx -> snapshot(ctx.getSource(), "heap", false, true)))
				.then(Commands.literal("errors").executes(ctx -> snapshot(ctx.getSource(), "errors", false, false)))
				.then(Commands.literal("tps").executes(ctx -> tps(ctx.getSource())))
				.then(Commands.literal("status").executes(ctx -> tps(ctx.getSource())))
				.then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource())))
				.then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
		);
	}

	private static int help(CommandSourceStack source) {
		source.sendSuccess(() -> PulseStyle.prefix().append(PulseStyle.muted("commands")), false);
		source.sendSuccess(() -> PulseStyle.muted("  /pulse profiler start [seconds] [intervalMs]"), false);
		source.sendSuccess(() -> PulseStyle.muted("  /pulse profiler stop  |  health  |  heap  |  errors  |  tps  |  reload"), false);
		source.sendSuccess(() -> PulseStyle.muted("  Settings: config/pulse.json"), false);
		return 1;
	}

	private static int reload(CommandSourceStack source) {
		Pulse.reloadConfig();
		source.sendSuccess(() -> PulseStyle.line("config reloaded"), false);
		return 1;
	}

	private static int start(CommandSourceStack source, int seconds, int intervalMs) {
		if (!Sampler.start(intervalMs)) {
			source.sendFailure(PulseStyle.prefix().append(PulseStyle.value("profiler is already running", PulseStyle.BAD)));
			return 0;
		}
		cancelAuto();
		MinecraftServer server = source.getServer();
		autoStop = TIMER.schedule(() -> server.execute(() -> stop(source, "profiler", true, Pulse.config().includeHeapOnProfilerStop)), seconds, TimeUnit.SECONDS);
		source.sendSuccess(
			() -> PulseStyle.prefix()
				.append(PulseStyle.muted("sampling every "))
				.append(PulseStyle.value(intervalMs + "ms", PulseStyle.CYAN))
				.append(PulseStyle.muted(" for "))
				.append(PulseStyle.value(seconds + "s", PulseStyle.CYAN)),
			true
		);
		return 1;
	}

	private static int stop(CommandSourceStack source, String kind, boolean sampler, boolean heap) {
		if (!Sampler.running() && sampler) {
			source.sendFailure(PulseStyle.prefix().append(PulseStyle.value("profiler is not running", PulseStyle.BAD)));
			return 0;
		}
		cancelAuto();
		Sampler.stop();
		publish(source, kind, sampler, heap);
		return 1;
	}

	private static int snapshot(CommandSourceStack source, String kind, boolean sampler, boolean heap) {
		publish(source, kind, sampler, heap);
		return 1;
	}

	private static void publish(CommandSourceStack source, String kind, boolean sampler, boolean heap) {
		MinecraftServer server = source.getServer();
		source.sendSuccess(() -> PulseStyle.prefix().append(PulseStyle.muted("building " + kind + " report...")), false);
		server.execute(() -> {
			JsonObject report = ReportBuilder.build(kind, sampler, heap);
			Thread worker = new Thread(() -> {
				ReportIO.Result result = ReportIO.saveAndUpload(report);
				server.execute(() -> tell(source, result));
			}, "pulse-upload");
			worker.setDaemon(true);
			worker.start();
		});
	}

	private static void tell(CommandSourceStack source, ReportIO.Result result) {
		if (result.file() != null) {
			source.sendSuccess(
				() -> PulseStyle.prefix().append(PulseStyle.muted("saved ")).append(PulseStyle.value(result.file().getFileName().toString(), PulseStyle.WHITE)),
				false
			);
		}
		if (result.viewerUrl() != null) {
			String url = result.viewerUrl();
			MutableComponent link = Component.literal(url).withStyle(style -> clickable(style, url).withUnderlined(true).withColor(net.minecraft.network.chat.TextColor.fromRgb(PulseStyle.CYAN)));
			source.sendSuccess(() -> PulseStyle.prefix().append(PulseStyle.muted("viewer ")).append(link), true);
		} else if (result.error() != null) {
			source.sendFailure(PulseStyle.prefix().append(PulseStyle.value("upload failed: " + result.error(), PulseStyle.BAD)));
		}
	}

	private static Style clickable(Style style, String url) {
		try {
			return style.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)));
		} catch (Throwable ignored) {
			return style;
		}
	}

	private static int tps(CommandSourceStack source) {
		List<Double> samples = TickClock.liveSamples();
		double mean = WorldProbe.mean(samples);
		double tps = mean <= 0 ? 20 : Math.min(20.0, 1000.0 / mean);
		double p95 = WorldProbe.percentile(samples, 0.95);
		double max = WorldProbe.max(samples);
		source.sendSuccess(
			() -> PulseStyle.prefix()
				.append(PulseStyle.muted("TPS "))
				.append(PulseStyle.value(String.format("%.2f", tps), PulseStyle.tpsColor(tps)))
				.append(PulseStyle.muted("  MSPT "))
				.append(PulseStyle.value(String.format("%.2f", mean), PulseStyle.msptColor(mean)))
				.append(PulseStyle.muted("  95% "))
				.append(PulseStyle.value(String.format("%.2f", p95), PulseStyle.msptColor(p95)))
				.append(PulseStyle.muted("  max "))
				.append(PulseStyle.value(String.format("%.2f", max), PulseStyle.msptColor(max))),
			false
		);
		return 1;
	}

	private static void cancelAuto() {
		ScheduledFuture<?> future = autoStop;
		if (future != null) {
			future.cancel(false);
			autoStop = null;
		}
	}
}
