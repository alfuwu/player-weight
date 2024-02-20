package com.alfred.weight.mixin;

import com.alfred.weight.WeightConfig;
import com.alfred.weight.WeightMod;
import com.alfred.weight.access.ServerPlayerEntityAccessor;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements ServerPlayerEntityAccessor {
	@Shadow public abstract boolean damage(DamageSource source, float amount);
	@Shadow public abstract ServerWorld getServerWorld();
	@Unique private float currentWeight = 0.0f;
	@Unique private Float maxWeight = null;
	@Unique private static final UUID SPEED_WEIGHT_UUID = UUID.fromString("2E5B6895-58B9-4E20-871F-AD0554C3012E");
	@Unique private final EntityAttributeModifier playerSpeedModifier = new EntityAttributeModifier(SPEED_WEIGHT_UUID, "Player weight speed modifier", 0.0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

	public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
	private void readMaxWeight(NbtCompound nbt, CallbackInfo ci) {
		if (nbt.contains("MaxWeight", NbtElement.FLOAT_TYPE))
			this.maxWeight = nbt.getFloat("MaxWeight");
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
	private void writeMaxWeight(NbtCompound nbt, CallbackInfo ci) {
		if (this.maxWeight != null)
			nbt.putFloat("MaxWeight", this.maxWeight);
	}

	@Override
	public float playerWeight$getMaxWeight() {
		return this.maxWeight != null ? this.maxWeight : WeightConfig.getInstance().defaultMaxWeight;
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void applyWeightDrawbacks(CallbackInfo ci) {
		WeightConfig config = WeightConfig.getInstance();
		EntityAttributeInstance moveSpeed = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		if (config.affectsCreativeModePlayers) {
			float totalWeight = 0.0f;
			for (DefaultedList<ItemStack> inventory : this.getInventory().combinedInventory)
				for (ItemStack item : inventory)
					totalWeight += WeightMod.calculateItemWeight(item);
			//System.out.println(totalWeight);
			this.currentWeight = totalWeight;

			float speedModifier = 1.0f;
			if (this.age % 20 == 0) {
				for (WeightConfig.WeightPunishment punishment : config.weightPunishments) {
					if (this.currentWeight > punishment.begin * this.playerWeight$getMaxWeight()) {
						if (punishment.type == WeightConfig.PunishmentType.DAMAGE_PER_SECOND)
							this.damage(WeightMod.tooHeavy(this.getServerWorld()), WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight));
						else if (punishment.type == WeightConfig.PunishmentType.DAMAGE_PER_SECOND_MOUNT && this.hasVehicle() && this.getVehicle() instanceof LivingEntity livingVehicle)
							livingVehicle.damage(WeightMod.tooHeavy(livingVehicle.getWorld()), WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight));
						else if (punishment.type == WeightConfig.PunishmentType.SPEED)
							speedModifier *= WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight);
					}
				}
			} else {
				for (WeightConfig.WeightPunishment punishment : config.weightPunishments)
					if (this.currentWeight > punishment.begin * this.playerWeight$getMaxWeight() && punishment.type == WeightConfig.PunishmentType.SPEED)
						speedModifier *= WeightMod.scale(this, this.currentWeight, punishment.value, punishment.begin, punishment.scaleWithWeight);
			}

			if (playerSpeedModifier.getValue() != speedModifier && moveSpeed != null) {
				moveSpeed.removeModifier(playerSpeedModifier.getId());
				moveSpeed.addTemporaryModifier(
						new EntityAttributeModifier(SPEED_WEIGHT_UUID, "Player weight speed modifier", speedModifier - 1.0f, EntityAttributeModifier.Operation.MULTIPLY_TOTAL)
				);
			}
		} else if (moveSpeed.getModifier(SPEED_WEIGHT_UUID) != null) {
			moveSpeed.removeModifier(SPEED_WEIGHT_UUID); // Remove any speed modifications done to the player when they're in creative mode
		}
	}

	@Inject(method = "startRiding", at = @At("HEAD"), cancellable = true)
	private void murderMount(Entity entity, boolean force, CallbackInfoReturnable<Boolean> cir) {
		WeightConfig config = WeightConfig.getInstance();
		if (config.affectsCreativeModePlayers) {
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