package net.claustra01.tfcoverdims.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;

import net.claustra01.tfcoverdims.TFCOverdims;
import net.claustra01.tfcoverdims.common.growth.CalendarGrowth;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GrowthGameTests {
    @GameTest(template = "empty", batch = "climate")
    public static void wartCalendar(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos.below(), Blocks.SOUL_SAND.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.NETHER_WART.defaultBlockState());
        helper.assertTrue(level.getBlockState(pos).canSurvive(level, pos), "Wart must grow on plain Soul Sand");
        long interval = CalendarGrowth.interval(Calendars.SERVER);
        for (int age = 0; age < 3; age++) {
            for (int i = 0; i < 20; i++) {
                level.getBlockState(pos).randomTick(level, pos, RandomSource.create(0));
            }
            helper.assertTrue(level.getBlockState(pos).getValue(NetherWartBlock.AGE) == age,
                "Random ticks without Calendar time must not grow Wart");
            Calendars.SERVER.skipForwardBy(interval - 1);
            level.getBlockState(pos).randomTick(level, pos, RandomSource.create(0));
            helper.assertTrue(level.getBlockState(pos).getValue(NetherWartBlock.AGE) == age,
                "Partial growth intervals must be retained");
            Calendars.SERVER.skipForwardBy(1);
            level.getBlockState(pos).randomTick(level, pos, RandomSource.create(0));
            helper.assertTrue(level.getBlockState(pos).getValue(NetherWartBlock.AGE) == age + 1,
                "Each successful Vanilla update must advance exactly one age");
        }
        Calendars.SERVER.skipForwardBy(20L * ICalendar.CALENDAR_TICKS_IN_DAY);
        level.getBlockState(pos).randomTick(level, pos, RandomSource.create(0));
        helper.assertTrue(level.getBlockState(pos).getValue(NetherWartBlock.AGE) == 3, "Wart must stop at age 3");
        helper.assertTrue(!level.getChunkAt(pos).getData(CalendarGrowth.LAST_UPDATE).contains(Long.toString(pos.asLong())),
            "Mature plants must release their growth timestamp");
        level.destroyBlock(pos, true);
        int drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).stream()
            .filter(entity -> entity.getItem().is(Items.NETHER_WART)).mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(drops >= 2 && drops <= 4, "Harvest must retain Vanilla's 2-4 mature Wart drops");
        level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        helper.assertTrue(!Blocks.NETHER_WART.defaultBlockState().canSurvive(level, pos), "Ordinary dirt must not replace Soul Sand");
        helper.succeed();
    }

    @GameTest(template = "nether_tree", batch = "climate")
    public static void chorusCalendar(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos root = helper.absolutePos(new BlockPos(9, 2, 9));
        level.setBlockAndUpdate(root.below(), Blocks.END_STONE.defaultBlockState());
        level.setBlockAndUpdate(root, Blocks.CHORUS_FLOWER.defaultBlockState());
        helper.assertTrue(level.getBlockState(root).canSurvive(level, root), "Chorus must grow on plain End Stone");
        level.getBlockState(root).randomTick(level, root, RandomSource.create(0));
        helper.assertTrue(level.isEmptyBlock(root.above()), "Chorus must wait for Calendar time");
        Calendars.SERVER.skipForwardBy(CalendarGrowth.interval(Calendars.SERVER));
        level.getBlockState(root).randomTick(level, root, RandomSource.create(0));
        helper.assertTrue(level.getBlockState(root).is(Blocks.CHORUS_PLANT)
            && level.getBlockState(root.above()).is(Blocks.CHORUS_FLOWER), "Vanilla must turn the old flower into a connected stem");
        Calendars.SERVER.skipForwardBy(20L * ICalendar.CALENDAR_TICKS_IN_DAY);
        level.getBlockState(root.above()).randomTick(level, root.above(), RandomSource.create(0));
        int flowers = 0;
        int stems = 0;
        boolean branched = false;
        for (BlockPos pos : BlockPos.betweenClosed(root.offset(-8, 0, -8), root.offset(8, 27, 8))) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.CHORUS_FLOWER)) {
                flowers++;
                helper.assertTrue(state.getValue(ChorusFlowerBlock.AGE) == 5, "Catch-up must reach Vanilla's dead flower age 5");
                helper.assertTrue(state.canSurvive(level, pos), "Each flower must retain a valid Vanilla support");
                state.randomTick(level, pos, RandomSource.create(0));
                helper.assertTrue(level.getBlockState(pos).equals(state), "Dead flowers must not grow again");
            } else if (state.is(Blocks.CHORUS_PLANT)) {
                stems++;
                helper.assertTrue(state.canSurvive(level, pos), "Each stem must retain Vanilla's shape and support rules");
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    if (level.getBlockState(pos.relative(direction)).is(Blocks.CHORUS_PLANT)) {
                        branched = true;
                    }
                }
            }
        }
        helper.assertTrue(flowers > 1 && stems > 3 && branched, "Elapsed time must grow multiple Vanilla Chorus branches");
        int fruit = 0;
        for (BlockPos pos : BlockPos.betweenClosed(root.offset(-8, 0, -8), root.offset(8, 27, 8))) {
            if (level.getBlockState(pos).is(Blocks.CHORUS_PLANT)) {
                level.destroyBlock(pos, true);
            }
        }
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(root).inflate(30))) {
            if (entity.getItem().is(Items.CHORUS_FRUIT)) {
                fruit += entity.getItem().getCount();
            }
        }
        helper.assertTrue(fruit > 0 && fruit <= stems, "Harvested stems must drop Vanilla Chorus Fruit, at most one each");
        level.setBlockAndUpdate(root.below(), Blocks.DIRT.defaultBlockState());
        helper.assertTrue(!Blocks.CHORUS_FLOWER.defaultBlockState().canSurvive(level, root), "Dirt must not replace End Stone");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void growthPersistence(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos wart = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos flower = helper.absolutePos(new BlockPos(3, 2, 3));
        level.setBlockAndUpdate(wart.below(), Blocks.SOUL_SAND.defaultBlockState());
        level.setBlockAndUpdate(wart, Blocks.NETHER_WART.defaultBlockState());
        level.setBlockAndUpdate(flower.below(), Blocks.END_STONE.defaultBlockState());
        level.setBlockAndUpdate(flower, Blocks.CHORUS_FLOWER.defaultBlockState());
        for (BlockPos pos : new BlockPos[] {wart, flower}) {
            var chunk = level.getChunkAt(pos);
            var saved = chunk.writeAttachmentsToNBT(level.registryAccess());
            helper.assertTrue(saved != null && saved.contains("tfcoverdims:plant_growth"), "Growth time must be serialized in chunk NBT");
            var before = chunk.getData(CalendarGrowth.LAST_UPDATE).copy();
            chunk.removeData(CalendarGrowth.LAST_UPDATE);
            chunk.readAttachmentsFromNBT(level.registryAccess(), saved);
            helper.assertTrue(chunk.getData(CalendarGrowth.LAST_UPDATE).equals(before), "Chunk reload must preserve growth time");
        }
        Calendars.SERVER.skipForwardBy(10L * ICalendar.CALENDAR_TICKS_IN_DAY);
        level.getBlockState(wart).randomTick(level, wart, RandomSource.create(0));
        helper.assertTrue(level.getBlockState(wart).getValue(NetherWartBlock.AGE) == 3, "Reloaded Wart must catch up to maturity");
        level.removeBlock(flower, false);
        helper.assertTrue(!level.getChunkAt(flower).getData(CalendarGrowth.LAST_UPDATE).contains(Long.toString(flower.asLong())),
            "Removing a flower must discard its time debt");
        level.setBlockAndUpdate(flower, Blocks.CHORUS_FLOWER.defaultBlockState());
        level.getBlockState(flower).randomTick(level, flower, RandomSource.create(0));
        helper.assertTrue(level.isEmptyBlock(flower.above()), "Replanted flowers must not inherit old elapsed time");
        Calendars.SERVER.skipForwardBy(CalendarGrowth.interval(Calendars.SERVER));
        level.getBlockState(flower).randomTick(level, flower, RandomSource.create(0));
        helper.assertTrue(level.getBlockState(flower.above()).is(Blocks.CHORUS_FLOWER), "Replanted flowers must grow after new Calendar time");
        helper.succeed();
    }
}
