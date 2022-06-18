package uk.me.desert_island.rer.rei_stuff;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.shedaniel.clothconfig2.ClothConfigInitializer;
import me.shedaniel.clothconfig2.api.ScissorsHandler;
import me.shedaniel.clothconfig2.api.ScrollingContainer;
import me.shedaniel.clothconfig2.gui.widget.DynamicEntryListWidget;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.math.impl.PointHelper;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.*;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.impl.client.gui.widget.EntryWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class LootCategory implements DisplayCategory<LootDisplay> {
    public static final CategoryIdentifier<LootDisplay> CATEGORY_ID = CategoryIdentifier.of("roughlyenoughresources", "loot_category");

    @Override
    public CategoryIdentifier<? extends LootDisplay> getCategoryIdentifier() {
        return CATEGORY_ID;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(Items.WOODEN_PICKAXE);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rer.loot.category");
    }

    @Override
    public List<Widget> setupDisplay(LootDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();

        widgets.add(Widgets.createLabel(new Point(bounds.getCenterX(), bounds.getMaxY() - (3 + 8 + 2)), Component.literal("")));

        Rectangle outputsArea = getOutputsArea(bounds);
        widgets.add(Widgets.createSlotBase(outputsArea));
        widgets.add(new ScrollableSlotsWidget(outputsArea, map(display.getOutputs(), t -> {
            EntryIngredient stacks = t.output.map(stack -> {
                //                return t.original.copy().setting(EntryStack.Settings.RENDER_COUNTS, EntryStack.Settings.FALSE);
                return t.original.copy();
            });
            List<String> lore = new ArrayList<>();
            lore.add(t.countText == null ? String.valueOf(t.original.<ItemStack>castValue().getCount()) : t.countText);
            if (t.extraTextCount != null) {
                lore.set(0, I18n.get("rer.function.and", lore.get(0), t.extraTextCount));
            }
            lore.set(0, "§e" + StringUtils.capitalize(I18n.get("rer.function.count", lore.get(0))));
            if (t.extraText != null) {
                lore.add("§e" + StringUtils.capitalize(t.extraText));
            }
            return new TooltipEntryWidget(outputsArea, 0, 0, t.original, CollectionUtils.map(lore, Component::literal), t.output).noBackground().entries(stacks);
        })));
        widgets.add(Widgets.createLabel(new Point(bounds.getCenterX(), bounds.getMaxY() - 10), Component.literal(display.getLocation().toString())).noShadow().color(-12566464, -4473925));
        registerWidget(display, widgets, bounds);
        return widgets;
    }

    protected void registerWidget(LootDisplay display, List<Widget> widgets, Rectangle bounds) {
        widgets.add(Widgets.createSlot(new Point(bounds.getMinX() - 1, bounds.getMinY() + 1)).entry(display.inputStack));
    }

    private static class TooltipEntryWidget extends EntryWidget {
        private final EntryStack<?> original;
        private final List<EntryStack<?>> stacks;
        private final Rectangle outputsArea;

        public TooltipEntryWidget(Rectangle outputsArea, int x, int y, EntryStack<?> original, List<Component> lore, List<EntryStack<?>> stacks) {
            super(new Point(x, y));
            if (lore != null) {
                this.original = original.copy().tooltip(entryStack -> lore);
            } else {
                this.original = original;
            }
            this.stacks = stacks;
            this.outputsArea = outputsArea;
        }

        @Override
        protected void drawCurrentEntry(PoseStack matrices, int mouseX, int mouseY, float delta) {
            EntryStack<?> entry = getCurrentEntry();
            entry.setZ(100);
            Rectangle innerBounds = getInnerBounds();
            entry.render(matrices, innerBounds, mouseX, mouseY, delta);
            if (stacks.isEmpty())
                return;
            @SuppressWarnings("IntegerDivisionInFloatingPointContext")
            EntryStack<?> stack = stacks.get(stacks.size() == 1 ? 0 : Mth.floor((System.currentTimeMillis() / 500 % (double) stacks.size())));
            ItemStack itemStack = stack.castValue();
            int count = itemStack.getCount();
            matrices.pushPose();
            String string = String.valueOf(count);
            matrices.translate(0.0D, 0.0D, getZ() + 400.0F);
            MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            font.drawInBatch(string, (float) (innerBounds.x + 19 - 2 - font.width(string)), (float) (innerBounds.y + 6 + 3), 16777215, true, matrices.last().pose(), immediate, false, 0, 15728880);
            immediate.endBatch();
            matrices.popPose();
        }

        @Override
        public Tooltip getCurrentTooltip(TooltipContext mouse) {
            return original.getTooltip(mouse);
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
            return Mth.clamp(v, -clampExtension, getMaxScroll() + clampExtension);
        }

        protected int getMaxScroll() {
            return Math.max(0, this.getMaxScrollPosition() - this.getBounds().height + 1);
        }

        protected int getMaxScrollPosition() {
            return Mth.ceil(widgets.size() / (float) ((bounds.width - 7) / 18)) * 18;
        }

        @Override
        public Rectangle getBounds() {
            return bounds;
        }

        @Override
        public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
            updatePosition(delta);
            Rectangle innerBounds = new Rectangle(bounds.x + 1, bounds.y + 1, bounds.width - 7, bounds.height - 2);
            ScissorsHandler.INSTANCE.scissor(innerBounds);
            int size = innerBounds.width / 18;
            for (int y = 0; y < Mth.ceil(widgets.size() / (float) size); y++) {
                for (int x = 0; x < size; x++) {
                    int index = y * size + x;
                    if (widgets.size() <= index)
                        break;
                    EntryWidget widget = widgets.get(index);
                    widget.getBounds().setLocation(bounds.x + 1 + x * 18, (int) (bounds.y + 1 + y * 18 - scroll));
                    ((Widget) widget).render(matrices, mouseX, mouseY, delta);
                }
            }
            ScissorsHandler.INSTANCE.removeLastScissor();
            ScissorsHandler.INSTANCE.scissor(bounds);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 771, 0, 1);
            RenderSystem.disableTexture();
            renderScrollBar();
            RenderSystem.enableTexture();
            RenderSystem.disableBlend();
            ScissorsHandler.INSTANCE.removeLastScissor();
        }

        private void renderScrollBar() {
            int maxScroll = getMaxScroll();
            int scrollbarPositionMinX = getBounds().getMaxX() - 7;
            int scrollbarPositionMaxX = scrollbarPositionMinX + 6;
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buffer = tessellator.getBuilder();
            if (maxScroll > 0) {
                int height = (int) (((this.getBounds().height - 2f) * (this.getBounds().height - 2f)) / this.getMaxScrollPosition());
                height = Mth.clamp(height, 32, this.getBounds().height - 2);
                height -= Math.min((scroll < 0 ? (int) -scroll : scroll > maxScroll ? (int) scroll - maxScroll : 0), height * .95);
                height = Math.max(10, height);
                int minY = Math.min(Math.max((int) scroll * (this.getBounds().height - 2 - height) / maxScroll + getBounds().y + 1, getBounds().y + 1), getBounds().getMaxY() - 1 - height);

                boolean hovered = new Rectangle(scrollbarPositionMinX, minY, scrollbarPositionMaxX - scrollbarPositionMinX, height).contains(PointHelper.ofMouse());
                int bottomC = hovered ? 168 : 128;
                int topC = hovered ? 222 : 172;

                RenderSystem.setShader(GameRenderer::getPositionColorShader);

                // Black Bar
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                buffer.vertex(scrollbarPositionMinX, this.getBounds().y + 1, 0.0D).color(0, 0, 0, 255).endVertex();
                buffer.vertex(scrollbarPositionMaxX, this.getBounds().y + 1, 0.0D).color(0, 0, 0, 255).endVertex();
                buffer.vertex(scrollbarPositionMaxX, getBounds().getMaxY() - 1, 0.0D).color(0, 0, 0, 255).endVertex();
                buffer.vertex(scrollbarPositionMinX, getBounds().getMaxY() - 1, 0.0D).color(0, 0, 0, 255).endVertex();

                // Bottom
                buffer.vertex(scrollbarPositionMinX, minY + height, 0.0D).color(bottomC, bottomC, bottomC, 255).endVertex();
                buffer.vertex(scrollbarPositionMaxX, minY + height, 0.0D).color(bottomC, bottomC, bottomC, 255).endVertex();
                buffer.vertex(scrollbarPositionMaxX, minY, 0.0D).color(bottomC, bottomC, bottomC, 255).endVertex();
                buffer.vertex(scrollbarPositionMinX, minY, 0.0D).color(bottomC, bottomC, bottomC, 255).endVertex();

                // Top
                buffer.vertex(scrollbarPositionMinX, (minY + height - 1), 0.0D).color(topC, topC, topC, 255).endVertex();
                buffer.vertex((scrollbarPositionMaxX - 1), (minY + height - 1), 0.0D).color(topC, topC, topC, 255).endVertex();
                buffer.vertex((scrollbarPositionMaxX - 1), minY, 0.0D).color(topC, topC, topC, 255).endVertex();
                buffer.vertex(scrollbarPositionMinX, minY, 0.0D).color(topC, topC, topC, 255).endVertex();
                tessellator.end();
            }
        }

        private void updatePosition(float delta) {
            double[] target = new double[]{this.target};
            this.scroll = ScrollingContainer.handleScrollingPosition(target, this.scroll, this.getMaxScroll(), delta, this.start, this.duration);
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

        @Nullable
        @Override
        public GuiEventListener getFocused() {
            return null;
        }

        @Override
        public void setFocused(@Nullable GuiEventListener focused) {
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        public boolean isDragging() {
            return false;
        }

        @Override
        public void setDragging(boolean dragging) {
        }

    }
}
