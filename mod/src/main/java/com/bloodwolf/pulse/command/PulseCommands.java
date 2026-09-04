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
import net.minecraft.server.level.ServerLevel;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.net.URI;
import java.nio.file.Path;
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
					.executes(ctx -> profilerStatus(ctx.getSource()))
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
					.then(Commands.literal("stop").executes(ctx -> stop(ctx.getSource()))))
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
		source.sendSuccess(() -> PulseStyle.muted("  /pulse profiler stop  |  health  |  heap  |  errors  |  tps"), false);
		source.sendSuccess(() -> PulseStyle.muted("  Reports save to config/pulse/reports/  —  drop them on the viewer"), false);
		return 1;
	}

	private static int reload(CommandSourceStack source) {
		Pulse.reloadConfig();
		source.sendSuccess(() -> PulseStyle.line("config reloaded"), false);
		return 1;
	}

	private static int profilerStatus(CommandSourceStack source) {
		if (!Sampler.running()) {
			source.sendSuccess(() -> PulseStyle.prefix().append(PulseStyle.muted("profiler is idle  —  /pulse profiler start")), false);
			return 1;
		}
		long elapsed = Math.max(0, System.currentTimeMillis() - Sampler.startedAt()) / 1000L;
		source.sendSuccess(
			() -> PulseStyle.prefix()
				.append(PulseStyle.muted("sampling "))
				.append(PulseStyle.value(elapsed + "s", PulseStyle.CYAN))
				.append(PulseStyle.muted("  interval "))
				.append(PulseStyle.value(Sampler.intervalMs() + "ms", PulseStyle.CYAN))
				.append(PulseStyle.muted("  dumps "))
				.append(PulseStyle.value(String.valueOf(Sampler.dumps()), PulseStyle.WHITE)),
			false
		);
		return 1;
	}

	private static int start(CommandSourceStack source, int seconds, int intervalMs) {
		if (!Sampler.start(intervalMs)) {
			source.sendFailure(PulseStyle.prefix().append(PulseStyle.value("profiler is already running", PulseStyle.BAD)));
			return 0;
		}
		cancelAuto();
		MinecraftServer server = source.getServer();
		autoStop = TIMER.schedule(() -> server.execute(() -> stop(source)), seconds, TimeUnit.SECONDS);
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

	private static int stop(CommandSourceStack source) {
		if (!Sampler.running()) {
			source.sendFailure(PulseStyle.prefix().append(PulseStyle.value("profiler is not running", PulseStyle.BAD)));
			return 0;
		}
		cancelAuto();
		Sampler.stop();
		publish(source, "profiler", true, Pulse.config().includeHeapOnProfilerStop);
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
				ReportIO.Result result = ReportIO.save(report);
				server.execute(() -> tell(source, result));
			}, "pulse-save");
			worker.setDaemon(true);
			worker.start();
		});
	}

	private static void tell(CommandSourceStack source, ReportIO.Result result) {
		if (result.file() != null) {
			Path file = result.file();
			String name = file.getFileName().toString();
			MutableComponent saved = PulseStyle.prefix()
				.append(PulseStyle.muted("saved "))
				.append(Component.literal(name).withStyle(style -> copyPath(style, file).withColor(net.minecraft.network.chat.TextColor.fromRgb(PulseStyle.WHITE)).withUnderlined(true)));
			source.sendSuccess(() -> saved, false);
			source.sendSuccess(() -> PulseStyle.muted("  click the name to copy the path, then drop the file on the viewer"), false);
			String viewer = Pulse.config().viewerBaseUrl;
			if (viewer != null && !viewer.isBlank()) {
				String url = viewer.replaceAll("/$", "");
				MutableComponent link = Component.literal(url).withStyle(style ->
					openUrl(style, url).withUnderlined(true).withColor(net.minecraft.network.chat.TextColor.fromRgb(PulseStyle.CYAN))
				);
				source.sendSuccess(() -> PulseStyle.prefix().append(PulseStyle.muted("viewer ")).append(link), false);
			}
		}
		if (result.shareUrl() != null) {
			String url = result.shareUrl();
			MutableComponent link = Component.literal(url).withStyle(style ->
				openUrl(style, url).withUnderlined(true).withColor(net.minecraft.network.chat.TextColor.fromRgb(PulseStyle.CYAN))
			);
			source.sendSuccess(() -> PulseStyle.prefix().append(PulseStyle.muted("report ")).append(link), true);
		} else if (result.file() == null && result.error() != null) {
			source.sendFailure(PulseStyle.prefix().append(PulseStyle.value(result.error(), PulseStyle.BAD)));
		}
	}

	private static Style copyPath(Style style, Path file) {
		try {
			return style.withClickEvent(new ClickEvent.CopyToClipboard(file.toAbsolutePath().toString()));
		} catch (Throwable ignored) {
			return style;
		}
	}

	private static Style openUrl(Style style, String url) {
		try {
			return style.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)));
		} catch (Throwable ignored) {
			return style;
		}
	}

	private static int tps(CommandSourceStack source) {
		MutableComponent line = PulseStyle.prefix().append(PulseStyle.muted("TPS"));
		for (TickClock.Window w : TickClock.windows()) {
			if (w.ticks() == 0) {
				continue;
			}
			line.append(PulseStyle.muted("  " + w.label() + " "))
				.append(PulseStyle.value(String.format("%.2f", w.tps()), PulseStyle.tpsColor(w.tps())));
		}
		source.sendSuccess(() -> line, false);

		MutableComponent mspt = PulseStyle.prefix().append(PulseStyle.muted("MSPT"));
		for (TickClock.Window w : TickClock.windows()) {
			if (w.ticks() == 0) {
				continue;
			}
			mspt.append(PulseStyle.muted("  " + w.label() + " "))
				.append(PulseStyle.value(String.format("%.1f", w.mspt()), PulseStyle.msptColor(w.mspt())));
		}
		source.sendSuccess(() -> mspt, false);

		MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		double heapPct = heap.getMax() > 0 ? (100.0 * heap.getUsed() / heap.getMax()) : 0;
		source.sendSuccess(
			() -> PulseStyle.prefix()
				.append(PulseStyle.muted("heap "))
				.append(PulseStyle.value(fmtBytes(heap.getUsed()) + " / " + fmtBytes(heap.getMax()), heapPct > 90 ? PulseStyle.BAD : PulseStyle.WHITE))
				.append(PulseStyle.muted(String.format("  %.0f%%", heapPct))),
			false
		);

		MinecraftServer server = source.getServer();
		for (ServerLevel level : server.getAllLevels()) {
			List<Double> samples = TickClock.worldLiveSamples().getOrDefault(level.dimension(), List.of());
			if (samples.isEmpty()) {
				continue;
			}
			double mean = WorldProbe.mean(samples);
			double tps = TickClock.tps(samples);
			String id = shortWorld(level.dimension().toString());
			source.sendSuccess(
				() -> PulseStyle.prefix()
					.append(PulseStyle.muted(id + "  "))
					.append(PulseStyle.value(String.format("%.2f", tps), PulseStyle.tpsColor(tps)))
					.append(PulseStyle.muted(" TPS  "))
					.append(PulseStyle.value(String.format("%.1f", mean), PulseStyle.msptColor(mean)))
					.append(PulseStyle.muted(" MSPT  "))
					.append(PulseStyle.muted(level.getChunkSource().getLoadedChunksCount() + " chunks  " + level.players().size() + " players")),
				false
			);
		}
		return 1;
	}

	private static String shortWorld(String id) {
		int slash = id.lastIndexOf('/');
		int colon = id.lastIndexOf(':');
		int cut = Math.max(slash, colon);
		return cut >= 0 ? id.substring(cut + 1) : id;
	}

	private static String fmtBytes(long bytes) {
		if (bytes < 1024) {
			return bytes + " B";
		}
		double kb = bytes / 1024.0;
		if (kb < 1024) {
			return String.format("%.0f KB", kb);
		}
		double mb = kb / 1024.0;
		if (mb < 1024) {
			return String.format("%.1f MB", mb);
		}
		return String.format("%.2f GB", mb / 1024.0);
	}

	private static void cancelAuto() {
		ScheduledFuture<?> future = autoStop;
		if (future != null) {
			future.cancel(false);
			autoStop = null;
		}
	}
}
