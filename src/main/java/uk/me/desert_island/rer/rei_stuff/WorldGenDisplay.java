package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

@Environment(EnvType.CLIENT)
public class WorldGenDisplay implements Display {
    private final List<EntryIngredient> output;
    private final Block outputBlock;
    private final RegistryKey<World> world;

    public WorldGenDisplay(EntryStack<?> outputStack, Block outputBlock, RegistryKey<World> world) {
        this.output = Collections.singletonList(EntryIngredient.of(outputStack));
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
