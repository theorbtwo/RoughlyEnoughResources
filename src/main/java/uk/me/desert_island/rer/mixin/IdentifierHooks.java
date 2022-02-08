package uk.me.desert_island.rer.mixin;

import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Identifier.class)
public interface IdentifierHooks {
    @Invoker("isPathValid")
    static boolean isPathValid(String namespace) {
        return false;
    }
}
