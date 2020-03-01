// Generally from https://github.com/JamiesWhiteShirt/clothesline-fabric/blob/354ab9a1d0d130fb29cc3479d2e2e3913afb9db6/src/main/java/com/jamieswhiteshirt/clotheslinefabric/mixin/server/world/ServerWorldMixin.java

package uk.me.desert_island.rer.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldSaveHandler;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.me.desert_island.rer.WorldGenState;

import java.util.concurrent.Executor;
import java.util.function.BiFunction;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World {
    protected MixinServerWorld(LevelProperties levelProperties, DimensionType dimensionType, BiFunction<World, Dimension, ChunkManager> biFunction, Profiler profiler, boolean boolean_1) {
        super(levelProperties, dimensionType, biFunction, profiler, boolean_1);
    }

    @Inject(at = @At("RETURN"),
            method = "<init>(Lnet/minecraft/server/MinecraftServer;Ljava/util/concurrent/Executor;Lnet/minecraft/world/WorldSaveHandler;Lnet/minecraft/world/level/LevelProperties;Lnet/minecraft/world/dimension/DimensionType;Lnet/minecraft/util/profiler/Profiler;Lnet/minecraft/server/WorldGenerationProgressListener;)V")
    private void constructor(MinecraftServer server, Executor executor, WorldSaveHandler oldWorldSaveHandler, LevelProperties levelProperties, DimensionType dimensionType, Profiler profiler, WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        PersistentStateManager psm = ((ServerWorld) (Object) this).getPersistentStateManager();
        WorldGenState.registerPsm(psm, dimensionType);
    }
}