package io.github.haykam821.hardcorebees.mixin;

import java.util.Arrays;
import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

@Environment(EnvType.SERVER)
@Mixin(BeeEntity.class)
public abstract class ServerBeeEntityMixin extends AnimalEntity {
	private ServerBeeEntityMixin(EntityType<? extends AnimalEntity> type, World world) {
		super(type, world);
	}

	@Unique
	private static String getBanReason(Random random) {
		char[] zCharacters = new char[random.nextInt(60) + 2];
		Arrays.fill(zCharacters, 'z');
		return "bu" + new String(zCharacters);
	}

	@Inject(method = "tryAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/BeeEntity;setHasStung(Z)V"))
    private void onBeeAttack(Entity target, CallbackInfoReturnable<Boolean> ci) {
		if (target instanceof ServerPlayerEntity) {
			ServerPlayerEntity player = (ServerPlayerEntity) target;
			String banReason = ServerBeeEntityMixin.getBanReason(player.getRandom());

			// Ban user
			BannedPlayerEntry entry = new BannedPlayerEntry(player.getGameProfile(), null, this.getEntityName(), null, banReason);
			player.getServer().getPlayerManager().getUserBanList().add(entry);

			player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 60));
			player.setGameMode(GameMode.SPECTATOR);
		}
	}
}