package com.alfred.weight;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class WeightClient implements ClientModInitializer {
	public static float currentWeight = 0.0f;
	public static float maxWeight = 300.0f;
	public static boolean affectsCreative = false;
	private static final Identifier[] ICONS = new Identifier[] {
			WeightMod.identifier("textures/gui/t0.png"),
			WeightMod.identifier("textures/gui/t1.png"),
			WeightMod.identifier("textures/gui/t2.png"),
			WeightMod.identifier("textures/gui/t3.png"),
			WeightMod.identifier("textures/gui/t4.png"),
			WeightMod.identifier("textures/gui/t5.png"),
			WeightMod.identifier("textures/gui/t6.png")
	};

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(WeightMod.WEIGHT_PACKET, (client, handler, buf, responseSender) ->
			currentWeight = buf.readFloat()
		);
		ClientPlayNetworking.registerGlobalReceiver(WeightMod.WEIGHT_MAX_PACKET, (client, handler, buf, responseSender) ->
			maxWeight = buf.readFloat()
		);
		ClientPlayNetworking.registerGlobalReceiver(WeightMod.CREATIVE_MODE_UPDATE, (client, handler, buf, responseSender) ->
			affectsCreative = buf.readBoolean()
		);
	}

	public static void render(DrawContext context, TextRenderer textRenderer, int x, int y) {
		WeightConfig.DisplayType type = WeightConfig.getInstance().displayType;
		if (type == WeightConfig.DisplayType.NUMBERS) // TODO: make text a smaller font
			context.drawText(textRenderer, WeightClient.currentWeight + "/" + WeightClient.maxWeight, x + 147 - (textRenderer.getWidth(WeightClient.currentWeight + "/" + WeightClient.maxWeight) / 2), y + 68, 4210752, false);
		else if (type == WeightConfig.DisplayType.ICON)
			context.drawTexture(ICONS[MathHelper.clamp(Math.round(WeightClient.currentWeight / (WeightClient.maxWeight != 0 ? WeightClient.maxWeight : 1) * (ICONS.length - 2)), 0, ICONS.length - 1)], x + 131, y + 64, 12, 12, 12, 12, 12, 12);
	}
}