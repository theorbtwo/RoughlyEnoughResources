package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.rei.api.EntryStack;
import me.shedaniel.rei.api.RecipeDisplay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

@Environment(EnvType.CLIENT)
public class WorldGenDisplay implements RecipeDisplay {
    private final EntryStack outputStack;
    private final Block outputBlock;
    private final RegistryKey<World> world;

    public WorldGenDisplay(EntryStack outputStack, Block outputBlock, RegistryKey<World> world) {
        this.outputStack = outputStack;
        this.outputBlock = outputBlock;
        this.world = world;
    }

    public RegistryKey<World> getWorld() {
        return world;
    }

    public Block getOutputBlock() {
        return outputBlock;
    }

    @Override
    public List<List<EntryStack>> getInputEntries() {
        return Collections.emptyList();
    }

    @Override
    public List<EntryStack> getOutputEntries() {
        return Collections.singletonList(outputStack);
    }

    @Override
    public Identifier getRecipeCategory() {
        return WorldGenCategory.WORLD_IDENTIFIER_MAP.get(world);
    }


}
