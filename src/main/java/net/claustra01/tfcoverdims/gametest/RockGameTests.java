package net.claustra01.tfcoverdims.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import net.dries007.tfc.util.loot.TFCLoot;
import net.dries007.tfc.util.tracker.WorldTracker;

import net.claustra01.tfcoverdims.TFCOverdims;
import net.claustra01.tfcoverdims.common.blocks.LooseRockBlock;
import net.claustra01.tfcoverdims.common.blocks.OverdimsBlocks;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RockGameTests {
    @GameTest(template = "empty", batch = "climate")
    public static void netherrackLoot(GameTestHelper helper) {
        assertRockLoot(helper, Blocks.NETHERRACK, OverdimsBlocks.LOOSE_NETHERRACK.get());
        assertOreLoot(helper, Blocks.NETHER_GOLD_ORE, Items.GOLD_NUGGET, 2, 6);
        assertOreLoot(helper, Blocks.NETHER_QUARTZ_ORE, Items.QUARTZ, 1, 1);
        assertOreLoot(helper, Blocks.ANCIENT_DEBRIS, Items.ANCIENT_DEBRIS, 1, 1);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void blackstoneLoot(GameTestHelper helper) {
        assertRockLoot(helper, Blocks.BLACKSTONE, OverdimsBlocks.LOOSE_BLACKSTONE.get());
        for (long seed = 1; seed <= 32; seed++) {
            var drops = drops(helper, Blocks.GILDED_BLACKSTONE, false, seed);
            helper.assertTrue(drops.size() == 1, "Gilded Blackstone must keep its vanilla alternatives");
            ItemStack drop = drops.getFirst();
            helper.assertTrue((drop.is(Items.GILDED_BLACKSTONE) && drop.getCount() == 1)
                || (drop.is(Items.GOLD_NUGGET) && drop.getCount() >= 2 && drop.getCount() <= 5),
                "Gilded Blackstone must retain its block or gold nuggets");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void endStoneLoot(GameTestHelper helper) {
        assertRockLoot(helper, Blocks.END_STONE, OverdimsBlocks.LOOSE_END_STONE.get());
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void netherRockPlacement(GameTestHelper helper) {
        assertPlacementAndPickup(helper, Blocks.NETHERRACK, OverdimsBlocks.LOOSE_NETHERRACK.get());
        assertPlacementAndPickup(helper, Blocks.BLACKSTONE, OverdimsBlocks.LOOSE_BLACKSTONE.get());
        assertFeature(helper, "netherrack", 5, BiomeTags.IS_NETHER);
        assertFeature(helper, "blackstone", 2, BiomeTags.IS_NETHER);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void endRockPlacement(GameTestHelper helper) {
        assertPlacementAndPickup(helper, Blocks.END_STONE, OverdimsBlocks.LOOSE_END_STONE.get());
        assertFeature(helper, "end_stone", 5, BiomeTags.IS_END);
        helper.succeed();
    }

    private static void assertRockLoot(GameTestHelper helper, Block rock, LooseRockBlock loose) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 3, 1));
        var level = helper.getLevel();
        for (Direction direction : Direction.values()) {
            level.setBlockAndUpdate(pos.relative(direction), Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(pos, rock.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), rock.defaultBlockState());
        Set<Integer> counts = new HashSet<>();
        for (long seed = 1; seed <= 32; seed++) {
            var drops = drops(helper, rock, false, seed);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(loose.asItem()), "Connected rock must drop loose rock");
            int count = drops.getFirst().getCount();
            helper.assertTrue(count >= 2 && count <= 4, "Connected rock must drop 2–4 pieces, seed " + seed);
            counts.add(count);
            drops = drops(helper, rock, true, seed);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(rock.asItem()) && drops.getFirst().getCount() == 1,
                "TFC's isolated loot context must return the original block");
        }
        helper.assertTrue(counts.equals(Set.of(2, 3, 4)), "Fixed seeds must exercise every drop count");
        var player = FakePlayerFactory.getMinecraft(level);
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        for (long seed = 1; seed <= 8; seed++) {
            level.setBlockAndUpdate(pos, rock.defaultBlockState());
            level.random.setSeed(seed);
            helper.assertTrue(player.gameMode.destroyBlock(pos), "Survival mining with a pickaxe must break the rock");
            var entities = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
            int total = entities.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
            helper.assertTrue(!entities.isEmpty() && entities.stream().allMatch(entity -> entity.getItem().is(loose.asItem()))
                && total >= 2 && total <= 4, "Actual survival mining must drop 2–4 loose rocks");
            entities.forEach(ItemEntity::discard);
        }
        level.setBlockAndUpdate(pos, rock.defaultBlockState());
        WorldTracker tracker = WorldTracker.get(level);
        tracker.addIsolatedPos(pos);
        tracker.tick();
        helper.assertTrue(level.getBlockState(pos).is(rock), "TFC must leave connected rock in place");
        level.removeBlock(pos.east(), false);
        tracker.addIsolatedPos(pos);
        tracker.tick();
        helper.assertTrue(level.isEmptyBlock(pos), "TFC must recover isolated rock through its existing tracker");
        helper.assertItemEntityCountIs(rock.asItem(), new BlockPos(1, 3, 1), 1, 1);
        helper.killAllEntities();
    }

    private static List<ItemStack> drops(GameTestHelper helper, Block block, boolean isolated, long seed) {
        LootParams.Builder params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 3, 1))))
            .withParameter(LootContextParams.BLOCK_STATE, block.defaultBlockState())
            .withParameter(LootContextParams.TOOL, new ItemStack(Items.NETHERITE_PICKAXE));
        if (isolated) {
            params.withParameter(TFCLoot.ISOLATED, true);
        }
        return helper.getLevel().getServer().reloadableRegistries().getLootTable(block.getLootTable())
            .getRandomItems(params.create(LootContextParamSets.BLOCK), seed);
    }

    private static void assertOreLoot(GameTestHelper helper, Block block, net.minecraft.world.item.Item item, int min, int max) {
        for (long seed = 1; seed <= 32; seed++) {
            var drops = drops(helper, block, false, seed);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item)
                && drops.getFirst().getCount() >= min && drops.getFirst().getCount() <= max,
                "Original ore loot must remain intact: " + BuiltInRegistries.BLOCK.getKey(block));
        }
    }

    private static void assertPlacementAndPickup(GameTestHelper helper, Block support, LooseRockBlock loose) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 3, 1));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        level.setBlockAndUpdate(pos.below(), support.defaultBlockState());
        level.removeBlock(pos, false);
        ItemStack held = new ItemStack(loose.asItem(), 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockHitResult ground = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
        helper.assertTrue(((net.minecraft.world.item.BlockItem) loose.asItem()).place(
            new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, ground))).consumesAction(),
            "Loose rock must place on the support through BlockItem");
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (int count = 1; count <= 4; count++) {
            var state = level.getBlockState(pos);
            helper.assertTrue(state.is(loose) && state.getValue(LooseRockBlock.COUNT) == count, "Placement must stack to " + count);
            helper.assertTrue(held.getCount() == 4 - count, "Stacking must consume one item");
            var drops = Block.getDrops(state, level, pos, null);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(loose.asItem()) && drops.getFirst().getCount() == count,
                "Breaking loose rock must recover its visible count");
            if (count < 4) {
                state.useItemOn(held, level, player, InteractionHand.MAIN_HAND, hit);
            }
        }
        held = new ItemStack(loose.asItem());
        level.getBlockState(pos).useItemOn(held, level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(level.getBlockState(pos).getValue(LooseRockBlock.COUNT) == 4 && held.getCount() == 1,
            "A full pile must neither grow nor consume items");
        for (int count = 1; count <= 4; count++) {
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var state = loose.defaultBlockState().setValue(LooseRockBlock.COUNT, count);
            level.setBlockAndUpdate(pos, state);
            state.useItemOn(ItemStack.EMPTY, level, player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(level.isEmptyBlock(pos), "TFC pickup must remove the pile");
            helper.assertTrue(player.getInventory().countItem(loose.asItem()) == count, "Pickup must return " + count + " rocks");
        }
        level.setBlockAndUpdate(pos, loose.defaultBlockState().setValue(LooseRockBlock.COUNT, 4));
        level.removeBlock(pos.below(), false);
        helper.assertTrue(level.isEmptyBlock(pos), "Unsupported piles must break");
        helper.assertItemEntityCountIs(loose.asItem(), new BlockPos(1, 3, 1), 1, 4);
        helper.killAllEntities();
    }

    private static void assertFeature(GameTestHelper helper, String rock, int count, TagKey<Biome> biomes) {
        var level = helper.getLevel();
        ResourceLocation patchId = ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID, "loose_" + rock + "_patch");
        var placed = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        PlacedFeature patch = placed.get(patchId);
        var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        JsonObject json = PlacedFeature.DIRECT_CODEC.encodeStart(ops, patch).getOrThrow().getAsJsonObject();
        var placement = json.getAsJsonArray("placement");
        helper.assertTrue(placement.size() == 3, "Only rarity, every-layer count and biome filters may change worldgen");
        helper.assertTrue(placement.get(0).getAsJsonObject().get("chance").getAsInt() == 3, "Patch rarity must be 3");
        helper.assertTrue(placement.get(1).getAsJsonObject().get("count").getAsInt() == count, "Wrong every-layer count");
        helper.assertTrue(placement.get(1).getAsJsonObject().get("type").getAsString().equals("minecraft:count_on_every_layer"),
            "Patches must visit every surface layer");
        JsonObject config = ConfiguredFeature.DIRECT_CODEC.encodeStart(ops, patch.feature().value()).getOrThrow()
            .getAsJsonObject().getAsJsonObject("config");
        helper.assertTrue(config.get("tries").getAsInt() == 16 && config.get("xz_spread").getAsInt() == 10
            && config.get("y_spread").getAsInt() == 1, "Patch settings must match the reference");
        for (var biome : level.registryAccess().registryOrThrow(Registries.BIOME).holders().toList()) {
            boolean present = biome.value().getGenerationSettings().features().get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal())
                .stream().anyMatch(feature -> feature.is(ResourceKey.create(Registries.PLACED_FEATURE, patchId)));
            helper.assertTrue(present == biome.is(biomes),
                "Biome modifier must target its dimension's biome tag: " + biome.key().location());
        }
        PlacedFeature feature = placed.get(ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID, "loose_" + rock));
        Block support = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(rock));
        Block loose = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID, "loose_" + rock));
        BlockPos pos = helper.absolutePos(new BlockPos(1, 3, 1));
        Set<Integer> generatedCounts = new HashSet<>();
        for (int seed = 1; seed <= 32; seed++) {
            level.removeBlock(pos, false);
            level.setBlockAndUpdate(pos.below(), support.defaultBlockState());
            feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed * 0x9E3779B97F4A7C15L), pos);
            helper.assertTrue(level.getBlockState(pos).is(loose), "The feature must place on matching rock");
            generatedCounts.add(level.getBlockState(pos).getValue(LooseRockBlock.COUNT));
            level.removeBlock(pos, false);
            level.setBlockAndUpdate(pos.below(), Blocks.BEDROCK.defaultBlockState());
            feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), pos);
            helper.assertTrue(level.isEmptyBlock(pos), "The feature must reject a different substrate");
            level.setBlockAndUpdate(pos.below(), support.defaultBlockState());
            level.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
            feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), pos);
            helper.assertTrue(level.getBlockState(pos).is(Blocks.BEDROCK), "The feature must not replace terrain");
        }
        helper.assertTrue(generatedCounts.equals(Set.of(1, 2, 3, 4)), "Natural placement must support all four pile sizes");
        level.removeBlock(pos, false);
    }
}
