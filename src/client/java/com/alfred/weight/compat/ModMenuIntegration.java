package com.alfred.weight.compat;

import com.alfred.weight.WeightConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.gui.ConfigScreenProvider;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigScreenProvider<WeightConfig> provider = (ConfigScreenProvider<WeightConfig>) AutoConfig.getConfigScreen(WeightConfig.class, parent);
            provider.setI13nFunction(manager -> "config.player-weight");
            provider.setOptionFunction((baseI13n, field) -> String.format("%s.%s", baseI13n, field.getName()));
            provider.setCategoryFunction((baseI13n, categoryName) -> String.format("%s.%s", baseI13n, categoryName));
            return provider.get();
        };
    }
}
