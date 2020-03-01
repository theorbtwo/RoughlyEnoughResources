package uk.me.desert_island.rer.rei_stuff;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.math.api.Point;
import me.shedaniel.math.api.Rectangle;
import me.shedaniel.rei.api.EntryStack;
import me.shedaniel.rei.api.RecipeCategory;
import me.shedaniel.rei.gui.widget.*;
import me.shedaniel.rei.impl.ScreenHelper;
import me.shedaniel.rei.plugin.DefaultPlugin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.StringUtils;
import uk.me.desert_island.rer.RERUtils;
import uk.me.desert_island.rer.client.ClientWorldGenState;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class WorldGenCategory implements RecipeCategory<WorldGenDisplay> {
    @Override
    public Identifier getIdentifier() {
        return DIMENSION_TYPE_IDENTIFIER_MAP.get(dimension);
    }

    static final Map<DimensionType, Identifier> DIMENSION_TYPE_IDENTIFIER_MAP = Maps.newHashMap();
    private final DimensionType dimension;

    public WorldGenCategory(DimensionType dimension) {
        DIMENSION_TYPE_IDENTIFIER_MAP.put(dimension, new Identifier("roughlyenoughresources", Registry.DIMENSION_TYPE.getId(dimension).getPath() + "_worldgen_category"));
        this.dimension = dimension;
    }

    public DimensionType getDimension() {
        return dimension;
    }

    @Override
    public EntryStack getLogo() {
        return EntryStack.create(RERUtils.fromDimensionTypeToItemStack(dimension));
    }

    @Override
    public String getCategoryName() {
        return I18n.translate("rer.worldgen.category", mapAndJoinToString(Registry.DIMENSION_TYPE.getId(dimension).getPath().split("_"), StringUtils::capitalize, " "));
    }

    public static <T> String mapAndJoinToString(T[] list, Function<T, String> function, String separator) {
        StringJoiner joiner = new StringJoiner(separator);
        for (T t : list) {
            joiner.add(function.apply(t));
        }
        return joiner.toString();
    }

    @Override
    public List<Widget> setupDisplay(Supplier<WorldGenDisplay> recipeDisplaySupplier, Rectangle bounds) {
        final WorldGenDisplay recipeDisplay = recipeDisplaySupplier.get();
        Block block = recipeDisplay.getOutputBlock();

        Point startPoint = new Point(bounds.getMinX() + 2, bounds.getMinY() + 3);

        List<Widget> widgets = new LinkedList<>();
        widgets.add(new SlotBaseWidget(new Rectangle(bounds.x + 1, bounds.y + 2, 130, 62)));
        widgets.add(new RecipeBaseWidget(bounds) {
            @Override
            public void render(int mouseX, int mouseY, float delta) {
                ClientWorldGenState worldGenState = ClientWorldGenState.byDimension(recipeDisplay.getDimension());

                int graph_height = 60;
                double maxPortion = worldGenState.getMaxPortion(block);

                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                MinecraftClient.getInstance().getTextureManager().bindTexture(DefaultPlugin.getDisplayTexture());

                int mouse_height = mouseX - startPoint.x;

                for (int height = 0; height < 128; height++) {
                    double portion = worldGenState.getPortionAtHeight(block, height);
                    double rel_portion;
                    if (maxPortion == 0) {
                        rel_portion = 0;
                    } else {
                        rel_portion = portion / maxPortion;
                    }

                    fill(/*startx*/ startPoint.x + height,
                            /*starty*/ startPoint.y + (int) (graph_height * (1 - rel_portion)),
                            /*endx  */ startPoint.x + height + 1,
                            /*endy  */ startPoint.y + graph_height,
                            /*color */ 0xff000000);
                }

                if (containsMouse(mouseX, mouseY) && mouse_height >= 0 && mouse_height < 128) {
                    double portion = worldGenState.getPortionAtHeight(block, mouse_height);
                    double rel_portion;
                    if (maxPortion == 0) {
                        rel_portion = 0;
                    } else {
                        rel_portion = portion / maxPortion;
                    }
                    fill(/*startx*/ mouseX,
                            /*starty*/ startPoint.y,
                            /*endx  */ mouseX + 1,
                            /*endy  */ startPoint.y + graph_height,
                            /*color */ 0xffebd534);
                    fill(/*startx*/ startPoint.x,
                            /*starty*/ startPoint.y + Math.min((int) (graph_height * (1 - rel_portion)), graph_height - 1),
                            /*endx  */ startPoint.x + 128,
                            /*endy  */ startPoint.y + Math.min((int) (graph_height * (1 - rel_portion)), graph_height - 1) + 1,
                            /*color */ 0xffebd534);
                    //noinspection UnstableApiUsage
                    ScreenHelper.getLastOverlay().addTooltip(QueuedTooltip.create(new Point(mouseX, mouseY), "Y: " + mouse_height, "Chance: " + LootDisplay.FORMAT_MORE.format(worldGenState.getPortionAtHeight(block, mouse_height) * 100) + "%"));
                }
            }
        });
        if (recipeDisplay.getOutputEntries().get(0).getType() == EntryStack.Type.RENDER)
            widgets.add(EntryWidget.create(bounds.getMaxX() - (16), bounds.getMinY() + 3).entries(recipeDisplay.getOutputEntries()).disableFavoritesInteractions());
        else
            widgets.add(EntryWidget.create(bounds.getMaxX() - (16), bounds.getMinY() + 3).entries(recipeDisplay.getOutputEntries()));
        widgets.add(LabelWidget.create(new Point(bounds.x + 65, bounds.getMaxY() - 10), Registry.BLOCK.getId(block).toString()).noShadow().color(ScreenHelper.isDarkModeEnabled() ? -4473925 : -12566464));
        return widgets;
    }

    @Override
    public int getDisplayHeight() {
        return 76;
    }
}
