package com.alfred.weight;

import com.alfred.weight.access.ServerPlayerEntityAccessor;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class WeightMod implements ModInitializer {
	private static WeightConfig CONFIG;
	public static final RegistryKey<DamageType> TOO_HEAVY = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, identifier("too_heavy"));

	@Override
	public void onInitialize() {
		// Can't use Jankson for whatever cosmic reason,
		// The gods above decided that lists of WeightTuples shan't be configured
		// Fuck me I guess
		// GSON works tho :')
		CONFIG = AutoConfig.register(WeightConfig.class, GsonConfigSerializer::new).getConfig();
	}

	public static Identifier identifier(String path) {
		return new Identifier("player-weight", path);
	}

	public static float calculateItemWeight(ItemStack item) {
		float itemWeight = CONFIG.globalDefaultWeight;
		for (WeightConfig.WeightTuple tuple : CONFIG.modifiers)
			if (find(item, tuple.regex, tuple.text.toLowerCase()) || find(item, tuple.regex, tuple.text.replace(" ", "_").toLowerCase()))
				itemWeight = CONFIG.weightModifiersAreMultiplicative || tuple.text.equalsIgnoreCase("air") ? itemWeight * tuple.modifier * item.getCount(): itemWeight + (tuple.modifier * item.getCount());
		if (item.hasNbt() && item.getNbt().contains("BlockEntityTag", NbtElement.COMPOUND_TYPE) && item.getSubNbt("BlockEntityTag").contains("Items", NbtElement.LIST_TYPE)) {
			try {
				NbtList nbtList = item.getSubNbt("BlockEntityTag").getList("Items", NbtElement.COMPOUND_TYPE);

				for (NbtElement compound : nbtList)
					itemWeight += calculateItemWeight(ItemStack.fromNbt((NbtCompound) compound));
			} catch (Exception ignored) { } // Somebody messed with NBT data and the item's inventory wasn't a list of NBT compounds, ignore
		}
		return itemWeight;
	}

	private static boolean find(ItemStack item, boolean regex, String text) {
		return Pattern.compile(regex ? text : Pattern.quote(text)).matcher(item.getItem().toString().toLowerCase()).find();
	}

	public static float scale(ServerPlayerEntityAccessor player, float weight, float value, float start, boolean shouldScale) {
		if (shouldScale) {
			float ratio = start / (weight / (start * player.playerWeight$getMaxWeight()));
			return value * ratio;
		}
		return value;
	}

	public static DamageSource tooHeavy(World world) {
		return new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(TOO_HEAVY));
	}
}