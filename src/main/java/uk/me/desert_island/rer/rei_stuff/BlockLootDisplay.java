package uk.me.desert_island.rer.rei_stuff;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContext.Builder;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import uk.me.desert_island.rer.RERUtils;

@Environment(EnvType.CLIENT)
public class BlockLootDisplay extends LootDisplay {

    private final Block inputBlock;

    public BlockLootDisplay(Block block) {
        this.inputBlock = block;
        this.inputStack = RERUtils.fromBlockToItemStack(block);
        this.dropTableId = block.getDropTableId();
        this.contextType = LootContextTypes.BLOCK;
    }

    @Override
    boolean fillContextBuilder(Builder contextBuilder, World world) {
        contextBuilder.put(LootContextParameters.TOOL, new ItemStack(Items.DIAMOND_PICKAXE));
        contextBuilder.put(LootContextParameters.POSITION, BlockPos.ORIGIN);
        contextBuilder.put(LootContextParameters.BLOCK_STATE, inputBlock.getDefaultState());

        return true;
    }

    @Override
    public Identifier getLocation() {
        return Registry.BLOCK.getId(inputBlock);
    }
}
