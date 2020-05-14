package uk.me.desert_island.rer.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import net.minecraft.class_5219;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UserCache;
import net.minecraft.world.dimension.DimensionType;
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
    private void init(LevelStorage.Session session, class_5219 arg, Proxy proxy, DataFixer dataFixer, CommandManager commandManager, MinecraftSessionService minecraftSessionService, GameProfileRepository gameProfileRepository, UserCache userCache, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory, CallbackInfo ci) {
        WorldGenState.persistentStateManagerMap.clear();
    }

    @Inject(method = "tickWorlds", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;tick(Ljava/util/function/BooleanSupplier;)V"))
    private void tickWorlds(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        if (this.ticks % 100 == 0) {
            for (DimensionType dimensionType : WorldGenState.persistentStateManagerMap.keySet()) {
                WorldGenState state = WorldGenState.byDimension(dimensionType);
                if (state.playerDirty) {
                    CompoundTag tag = state.toTag(new CompoundTag());
                    for (ServerPlayerEntity entity : playerManager.getPlayerList()) {
                        state.sendToPlayer(entity, tag, dimensionType);
                    }
                    state.playerDirty = false;
                }
            }
        }
    }
}
