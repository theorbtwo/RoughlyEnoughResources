package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.rei.api.EntryStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EntityType;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

@Environment(EnvType.CLIENT)
public class EntityLootDisplay extends LootDisplay {

    private final EntityType<?> inputEntity;

    public EntityLootDisplay(EntityType<?> inputEntity) {
        this.inputEntity = inputEntity;
        this.inputStack = EntryStack.create(SpawnEggItem.forEntity(inputEntity));
        this.lootTableId = inputEntity.getLootTableId();
        this.contextType = LootContextTypes.ENTITY;
    }

    public EntityType<?> getInputEntity() {
        return inputEntity;
    }

    @Override
    public Identifier getLocation() {
        return Registry.ENTITY_TYPE.getId(inputEntity);
    }

    @Override
    public Identifier getRecipeCategory() {
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
