package uk.me.desert_island.rer.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.me.desert_island.rer.RoughlyEnoughResources;
import uk.me.desert_island.rer.WorldGenState;

import java.util.Collections;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {
    @Inject(method = "onPlayerConnect",
            at = @At(value = "RETURN"))
    private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        for (DimensionType dimensionType : WorldGenState.persistentStateManagerMap.keySet()) {
            WorldGenState state = WorldGenState.byDimension(dimensionType);
            state.sendToPlayer(player, state.toTag(new CompoundTag()), dimensionType);
        }
        RoughlyEnoughResources.sendLootToPlayers(((PlayerManager) (Object) this).getServer(), Collections.singletonList(player));
    }

    @Inject(method = "onDataPacksReloaded", at = @At("HEAD"))
    private void onDataPacksReloaded(CallbackInfo info) {
        RoughlyEnoughResources.sendLootToPlayers(((PlayerManager) (Object) this).getServer(), ((PlayerManager) (Object) this).getPlayerList());
    }
}
