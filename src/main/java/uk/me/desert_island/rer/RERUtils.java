package uk.me.desert_island.rer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.dimension.DimensionType;

public class RERUtils {

    private static final Logger LOGGER;

	static {
		LOGGER = LogManager.getFormatterLogger("rer-ru");
	}

    public static ItemStack DimensionType_to_ItemStack(DimensionType dt) {
        if (dt == DimensionType.OVERWORLD) {
            return new ItemStack(Items.GRASS_BLOCK);
        }
        if (dt == DimensionType.THE_NETHER) {
            return new ItemStack(Items.NETHERRACK);
        }
        if (dt == DimensionType.THE_END) {
            return new ItemStack(Items.END_STONE);
        }
        return new ItemStack(Items.GLASS);
    }

    public static ItemStack Block_to_ItemStack(Block block) {
        Item item = block.asItem();

        if (/* FIXME: make properly generic? */
            block == Blocks.LAVA || 
            block == Blocks.WATER
        ) {
            ItemStack is = new ItemStack(block.getFluidState(block.getDefaultState()).getFluid().getBucketItem());
            LOGGER.info("block %s is fluid -> ItemStack %s", block, is);
            return is;
        }

        if (block == Blocks.FIRE) {
            return new ItemStack(Items.FLINT_AND_STEEL);
        }

        ItemStack picked = null;
        try {
            picked = block.getPickStack(null, null, block.getDefaultState());
        } catch (Exception e) {
            picked = null;
            LOGGER.info("getPickStack for block %s failed", block);
        };
        if (picked != null && picked != ItemStack.EMPTY) {
            return picked;
        }

        if (Block.getBlockFromItem(item) != block) {
            LOGGER.info("block %s to item %s to block %s", block, item, Block.getBlockFromItem(item));
        }

        return new ItemStack(item);
    }
}