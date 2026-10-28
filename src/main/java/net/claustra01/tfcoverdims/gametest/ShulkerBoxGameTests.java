package net.claustra01.tfcoverdims.gametest;

import java.util.List;
import java.util.Map;
import net.claustra01.tfcoverdims.TFCOverdims;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.size.Size;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShulkerBoxGameTests {
    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    @GameTest(template = "empty", batch = "climate")
    public static void shulkerStorageRules(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, Blocks.SHULKER_BOX.defaultBlockState());
        var box = (ShulkerBoxBlockEntity) level.getBlockEntity(pos);
        var slot = new ShulkerBoxSlot(box, 0, 0, 0);
        for (var entry : Map.of(
            "tfcoverdims:loose_blackstone", true,
            "tfc:ceramic/vessel", true,
            "tfc:metal/ingot/gold", true,
            "tfc:metal/anvil/steel", false,
            "minecraft:shulker_box", false).entrySet()) {
            String id = entry.getKey();
            boolean allowed = entry.getValue();
            var item = stack(id);
            if (!id.equals("minecraft:shulker_box")) {
                Size expectedSize = switch (id) {
                    case "tfcoverdims:loose_blackstone" -> Size.SMALL;
                    case "tfc:ceramic/vessel" -> Size.NORMAL;
                    case "tfc:metal/ingot/gold" -> Size.LARGE;
                    default -> Size.HUGE;
                };
                helper.assertTrue(ItemSizeManager.get(item).getSize(item) == expectedSize, "TFC size: " + id);
            }
            helper.assertTrue(slot.mayPlace(item) == allowed, "GUI placement: " + id);
            if (allowed) {
                slot.set(item.copy());
                helper.assertTrue(ItemStack.isSameItemSameComponents(box.getItem(0), item), "GUI stores " + id);
            }
            box.setItem(0, ItemStack.EMPTY);
            helper.assertTrue(box.canPlaceItem(0, item) == allowed, "Container placement: " + id);
            helper.assertTrue(box.canPlaceItemThroughFace(0, item, Direction.UP) == allowed, "Hopper placement: " + id);
            var remainder = HopperBlockEntity.addItem(new SimpleContainer(1), box, item.copy(), Direction.UP);
            helper.assertTrue(remainder.isEmpty() == allowed, "Hopper transfer: " + id);
            helper.assertTrue(box.getItem(0).isEmpty() != allowed, "Hopper inventory: " + id);
            box.setItem(0, ItemStack.EMPTY);
            var carried = new ItemStack(Items.BLUE_SHULKER_BOX);
            carried.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(item.copy())));
            box.applyComponentsFromItemStack(carried);
            helper.assertTrue(box.getItem(0).isEmpty() != allowed, "Component inventory: " + id);
            if (allowed) {
                helper.assertTrue(ItemStack.isSameItemSameComponents(box.getItem(0), item), "Component stores " + id);
            }
            box.setItem(0, ItemStack.EMPTY);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void shulkerContentsPreserved(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(Level.END);
        var pos = new BlockPos(1024, 101, 0);
        level.getChunkAt(pos);
        try {
            level.setBlockAndUpdate(pos.below(), Blocks.END_STONE.defaultBlockState());
            var player = FakePlayerFactory.getMinecraft(level);
            var namedBox = new ItemStack(Items.BLUE_SHULKER_BOX);
            namedBox.set(DataComponents.CUSTOM_NAME, Component.literal("Carried box"));
            player.setItemInHand(InteractionHand.MAIN_HAND, namedBox);
            helper.assertTrue(player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false))).consumesAction(),
                "Named box can be placed in the End");
            var box = (ShulkerBoxBlockEntity) level.getBlockEntity(pos);
            var contents = stack("tfc:metal/ingot/gold");
            contents.setCount(4);
            contents.set(DataComponents.CUSTOM_NAME, Component.literal("Stored ingots"));
            helper.assertTrue(box.canPlaceItem(0, contents), "TFC ingots must fit");
            box.setItem(0, contents);
            level.destroyBlock(pos, true);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
            var carried = drops.stream().map(ItemEntity::getItem).filter(item -> item.is(Items.BLUE_SHULKER_BOX)).findFirst().orElse(ItemStack.EMPTY);
            helper.assertTrue(!carried.isEmpty(), "Blue Vanilla shulker box drops after breaking");
            helper.assertTrue(Component.literal("Carried box").equals(carried.get(DataComponents.CUSTOM_NAME)), "Box name survives breaking");
            var stored = carried.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems().iterator().next();
            helper.assertTrue(ItemStack.isSameItemSameComponents(stored, contents) && stored.getCount() == 4,
                "Dropped box retains its contents and item components");
            player.setItemInHand(InteractionHand.MAIN_HAND, carried.copy());
            helper.assertTrue(player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false))).consumesAction(),
                "Carried box can be placed with its item");
            helper.assertTrue(level.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock
                && level.getBlockState(pos).is(Blocks.BLUE_SHULKER_BOX), "Replaced box keeps Vanilla block and color");
            var placed = (ShulkerBoxBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(ItemStack.isSameItemSameComponents(placed.getItem(0), contents)
                && placed.getItem(0).getCount() == 4, "Placement restores contents");
            helper.assertTrue(Component.literal("Carried box").equals(placed.getCustomName()), "Placement restores box name");
            var chunkData = ChunkSerializer.write(level, level.getChunkAt(pos));
            var blockEntities = chunkData.getList("block_entities", 10);
            BlockEntity reloaded = null;
            for (int index = 0; index < blockEntities.size(); index++) {
                var tag = blockEntities.getCompound(index);
                if (tag.getInt("x") == pos.getX() && tag.getInt("y") == pos.getY() && tag.getInt("z") == pos.getZ()) {
                    reloaded = BlockEntity.loadStatic(pos, level.getBlockState(pos), tag, level.registryAccess());
                    break;
                }
            }
            helper.assertTrue(reloaded instanceof ShulkerBoxBlockEntity, "Chunk reload retains Vanilla shulker block entity");
            level.setBlockEntity(reloaded);
            var restored = (ShulkerBoxBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(level.getBlockState(pos).is(Blocks.BLUE_SHULKER_BOX)
                && ItemStack.isSameItemSameComponents(restored.getItem(0), contents)
                && restored.getItem(0).getCount() == 4, "Chunk reload retains color and contents");
            helper.assertTrue(Component.literal("Carried box").equals(restored.getCustomName()), "Chunk reload retains box component");
        } finally {
            level.removeBlock(pos, false);
            level.removeBlock(pos.below(), false);
            level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).forEach(ItemEntity::discard);
        }
        helper.succeed();
    }
}
