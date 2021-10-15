package uk.me.desert_island.rer.mixin;

import io.netty.buffer.Unpooled;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.me.desert_island.rer.RoughlyEnoughResources;
import uk.me.desert_island.rer.WorldGenState;

import java.util.Collections;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {
    @Inject(method = "onPlayerConnect", at = @At(value = "RETURN"))
    private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        for (RegistryKey<World> world : WorldGenState.persistentStateManagerMap.keySet()) {
            WorldGenState state = WorldGenState.byWorld(world);
            state.sendToPlayers(Collections.singletonList(player), state.toNetwork(false, new PacketByteBuf(Unpooled.buffer()), state.buildEverythingLevels()), world);
        }
        RoughlyEnoughResources.sendLootToPlayers(((PlayerManager) (Object) this).getServer(), Collections.singletonList(player));
    }

    @Inject(method = "onDataPacksReloaded", at = @At("HEAD"))
    private void onDataPacksReloaded(CallbackInfo info) {
        RoughlyEnoughResources.sendLootToPlayers(((PlayerManager) (Object) this).getServer(), ((PlayerManager) (Object) this).getPlayerList());
    }
}
