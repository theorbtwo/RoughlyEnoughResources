package uk.me.desert_island.rer.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ResourceLocation.class)
public interface IdentifierHooks {
    @Invoker("isValidPath")
    static boolean isPathValid(String namespace) {
        return false;
    }
}
