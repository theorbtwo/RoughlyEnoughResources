package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

@Environment(EnvType.CLIENT)
public class EntityLootDisplay extends LootDisplay {

    private final EntityType<?> inputEntity;

    public EntityLootDisplay(EntityType<?> inputEntity) {
        this.inputEntity = inputEntity;
        this.inputStack = EntryStacks.of(SpawnEggItem.byId(inputEntity));
        this.lootTableId = inputEntity.getDefaultLootTable();
        this.contextType = LootContextParamSets.ENTITY;
    }

    public EntityType<?> getInputEntity() {
        return inputEntity;
    }

    @Override
    public ResourceLocation getLocation() {
        return Registry.ENTITY_TYPE.getKey(inputEntity);
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return EntityLootCategory.CATEGORY_ID;
    }

    //    @Override
    //    boolean fillContextBuilder(Builder contextBuilder, World world) {
    //        Entity entity = inputEntity.create(world);
    //
    //        if (entity == null) {
    //            return false;
    //        }
    //
    //        contextBuilder.put(LootContextParameters.POSITION, new net.minecraft.util.math.BlockPos(0, 0, 0));
    //        contextBuilder.put(LootContextParameters.THIS_ENTITY, entity);
    //        contextBuilder.put(LootContextParameters.DAMAGE_SOURCE, DamageSource.SWEET_BERRY_BUSH);
    //
    //        return true;
    //    }
}
