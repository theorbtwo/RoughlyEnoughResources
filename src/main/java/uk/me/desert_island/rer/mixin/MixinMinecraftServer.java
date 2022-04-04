package uk.me.desert_island.rer.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.util.UserCache;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;
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
    @Shadow private PlayerManager playerManager;

    @Shadow private int ticks;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, MinecraftSessionService sessionService, GameProfileRepository gameProfileRepo, UserCache userCache, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory, CallbackInfo ci) {
        WorldGenState.persistentStateManagerMap.clear();
    }

    @Inject(method = "tickWorlds", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;tick(Ljava/util/function/BooleanSupplier;)V"))
    private void tickWorlds(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        if (this.ticks % 200 == 0) {
            for (RegistryKey<World> world : WorldGenState.persistentStateManagerMap.keySet()) {
                WorldGenState state = WorldGenState.byWorld(world);
                if (!state.playerDirty.isEmpty()) {
                    state.lockPlayerDirty();
                    PacketByteBuf buf = state.toNetwork(true, new PacketByteBuf(Unpooled.buffer()), state.playerDirty);
                    state.sendToPlayers(playerManager.getPlayerList(), buf, world);
                    state.playerDirty.clear();
                    state.unlockPlayerDirty();
                }
            }
        }
    }
}
