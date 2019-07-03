package uk.me.desert_island.rer.rei_stuff;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.GlStateManager;

import me.shedaniel.rei.api.RecipeCategory;
import me.shedaniel.rei.api.Renderable;
import me.shedaniel.rei.api.Renderer;
import me.shedaniel.rei.gui.widget.LabelWidget;
import me.shedaniel.rei.gui.widget.RecipeBaseWidget;
import me.shedaniel.rei.gui.widget.SlotWidget;
import me.shedaniel.rei.gui.widget.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;
import uk.me.desert_island.rer.WorldGenState;




public class LootCategory implements RecipeCategory<LootDisplay> {
    private static final Identifier DISPLAY_TEXTURE = new Identifier("roughlyenoughitems", "textures/gui/display.png");
    
    public static final Identifier CATEGORY_ID = new Identifier("roughlyenoughresources", "loot_category");
    @Override
    public Identifier getIdentifier() {
        return CATEGORY_ID;
    }
    
    @Override
    public Renderer getIcon() {
        // ???
        return Renderable.fromItemStack(new ItemStack(Items.WOODEN_PICKAXE));
    }

    @Override
    public String getCategoryName() {
        return I18n.translate("rer.loot.category");
    }
    
    @Override
    public List<Widget> setupDisplay(Supplier<LootDisplay> displaySupplier, Rectangle bounds) {
        final LootDisplay display = displaySupplier.get();
        //WorldGenRecipe recipe = display.getRecipe().get();
        //Block block = recipe.output_block;

        Point startPoint = new Point((int) bounds.getMinX()+2, (int) bounds.getMinY()+3);
        
        List<Widget> widgets = new LinkedList<>();
        LeftLabelWidget y_widget   = new LeftLabelWidget(startPoint.x, startPoint.y + 3, "");
        LeftLabelWidget pct_widget = new LeftLabelWidget(startPoint.x, startPoint.y + 13, "");

        Map<ItemStack, Integer> outputs = display.getOutputs();

        final int slot_widget_size = 18;
        final int columns = (int)(bounds.getWidth()/slot_widget_size);
        final int rows = (int)(bounds.getHeight()/slot_widget_size);

        int stack_i=0;

        for (ItemStack stack : outputs.keySet()) {
            int col = (int)stack_i % columns;
            int row = (int)stack_i / columns;

            if (row >= rows) {
                break;
            }

            stack.setCount(outputs.get(stack));
            SlotWidget this_widget = new SlotWidget((int)bounds.getMinX()+col*slot_widget_size, (int)bounds.getMinY()+row*slot_widget_size, stack, true, true);
            widgets.add(this_widget);

            stack_i = stack_i + 1;
        }

        widgets.add(new LabelWidget((int)bounds.getCenterX(), (int)bounds.getMaxY()-(3+8+2), display.in_stack.toString()));

        return widgets;
    }
}
