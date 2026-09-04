package com.bloodwolf.pulse.collect;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ModIndex {
	private static final Map<String, String> packages = new ConcurrentHashMap<>();
	private static volatile boolean built;

	private ModIndex() {}

	public static String modFor(String className) {
		ensure();
		int dots = 0;
		int cut = className.length();
		for (int i = 0; i < className.length(); i++) {
			if (className.charAt(i) == '.') {
				dots++;
				if (dots == 3) {
					cut = i;
					break;
				}
			}
		}
		String prefix = className.substring(0, cut);
		return packages.get(prefix);
	}

	private static void ensure() {
		if (built) {
			return;
		}
		synchronized (ModIndex.class) {
			if (built) {
				return;
			}
			for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
				String id = mod.getMetadata().getId();
				if ("java".equals(id) || "minecraft".equals(id) || "fabricloader".equals(id)) {
					continue;
				}
				String compact = id.replace("-", "");
				packages.put("com." + compact, id);
				packages.put("net." + compact, id);
			}
			packages.put("com.bloodwolf.pulse", "pulse");
			built = true;
		}
	}
}
