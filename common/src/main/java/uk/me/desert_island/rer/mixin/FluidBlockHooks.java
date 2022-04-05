package uk.me.desert_island.rer.mixin;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LiquidBlock.class)
public interface FluidBlockHooks {
    @Accessor("fluid")
    FlowingFluid getFluid();
}
