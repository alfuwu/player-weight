package com.alfred.weight;

import com.alfred.weight.access.ServerPlayerEntityAccessor;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class WeightMod implements ModInitializer {
	private static WeightConfig CONFIG;
	public static final RegistryKey<DamageType> TOO_HEAVY = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, identifier("too_heavy"));
	public static final Identifier WEIGHT_PACKET = identifier("player_weight_update");
	public static final Identifier WEIGHT_MAX_PACKET = identifier("player_max_weight_update");
	public static final Identifier CREATIVE_MODE_UPDATE = identifier("creative_update");

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
		float itemWeight = CONFIG.globalDefaultWeight * item.getCount();
		for (WeightConfig.WeightTuple tuple : CONFIG.modifiers)
			if (find(item, tuple.regex, tuple.text.replace(" ", "_")) || find(item, tuple.regex, tuple.text))
				itemWeight = CONFIG.weightModifiersAreMultiplicative || tuple.text.equalsIgnoreCase("air") ? itemWeight * tuple.modifier : itemWeight + (tuple.modifier * item.getCount());
		if (item.hasNbt()) {
			try {
				NbtList nbtList = null;
				if (item.getNbt().contains("BlockEntityTag", NbtElement.COMPOUND_TYPE) && item.getSubNbt("BlockEntityTag").contains("Items", NbtElement.LIST_TYPE)) // Shulker boxes, chests, barrels, etcetera
					nbtList = item.getSubNbt("BlockEntityTag").getList("Items", NbtElement.COMPOUND_TYPE);
				else if (item.getNbt().contains("Items", NbtElement.LIST_TYPE)) // Bundles
					nbtList = item.getNbt().getList("Items", NbtElement.COMPOUND_TYPE);

				if (nbtList != null)
					for (NbtElement compound : nbtList)
						itemWeight += calculateItemWeight(ItemStack.fromNbt((NbtCompound) compound));
			} catch (Exception ignored) { } // Somebody messed with NBT data and the item's inventory wasn't a list of NBT compounds, ignore
		}
		return itemWeight;
	}

	private static boolean find(ItemStack item, boolean regex, String text) {
		return regex ? item.getItem().toString().toLowerCase().contains(text.toLowerCase()) : Pattern.compile(text.toLowerCase()).matcher(item.getItem().toString().toLowerCase()).find();
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