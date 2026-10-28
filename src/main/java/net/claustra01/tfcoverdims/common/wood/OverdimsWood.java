package net.claustra01.tfcoverdims.common.wood;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.dries007.tfc.common.blocks.wood.Wood;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.registry.RegistryWood;
import net.claustra01.tfcoverdims.TFCOverdims;

public enum OverdimsWood implements RegistryWood {
    CRIMSON(MapColor.TERRACOTTA_RED, MapColor.COLOR_RED),
    WARPED(MapColor.TERRACOTTA_BLUE, MapColor.COLOR_BLUE);

    private final String name = name().toLowerCase(Locale.ROOT);
    private final MapColor woodColor;
    private final MapColor barkColor;
    private final TreeGrower tree;
    private final BlockSetType blockSet;
    private final WoodType woodType;

    OverdimsWood(MapColor woodColor, MapColor barkColor) {
        this.woodColor = woodColor;
        this.barkColor = barkColor;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID, "tree/" + name);
        tree = new TreeGrower(TFCOverdims.MOD_ID + ":" + name, Optional.empty(),
            Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, id)), Optional.empty());
        blockSet = new BlockSetType(TFCOverdims.MOD_ID + ":" + name);
        woodType = new WoodType(TFCOverdims.MOD_ID + ":" + name, blockSet);
    }

    public static void registerWoodTypes() {
        for (OverdimsWood wood : values()) {
            BlockSetType.register(wood.blockSet);
            WoodType.register(wood.woodType);
        }
    }

    @Override public String getSerializedName() { return name; }
    @Override public MapColor woodColor() { return woodColor; }
    @Override public MapColor barkColor() { return barkColor; }
    @Override public TreeGrower tree() { return tree; }
    @Override public Supplier<Integer> ticksToGrow() { return () -> 8 * ICalendar.CALENDAR_TICKS_IN_DAY; }
    @Override public int autumnIndex() { return 0; }
    @Override public boolean isConifer() { return true; }
    @Override public float getFlowerOffset() { return 0; }
    @Override public Supplier<Block> getBlock(Wood.BlockType type) { return OverdimsWoodBlocks.WOODS.get(this).get(type); }
    @Override public BlockSetType getBlockSet() { return blockSet; }
    @Override public WoodType getVanillaWoodType() { return woodType; }
}
