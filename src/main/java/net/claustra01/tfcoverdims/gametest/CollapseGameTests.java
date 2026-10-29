package net.claustra01.tfcoverdims.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.recipes.CollapseRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.util.data.Support;

import net.claustra01.tfcoverdims.TFCOverdims;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CollapseGameTests {
    @GameTest(template = "collapse", batch = "climate", timeoutTicks = 120)
    public static void blackstoneCollapse(GameTestHelper helper) {
        assertCollapse(helper, Blocks.BLACKSTONE, Blocks.BLACKSTONE);
    }

    @GameTest(template = "collapse", batch = "climate", timeoutTicks = 120)
    public static void tfcBasaltCollapse(GameTestHelper helper) {
        assertCollapse(helper, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:rock/raw/basalt")),
            BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:rock/cobble/basalt")));
    }

    @GameTest(template = "collapse", batch = "climate", timeoutTicks = 120)
    public static void supportBeams(GameTestHelper helper) {
        var level = helper.getLevel();
        Block vertical = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:wood/vertical_support/oak"));
        Block horizontal = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:wood/horizontal_support/oak"));
        prepareRoof(helper, Blocks.BLACKSTONE);
        // Place the complete frame before neighbor shape updates can remove an unfinished span.
        for (int x : List.of(6, 10)) {
            for (int y = 2; y <= 5; y++) {
                level.setBlock(helper.absolutePos(new BlockPos(x, y, 6)), vertical.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
        for (int x = 7; x <= 9; x++) {
            level.setBlock(helper.absolutePos(new BlockPos(x, 5, 6)), horizontal.defaultBlockState()
                .setValue(BlockStateProperties.EAST, true).setValue(BlockStateProperties.WEST, true), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        for (int x = 7; x <= 9; x++) {
            BlockPos beam = helper.absolutePos(new BlockPos(x, 5, 6));
            helper.assertTrue(level.getBlockState(beam).is(horizontal) && level.getBlockState(beam).canSurvive(level, beam),
                "The assembled support span must be a valid TFC beam");
        }
        for (Block rock : List.of(Blocks.BLACKSTONE)) {
            prepareRoof(helper, rock);
            for (BlockPos pos : BlockPos.betweenClosed(7, 7, 7, 9, 7, 9)) {
                helper.assertTrue(Support.isSupported(level, helper.absolutePos(pos)), "The real horizontal beam must protect the roof");
            }
            for (int seed = 0; seed < 64; seed++) {
                helper.setBlock(new BlockPos(8, 6, 8), rock);
                level.random.setSeed(RandomSource.create(seed).nextLong());
                minePillar(helper);
                helper.assertTrue(!CollapseRecipe.tryTriggerCollapse(level, helper.absolutePos(new BlockPos(8, 6, 8))),
                    "Mining under a protected roof must not trigger a collapse");
            }
            helper.assertTrue(falling(helper).isEmpty(), "Beam-protected rock must not create falling entities");
            for (BlockPos pos : BlockPos.betweenClosed(7, 7, 7, 9, 7, 9)) {
                helper.assertBlockPresent(rock, pos);
            }
        }
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(falling(helper).isEmpty(), "Protected roof must remain in place after world ticks");
            helper.succeed();
        });
    }

    @GameTest(template = "collapse", batch = "climate")
    public static void collapseExclusions(GameTestHelper helper) {
        for (Block block : List.of(Blocks.NETHER_BRICKS, Blocks.END_STONE_BRICKS, Blocks.BASALT,
            Blocks.POLISHED_BASALT, Blocks.SMOOTH_BASALT)) {
            prepareRoof(helper, block);
            var state = block.defaultBlockState();
            helper.assertTrue(!state.is(TFCTags.Blocks.CAN_TRIGGER_COLLAPSE)
                && !state.is(TFCTags.Blocks.CAN_START_COLLAPSE) && !state.is(TFCTags.Blocks.CAN_COLLAPSE),
                "Excluded blocks must not acquire collapse tags: " + block);
            helper.assertTrue(CollapseRecipe.getRecipe(state) == null, "Excluded blocks must not acquire a collapse recipe: " + block);
            for (int seed = 0; seed < 64; seed++) {
                helper.setBlock(new BlockPos(8, 6, 8), block);
                helper.getLevel().random.setSeed(RandomSource.create(seed).nextLong());
                minePillar(helper);
            }
            helper.assertTrue(!CollapseRecipe.canStartCollapse(helper.getLevel(), helper.absolutePos(new BlockPos(8, 7, 8))),
                "Unsupported excluded blocks must not start a collapse");
            helper.assertTrue(!CollapseRecipe.collapseBlock(helper.getLevel(), helper.absolutePos(new BlockPos(8, 7, 8)), state),
                "Excluded blocks must not fall even when checked during propagation");
            for (BlockPos pos : BlockPos.betweenClosed(7, 7, 7, 9, 7, 9)) {
                helper.assertBlockPresent(block, pos);
            }
            helper.assertTrue(falling(helper).isEmpty(), "Excluded structures must not produce falling entities");
        }
        for (Block ore : List.of(Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS, Blocks.GILDED_BLACKSTONE)) {
            var state = ore.defaultBlockState();
            boolean existingOre = state.is(TagKey.create(Registries.BLOCK, ResourceLocation.parse("c:ores")));
            helper.assertTrue(state.is(TFCTags.Blocks.CAN_COLLAPSE) == existingOre
                && state.is(TFCTags.Blocks.CAN_START_COLLAPSE) == existingOre
                && state.is(TFCTags.Blocks.CAN_TRIGGER_COLLAPSE) == existingOre,
                "Vanilla ores must keep TFC's existing common ore tag behavior");
            helper.assertTrue(helper.getLevel().getRecipeManager().getAllRecipesFor(TFCRecipeTypes.COLLAPSE.get()).stream()
                .noneMatch(holder -> holder.value().matches(state)), "Vanilla ores must not acquire a conversion recipe");
            if (existingOre) {
                helper.assertTrue(CollapseRecipe.getRecipe(state).assembleBlock(state).equals(state),
                    "Existing ore collapse must keep its unchanged fallback result");
            }
        }
        helper.succeed();
    }

    private static void assertCollapse(GameTestHelper helper, Block rock, Block result) {
        prepareRoof(helper, rock);
        var state = rock.defaultBlockState();
        helper.assertTrue(state.is(TFCTags.Blocks.CAN_TRIGGER_COLLAPSE) && state.is(TFCTags.Blocks.CAN_START_COLLAPSE)
            && state.is(TFCTags.Blocks.CAN_COLLAPSE), "Rock must be connected to all three TFC collapse stages");
        var recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(TFCRecipeTypes.COLLAPSE.get()).stream()
            .filter(holder -> holder.value().matches(state)).toList();
        helper.assertTrue(recipes.size() == 1 && recipes.getFirst().value().assembleBlock(state).is(result),
            "Loaded collapse recipe must yield the existing block");
        helper.assertTrue(CollapseRecipe.canStartCollapse(helper.getLevel(), helper.absolutePos(new BlockPos(7, 7, 8))),
            "The roof around the pillar must contain unsupported collapse starting points");
        // Exercise the configured mining trigger without changing server probabilities.
        for (int seed = 0; seed < 64 && falling(helper).isEmpty(); seed++) {
            helper.setBlock(new BlockPos(8, 6, 8), rock);
            helper.getLevel().random.setSeed(RandomSource.create(seed).nextLong());
            minePillar(helper);
        }
        var entities = falling(helper).stream().filter(entity -> entity.blockPosition().getY()
            == helper.absolutePos(new BlockPos(0, 7, 0)).getY()).toList();
        helper.assertTrue(!entities.isEmpty() && entities.stream().allMatch(entity -> entity.getBlockState().is(result)),
            "TFC falling entities must carry the expected existing block");
        List<BlockPos> landings = entities.stream().map(entity -> new BlockPos(entity.blockPosition().getX(),
            helper.absolutePos(new BlockPos(0, 2, 0)).getY(), entity.blockPosition().getZ())).toList();
        helper.succeedWhen(() -> {
            helper.assertTrue(falling(helper).isEmpty(), "Falling blocks must land");
            for (BlockPos landing : landings) {
                helper.assertTrue(helper.getLevel().getBlockState(landing).is(result), "Collapse must place the existing block on the floor");
            }
        });
    }

    private static void minePillar(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(8, 6, 8))),
            "Survival mining must remove the roof pillar");
    }

    private static void prepareRoof(GameTestHelper helper, Block rock) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(8, 6, 8));
        // TFC skips mining collapse checks unless the surrounding 32 blocks are loaded.
        for (int x = (center.getX() - 32) >> 4; x <= (center.getX() + 32) >> 4; x++) {
            for (int z = (center.getZ() - 32) >> 4; z <= (center.getZ() + 32) >> 4; z++) {
                level.getChunk(x, z);
            }
        }
        for (BlockPos pos : BlockPos.betweenClosed(1, 1, 1, 15, 1, 15)) {
            helper.setBlock(pos, Blocks.BEDROCK);
        }
        for (BlockPos pos : BlockPos.betweenClosed(7, 7, 7, 9, 7, 9)) {
            helper.setBlock(pos, rock);
        }
        helper.setBlock(new BlockPos(8, 6, 8), rock);
    }

    private static List<FallingBlockEntity> falling(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(FallingBlockEntity.class,
            new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(1, 1, 1))),
                Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(16, 11, 16)))));
    }
}
