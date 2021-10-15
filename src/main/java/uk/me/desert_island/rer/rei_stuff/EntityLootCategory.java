package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.clothconfig2.api.ScissorsHandler;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;
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
    public Text getTitle() {
        return new TranslatableText("rer.entity.loot.category");
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
        Entity entity = entityLootDisplay.getInputEntity().create(MinecraftClient.getInstance().world);

        if (entity == null) {
            // 
            RERUtils.LOGGER.warn("can't create a %s entity", entityLootDisplay.getInputEntity());
            return;
        }
        widgets.add(Widgets.createSlotBase(entityBounds));
        widgets.add(Widgets.createDrawableWidget((helper, matrices, mouseX, mouseY, delta) -> {
            ScissorsHandler.INSTANCE.scissor(new Rectangle(entityBounds.x + 1, entityBounds.y + 1, entityBounds.width - 2, entityBounds.height - 2));
            float f = (float) Math.atan((entityBounds.getCenterX() - mouseX) / 40.0F);
            float g = (float) Math.atan((entityBounds.getCenterY() - mouseY) / 40.0F);
            float size = 32;
            if (Math.max(entity.getWidth(), entity.getHeight()) > 1.0) {
                size /= Math.max(entity.getWidth(), entity.getHeight());
            }
            matrices.push();
            matrices.translate(entityBounds.getCenterX(), entityBounds.getCenterY() + 20, 1050.0);
            matrices.scale(1, 1, -1);
            matrices.translate(0.0D, 0.0D, 1000.0D);
            matrices.scale(size, size, size);
            Quaternion quaternion = Vec3f.POSITIVE_Z.getDegreesQuaternion(180.0F);
            Quaternion quaternion2 = Vec3f.POSITIVE_X.getDegreesQuaternion(g * 20.0F);
            quaternion.hamiltonProduct(quaternion2);
            matrices.multiply(quaternion);
            float i = entity.getYaw();
            float j = entity.getPitch();
            float h = 0, k = 0, l = 0;
            entity.setYaw(180.0F + f * 40.0F);
            entity.setPitch(-g * 20.0F);
            if (entity instanceof LivingEntity) {
                h = ((LivingEntity) entity).bodyYaw;
                k = ((LivingEntity) entity).prevHeadYaw;
                l = ((LivingEntity) entity).headYaw;
                ((LivingEntity) entity).bodyYaw = 180.0F + f * 20.0F;
                ((LivingEntity) entity).headYaw = entity.getYaw();
                ((LivingEntity) entity).prevHeadYaw = entity.getYaw();
            }
            EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
            quaternion2.conjugate();
            entityRenderDispatcher.setRotation(quaternion2);
            entityRenderDispatcher.setRenderShadows(false);
            VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
            entityRenderDispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, matrices, immediate, 15728880);
            immediate.draw();
            entityRenderDispatcher.setRenderShadows(true);
            entity.setYaw(i);
            entity.setPitch(j);
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).bodyYaw = h;
                ((LivingEntity) entity).prevHeadYaw = k;
                ((LivingEntity) entity).headYaw = l;
            }
            matrices.pop();
            ScissorsHandler.INSTANCE.removeLastScissor();
        }));
        widgets.add(Widgets.createSlot(new Point(bounds.getMinX() + 54 - 18, bounds.getMinY() + 1)).entry(entityLootDisplay.inputStack).disableBackground().disableHighlight());
    }
}
