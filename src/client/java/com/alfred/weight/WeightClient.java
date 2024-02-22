package com.alfred.weight;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class WeightClient implements ClientModInitializer {
	public static float currentWeight = 0.0f;
	public static float maxWeight = 300.0f;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(WeightMod.WEIGHT_PACKET, (client, handler, buf, responseSender) -> {
			currentWeight = buf.readFloat();
		});

		ClientPlayNetworking.registerGlobalReceiver(WeightMod.WEIGHT_MAX_PACKET, (client, handler, buf, responseSender) -> {
			maxWeight = buf.readFloat();
		});
	}
}