package uk.me.desert_island.rer.rei_stuff;

import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.gui.DisplayRenderer;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Button;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringUtils;
import uk.me.desert_island.rer.RERUtils;
import uk.me.desert_island.rer.client.ClientWorldGenState;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import static uk.me.desert_island.rer.RoughlyEnoughResources.MAX_WORLD_Y;
import static uk.me.desert_island.rer.RoughlyEnoughResources.MIN_WORLD_Y;
import static uk.me.desert_island.rer.RoughlyEnoughResources.WORLD_HEIGHT;

@Environment(EnvType.CLIENT)
public class WorldGenCategory implements DisplayCategory<WorldGenDisplay> {
    @Override
    @SuppressWarnings("unchecked")
    public CategoryIdentifier<? extends WorldGenDisplay> getCategoryIdentifier() {
        return (CategoryIdentifier<? extends WorldGenDisplay>) WORLD_IDENTIFIER_MAP.get(world);
    }

    static final Map<RegistryKey<World>, CategoryIdentifier<?>> WORLD_IDENTIFIER_MAP = Maps.newHashMap();
    private final RegistryKey<World> world;
    private int scroll;

    public WorldGenCategory(RegistryKey<World> world) {
        WORLD_IDENTIFIER_MAP.put(world, CategoryIdentifier.of("roughlyenoughresources", world.getValue().getPath() + "_worldgen_category"));
        this.world = world;
    }

    public RegistryKey<World> getWorld() {
        return world;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(RERUtils.fromWorldToItemStack(world));
    }

    public DisplayRenderer getSimpleRenderer(WorldGenDisplay recipe) {
        EntryIngredient stacks = recipe.getOutputEntries().get(0);
        Tooltip[] tooltip = {null};
        return new DisplayRenderer() {
            private EntryStack<?> getCurrent() {
                if (stacks.isEmpty()) {
                    return EntryStack.empty();
                } else {
                    return stacks.size() == 1 ? stacks.get(0) : stacks.get(MathHelper.floor((double) (System.currentTimeMillis() / 500L) % (double) stacks.size()));
                }
            }

            @Override
            public Tooltip getTooltip(Point mouse) {
                return tooltip[0];
            }

            @Override
            public int getHeight() {
                return 22;
            }

            @Override
            public void render(MatrixStack matrices, Rectangle rectangle, int mouseX, int mouseY, float delta) {
                EntryStack<?> current = getCurrent();
                Rectangle innerBounds = new Rectangle(rectangle.x + rectangle.width / 2 - 8, rectangle.y + 3, 16, 16);
                current.render(matrices, innerBounds, mouseX, mouseY, delta);
                tooltip[0] = innerBounds.contains(mouseX, mouseY) ? current.getTooltip(new Point(mouseX, mouseY)) : null;
            }
        };
    }

    @Override
    public Text getTitle() {
        return new TranslatableText("rer.worldgen.category", mapAndJoinToString(world.getValue().getPath().split("_"), StringUtils::capitalize, " "));
    }

    public static <T> String mapAndJoinToString(T[] list, Function<T, String> function, String separator) {
        StringJoiner joiner = new StringJoiner(separator);
        for (T t : list) {
            joiner.add(function.apply(t));
        }
        return joiner.toString();
    }

    @Override
    public List<Widget> setupDisplay(WorldGenDisplay display, Rectangle bounds) {
        scroll = 0;
        Block block = display.getOutputBlock();

        Point startPoint = new Point(bounds.getMinX() + 2, bounds.getMinY() + 3);

        List<Widget> widgets = new LinkedList<>();
        widgets.add(Widgets.createSlotBase(new Rectangle(bounds.x + 1, bounds.y + 2, 130, 62)));
        widgets.add(Widgets.createDrawableWidget((helper, matrices, mouseX, mouseY, delta) -> {
            ClientWorldGenState worldGenState = ClientWorldGenState.byWorld(display.getWorld());

            int graphHeight = 60;
            double maxPortion = worldGenState.getMaxPortion(block);

            //            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            //            MinecraftClient.getInstance().getTextureManager().bindTexture(DefaultPlugin.getDisplayTexture());

            int mouseH = mouseX - startPoint.x;
            int mouseHeight = mouseX - startPoint.x + MIN_WORLD_Y + scroll;

            for (int height = 0; height < 128; height++) {
                double portion = worldGenState.getPortionAtHeight(block, height + scroll);
                double relPortion;
                if (maxPortion == 0) {
                    relPortion = 0;
                } else {
                    relPortion = portion / maxPortion;
                }

                DrawableHelper.fill(matrices,
                        /*startx*/ startPoint.x + height,
                        /*starty*/ startPoint.y + (int) (graphHeight * (1 - relPortion)),
                        /*endx  */ startPoint.x + height + 1,
                        /*endy  */ startPoint.y + graphHeight,
                        /*color */ 0xff000000);
            }

            if (bounds.contains(mouseX, mouseY) && mouseH >= 0 && mouseH < 128 && mouseY < bounds.y + 64
                    && mouseHeight >= MIN_WORLD_Y && mouseHeight < MAX_WORLD_Y) {
                double portion = worldGenState.getPortionAtHeight(block, mouseHeight - MIN_WORLD_Y);
                double rel_portion;
                if (maxPortion == 0) {
                    rel_portion = 0;
                } else {
                    rel_portion = portion / maxPortion;
                }
                DrawableHelper.fill(matrices,
                        /*startx*/ mouseX,
                        /*starty*/ startPoint.y,
                        /*endx  */ mouseX + 1,
                        /*endy  */ startPoint.y + graphHeight,
                        /*color */ 0xffebd534);
                DrawableHelper.fill(matrices,
                        /*startx*/ startPoint.x,
                        /*starty*/ startPoint.y + Math.min((int) (graphHeight * (1 - rel_portion)), graphHeight - 1),
                        /*endx  */ startPoint.x + 128,
                        /*endy  */ startPoint.y + Math.min((int) (graphHeight * (1 - rel_portion)), graphHeight - 1) + 1,
                        /*color */ 0xffebd534);
                REIRuntime.getInstance().queueTooltip(Tooltip.create(new Point(mouseX, mouseY),
                        new LiteralText("Y: " + mouseHeight),
                        new LiteralText("Chance: "
                                + LootDisplay.FORMAT_MORE.format(portion * 100) + "%")));
            }
        }));
        widgets.add(Widgets.createSlot(new Point(bounds.getMaxX() - (16), bounds.getMinY() + 3)).entries(display.getOutputEntries().get(0)));
        widgets.add(Widgets.createLabel(new Point(bounds.x + 65, bounds.getMaxY() - 10), new LiteralText(Registry.BLOCK.getId(block).toString())).noShadow().color(-12566464, -4473925));

        Button scrollLeft = Widgets.createButton(new Rectangle(bounds.x + 1, bounds.getMaxY(), 16, 16), new LiteralText("-"));
        scrollLeft.setOnClick(button -> scroll(-50));
        widgets.add(scrollLeft);

        Button scrollRight = Widgets.createButton(new Rectangle(bounds.x + 114, bounds.getMaxY(), 16, 16), new LiteralText("+"));
        scrollRight.setOnClick(button -> scroll(50));
        widgets.add(scrollRight);

        return widgets;
    }

    @Override
    public int getDisplayHeight() {
        return 76;
    }

    public void scroll(int incr) {
        scroll = Ints.constrainToRange(scroll + incr, 0, WORLD_HEIGHT - 128);
    }
}
