package uk.me.desert_island.rer.rei_stuff;

import net.minecraft.block.Block;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class WorldGenRecipe implements Recipe<Inventory> {
    public ItemStack output_stack;
    public Block output_block;

    public WorldGenRecipe(ItemStack stack, Block block) {
        this.output_stack = stack;
        this.output_block = block;
    }

    @Override
    public ItemStack craft(Inventory arg0) {
        return null;
    }

    @Override
    public boolean fits(int arg0, int arg1) {
        return false;
    }

    @Override
    public Identifier getId() {
        return null;
    }

    @Override
    public ItemStack getOutput() {
        return output_stack;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return null;
    }

    @Override
    public RecipeType<?> getType() {
        return null;
    }

    @Override
    public boolean matches(Inventory arg0, World arg1) {
        return false;
    }

}