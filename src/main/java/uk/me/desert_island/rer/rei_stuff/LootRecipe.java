package uk.me.desert_island.rer.rei_stuff;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.loot.context.LootContext;
import net.minecraft.world.loot.context.LootContextType;

public class LootRecipe implements Recipe<Inventory> {
    public ItemStack in_stack;
    public LootContextType context_type;
    public LootContext.Builder context_builder;
    public Identifier drop_table_id;
    public ServerWorld server_world;

    public LootRecipe(ItemStack in_stack, Identifier drop_table_id, LootContextType context_type, LootContext.Builder context_builder) {
        this.in_stack = in_stack;
        this.context_type = context_type;
        this.context_builder = context_builder;
        this.drop_table_id = drop_table_id;
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
        // this must be nonnull, or Arcane Magic null-pointer-exceptions whenever you try to look at a rer recipe with arcane magic installed.
        return new Identifier("rer", "lootrecipe");
    }

    @Override
    public ItemStack getOutput() {
        return null;
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