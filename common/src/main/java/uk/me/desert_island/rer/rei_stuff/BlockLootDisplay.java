package uk.me.desert_island.rer.rei_stuff;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import uk.me.desert_island.rer.RERUtils;

@Environment(EnvType.CLIENT)
public class BlockLootDisplay extends LootDisplay {
    private final Block inputBlock;

    public BlockLootDisplay(Block block) {
        this.inputBlock = block;
        this.inputStack = RERUtils.fromBlockToItemStack(block);
        this.lootTableId = block.getLootTable();
        this.contextType = LootContextParamSets.BLOCK;
    }

    //    @Override
    //    boolean fillContextBuilder(Builder contextBuilder, World world) {
    //        contextBuilder.put(LootContextParameters.TOOL, new ItemStack(Items.DIAMOND_PICKAXE));
    //        contextBuilder.put(LootContextParameters.POSITION, BlockPos.ORIGIN);
    //        contextBuilder.put(LootContextParameters.BLOCK_STATE, inputBlock.getDefaultState());
    //
    //        return true;
    //    }

    @Override
    public ResourceLocation getLocation() {
        return Registry.BLOCK.getKey(inputBlock);
    }
}
