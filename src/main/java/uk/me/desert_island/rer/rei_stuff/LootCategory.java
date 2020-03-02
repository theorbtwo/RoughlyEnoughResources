package uk.me.desert_island.rer.rei_stuff;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.clothconfig2.ClothConfigInitializer;
import me.shedaniel.clothconfig2.api.ScissorsHandler;
import me.shedaniel.clothconfig2.gui.widget.DynamicEntryListWidget;
import me.shedaniel.math.api.Point;
import me.shedaniel.math.api.Rectangle;
import me.shedaniel.math.impl.PointHelper;
import me.shedaniel.rei.api.EntryStack;
import me.shedaniel.rei.api.RecipeCategory;
import me.shedaniel.rei.gui.widget.*;
import me.shedaniel.rei.impl.ScreenHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Element;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class LootCategory implements RecipeCategory<LootDisplay> {
    public static final Identifier CATEGORY_ID = new Identifier("roughlyenoughresources", "loot_category");

    @Override
    public Identifier getIdentifier() {
        return CATEGORY_ID;
    }

    @Override
    public EntryStack getLogo() {
        return EntryStack.create(Items.WOODEN_PICKAXE);
    }

    @Override
    public String getCategoryName() {
        return I18n.translate("rer.loot.category");
    }

    @Override
    public List<Widget> setupDisplay(Supplier<LootDisplay> displaySupplier, Rectangle bounds) {
        final LootDisplay display = displaySupplier.get();
        List<Widget> widgets = new ArrayList<>();

        widgets.add(LabelWidget.create(new Point(bounds.getCenterX(), bounds.getMaxY() - (3 + 8 + 2)), ""));

        Rectangle outputsArea = getOutputsArea(bounds);
        widgets.add(new SlotBaseWidget(outputsArea));
        widgets.add(new ScrollableSlotsWidget(outputsArea, map(display.getOutputs(), t -> {
            List<EntryStack> stacks = new ArrayList<>();
            for (EntryStack stack : t.output) {
                stacks.add(t.original.copy().setting(EntryStack.Settings.RENDER_COUNTS, EntryStack.Settings.FALSE));
            }
            List<String> lore = new ArrayList<>();
            lore.add(t.countText == null ? String.valueOf(t.original.getAmount()) : t.countText);
            if (t.extraTextCount != null) {
                lore.set(0, I18n.translate("rer.function.and", lore.get(0), t.extraTextCount));
            }
            lore.set(0, "§e" + StringUtils.capitalize(I18n.translate("rer.function.count", lore.get(0))));
            if (t.extraText != null) {
                lore.add("§e" + StringUtils.capitalize(t.extraText));
            }
            return new TooltipEntryWidget(outputsArea, 0, 0, t.original, lore, t.output).noBackground().entries(stacks);
        })));
        widgets.add(LabelWidget.create(new Point(bounds.getCenterX(), bounds.getMaxY() - 10), display.getLocation().toString()).noShadow().color(ScreenHelper.isDarkModeEnabled() ? -4473925 : -12566464));
        registerWidget(display, widgets, bounds);
        return widgets;
    }

    protected void registerWidget(LootDisplay display, List<Widget> widgets, Rectangle bounds) {
        widgets.add(EntryWidget.create(bounds.getMinX() - 1, bounds.getMinY() + 1).entry(display.inputStack));
    }

    private static class TooltipEntryWidget extends EntryWidget {
        private final EntryStack original;
        private final List<EntryStack> stacks;
        private final Rectangle outputsArea;

        public TooltipEntryWidget(Rectangle outputsArea, int x, int y, EntryStack original, List<String> lore, List<EntryStack> stacks) {
            super(x, y);
            if (lore != null) {
                this.original = original.copy().setting(EntryStack.Settings.TOOLTIP_APPEND_EXTRA, entryStack -> lore);
            } else {
                this.original = original;
            }
            this.stacks = stacks;
            this.outputsArea = outputsArea;
        }

        @Override
        protected void drawCurrentEntry(int mouseX, int mouseY, float delta) {
            EntryStack entry = getCurrentEntry();
            entry.setZ(100);
            Rectangle innerBounds = getInnerBounds();
            entry.render(innerBounds, mouseX, mouseY, delta);
            if (stacks.isEmpty())
                return;
            @SuppressWarnings("IntegerDivisionInFloatingPointContext")
            EntryStack stack = stacks.get(stacks.size() == 1 ? 0 : MathHelper.floor((System.currentTimeMillis() / 500 % (double) stacks.size()) / 1f));
            if (stack.getAmount() == 1)
                return;
            MatrixStack matrixStack = new MatrixStack();
            String string = String.valueOf(stack.getAmount());
            matrixStack.translate(0.0D, 0.0D, getZ() + 400.0F);
            VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
            font.draw(string, (float) (innerBounds.x + 19 - 2 - font.getStringWidth(string)), (float) (innerBounds.y + 6 + 3), 16777215, true, matrixStack.peek().getModel(), immediate, false, 0, 15728880);
            immediate.draw();
        }

        @Override
        public QueuedTooltip getCurrentTooltip(int mouseX, int mouseY) {
            return original.getTooltip(mouseX, mouseY);
        }

        @Override
        public boolean containsMouse(double mouseX, double mouseY) {
            return super.containsMouse(mouseX, mouseY) && outputsArea.contains(mouseX, mouseY);
        }
    }

    public static <T, R> List<R> map(List<T> list, Function<T, R> function) {
        List<R> l = Lists.newArrayList();
        for (T t : list) {
            l.add(function.apply(t));
        }
        return l;
    }

    protected Rectangle getOutputsArea(Rectangle root) {
        return new Rectangle(root.x + 18, root.y, root.width - 17, root.height - 12);
    }

    private static class ScrollableSlotsWidget extends WidgetWithBounds {
        private final Rectangle bounds;
        private final List<EntryWidget> widgets;
        private double target;
        private double scroll;
        private long start;
        private long duration;

        public ScrollableSlotsWidget(Rectangle bounds, List<EntryWidget> widgets) {
            this.bounds = bounds;
            this.widgets = Lists.newArrayList(widgets);
        }

        @Override
        public boolean mouseScrolled(double double_1, double double_2, double double_3) {
            if (containsMouse(double_1, double_2)) {
                offset(ClothConfigInitializer.getScrollStep() * -double_3, true);
                return true;
            }
            return false;
        }

        public void offset(double value, boolean animated) {
            scrollTo(target + value, animated);
        }

        public void scrollTo(double value, boolean animated) {
            scrollTo(value, animated, ClothConfigInitializer.getScrollDuration());
        }

        public void scrollTo(double value, boolean animated, long duration) {
            target = clamp(value);

            if (animated) {
                start = System.currentTimeMillis();
                this.duration = duration;
            } else
                scroll = target;
        }

        public final double clamp(double v) {
            return clamp(v, DynamicEntryListWidget.SmoothScrollingSettings.CLAMP_EXTENSION);
        }

        public final double clamp(double v, double clampExtension) {
            return MathHelper.clamp(v, -clampExtension, getMaxScroll() + clampExtension);
        }

        protected int getMaxScroll() {
            return Math.max(0, this.getMaxScrollPosition() - this.getBounds().height + 1);
        }

        protected int getMaxScrollPosition() {
            return MathHelper.ceil(widgets.size() / (float) ((bounds.width - 7) / 18)) * 18;
        }

        @Override
        public Rectangle getBounds() {
            return bounds;
        }

        @Override
        public void render(int mouseX, int mouseY, float delta) {
            updatePosition(delta);
            Rectangle innerBounds = new Rectangle(bounds.x + 1, bounds.y + 1, bounds.width - 7, bounds.height - 2);
            ScissorsHandler.INSTANCE.scissor(innerBounds);
            int size = innerBounds.width / 18;
            for (int y = 0; y < MathHelper.ceil(widgets.size() / (float) size); y++) {
                for (int x = 0; x < size; x++) {
                    int index = y * size + x;
                    if (widgets.size() <= index)
                        break;
                    EntryWidget widget = widgets.get(index);
                    widget.getBounds().setLocation(bounds.x + 1 + x * 18, (int) (bounds.y + 1 + y * 18 - scroll));
                    widget.render(mouseX, mouseY, delta);
                }
            }
            ScissorsHandler.INSTANCE.removeLastScissor();
            ScissorsHandler.INSTANCE.scissor(bounds);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 771, 0, 1);
            RenderSystem.disableAlphaTest();
            RenderSystem.shadeModel(7425);
            RenderSystem.disableTexture();
            renderScrollBar();
            RenderSystem.enableTexture();
            RenderSystem.shadeModel(7424);
            RenderSystem.enableAlphaTest();
            RenderSystem.disableBlend();
            ScissorsHandler.INSTANCE.removeLastScissor();
        }

        @SuppressWarnings("deprecation")
        private void renderScrollBar() {
            int maxScroll = getMaxScroll();
            int scrollbarPositionMinX = getBounds().getMaxX() - 7;
            int scrollbarPositionMaxX = scrollbarPositionMinX + 6;
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            if (maxScroll > 0) {
                int height = (int) (((this.getBounds().height - 2f) * (this.getBounds().height - 2f)) / this.getMaxScrollPosition());
                height = MathHelper.clamp(height, 32, this.getBounds().height - 2);
                height -= Math.min((scroll < 0 ? (int) -scroll : scroll > maxScroll ? (int) scroll - maxScroll : 0), height * .95);
                height = Math.max(10, height);
                int minY = Math.min(Math.max((int) scroll * (this.getBounds().height - 2 - height) / maxScroll + getBounds().y + 1, getBounds().y + 1), getBounds().getMaxY() - 1 - height);

                boolean hovered = new Rectangle(scrollbarPositionMinX, minY, scrollbarPositionMaxX - scrollbarPositionMinX, height).contains(PointHelper.fromMouse());
                int bottomC = hovered ? 168 : 128;
                int topC = hovered ? 222 : 172;

                // Black Bar
                buffer.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
                buffer.vertex(scrollbarPositionMinX, this.getBounds().y + 1, 0.0D).texture(0, 1).color(0, 0, 0, 255).next();
                buffer.vertex(scrollbarPositionMaxX, this.getBounds().y + 1, 0.0D).texture(1, 1).color(0, 0, 0, 255).next();
                buffer.vertex(scrollbarPositionMaxX, getBounds().getMaxY() - 1, 0.0D).texture(1, 0).color(0, 0, 0, 255).next();
                buffer.vertex(scrollbarPositionMinX, getBounds().getMaxY() - 1, 0.0D).texture(0, 0).color(0, 0, 0, 255).next();
                tessellator.draw();

                // Bottom
                buffer.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
                buffer.vertex(scrollbarPositionMinX, minY + height, 0.0D).texture(0, 1).color(bottomC, bottomC, bottomC, 255).next();
                buffer.vertex(scrollbarPositionMaxX, minY + height, 0.0D).texture(1, 1).color(bottomC, bottomC, bottomC, 255).next();
                buffer.vertex(scrollbarPositionMaxX, minY, 0.0D).texture(1, 0).color(bottomC, bottomC, bottomC, 255).next();
                buffer.vertex(scrollbarPositionMinX, minY, 0.0D).texture(0, 0).color(bottomC, bottomC, bottomC, 255).next();
                tessellator.draw();

                // Top
                buffer.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
                buffer.vertex(scrollbarPositionMinX, (minY + height - 1), 0.0D).texture(0, 1).color(topC, topC, topC, 255).next();
                buffer.vertex((scrollbarPositionMaxX - 1), (minY + height - 1), 0.0D).texture(1, 1).color(topC, topC, topC, 255).next();
                buffer.vertex((scrollbarPositionMaxX - 1), minY, 0.0D).texture(1, 0).color(topC, topC, topC, 255).next();
                buffer.vertex(scrollbarPositionMinX, minY, 0.0D).texture(0, 0).color(topC, topC, topC, 255).next();
                tessellator.draw();
            }
        }

        private void updatePosition(float delta) {
            double[] target = new double[]{this.target};
            this.scroll = ClothConfigInitializer.handleScrollingPosition(target, this.scroll, this.getMaxScroll(), delta, this.start, this.duration);
            this.target = target[0];
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            for (EntryWidget widget : widgets) {
                if (widget.keyPressed(keyCode, scanCode, modifiers))
                    return true;
            }
            return false;
        }

        @Override
        public List<? extends Element> children() {
            return widgets;
        }
    }
}
