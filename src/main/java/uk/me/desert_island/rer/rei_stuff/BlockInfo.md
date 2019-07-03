Block methods
 isAir
 getLuminance
 getMaterial -> net.minecraft.block.Material
  isLiquid, isSolid, blocksMovement, isBurnable, isReplaceable, blocksLight, canBreakByHand, getPistonBehavior, getColor
  ... show if == the static members?
 matches(Tag) ?
 getRenderType -> BlockRenderType ?
  one of INVISIBLE, ENTITYBLOCK_ANIMATED, MODEL
 hasBlockEntity
 isOpaque
 hasSidedTransparency ?
 getBlastResistance
 getRenderLayer
  SOLID, CUTOUT_MIPPED, CUTOUT, TRANSLUCENT
 canMobSpawnInside
 getTranslationKey ?
  -- show en_US name if game not en_US?
 getPistonBehavior -> net.minecraft.block.piston.PistonBehavior
  NORMAL, DESTROY, BLOCK, IGNORE, PUSH_ONLY
 getSlipperiness
 hasComparatorOutput
 getOffsetType ?
 getSoundGroup
 hasDynamicBounds
 isNaturalStone
 isNaturalDirt
