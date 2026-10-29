package net.claustra01.tfcoverdims.common.wood;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.IForgeBlockExtension;
import net.dries007.tfc.common.blocks.wood.Wood;

import net.claustra01.tfcoverdims.common.blocks.OverdimsBlocks;
import net.claustra01.tfcoverdims.common.items.OverdimsItems;

public final class OverdimsWoodBlocks {
    public static final Map<OverdimsWood, Map<Wood.BlockType, DeferredBlock<Block>>> WOODS = new EnumMap<>(OverdimsWood.class);
    public static final Map<OverdimsWood, DeferredItem<Item>> LUMBER = new EnumMap<>(OverdimsWood.class);

    static {
        for (OverdimsWood wood : OverdimsWood.values()) {
            Map<Wood.BlockType, DeferredBlock<Block>> blocks = new EnumMap<>(Wood.BlockType.class);
            WOODS.put(wood, blocks);
            LUMBER.put(wood, OverdimsItems.ITEMS.register("wood/lumber/" + wood.getSerializedName(),
                () -> new Item(new Item.Properties())));
            for (Wood.BlockType type : Wood.BlockType.values()) {
                String name = type.nameFor(wood);
                DeferredBlock<Block> block = OverdimsBlocks.BLOCKS.register(name, () -> {
                    if (type == Wood.BlockType.SAPLING) {
                        return new NetherSaplingBlock(wood, ExtendedProperties.of(wood.woodColor())
                            .noCollission().randomTicks().strength(0).blockEntity(TFCBlockEntities.TICK_COUNTER));
                    }
                    Block created = type.create(wood).get();
                    if (created instanceof IForgeBlockExtension extended) {
                        extended.getExtendedProperties().flammable(0, 0);
                    }
                    return created;
                });
                blocks.put(type, block);
                if (type.needsItem()) {
                    OverdimsItems.ITEMS.register(name, () -> type.createBlockItem(wood, new Item.Properties()).apply(block.get()));
                }
            }
            OverdimsItems.ITEMS.register("wood/support/" + wood.getSerializedName(), () ->
                new StandingAndWallBlockItem(blocks.get(Wood.BlockType.VERTICAL_SUPPORT).get(),
                    blocks.get(Wood.BlockType.HORIZONTAL_SUPPORT).get(), new Item.Properties(), Direction.DOWN));
            OverdimsItems.ITEMS.register("wood/sign/" + wood.getSerializedName(), () ->
                new SignItem(new Item.Properties(), blocks.get(Wood.BlockType.SIGN).get(),
                    blocks.get(Wood.BlockType.WALL_SIGN).get()));
        }
    }

    public static void register(IEventBus bus) {
        bus.addListener(OverdimsWoodBlocks::addBlockEntities);
        bus.addListener(OverdimsWoodBlocks::registerFlowerPots);
    }

    private static void registerFlowerPots(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FlowerPotBlock pot = (FlowerPotBlock) Blocks.FLOWER_POT;
            for (OverdimsWood wood : OverdimsWood.values()) {
                pot.addPlant(WOODS.get(wood).get(Wood.BlockType.SAPLING).getId(),
                    WOODS.get(wood).get(Wood.BlockType.POTTED_SAPLING));
            }
        });
    }

    public static void addBlockEntities(BlockEntityTypeAddBlocksEvent event) {
        add(event, Wood.BlockType.CHEST, TFCBlockEntities.CHEST.get());
        add(event, Wood.BlockType.TRAPPED_CHEST, TFCBlockEntities.TRAPPED_CHEST.get());
        add(event, Wood.BlockType.CRATE, TFCBlockEntities.CRATE.get());
        add(event, Wood.BlockType.BARREL, TFCBlockEntities.BARREL.get());
        add(event, Wood.BlockType.LOOM, TFCBlockEntities.LOOM.get());
        add(event, Wood.BlockType.SLUICE, TFCBlockEntities.SLUICE.get());
        add(event, Wood.BlockType.TOOL_RACK, TFCBlockEntities.TOOL_RACK.get());
        add(event, Wood.BlockType.BOOKSHELF, TFCBlockEntities.BOOKSHELF.get());
        add(event, Wood.BlockType.SIGN, TFCBlockEntities.SIGN.get());
        add(event, Wood.BlockType.WALL_SIGN, TFCBlockEntities.SIGN.get());
        add(event, Wood.BlockType.SAPLING, TFCBlockEntities.TICK_COUNTER.get());
        add(event, Wood.BlockType.AXLE, TFCBlockEntities.AXLE.get());
        add(event, Wood.BlockType.BLADED_AXLE, TFCBlockEntities.BLADED_AXLE.get());
        add(event, Wood.BlockType.ENCASED_AXLE, TFCBlockEntities.ENCASED_AXLE.get());
        add(event, Wood.BlockType.CLUTCH, TFCBlockEntities.CLUTCH.get());
        add(event, Wood.BlockType.GEAR_BOX, TFCBlockEntities.GEAR_BOX.get());
        add(event, Wood.BlockType.WINDMILL, TFCBlockEntities.WINDMILL.get());
        add(event, Wood.BlockType.WATER_WHEEL, TFCBlockEntities.WATER_WHEEL.get());
        add(event, Wood.BlockType.LECTERN, TFCBlockEntities.LECTERN.get());
        add(event, Wood.BlockType.SHELF, TFCBlockEntities.SHELF.get());
    }

    private static void add(BlockEntityTypeAddBlocksEvent event, Wood.BlockType type,
        BlockEntityType<?> blockEntity) {
        event.modify(blockEntity, WOODS.get(OverdimsWood.CRIMSON).get(type).get(),
            WOODS.get(OverdimsWood.WARPED).get(type).get());
    }
}
