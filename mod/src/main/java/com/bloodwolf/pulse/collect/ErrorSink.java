package com.bloodwolf.pulse.collect;

import com.bloodwolf.pulse.Pulse;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ErrorSink {
	private static final Map<String, Entry> byHash = new ConcurrentHashMap<>();

	public record Entry(
		long at,
		String level,
		String logger,
		String thread,
		String message,
		String stack,
		String fingerprint,
		int count
	) {
		Entry bump() {
			return new Entry(at, level, logger, thread, message, stack, fingerprint, count + 1);
		}
	}

	private ErrorSink() {}

	public static void installUncaught() {
		Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((thread, err) -> {
			record("ERROR", "uncaught", thread.getName(), err.getMessage(), stack(err), err);
			if (previous != null) {
				previous.uncaughtException(thread, err);
			} else {
				err.printStackTrace();
			}
		});
	}

	public static void fromLog(LogEvent event) {
		if (!event.getLevel().isMoreSpecificThan(Level.ERROR)) {
			return;
		}
		Throwable thrown = event.getThrown();
		String msg = event.getMessage() == null ? "" : event.getMessage().getFormattedMessage();
		record(
			event.getLevel().name(),
			event.getLoggerName(),
			event.getThreadName(),
			msg,
			thrown == null ? "" : stack(thrown),
			thrown
		);
	}

	public static void record(String level, String logger, String thread, String message, String stack, Throwable thrown) {
		String fp = fingerprint(logger, message, stack);
		byHash.compute(fp, (k, old) -> {
			if (old == null) {
				if (byHash.size() >= Pulse.config().errorCap) {
					return null;
				}
				return new Entry(System.currentTimeMillis(), level, logger, thread, message == null ? "" : message, stack, fp, 1);
			}
			return old.bump();
		});
	}

	public static List<Entry> snapshot() {
		return new ArrayList<>(byHash.values());
	}

	public static void clear() {
		byHash.clear();
	}

	private static String stack(Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	private static String fingerprint(String logger, String message, String stack) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(String.valueOf(logger).getBytes(StandardCharsets.UTF_8));
			md.update(String.valueOf(message).getBytes(StandardCharsets.UTF_8));
			String head = stack == null ? "" : stack.lines().limit(8).reduce("", (a, b) -> a + b);
			md.update(head.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(md.digest()).substring(0, 16);
		} catch (Exception e) {
			return Integer.toHexString((logger + message).hashCode());
		}
	}
}
