package de.siphalor.finallydrawers.util;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;

public class ClientProxy {
	public static HitResult getCrosshairTarget() {
		return MinecraftClient.getInstance().crosshairTarget;
	}

	public static void sendPacket(Identifier channel, PacketByteBuf buf) {
		ClientPlayNetworking.send(channel, buf);
	}
}
