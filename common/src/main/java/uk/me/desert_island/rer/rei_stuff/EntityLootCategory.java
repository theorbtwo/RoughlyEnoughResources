package uk.me.desert_island.rer.rei_stuff;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import me.shedaniel.clothconfig2.api.ScissorsHandler;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import uk.me.desert_island.rer.RERUtils;

import java.util.List;

public class EntityLootCategory extends LootCategory {
    public static final CategoryIdentifier<LootDisplay> CATEGORY_ID = CategoryIdentifier.of("roughlyenoughresources", "entity_loot_category");

    @Override
    public CategoryIdentifier<? extends LootDisplay> getCategoryIdentifier() {
        return CATEGORY_ID;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(Items.SPAWNER);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rer.entity.loot.category");
    }

    @Override
    protected Rectangle getOutputsArea(Rectangle root) {
        return new Rectangle(root.x + 56, root.y, root.width - 55, root.height - 12);
    }

    @Override
    @SuppressWarnings({"resource"}) // MinecraftClient.getInstance() is a singleton, and won't actually leak.
    protected void registerWidget(LootDisplay display, List<Widget> widgets, Rectangle bounds) {
        EntityLootDisplay entityLootDisplay = (EntityLootDisplay) display;
        Rectangle entityBounds = new Rectangle(bounds.getMinX(), bounds.getMinY(), 54, 54);
        Entity entity = entityLootDisplay.getInputEntity().create(Minecraft.getInstance().level);

        if (entity == null) {
            RERUtils.LOGGER.warn("can't create a %s entity", entityLootDisplay.getInputEntity());
            return;
        }

        widgets.add(Widgets.createSlotBase(entityBounds));
        widgets.add(Widgets.createDrawableWidget((helper, matrices, mouseX, mouseY, delta) -> {
            ScissorsHandler.INSTANCE.scissor(new Rectangle(entityBounds.x + 1, entityBounds.y + 1, entityBounds.width - 2, entityBounds.height - 2));
            float f = (float) Math.atan((entityBounds.getCenterX() - mouseX) / 40.0F);
            float g = (float) Math.atan((entityBounds.getCenterY() - mouseY) / 40.0F);
            float size = 32;
            if (Math.max(entity.getBbWidth(), entity.getBbHeight()) > 1.0) {
                size /= Math.max(entity.getBbWidth(), entity.getBbHeight());
            }

            matrices.pushPose();
            matrices.translate(entityBounds.getCenterX(), entityBounds.getCenterY() + 20, 1050.0);
            matrices.scale(1, 1, -1);
            matrices.translate(0.0D, 0.0D, 1000.0D);
            matrices.scale(size, size, size);
            Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
            Quaternion quaternion2 = Vector3f.XP.rotationDegrees(g * 20.0F);
            quaternion.mul(quaternion2);
            matrices.mulPose(quaternion);
            float i = entity.getYRot();
            float j = entity.getXRot();
            float h = 0, k = 0, l = 0;
            entity.setYRot(180.0F + f * 40.0F);
            entity.setXRot(-g * 20.0F);

            if (entity instanceof LivingEntity) {
                h = ((LivingEntity) entity).yBodyRot;
                k = ((LivingEntity) entity).yHeadRotO;
                l = ((LivingEntity) entity).yHeadRot;
                ((LivingEntity) entity).yBodyRot = 180.0F + f * 20.0F;
                ((LivingEntity) entity).yHeadRot = entity.getYRot();
                ((LivingEntity) entity).yHeadRotO = entity.getYRot();
            }

            EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            quaternion2.conj();
            entityRenderDispatcher.overrideCameraOrientation(quaternion2);
            entityRenderDispatcher.setRenderShadow(false);
            MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
            entityRenderDispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, matrices, immediate, 15728880);
            immediate.endBatch();
            entityRenderDispatcher.setRenderShadow(true);
            entity.setYRot(i);
            entity.setXRot(j);

            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).yBodyRot = h;
                ((LivingEntity) entity).yHeadRotO = k;
                ((LivingEntity) entity).yHeadRot = l;
            }

            matrices.popPose();
            ScissorsHandler.INSTANCE.removeLastScissor();
        }));

        widgets.add(Widgets.createSlot(new Point(bounds.getMinX() + 54 - 18, bounds.getMinY() + 1)).entry(entityLootDisplay.inputStack).disableBackground().disableHighlight());
    }
}
