package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.List;

@Environment(EnvType.CLIENT)
public class WorldGenDisplay implements Display {
    private final List<EntryIngredient> output;
    private final Block outputBlock;
    private final ResourceKey<Level> world;

    public WorldGenDisplay(EntryStack<?> outputStack, Block outputBlock, ResourceKey<Level> world) {
        this.output = Collections.singletonList(EntryIngredient.of(outputStack));
        this.outputBlock = outputBlock;
        this.world = world;
    }

    public ResourceKey<Level> getWorld() {
        return world;
    }

    public Block getOutputBlock() {
        return outputBlock;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return Collections.emptyList();
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return output;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return WorldGenCategory.WORLD_IDENTIFIER_MAP.get(world);
    }
}
