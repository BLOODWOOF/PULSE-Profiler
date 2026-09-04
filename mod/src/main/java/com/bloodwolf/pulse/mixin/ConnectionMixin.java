package com.bloodwolf.pulse.mixin;

import com.bloodwolf.pulse.collect.NetworkCounters;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
	@Inject(method = "channelRead0", at = @At("HEAD"))
	private void pulse$in(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
		NetworkCounters.packetsIn.incrementAndGet();
		NetworkCounters.bytesIn.addAndGet(estimate(packet));
	}

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
	private void pulse$out(Packet<?> packet, CallbackInfo ci) {
		NetworkCounters.packetsOut.incrementAndGet();
		NetworkCounters.bytesOut.addAndGet(estimate(packet));
	}

	private static int estimate(Packet<?> packet) {
		String name = packet.getClass().getSimpleName();
		return 24 + name.length() * 2;
	}
}
