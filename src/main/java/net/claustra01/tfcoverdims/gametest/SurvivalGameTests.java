package net.claustra01.tfcoverdims.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.dries007.tfc.common.blockentities.AbstractFirepitBlockEntity;
import net.dries007.tfc.common.blockentities.AnvilBlockEntity;
import net.dries007.tfc.common.blockentities.BarrelBlockEntity;
import net.dries007.tfc.common.blockentities.FirepitBlockEntity;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.Nutrient;
import net.dries007.tfc.common.component.forge.ForgeStep;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.player.IPlayerInfo;
import net.dries007.tfc.common.player.PlayerInfo;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.util.calendar.Calendars;

import net.claustra01.tfcoverdims.TFCOverdims;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SurvivalGameTests {
    @GameTest(template = "empty", batch = "climate")
    public static void hoglinFood(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(Level.NETHER);
        var hoglin = EntityType.HOGLIN.create(level);
        var params = new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, hoglin)
            .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
            .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
            .create(LootContextParamSets.ENTITY);
        var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("entities/hoglin")));
        for (long seed : new long[] {1, 17, 42, 99, 256, 1024, 8191, 65536}) {
            var drops = table.getRandomItems(params, seed);
            int pork = 0;
            int bone = 0;
            for (ItemStack stack : drops) {
                if (stack.is(item("tfc:food/pork").getItem())) {
                    pork += stack.getCount();
                    helper.assertTrue(FoodCapability.has(stack), "Hoglin Pork must initialize TFC food components");
                } else {
                    helper.assertTrue(stack.is(Items.BONE), "Hoglin must not drop Vanilla meat or Cursed Hide");
                    bone += stack.getCount();
                }
            }
            helper.assertTrue(pork >= 2 && pork <= 4 && bone >= 3 && bone <= 6, "Hoglin loot counts must match without Looting");
        }
        BlockPos killPos = new BlockPos(80, 80, 80);
        level.getChunkAt(killPos);
        hoglin.setPos(Vec3.atCenterOf(killPos));
        level.addFreshEntity(hoglin);
        var killedDrops = new ArrayList<ItemEntity>();
        Consumer<LivingDropsEvent> capture = event -> {
            if (event.getEntity() == hoglin) {
                killedDrops.addAll(event.getDrops());
            }
        };
        NeoForge.EVENT_BUS.addListener(capture);
        try {
            hoglin.hurt(level.damageSources().generic(), 1000);
        } finally {
            NeoForge.EVENT_BUS.unregister(capture);
        }
        int killedPork = killedDrops.stream().filter(entity -> entity.getItem().is(item("tfc:food/pork").getItem()))
            .mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(hoglin.isDeadOrDying() && killedPork >= 2 && killedPork <= 4,
            "Killing an adult Hoglin must drop 2-4 Pork: dead=" + hoglin.isDeadOrDying() + ", pork=" + killedPork
                + ", drops=" + killedDrops.size());
        int killedBone = killedDrops.stream().filter(entity -> entity.getItem().is(Items.BONE))
            .mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(killedBone >= 3 && killedBone <= 6, "Killing an adult Hoglin must drop 3-6 Bone");
        helper.assertTrue(killedDrops.stream().allMatch(entity -> entity.getItem().is(item("tfc:food/pork").getItem())
            || entity.getItem().is(Items.BONE)), "Killed Hoglins must drop only TFC Pork and Bone");
        ItemStack pork = killedDrops.stream().map(ItemEntity::getItem)
            .filter(stack -> stack.is(item("tfc:food/pork").getItem())).findFirst().orElseThrow();
        helper.assertTrue(FoodCapability.has(pork), "Dropped Pork must initialize TFC food components");
        killedDrops.forEach(ItemEntity::discard);
        hoglin.discard();
        var recipe = HeatingRecipe.getRecipe(pork);
        helper.assertTrue(recipe != null, "Pork must use an existing TFC heating recipe");
        ItemStack cooked = recipe.assembleItem(pork);
        helper.assertTrue(cooked.is(item("tfc:food/cooked_pork").getItem()) && FoodCapability.has(cooked),
            "Cooking must produce existing TFC cooked Pork with food data");
        assertFoodPersistence(helper, level, pork);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void chorusFood(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(Level.END);
        ItemStack fruit = FoodCapability.setCreatedNow(new ItemStack(Items.CHORUS_FRUIT, 2));
        var food = FoodCapability.get(fruit);
        helper.assertTrue(food != null && !food.isRotten() && food.getData().hunger() == 4
            && food.getData().nutrient(Nutrient.FRUIT) > 0, "Chorus must be fresh TFC fruit with hunger and nutrition");
        var player = FakePlayerFactory.getMinecraft(level);
        BlockPos pos = new BlockPos(0, 80, 0);
        for (BlockPos floor : BlockPos.betweenClosed(pos.offset(-9, -1, -9), pos.offset(9, -1, 9))) {
            level.setBlockAndUpdate(floor, Blocks.END_STONE.defaultBlockState());
        }
        player.setPos(Vec3.atBottomCenterOf(pos));
        player.getFoodData().setFoodLevel(10);
        fruit.finishUsingItem(level, player);
        helper.assertTrue(fruit.getCount() == 1 && player.getFoodData().getFoodLevel() > 10,
            "Eating fresh Chorus must consume one and feed the player through TFC");
        helper.assertTrue(player.getCooldowns().isOnCooldown(Items.CHORUS_FRUIT),
            "Vanilla Chorus finishUsingItem must apply its teleport cooldown");
        assertFoodPersistence(helper, level, fruit);
        player.getFoodData().setFoodLevel(10);
        fruit.finishUsingItem(level, player);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 10,
            "Rotten Chorus must follow TFC's rule of providing no food value");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void netherWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(Level.NETHER);
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(32.5, 80, 32.5);
        var info = (PlayerInfo) IPlayerInfo.get(player);
        info.setThirst(40);
        ItemStack jug = item("tfc:ceramic/jug");
        var fluid = jug.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(fluid != null && fluid.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE) == 100,
            "Jug must accept water in the Nether");
        ItemStack restored = ItemStack.parse(level.registryAccess(), jug.save(level.registryAccess())).orElseThrow();
        helper.assertTrue(restored.getCapability(Capabilities.FluidHandler.ITEM).getFluidInTank(0).getAmount() == 100,
            "Jug water must survive serialization in ultraWarm dimensions");
        restored.finishUsingItem(level, player);
        helper.assertTrue(info.getThirst() > 40, "Jug water must remain drinkable in the Nether");
        BlockPos waterPos = new BlockPos(32, 82, 32);
        level.setBlockAndUpdate(waterPos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(((BucketItem) Items.WATER_BUCKET).emptyContents(player, level, waterPos, null),
            "Vanilla water bucket placement must execute");
        helper.assertTrue(level.getFluidState(waterPos).isEmpty(), "Placed water must evaporate in the Nether");
        info.setThirst(80);
        info.setExhaustion(4.1f);
        float heat = info.getThirstContributionFromTemperature();
        info.tick(player);
        float hotLoss = 80 - info.getThirst();
        var endPlayer = FakePlayerFactory.getMinecraft(helper.getLevel().getServer().getLevel(Level.END));
        var endInfo = (PlayerInfo) IPlayerInfo.get(endPlayer);
        endInfo.setThirst(80);
        endInfo.setExhaustion(4.1f);
        endInfo.tick(endPlayer);
        helper.assertTrue(heat > 0 && hotLoss > 80 - endInfo.getThirst() && endInfo.getThirst() < 80,
            "Nether heat must increase TFC thirst loss compared with the temperate End");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void dimensionEquipment(GameTestHelper helper) {
        for (var dimension : List.of(Level.NETHER, Level.END)) {
            ServerLevel level = helper.getLevel().getServer().getLevel(dimension);
            BlockPos pos = new BlockPos(48, 80, 48);
            for (BlockPos floor : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(5, -1, 1))) {
                level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            }
            level.setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:firepit")).defaultBlockState());
            var firepit = (FirepitBlockEntity) level.getBlockEntity(pos);
            firepit.getInventory().setStackInSlot(AbstractFirepitBlockEntity.SLOT_FUEL_CONSUME, item("tfc:wood/log/oak"));
            firepit.getInventory().setStackInSlot(FirepitBlockEntity.SLOT_ITEM_INPUT, FoodCapability.setCreatedNow(item("tfc:food/pork")));
            helper.assertTrue(firepit.light(level.getBlockState(pos)), "Existing Firepit must light in " + dimension.location());
            for (int tick = 0; tick < 2000; tick++) {
                AbstractFirepitBlockEntity.serverTick(level, pos, level.getBlockState(pos), firepit);
            }
            helper.assertTrue(firepit.getTemperature() > 0
                && firepit.getInventory().getStackInSlot(FirepitBlockEntity.SLOT_OUTPUT_1).is(item("tfc:food/cooked_pork").getItem()),
                "Firepit must heat and cook Pork using TFC's existing recipe");

            BlockPos barrelPos = pos.offset(2, 0, 0);
            level.setBlockAndUpdate(barrelPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:wood/barrel/oak")).defaultBlockState());
            var barrel = (BarrelBlockEntity) level.getBlockEntity(barrelPos);
            var tank = barrel.getInventory();
            helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 1000,
                "Barrel must accept water in " + dimension.location());
            BarrelBlockEntity.serverTick(level, barrelPos, level.getBlockState(barrelPos), barrel);
            var saved = barrel.saveWithFullMetadata(level.registryAccess());
            var restored = (BarrelBlockEntity) BlockEntity.loadStatic(barrelPos, level.getBlockState(barrelPos), saved, level.registryAccess());
            helper.assertTrue(restored.getInventory().getFluidInTank(0).getAmount() == 1000,
                "Barrel must retain stored water through ticking and reload");
            helper.assertTrue(tank.drain(250, FluidAction.EXECUTE).getAmount() == 250 && tank.getFluidInTank(0).getAmount() == 750,
                "Stored water must remain retrievable in ordinary quantities");

            BlockPos anvilPos = pos.offset(4, 0, 0);
            level.setBlockAndUpdate(anvilPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:metal/anvil/copper")).defaultBlockState());
            var anvil = (AnvilBlockEntity) level.getBlockEntity(anvilPos);
            ItemStack ingot = item("tfc:metal/ingot/copper");
            ItemStack hammer = item("tfc:metal/hammer/copper");
            anvil.getInventory().setStackInSlot(0, ingot);
            anvil.getInventory().setStackInSlot(2, hammer);
            anvil.chooseRecipe(ResourceLocation.parse("tfc:anvil/metal/rod/copper"));
            var player = FakePlayerFactory.getMinecraft(level);
            player.setPos(Vec3.atCenterOf(anvilPos));
            anvil.work(player, ForgeStep.PUNCH);
            helper.assertTrue(anvil.getMainInputForging().work() == 0 && hammer.getDamageValue() == 0,
                "Cold copper must reject working without consuming hammer durability");
            HeatCapability.setTemperature(ingot, 800);
            int target = anvil.getMainInputForging().target() + 9;
            int steps = 0;
            if (target % 2 != 0) {
                anvil.work(player, ForgeStep.BEND);
                target -= 7;
                steps++;
            }
            for (int work = 0; work < target; work += 2) {
                anvil.work(player, ForgeStep.PUNCH);
                steps++;
            }
            for (int hit = 0; hit < 3; hit++) {
                anvil.work(player, ForgeStep.HIT_LIGHT);
                steps++;
            }
            ItemStack output = anvil.getInventory().getStackInSlot(0);
            helper.assertTrue(output.is(item("tfc:metal/rod/copper").getItem()) && output.getCount() == 2,
                "Existing Anvil recipe must forge two copper rods in " + dimension.location());
            helper.assertTrue(hammer.getDamageValue() == steps && HeatCapability.get(output).getTemperature() > 0,
                "Forging must consume hammer durability and preserve output heat");
            level.removeBlock(pos, false);
            level.removeBlock(barrelPos, false);
            level.removeBlock(anvilPos, false);
        }
        helper.succeed();
    }

    private static void assertFoodPersistence(GameTestHelper helper, ServerLevel level, ItemStack stack) {
        var food = FoodCapability.get(stack);
        long created = food.getCreationDate();
        ItemStack restored = ItemStack.parse(level.registryAccess(), stack.save(level.registryAccess())).orElseThrow();
        helper.assertTrue(FoodCapability.get(restored).getCreationDate() == created
            && !FoodCapability.isRotten(restored), "Food age must survive saving and loading");
        Calendars.SERVER.skipForwardBy(food.getRottenDate() - Calendars.SERVER.getTicks() + 1);
        helper.assertTrue(FoodCapability.isRotten(restored), "Calendar time must spoil food using TFC decay");
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }
}
