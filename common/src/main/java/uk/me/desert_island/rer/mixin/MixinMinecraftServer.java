package uk.me.desert_island.rer.mixin;

import com.mojang.datafixers.DataFixer;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.me.desert_island.rer.WorldGenState;

import java.net.Proxy;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Shadow private PlayerList playerList;

    @Shadow private int tickCount;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory, CallbackInfo ci) {
        WorldGenState.persistentStateManagerMap.clear();
    }

    @Inject(method = "tickChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tick(Ljava/util/function/BooleanSupplier;)V"))
    private void tickWorlds(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        if (this.tickCount % 200 == 0) {
            for (ResourceKey<Level> world : WorldGenState.persistentStateManagerMap.keySet()) {
                WorldGenState state = WorldGenState.byWorld(world);
                if (!state.playerDirty.isEmpty()) {
                    state.lockPlayerDirty();
                    FriendlyByteBuf buf = state.toNetwork(true, new FriendlyByteBuf(Unpooled.buffer()), state.playerDirty);
                    state.sendToPlayers(playerList.getPlayers(), buf, world);
                    state.playerDirty.clear();
                    state.unlockPlayerDirty();
                }
            }
        }
    }
}
