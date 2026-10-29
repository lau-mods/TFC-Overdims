package net.claustra01.tfcoverdims.common.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.claustra01.tfcoverdims.TFCOverdims;
import net.claustra01.tfcoverdims.common.blocks.OverdimsBlocks;

public final class OverdimsItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TFCOverdims.MOD_ID);
    public static final DeferredItem<BlockItem> LOOSE_NETHERRACK = ITEMS.registerSimpleBlockItem(OverdimsBlocks.LOOSE_NETHERRACK);
    public static final DeferredItem<BlockItem> LOOSE_BLACKSTONE = ITEMS.registerSimpleBlockItem(OverdimsBlocks.LOOSE_BLACKSTONE);
    public static final DeferredItem<BlockItem> LOOSE_END_STONE = ITEMS.registerSimpleBlockItem(OverdimsBlocks.LOOSE_END_STONE);
    public static final DeferredItem<BlockItem> COBBLE_NETHERRACK = ITEMS.registerSimpleBlockItem(OverdimsBlocks.COBBLE_NETHERRACK);
    public static final DeferredItem<BlockItem> HARDENED_NETHERRACK = ITEMS.registerSimpleBlockItem(OverdimsBlocks.HARDENED_NETHERRACK);
    public static final DeferredItem<BlockItem> COBBLE_END_STONE = ITEMS.registerSimpleBlockItem(OverdimsBlocks.COBBLE_END_STONE);
    public static final DeferredItem<BlockItem> HARDENED_END_STONE = ITEMS.registerSimpleBlockItem(OverdimsBlocks.HARDENED_END_STONE);
    public static final DeferredItem<Item> BLACKSTONE_BRICK = ITEMS.registerSimpleItem("blackstone_brick");
    public static final DeferredItem<Item> END_STONE_BRICK = ITEMS.registerSimpleItem("end_stone_brick");
}
