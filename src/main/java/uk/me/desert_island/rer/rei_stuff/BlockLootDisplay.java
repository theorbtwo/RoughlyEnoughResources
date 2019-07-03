package uk.me.desert_island.rer.rei_stuff;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.loot.context.LootContextParameters;
import net.minecraft.world.loot.context.LootContextTypes;
import net.minecraft.world.loot.context.LootContext.Builder;
import uk.me.desert_island.rer.RERUtils;

public class BlockLootDisplay extends LootDisplay {

    private Block in_block;

    public BlockLootDisplay(Block block) {
        this.in_block = block;
        this.in_stack = RERUtils.Block_to_ItemStack(block);
        this.drop_table_id = block.getDropTableId();
        this.context_type = LootContextTypes.BLOCK;
	}

	@Override
    void fill_context_builder(Builder context_builder) {
        context_builder.put(LootContextParameters.TOOL, new ItemStack(Items.DIAMOND_PICKAXE));
        context_builder.put(LootContextParameters.POSITION, new net.minecraft.util.math.BlockPos(0, 0, 0));
        context_builder.put(LootContextParameters.BLOCK_STATE, in_block.getDefaultState());
    }
}
