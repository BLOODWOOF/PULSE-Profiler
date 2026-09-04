package com.bloodwolf.pulse.command;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class PulseStyle {
	public static final int CYAN = 0x67E8F9;
	public static final int RED = 0xE10600;
	public static final int MUTED = 0x9A9A9A;
	public static final int OK = 0x86EFAC;
	public static final int WARN = 0xFBBF24;
	public static final int BAD = 0xFF4D4D;
	public static final int WHITE = 0xF5F5F5;

	private PulseStyle() {}

	public static Style color(int rgb) {
		return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
	}

	public static MutableComponent prefix() {
		return Component.literal("PULSE").withStyle(color(CYAN).withBold(true))
			.append(Component.literal(" » ").withStyle(color(RED)));
	}

	public static MutableComponent line(String text) {
		return prefix().append(Component.literal(text).withStyle(color(WHITE)));
	}

	public static MutableComponent muted(String text) {
		return Component.literal(text).withStyle(color(MUTED));
	}

	public static MutableComponent value(String text, int rgb) {
		return Component.literal(text).withStyle(color(rgb));
	}

	public static int tpsColor(double tps) {
		if (tps >= 19.5) {
			return OK;
		}
		if (tps >= 15) {
			return WARN;
		}
		return BAD;
	}

	public static int msptColor(double ms) {
		if (ms <= 25) {
			return OK;
		}
		if (ms <= 50) {
			return WARN;
		}
		return BAD;
	}
}
