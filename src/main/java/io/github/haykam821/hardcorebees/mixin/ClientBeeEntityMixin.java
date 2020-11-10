package io.github.haykam821.hardcorebees.mixin;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.options.ServerList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
@Mixin(BeeEntity.class)
public abstract class ClientBeeEntityMixin extends AnimalEntity {
	private ClientBeeEntityMixin(EntityType<? extends AnimalEntity> type, World world) {
		super(type, world);
	}

	@Unique
	private static void deleteServersWithAddress(String address, ServerList serverList) {
		for (int index = 0; index < serverList.size(); index++) {
			ServerInfo serverInfo = serverList.get(index);
			if (serverInfo.address.equals(address)) {
				serverList.remove(serverInfo);
			}
		}
	}

	@Inject(method = "tryAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/BeeEntity;setHasStung(Z)V"))
	private void onBeeAttack(Entity target, CallbackInfoReturnable<Boolean> ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (target instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity) target;
			if (client.isInSingleplayer()) {
				IntegratedServer server = (IntegratedServer) player.getServer();
				Path path = server.getSavePath(WorldSavePath.ROOT);

				// Delete world
				try {
					FileUtils.forceDelete(path.toFile());
				} catch (IOException exception) {
					this.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 40, 1));
				}

				player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 60));
			} else {
				String address = player.getServer().getServerIp();

				// Remove from server list
				MultiplayerScreen screen = new MultiplayerScreen(new TitleScreen());
				ClientBeeEntityMixin.deleteServersWithAddress(address, screen.getServerList());

				screen.getServerList().saveFile();
			}

			client.openScreen(new CreditsScreen(false, () -> {}));
		}
	}
}