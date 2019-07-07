package uk.me.desert_island.rer.rei_stuff;

import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import me.shedaniel.rei.api.RecipeCategory;
import me.shedaniel.rei.api.Renderable;
import me.shedaniel.rei.api.Renderer;
import me.shedaniel.rei.gui.widget.LabelWidget;
import me.shedaniel.rei.gui.widget.SlotWidget;
import me.shedaniel.rei.gui.widget.Widget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.Identifier;
import uk.me.desert_island.rer.LootOutput;




public class LootCategory implements RecipeCategory<LootDisplay> {
    //private static final Identifier DISPLAY_TEXTURE = new Identifier("roughlyenoughitems", "textures/gui/display.png");
    
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

        //Point startPoint = new Point((int) bounds.getMinX()+2, (int) bounds.getMinY()+3);
        
        List<Widget> widgets = new LinkedList<>();
        //LeftLabelWidget y_widget   = new LeftLabelWidget(startPoint.x, startPoint.y + 3, "");
        //LeftLabelWidget pct_widget = new LeftLabelWidget(startPoint.x, startPoint.y + 13, "");

        List<LootOutput> outputs = display.getOutputs();

        LabelWidget extra_text_widget = new LabelWidget((int)bounds.getCenterX(), (int)bounds.getMaxY()-(3+8+2), "");
        widgets.add(extra_text_widget);

        final int slot_widget_size = 18;
        final int columns = (int)(bounds.getWidth()/slot_widget_size);
        final int rows = (int)(bounds.getHeight()/slot_widget_size);

        SlotWidget in_widget = new SlotWidget((int)bounds.getMinX(), (int)bounds.getMinY(), display.in_stack, true, true);
        widgets.add(in_widget);

        int stack_i=2;

        for (LootOutput output : outputs) {
            ItemStack stack = output.output;
            if (output.extra_text != null) {
                CompoundTag display_tag = stack.getOrCreateSubTag("display");
                ListTag lore_list = new ListTag();
                lore_list.add(new StringTag(output.extra_text));
                display_tag.put("Lore", lore_list);
            }
      
            int col = (int)stack_i % columns;
            int row = (int)stack_i / columns;

            if (row >= rows) {
                break;
            }

            //stack.setCount(outputs.get(stack));
            SlotWidget this_widget = new SlotWidget((int)bounds.getMinX()+col*slot_widget_size, (int)bounds.getMinY()+row*slot_widget_size, stack, true, true);
            widgets.add(this_widget);

            stack_i = stack_i + 1;
        }


        return widgets;
    }
}
