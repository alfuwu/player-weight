package com.alfred.weight.mixin.client;

import com.alfred.weight.WeightClient;
import com.alfred.weight.WeightConfig;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> {
    public InventoryScreenMixin(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void displayCurrentWeight(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (WeightConfig.getInstance().displayType == WeightConfig.DisplayType.NUMBERS) // TODO: make text a smaller font
            context.drawText(this.textRenderer, WeightClient.currentWeight + "/" + WeightClient.maxWeight, this.x + 147 - (this.textRenderer.getWidth(WeightClient.currentWeight + "/" + 0.0f) / 2), this.y + 68, 4210752, false);
        else // Not implemented yet
            return;
    }
}
