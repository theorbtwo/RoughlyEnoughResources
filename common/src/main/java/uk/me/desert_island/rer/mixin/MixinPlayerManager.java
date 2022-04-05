package uk.me.desert_island.rer.mixin;

import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.me.desert_island.rer.RoughlyEnoughResources;
import uk.me.desert_island.rer.WorldGenState;

import java.util.Collections;

@Mixin(PlayerList.class)
public class MixinPlayerManager {
    @Inject(method = "placeNewPlayer", at = @At(value = "RETURN"))
    private void onPlayerConnect(Connection connection, ServerPlayer player, CallbackInfo ci) {
        for (ResourceKey<Level> world : WorldGenState.persistentStateManagerMap.keySet()) {
            WorldGenState state = WorldGenState.byWorld(world);
            state.sendToPlayers(Collections.singletonList(player), state.toNetwork(false, new FriendlyByteBuf(Unpooled.buffer()), state.buildEverythingLevels()), world);
        }
        RoughlyEnoughResources.sendLootToPlayers(((PlayerList) (Object) this).getServer(), Collections.singletonList(player));
    }

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void onDataPacksReloaded(CallbackInfo info) {
        RoughlyEnoughResources.sendLootToPlayers(((PlayerList) (Object) this).getServer(), ((PlayerList) (Object) this).getPlayers());
    }
}
