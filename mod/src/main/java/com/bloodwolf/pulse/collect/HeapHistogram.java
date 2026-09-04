package com.bloodwolf.pulse.collect;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public final class HeapHistogram {
	public record Row(String className, long instances, long bytes) {}

	private HeapHistogram() {}

	public static List<Row> capture(int limit) {
		List<Row> rows = new ArrayList<>();
		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			ObjectName name = new ObjectName("com.sun.management:type=DiagnosticCommand");
			Object raw = server.invoke(name, "gcClassHistogram", new Object[] { new String[] {} }, new String[] { "[Ljava.lang.String;" });
			if (!(raw instanceof String text)) {
				return rows;
			}
			for (String line : text.split("\n")) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("num") || trimmed.startsWith("Total") || trimmed.startsWith("Class")) {
					continue;
				}
				String[] parts = trimmed.split("\\s+");
				if (parts.length < 4) {
					continue;
				}
				try {
					long instances = Long.parseLong(parts[1]);
					long bytes = Long.parseLong(parts[2]);
					String className = parts[3];
					if (className.toLowerCase(Locale.ROOT).startsWith("[")) {
						continue;
					}
					rows.add(new Row(className, instances, bytes));
				} catch (NumberFormatException ignored) {
					// skip header junk
				}
			}
			rows.sort((a, b) -> Long.compare(b.bytes, a.bytes));
			if (rows.size() > limit) {
				return rows.subList(0, limit);
			}
		} catch (Exception ignored) {
			// diagnostic command is not always available
		}
		return rows;
	}
}
