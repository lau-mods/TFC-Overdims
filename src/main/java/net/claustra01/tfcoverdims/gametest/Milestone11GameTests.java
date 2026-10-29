package net.claustra01.tfcoverdims.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.recipes.CollapseRecipe;
import net.dries007.tfc.common.recipes.LandslideRecipe;
import net.dries007.tfc.util.loot.TFCLoot;

import net.claustra01.tfcoverdims.TFCOverdims;
import net.claustra01.tfcoverdims.worldgen.HardenNetherCeilingFeature;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Milestone11GameTests {
    @GameTest(template = "empty", batch = "climate")
    public static void cobbleCraft(GameTestHelper helper) {
        for (String rock : List.of("netherrack", "end_stone")) {
            String loose = "tfcoverdims:loose_" + rock;
            String cobble = "tfcoverdims:cobble_" + rock;
            checkCraft(helper, 2, 2, List.of(loose, loose, loose, loose), cobble, 1);
            checkCraft(helper, 1, 1, List.of(cobble), loose, 4);
            var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                input(2, 2, List.of(loose, loose, loose, loose)), helper.getLevel()).orElseThrow();
            helper.assertTrue(!recipe.value().getResultItem(helper.getLevel().registryAccess()).is(item("minecraft:" + rock)),
                "Loose stone must make cobble, not raw rock");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void hardenedCraft(GameTestHelper helper) {
        for (String rock : List.of("netherrack", "end_stone")) {
            String raw = "minecraft:" + rock;
            String mortar = "tfc:mortar";
            checkCraft(helper, 3, 3, List.of(raw, mortar, raw, mortar, raw, mortar, raw, mortar, raw),
                "tfcoverdims:hardened_" + rock, 2);
            helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                input(3, 3, List.of(raw, raw, raw, raw, raw, raw, raw, raw, raw)), helper.getLevel()).isEmpty(),
                "Hardened rock must consume four mortar");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void rockCollapseRules(GameTestHelper helper) {
        for (String rock : List.of("netherrack", "end_stone")) {
            Block raw = block("minecraft:" + rock);
            Block cobble = block("tfcoverdims:cobble_" + rock);
            Block hardened = block("tfcoverdims:hardened_" + rock);
            helper.assertTrue(raw.defaultBlockState().is(TFCTags.Blocks.CAN_START_COLLAPSE), "Raw rock starts collapse");
            helper.assertTrue(hardened.defaultBlockState().is(TFCTags.Blocks.CAN_COLLAPSE)
                && hardened.defaultBlockState().is(TFCTags.Blocks.CAN_TRIGGER_COLLAPSE)
                && !hardened.defaultBlockState().is(TFCTags.Blocks.CAN_START_COLLAPSE),
                "Hardened rock propagates collapse but cannot start it");
            helper.assertTrue(CollapseRecipe.getRecipe(raw.defaultBlockState()).assembleBlock(raw.defaultBlockState()).is(cobble)
                && CollapseRecipe.getRecipe(hardened.defaultBlockState()).assembleBlock(hardened.defaultBlockState()).is(cobble),
                "Raw and hardened rock must collapse to cobble");
            helper.assertTrue(cobble.defaultBlockState().is(TFCTags.Blocks.CAN_LANDSLIDE)
                && LandslideRecipe.canLandslide(cobble.defaultBlockState())
                && !cobble.defaultBlockState().is(TFCTags.Blocks.CAN_COLLAPSE)
                && !cobble.defaultBlockState().is(TFCTags.Blocks.CAN_START_COLLAPSE)
                && !cobble.defaultBlockState().is(TFCTags.Blocks.CAN_TRIGGER_COLLAPSE)
                && !raw.defaultBlockState().is(TFCTags.Blocks.CAN_LANDSLIDE),
                "Cobble must landslide without collapse tags");
            for (String type : List.of("cobble", "hardened")) {
                Block actual = block("tfcoverdims:" + type + "_" + rock);
                Block reference = block("tfc:rock/" + type + "/" + (rock.equals("end_stone") ? "limestone" : "basalt"));
                ItemStack actualItem = new ItemStack(actual);
                ItemStack referenceItem = new ItemStack(reference);
                var actualSize = ItemSizeManager.get(actualItem);
                var referenceSize = ItemSizeManager.get(referenceItem);
                helper.assertTrue(actual.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                    && reference.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                    && actual.defaultBlockState().requiresCorrectToolForDrops()
                    == reference.defaultBlockState().requiresCorrectToolForDrops()
                    && actual.defaultBlockState().is(BlockTags.NEEDS_STONE_TOOL)
                    == reference.defaultBlockState().is(BlockTags.NEEDS_STONE_TOOL)
                    && actual.defaultBlockState().is(BlockTags.NEEDS_IRON_TOOL)
                    == reference.defaultBlockState().is(BlockTags.NEEDS_IRON_TOOL)
                    && actual.defaultBlockState().is(BlockTags.NEEDS_DIAMOND_TOOL)
                    == reference.defaultBlockState().is(BlockTags.NEEDS_DIAMOND_TOOL)
                    && actual.defaultBlockState().is(TFCTags.Blocks.CAN_COLLAPSE)
                    == reference.defaultBlockState().is(TFCTags.Blocks.CAN_COLLAPSE)
                    && actual.defaultBlockState().is(TFCTags.Blocks.CAN_START_COLLAPSE)
                    == reference.defaultBlockState().is(TFCTags.Blocks.CAN_START_COLLAPSE)
                    && actual.defaultBlockState().is(TFCTags.Blocks.CAN_TRIGGER_COLLAPSE)
                    == reference.defaultBlockState().is(TFCTags.Blocks.CAN_TRIGGER_COLLAPSE)
                    && actual.defaultBlockState().is(TFCTags.Blocks.CAN_LANDSLIDE)
                    == reference.defaultBlockState().is(TFCTags.Blocks.CAN_LANDSLIDE)
                    && actual.defaultBlockState().getDestroySpeed(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                    == reference.defaultBlockState().getDestroySpeed(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                    && actualSize.getSize(actualItem) == referenceSize.getSize(referenceItem)
                    && actualSize.getWeight(actualItem) == referenceSize.getWeight(referenceItem),
                    "Rock properties must match TFC " + type);
                helper.assertTrue(ItemSizeManager.MANAGER.getValues().stream()
                    .filter(definition -> definition.ingredient().test(actualItem)).count() == 1,
                    "Rock must have one item size definition");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void hardenedLoot(GameTestHelper helper) {
        for (String rock : List.of("netherrack", "end_stone")) {
            Block hardened = block("tfcoverdims:hardened_" + rock);
            Block cobble = block("tfcoverdims:cobble_" + rock);
            for (long seed = 1; seed <= 16; seed++) {
                var params = new LootParams.Builder(helper.getLevel())
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 3, 1))))
                    .withParameter(LootContextParams.BLOCK_STATE, hardened.defaultBlockState())
                    .withParameter(LootContextParams.TOOL, new ItemStack(Items.NETHERITE_PICKAXE));
                var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(hardened.getLootTable());
                var drops = table.getRandomItems(params.create(LootContextParamSets.BLOCK), seed);
                helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item("tfcoverdims:loose_" + rock))
                    && drops.getFirst().getCount() >= 1 && drops.getFirst().getCount() <= 4,
                    "Nonisolated hardened rock must drop loose rock");
                params.withParameter(TFCLoot.ISOLATED, true);
                drops = table.getRandomItems(params.create(LootContextParamSets.BLOCK), seed);
                helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item("minecraft:" + rock))
                    && drops.getFirst().getCount() == 1, "Isolated hardened rock must drop raw rock");
                var cobbleParams = new LootParams.Builder(helper.getLevel())
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 3, 1))))
                    .withParameter(LootContextParams.BLOCK_STATE, cobble.defaultBlockState())
                    .withParameter(LootContextParams.TOOL, new ItemStack(Items.NETHERITE_PICKAXE));
                drops = helper.getLevel().getServer().reloadableRegistries().getLootTable(cobble.getLootTable())
                    .getRandomItems(cobbleParams.create(LootContextParamSets.BLOCK), seed);
                helper.assertTrue(drops.size() == 1 && drops.getFirst().is(cobble.asItem()),
                    "Cobble must drop itself");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void netherCeilingSelection(GameTestHelper helper) {
        var level = helper.getLevel();
        ChunkPos chunk = new ChunkPos(helper.absolutePos(new BlockPos(2, 4, 2)));
        BlockPos ceiling = new BlockPos(chunk.getMinBlockX() + 2,
            helper.absolutePos(new BlockPos(2, 4, 2)).getY(), chunk.getMinBlockZ() + 2);
        BlockPos cave = ceiling.below();
        BlockPos buried = ceiling.above();
        BlockPos floor = cave.below();
        BlockPos protectedRock = ceiling.east();
        level.setBlock(ceiling, Blocks.NETHERRACK.defaultBlockState(), 2);
        level.setBlock(cave, Blocks.CAVE_AIR.defaultBlockState(), 2);
        level.setBlock(buried, Blocks.NETHERRACK.defaultBlockState(), 2);
        level.setBlock(floor, Blocks.NETHERRACK.defaultBlockState(), 2);
        level.setBlock(floor.below(), Blocks.BEDROCK.defaultBlockState(), 2);
        level.setBlock(protectedRock, Blocks.NETHERRACK.defaultBlockState(), 2);
        level.setBlock(protectedRock.below(), Blocks.AIR.defaultBlockState(), 2);
        List<Block> excluded = List.of(Blocks.BEDROCK, Blocks.BLACKSTONE, Blocks.BASALT, Blocks.SOUL_SAND,
            Blocks.SOUL_SOIL, Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS,
            Blocks.LAVA);
        for (int i = 0; i < excluded.size(); i++) {
            BlockPos pos = new BlockPos(chunk.getMinBlockX() + i, ceiling.getY(), chunk.getMinBlockZ() + 4);
            level.setBlock(pos, excluded.get(i).defaultBlockState(), 2);
            level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 2);
        }
        BlockPos untouched = new BlockPos(chunk.getMaxBlockX() + 2, ceiling.getY(), chunk.getMinBlockZ() + 2);
        level.setBlock(untouched, Blocks.NETHERRACK.defaultBlockState(), 2);
        level.setBlock(untouched.below(), Blocks.AIR.defaultBlockState(), 2);
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
            .get(ResourceLocation.withDefaultNamespace("fortress"));
        var piece = new NetherFortressPieces.BridgeCrossing(0,
            BoundingBox.fromCorners(protectedRock, protectedRock), Direction.NORTH);
        var start = new StructureStart(structure, chunk, 0, new PiecesContainer(List.of(piece)));
        var chunkAccess = level.getChunk(chunk.x, chunk.z);
        chunkAccess.setStartForStructure(structure, start);
        chunkAccess.addReferenceForStructure(structure, chunk.toLong());
        helper.assertTrue(level.structureManager().startsForStructure(chunk, candidate -> candidate == structure)
            .contains(start), "Structure lookup must find the protected ceiling");
        var feature = (HardenNetherCeilingFeature) BuiltInRegistries.FEATURE.get(
            ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID, "harden_nether_ceiling"));
        feature.place(NoneFeatureConfiguration.INSTANCE, level, level.getChunkSource().getGenerator(),
            RandomSource.create(17), ceiling);
        helper.assertTrue(level.getBlockState(ceiling).is(block("tfcoverdims:hardened_netherrack")),
            "Exposed underside must harden");
        helper.assertTrue(level.getBlockState(buried).is(Blocks.NETHERRACK)
            && level.getBlockState(floor).is(Blocks.NETHERRACK)
            && level.getBlockState(protectedRock).is(Blocks.NETHERRACK)
            && level.getBlockState(untouched).is(Blocks.NETHERRACK),
            "Buried, floor, structure and other-chunk rock must remain raw");
        for (int i = 0; i < excluded.size(); i++) {
            BlockPos pos = new BlockPos(chunk.getMinBlockX() + i, ceiling.getY(), chunk.getMinBlockZ() + 4);
            helper.assertTrue(level.getBlockState(pos).is(excluded.get(i)),
                "Non-netherrack ceiling blocks must remain unchanged");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate", timeoutTicks = 120)
    public static void netherCeilingDimension(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID, "harden_nether_ceiling");
        var registries = helper.getLevel().registryAccess();
        var placed = registries.registryOrThrow(Registries.PLACED_FEATURE);
        helper.assertTrue(placed.containsKey(id), "Ceiling placed feature must load");
        helper.assertTrue(registries.registryOrThrow(Registries.CONFIGURED_FEATURE).containsKey(id),
            "Ceiling configured feature must load");
        var key = ResourceKey.create(Registries.PLACED_FEATURE, id);
        for (var biome : registries.registryOrThrow(Registries.BIOME).holders().toList()) {
            var steps = biome.value().getGenerationSettings().features();
            int step = GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal();
            boolean attached = step < steps.size() && steps.get(step).stream()
                .anyMatch(feature -> feature.is(key));
            helper.assertTrue(attached == biome.is(BiomeTags.IS_NETHER),
                "Only Nether biomes may receive the ceiling feature: " + biome.key().location());
        }

        var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        int x = 1200 + (int) (helper.getLevel().getGameTime() % 1000);
        int z = -1200 - (int) (helper.getLevel().getGameTime() % 1000);
        helper.assertTrue(!nether.getChunkSource().hasChunk(x, z), "Sample chunk must be new");
        var chunk = nether.getChunk(x, z);
        boolean found = false;
        for (int px = chunk.getPos().getMinBlockX(); px <= chunk.getPos().getMaxBlockX(); px++) {
            for (int pz = chunk.getPos().getMinBlockZ(); pz <= chunk.getPos().getMaxBlockZ(); pz++) {
                for (int py = nether.getMinBuildHeight() + 1; py < nether.getMaxBuildHeight(); py++) {
                    BlockPos pos = new BlockPos(px, py, pz);
                    if (nether.getBlockState(pos).is(block("tfcoverdims:hardened_netherrack"))) {
                        found = true;
                        helper.assertTrue(nether.getBlockState(pos.below()).is(Blocks.AIR)
                            || nether.getBlockState(pos.below()).is(Blocks.CAVE_AIR),
                            "Generated hardened rock must face a cave");
                    }
                }
            }
        }
        helper.assertTrue(found, "Fresh Nether terrain must contain a hardened ceiling");
        helper.succeed();
    }

    private static void checkCraft(GameTestHelper helper, int width, int height, List<String> ingredients,
        String result, int count) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), width, height);
        for (int i = 0; i < ingredients.size(); i++) {
            grid.setItem(i, new ItemStack(item(ingredients.get(i))));
        }
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
            grid.asCraftInput(), helper.getLevel()).orElseThrow();
        ItemStack output = recipe.value().assemble(grid.asCraftInput(), helper.getLevel().registryAccess());
        helper.assertTrue(output.is(item(result)) && output.getCount() == count,
            "Crafting result must be " + result + " x" + count);
        var resultContainer = new ResultContainer();
        resultContainer.setRecipeUsed(recipe);
        resultContainer.setItem(0, output);
        var slot = new ResultSlot(player, grid, resultContainer, 0, 0, 0);
        slot.onTake(player, slot.remove(count));
        for (int i = 0; i < ingredients.size(); i++) {
            helper.assertTrue(grid.getItem(i).isEmpty(), "Crafting must consume " + ingredients.get(i));
        }
    }

    private static CraftingInput input(int width, int height, List<String> ingredients) {
        return CraftingInput.of(width, height, ingredients.stream().map(id -> new ItemStack(item(id))).toList());
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }
}
