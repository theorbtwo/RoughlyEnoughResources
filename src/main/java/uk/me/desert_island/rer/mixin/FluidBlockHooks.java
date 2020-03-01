package uk.me.desert_island.rer.mixin;

import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.BaseFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FluidBlock.class)
public interface FluidBlockHooks {
    @Accessor("fluid")
    BaseFluid getFluid();
}
