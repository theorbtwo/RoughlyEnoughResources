package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.rei.api.EntryStack;
import me.shedaniel.rei.api.RecipeDisplay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;

import java.util.Collections;
import java.util.List;

@Environment(EnvType.CLIENT)
public class WorldGenDisplay implements RecipeDisplay {
    private final EntryStack outputStack;
    private final Block outputBlock;
    private final DimensionType dimension;

    public WorldGenDisplay(EntryStack outputStack, Block outputBlock, DimensionType dimension) {
        this.outputStack = outputStack;
        this.outputBlock = outputBlock;
        this.dimension = dimension;
    }

    public DimensionType getDimension() {
        return dimension;
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
        return WorldGenCategory.DIMENSION_TYPE_IDENTIFIER_MAP.get(dimension);
    }


}
