package com.alfred.weight.mixin;

import com.alfred.weight.WeightConfig;
import com.alfred.weight.WeightMod;
import com.alfred.weight.access.ServerPlayerEntityAccessor;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements ServerPlayerEntityAccessor {
	@Shadow public abstract boolean damage(DamageSource source, float amount);
	@Shadow public abstract ServerWorld getServerWorld();
	@Shadow public abstract boolean isCreative();
	@Shadow public abstract void sendMessage(Text message, boolean overlay);

	@Unique private float currentWeight = 0.0f;
	@Unique private Float maxWeight = null;
	@Unique private boolean oldBl = false;
	@Unique private boolean oldAffectsCreative = false;
	@Unique private static final UUID SPEED_WEIGHT_UUID = UUID.fromString("2E5B6895-58B9-4E20-871F-AD0554C3012E");
	@Unique private EntityAttributeModifier playerSpeedModifier = new EntityAttributeModifier(SPEED_WEIGHT_UUID, "Player weight speed modifier", 0.0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
	@Unique private List<List<ItemStack>> oldInventory = new ArrayList<>();

	public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
	private void readMaxWeight(NbtCompound nbt, CallbackInfo ci) {
		if (nbt.contains("MaxWeight", NbtElement.FLOAT_TYPE)) {
			this.maxWeight = nbt.getFloat("MaxWeight");
			ServerPlayNetworking.send((ServerPlayerEntity) (Object) this, WeightMod.WEIGHT_MAX_PACKET, new PacketByteBuf(Unpooled.copyFloat(maxWeight)));
		}
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
	private void writeMaxWeight(NbtCompound nbt, CallbackInfo ci) {
		if (this.maxWeight != null && this.maxWeight != WeightConfig.getInstance().defaultMaxWeight)
			nbt.putFloat("MaxWeight", this.maxWeight);
	}

	@Override
	public float playerWeight$getMaxWeight() {
		return this.maxWeight != null && this.maxWeight != 0 ? this.maxWeight : WeightConfig.getInstance().defaultMaxWeight != 0 ? WeightConfig.getInstance().defaultMaxWeight : 1;
	}

	@Override
	public void playerWeight$setMaxWeight(float value) {
		this.maxWeight = value;
		ServerPlayNetworking.send((ServerPlayerEntity) (Object) this, WeightMod.WEIGHT_MAX_PACKET, new PacketByteBuf(Unpooled.copyFloat(this.maxWeight)));
	}

	@Unique
	private static boolean isEqual(ItemStack a, ItemStack b) {
		// For some nebulous reason, item NBT is saved in slots that don't contain the item anymore when the item is dropped
		// Thus, either the NBT tags need to be the same, or the item needs to be empty
		return a.getCount() == b.getCount() && (a.getCount() <= 0 || (a.isOf(b.getItem()) && Objects.equals(a.getNbt(), b.getNbt())));
	}

	@Unique
	private static boolean isEqual(List<List<ItemStack>> a, List<DefaultedList<ItemStack>> b) {
		if (a.size() != b.size()) // Break if there's more inventories in `a` or `b`
			return false;
		for (int i = 0; i < a.size(); ++i) {
			if (a.get(i).size() != b.get(i).size()) // Break if one inventory is bigger than the other
				return false;
			for (int j = 0; j < a.get(i).size(); ++j)
				if (!isEqual(a.get(i).get(j), b.get(i).get(j))) // Break if an item is not equal to its counterpart in the other inventory
					return false;
		}
		return true; // Both inventories are (for our purposes) the same
	}

	@Unique
	private void calculateWeight() {
		float totalWeight = 0.0f;
		for (DefaultedList<ItemStack> inventory : this.getInventory().combinedInventory)
			for (ItemStack item : inventory)
				totalWeight += WeightMod.calculateItemWeight(item);
		if (totalWeight >= this.playerWeight$getMaxWeight() && this.currentWeight < this.playerWeight$getMaxWeight() && WeightConfig.getInstance().weightWarningType != WeightConfig.WarningType.NONE)
			this.sendMessage(Text.translatable("warning.player-weight.encumbered").withColor(0xFF1111), WeightConfig.getInstance().weightWarningType == WeightConfig.WarningType.CENTERED_MESSAGE);
		this.currentWeight = totalWeight;

		ServerPlayNetworking.send((ServerPlayerEntity) (Object) this, WeightMod.WEIGHT_PACKET, new PacketByteBuf(Unpooled.copyFloat(this.currentWeight)));
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void applyWeightDrawbacks(CallbackInfo ci) {
		WeightConfig config = WeightConfig.getInstance();
		EntityAttributeInstance moveSpeed = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);

		if (config.affectsCreativeModePlayers != this.oldAffectsCreative)
			ServerPlayNetworking.send((ServerPlayerEntity) (Object) this, WeightMod.CREATIVE_MODE_UPDATE, new PacketByteBuf(Unpooled.copyBoolean(config.affectsCreativeModePlayers)));

		boolean bl = config.affectsCreativeModePlayers || !this.isCreative();
		if (!bl && moveSpeed != null && moveSpeed.getModifier(SPEED_WEIGHT_UUID) != null)
			moveSpeed.removeModifier(SPEED_WEIGHT_UUID); // Remove any speed modifications done to the player when they're in creative mode

		if (isEqual(this.oldInventory, this.getInventory().combinedInventory) && oldBl == this.isCreative() && config.affectsCreativeModePlayers == this.oldAffectsCreative) { // Inventory hasn't changed, don't do (expensive) inventory weight calculations
			if (this.age % 20 == 0) {
				for (WeightConfig.WeightPunishment punishment : config.weightPunishments) {
					if (this.currentWeight > punishment.begin * this.playerWeight$getMaxWeight()) {
						if (punishment.type == WeightConfig.PunishmentType.DAMAGE_PER_SECOND)
							this.damage(WeightMod.tooHeavy(this.getServerWorld()), WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight));
						else if (punishment.type == WeightConfig.PunishmentType.DAMAGE_PER_SECOND_MOUNT && this.hasVehicle())
							this.getVehicle().damage(WeightMod.tooHeavy(this.getVehicle().getWorld()), WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight));
						else if (punishment.type == WeightConfig.PunishmentType.EXHAUSTION_PER_SECOND)
							this.hungerManager.addExhaustion(WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight));
					}
				}
			} else {
				for (WeightConfig.WeightPunishment punishment : config.weightPunishments)
					if (this.currentWeight > punishment.begin * this.playerWeight$getMaxWeight() && punishment.type == WeightConfig.PunishmentType.EXHAUSTION_PER_TICK)
						this.hungerManager.addExhaustion(WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight));
			}
			oldBl = this.isCreative();
			oldAffectsCreative = config.affectsCreativeModePlayers;
			return;
		} else {
			this.oldInventory = new ArrayList<>();
			for (DefaultedList<ItemStack> inventory : this.getInventory().combinedInventory) {
				List<ItemStack> newInventory = new ArrayList<>();
				for (ItemStack item : inventory)
					newInventory.add(item.copy());
				oldInventory.add(newInventory);
			} // Copy inventory
			this.calculateWeight();
		}
		if (bl || this.oldBl != this.isCreative() || config.affectsCreativeModePlayers != this.oldAffectsCreative) {
			float speedModifier = 1.0f;
			if (this.age % 20 == 0) {
				for (WeightConfig.WeightPunishment punishment : config.weightPunishments) {
					if (this.currentWeight > punishment.begin * this.playerWeight$getMaxWeight()) {
						if (punishment.type == WeightConfig.PunishmentType.DAMAGE_PER_SECOND)
							this.damage(WeightMod.tooHeavy(this.getServerWorld()), WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight));
						else if (punishment.type == WeightConfig.PunishmentType.DAMAGE_PER_SECOND_MOUNT && this.hasVehicle())
							this.getVehicle().damage(WeightMod.tooHeavy(this.getVehicle().getWorld()), WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight));
						else if (punishment.type == WeightConfig.PunishmentType.SPEED)
							speedModifier *= WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight);
						else if (punishment.type == WeightConfig.PunishmentType.EXHAUSTION_PER_SECOND)
							this.hungerManager.addExhaustion(WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight));
						else if (punishment.type == WeightConfig.PunishmentType.EXHAUSTION_PER_TICK)
							this.hungerManager.addExhaustion(WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight));
					}
				}
			} else {
				for (WeightConfig.WeightPunishment punishment : config.weightPunishments) {
					if (this.currentWeight > punishment.begin * this.playerWeight$getMaxWeight()) {
						if (punishment.type == WeightConfig.PunishmentType.SPEED)
							speedModifier *= WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight);
						else if (punishment.type == WeightConfig.PunishmentType.EXHAUSTION_PER_TICK)
							this.hungerManager.addExhaustion(WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight));
					}
				}
			}

			if (playerSpeedModifier.getValue() != speedModifier && moveSpeed != null) {
				moveSpeed.removeModifier(SPEED_WEIGHT_UUID);
				playerSpeedModifier = new EntityAttributeModifier(SPEED_WEIGHT_UUID, "Player weight speed modifier", speedModifier - 1.0f, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
				moveSpeed.addTemporaryModifier(playerSpeedModifier);
			}
		}
		oldBl = this.isCreative();
		oldAffectsCreative = config.affectsCreativeModePlayers;
	}

	@Inject(method = "startRiding", at = @At("HEAD"), cancellable = true)
	private void murderMount(Entity entity, boolean force, CallbackInfoReturnable<Boolean> cir) {
		WeightConfig config = WeightConfig.getInstance();
		if (config.affectsCreativeModePlayers || !this.isCreative()) {
			for (WeightConfig.WeightPunishment punishment : WeightConfig.getInstance().weightPunishments) {
				if (this.currentWeight > punishment.begin * this.playerWeight$getMaxWeight()) {
					if (punishment.type == WeightConfig.PunishmentType.PREVENT_MOUNT)
						cir.setReturnValue(false); // Prevent mounting
					else if (punishment.type == WeightConfig.PunishmentType.KILL_MOUNT)
						entity.damage(WeightMod.tooHeavy(entity.getWorld()), Float.MAX_VALUE);
				}
			}
		}
	}
}