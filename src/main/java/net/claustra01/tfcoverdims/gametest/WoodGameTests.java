package net.claustra01.tfcoverdims.gametest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.biome.NetherBiomes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.CountOnEveryLayerPlacement;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.dries007.tfc.common.blockentities.AbstractFirepitBlockEntity;
import net.dries007.tfc.common.blockentities.FirepitBlockEntity;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.blockentities.LogPileBlockEntity;
import net.dries007.tfc.common.blockentities.TickCounterBlockEntity;
import net.dries007.tfc.common.blockentities.rotation.CreativeRotationBlockEntity;
import net.dries007.tfc.common.blockentities.rotation.RotatingBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.wood.TFCLeavesBlock;
import net.dries007.tfc.common.blocks.wood.TFCSaplingBlock;
import net.dries007.tfc.common.blocks.wood.Wood;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.recipes.CollapseRecipe;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.data.Fuel;
import net.dries007.tfc.util.data.Support;
import net.dries007.tfc.world.feature.tree.RandomTreeConfig;

import net.claustra01.tfcoverdims.TFCOverdims;
import net.claustra01.tfcoverdims.climate.NetherClimateModel;
import net.claustra01.tfcoverdims.common.wood.OverdimsWood;
import net.claustra01.tfcoverdims.common.wood.OverdimsWoodBlocks;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WoodGameTests {
    @GameTest(template = "empty", batch = "climate", timeoutTicks = 200)
    public static void netherSaplingGrowth(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null && Climate.get(nether) == NetherClimateModel.INSTANCE,
            "Nether saplings must use the Nether climate model");
        for (OverdimsWood wood : OverdimsWood.values()) {
            BlockPos pos = new BlockPos(264 + 64 * wood.ordinal(), 80, 264);
            for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-8, 0, -8), pos.offset(8, 29, 8))) {
                nether.setBlock(scan, Blocks.AIR.defaultBlockState(), 2);
            }
            nether.setBlockAndUpdate(pos.below(),
                (wood == OverdimsWood.CRIMSON ? Blocks.CRIMSON_NYLIUM : Blocks.WARPED_NYLIUM).defaultBlockState());
            TFCSaplingBlock sapling = (TFCSaplingBlock) OverdimsWoodBlocks.WOODS.get(wood).get(Wood.BlockType.SAPLING).get();
            nether.setBlockAndUpdate(pos, sapling.defaultBlockState());
            helper.assertTrue(nether.getBlockState(pos).canSurvive(nether, pos)
                && nether.getMaxLocalRawBrightness(pos.above()) < 9,
                "Nether sapling must survive on nylium in low light: " + wood);
            var counter = (TickCounterBlockEntity) nether.getBlockEntity(pos);
            helper.assertTrue(counter != null, "Nether sapling needs a Calendar counter: " + wood);
            counter.setLastUpdateTick(Calendars.SERVER.getTicks() - 7L * ICalendar.CALENDAR_TICKS_IN_DAY);
            for (int attempt = 0; attempt < 50; attempt++) {
                nether.getBlockState(pos).randomTick(nether, pos, RandomSource.create(attempt));
            }
            helper.assertTrue(nether.getBlockState(pos).is(sapling)
                && nether.getBlockState(pos).getValue(TFCSaplingBlock.STAGE) == 0,
                "Sapling must wait for eight TFC days in the Nether: " + wood);
            counter.setLastUpdateTick(Calendars.SERVER.getTicks() - 9L * ICalendar.CALENDAR_TICKS_IN_DAY);
            for (int attempt = 0; attempt < 100 && nether.getBlockState(pos).is(sapling); attempt++) {
                nether.getBlockState(pos).randomTick(nether, pos, RandomSource.create(attempt));
            }
            int logs = 0;
            int leaves = 0;
            for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-8, 0, -8), pos.offset(8, 29, 8))) {
                Block block = nether.getBlockState(scan).getBlock();
                if (block == OverdimsWoodBlocks.WOODS.get(wood).get(Wood.BlockType.LOG).get()) logs++;
                if (block == OverdimsWoodBlocks.WOODS.get(wood).get(Wood.BlockType.LEAVES).get()) leaves++;
            }
            helper.assertTrue(logs >= 3 && leaves > 0, "Dark Nether sapling must grow into a tree: " + wood);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void woodRegistry(GameTestHelper helper) {
        Set<ResourceLocation> expectedItems = new HashSet<>();
        List<String> sizeErrors = new ArrayList<>();
        helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet().stream()
            .noneMatch(id -> id.getNamespace().equals(TFCOverdims.MOD_ID))
            && BuiltInRegistries.MENU.keySet().stream().noneMatch(id -> id.getNamespace().equals(TFCOverdims.MOD_ID)),
            "Wood must use existing TFC block entities and menus");
        for (OverdimsWood wood : OverdimsWood.values()) {
            for (Wood.BlockType type : Wood.BlockType.values()) {
                Block block = OverdimsWoodBlocks.WOODS.get(wood).get(type).get();
                helper.assertTrue(BuiltInRegistries.BLOCK.getKey(block).equals(ResourceLocation.fromNamespaceAndPath(
                    TFCOverdims.MOD_ID, type.nameFor(wood))), "Missing TFC wood block " + type + " / " + wood);
                if (type.needsItem()) {
                    expectedItems.add(BuiltInRegistries.ITEM.getKey(block.asItem()));
                    ItemStack stack = new ItemStack(block);
                    helper.assertTrue(!stack.isEmpty(), "Missing wood item " + type + " / " + wood);
                    assertSize(sizeErrors, stack, "tfc:" + type.nameFor(Wood.OAK));
                }
            }
            for (String kind : List.of("lumber", "support", "sign")) {
                expectedItems.add(ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID,
                    "wood/" + kind + "/" + wood.getSerializedName()));
                assertSize(sizeErrors, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                    TFCOverdims.MOD_ID, "wood/" + kind + "/" + wood.getSerializedName()))),
                    "tfc:wood/" + kind + "/oak");
            }
        }
        Set<ResourceLocation> actualItems = new HashSet<>();
        BuiltInRegistries.ITEM.keySet().stream().filter(id -> id.getNamespace().equals(TFCOverdims.MOD_ID)
            && id.getPath().startsWith("wood/")).forEach(actualItems::add);
        helper.assertTrue(actualItems.equals(expectedItems), "Wood item set must be BlockType items plus lumber, support and sign");
        for (Block block : List.of(Blocks.CRIMSON_STEM, Blocks.WARPED_STEM, Blocks.STRIPPED_CRIMSON_STEM,
            Blocks.STRIPPED_WARPED_STEM, Blocks.CRIMSON_HYPHAE, Blocks.WARPED_HYPHAE,
            Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE, Blocks.CRIMSON_PLANKS,
            Blocks.WARPED_PLANKS, Blocks.CRIMSON_FUNGUS, Blocks.WARPED_FUNGUS, Blocks.CRIMSON_NYLIUM,
            Blocks.WARPED_NYLIUM, Blocks.NETHER_WART_BLOCK, Blocks.WARPED_WART_BLOCK,
            Blocks.CRIMSON_STAIRS, Blocks.WARPED_STAIRS, Blocks.CRIMSON_SLAB, Blocks.WARPED_SLAB,
            Blocks.CRIMSON_FENCE, Blocks.WARPED_FENCE, Blocks.CRIMSON_FENCE_GATE, Blocks.WARPED_FENCE_GATE,
            Blocks.CRIMSON_DOOR, Blocks.WARPED_DOOR, Blocks.CRIMSON_TRAPDOOR, Blocks.WARPED_TRAPDOOR,
            Blocks.CRIMSON_BUTTON, Blocks.WARPED_BUTTON, Blocks.CRIMSON_PRESSURE_PLATE, Blocks.WARPED_PRESSURE_PLATE,
            Blocks.CRIMSON_SIGN, Blocks.WARPED_SIGN, Blocks.CRIMSON_WALL_SIGN, Blocks.WARPED_WALL_SIGN,
            Blocks.CRIMSON_HANGING_SIGN, Blocks.WARPED_HANGING_SIGN, Blocks.CRIMSON_WALL_HANGING_SIGN,
            Blocks.WARPED_WALL_HANGING_SIGN, Blocks.POTTED_CRIMSON_FUNGUS, Blocks.POTTED_WARPED_FUNGUS)) {
            String name = block.getDescriptionId().substring("block.minecraft.".length());
            if (block instanceof WallSignBlock) name = name.replace("_sign", "_wall_sign");
            if (block instanceof WallHangingSignBlock) name = name.replace("_hanging_sign", "_wall_hanging_sign");
            ResourceLocation id = ResourceLocation.withDefaultNamespace(name);
            helper.assertTrue(BuiltInRegistries.BLOCK.get(id) == block, "Vanilla Nether block must retain its original ID: " + id);
        }
        for (var recipe : helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (recipe.id().getNamespace().equals("tfc") && recipe.id().getPath().startsWith("crafting/wood/")
                && recipe.id().getPath().contains("/oak")) {
                String result = BuiltInRegistries.ITEM.getKey(recipe.value().getResultItem(helper.getLevel().registryAccess()).getItem()).getPath();
                for (OverdimsWood wood : OverdimsWood.values()) {
                    ResourceLocation product = ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID,
                        result.replace("/oak", "/" + wood.getSerializedName()));
                    if (expectedItems.contains(product)) {
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID,
                            recipe.id().getPath().replace("crafting/", "").replace("/oak", "/" + wood.getSerializedName()));
                        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id).isPresent(), "Missing TFC-style recipe: " + id);
                    }
                }
            }
        }
        helper.assertTrue(sizeErrors.isEmpty(), String.join("; ", sizeErrors));
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void woodStorage(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            helper.getLevel().setBlock(pos.relative(direction), Blocks.STONE.defaultBlockState(), 3);
        }
        for (OverdimsWood wood : OverdimsWood.values()) {
            for (Wood.BlockType support : List.of(Wood.BlockType.VERTICAL_SUPPORT,
                Wood.BlockType.HORIZONTAL_SUPPORT)) {
                helper.assertTrue(OverdimsWoodBlocks.WOODS.get(wood).get(support).get().defaultBlockState()
                    .is(TagKey.create(Registries.BLOCK, ResourceLocation.parse("tfc:support_beams"))),
                    "Nether support must protect collapsible blocks");
            }
            for (Wood.BlockType type : Wood.BlockType.values()) {
                Block block = OverdimsWoodBlocks.WOODS.get(wood).get(type).get();
                if (!(block instanceof EntityBlock)) continue;
                helper.getLevel().setBlock(pos.below(), Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
                helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
                BlockEntity entity = helper.getLevel().getBlockEntity(pos);
                helper.assertTrue(entity != null && entity.getType().isValid(block.defaultBlockState()),
                    "TFC block entity must accept " + type + " / " + wood);
                if (type == Wood.BlockType.CHEST) {
                    ((Container) entity).setItem(0, new ItemStack(Items.DIAMOND, 3));
                } else if (type == Wood.BlockType.CRATE || type == Wood.BlockType.BARREL) {
                    ((InventoryBlockEntity<?>) entity).getInventory().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
                }
                var saved = entity.saveWithFullMetadata(helper.getLevel().registryAccess());
                BlockEntity loaded = BlockEntity.loadStatic(pos, block.defaultBlockState(), saved,
                    helper.getLevel().registryAccess());
                helper.assertTrue(loaded != null && loaded.getType().isValid(block.defaultBlockState()),
                    "Wood block entity must reload " + type + " / " + wood);
                if (type == Wood.BlockType.CHEST) {
                    helper.assertTrue(((Container) loaded).getItem(0).getCount() == 3, "Chest lost inventory");
                } else if (type == Wood.BlockType.CRATE || type == Wood.BlockType.BARREL) {
                    helper.assertTrue(((InventoryBlockEntity<?>) loaded).getInventory().getStackInSlot(0).getCount() == 3,
                        "Wood storage lost inventory");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate", timeoutTicks = 80)
    public static void woodHarvest(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        player.setGameMode(GameType.SURVIVAL);
        ItemStack axe = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfc:metal/axe/copper")));
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        for (OverdimsWood wood : OverdimsWood.values()) {
            BlockPos pos = helper.absolutePos(new BlockPos(1 + wood.ordinal() * 3, 2, 1));
            Block log = wood.getBlock(Wood.BlockType.LOG).get();
            level.setBlock(pos, log.defaultBlockState(), 3);
            helper.assertTrue(axe.isCorrectToolForDrops(log.defaultBlockState())
                && axe.getDestroySpeed(log.defaultBlockState()) > 1, "TFC axe must harvest Nether logs efficiently");
            helper.assertTrue(player.gameMode.destroyBlock(pos), "Survival axe must mine Nether log");
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).stream()
                .anyMatch(entity -> entity.getItem().is(log.asItem())), "Harvested Nether log must drop itself");
            level.setBlock(pos, wood.getBlock(Wood.BlockType.LEAVES).get().defaultBlockState(), 3);
            level.setBlock(pos.west(), log.defaultBlockState(), 3);
        }
        helper.runAfterDelay(20, () -> {
            for (OverdimsWood wood : OverdimsWood.values()) {
                BlockPos pos = helper.absolutePos(new BlockPos(1 + wood.ordinal() * 3, 2, 1));
                var state = level.getBlockState(pos);
                helper.assertTrue(state.is(wood.getBlock(Wood.BlockType.LEAVES).get())
                    && state.getValue(TFCLeavesBlock.DISTANCE) == 1, "Adjacent logs must support nonpersistent leaves");
                state.randomTick(level, pos, RandomSource.create(123));
                helper.assertTrue(level.getBlockState(pos).is(state.getBlock()), "Supported leaves must not decay");
                helper.assertTrue(new ItemStack(OverdimsWoodBlocks.LUMBER.get(wood).get()).is(
                    TagKey.create(Registries.ITEM, ResourceLocation.parse("tfc:lumber"))), "Generic lumber recipes must accept Nether lumber");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "collapse", batch = "climate", timeoutTicks = 100)
    public static void woodSupportsAndMachines(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(8, 4, 8));
        for (int x = (center.getX() - 32) >> 4; x <= (center.getX() + 32) >> 4; x++) {
            for (int z = (center.getZ() - 32) >> 4; z <= (center.getZ() + 32) >> 4; z++) level.getChunk(x, z);
        }
        var player = FakePlayerFactory.getMinecraft(level);
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        for (OverdimsWood wood : OverdimsWood.values()) {
            int z = 3 + wood.ordinal() * 8;
            for (int x : List.of(2, 6)) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                for (int y = 2; y <= 4; y++) {
                    level.setBlock(helper.absolutePos(new BlockPos(x, y, z)),
                        wood.getBlock(Wood.BlockType.VERTICAL_SUPPORT).get().defaultBlockState(), 18);
                }
            }
            for (int x = 3; x <= 5; x++) {
                BlockPos beam = helper.absolutePos(new BlockPos(x, 4, z));
                var state = wood.getBlock(Wood.BlockType.HORIZONTAL_SUPPORT).get().defaultBlockState()
                    .setValue(BlockStateProperties.EAST, true).setValue(BlockStateProperties.WEST, true);
                level.setBlock(beam, state, 18);
            }
            for (int x = 3; x <= 5; x++) {
                BlockPos beam = helper.absolutePos(new BlockPos(x, 4, z));
                helper.assertTrue(level.getBlockState(beam).canSurvive(level, beam), "Nether support span must be valid");
            }
            for (Block rock : List.of(Blocks.BLACKSTONE, Blocks.END_STONE)) {
                for (int x = 3; x <= 5; x++) {
                    BlockPos roof = helper.absolutePos(new BlockPos(x, 6, z));
                    level.setBlock(roof, rock.defaultBlockState(), 3);
                    helper.assertTrue(Support.isSupported(level, roof),
                        "Nether beams must protect collapsible rock");
                }
                BlockPos pillar = helper.absolutePos(new BlockPos(4, 5, z));
                level.setBlock(pillar, rock.defaultBlockState(), 3);
                helper.assertTrue(player.gameMode.destroyBlock(pillar), "Mine the pillar beneath the supported roof");
                helper.assertTrue(!CollapseRecipe.tryTriggerCollapse(level, helper.absolutePos(new BlockPos(4, 5, z))),
                    "Protected roof must not collapse after mining below it");
            }
            BlockPos motorPos = helper.absolutePos(new BlockPos(9, 3, z));
            level.setBlock(motorPos, TFCBlocks.CREATIVE_MOTOR.get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X), 3);
            level.setBlock(motorPos.east(), wood.getBlock(Wood.BlockType.AXLE).get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X), 3);
            level.setBlock(motorPos.east(2), wood.getBlock(Wood.BlockType.GEAR_BOX).get().defaultBlockState()
                .setValue(BlockStateProperties.WEST, true).setValue(BlockStateProperties.EAST, true), 3);
            level.setBlock(motorPos.east(3), wood.getBlock(Wood.BlockType.CLUTCH).get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X), 3);
            level.setBlock(motorPos.east(4), wood.getBlock(Wood.BlockType.AXLE).get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X), 3);
            ((CreativeRotationBlockEntity) level.getBlockEntity(motorPos)).incrementSpeed();
        }
        helper.runAfterDelay(20, () -> {
            for (OverdimsWood wood : OverdimsWood.values()) {
                BlockPos motorPos = helper.absolutePos(new BlockPos(9, 3, 3 + wood.ordinal() * 8));
                for (int x = 3; x <= 5; x++) {
                    helper.assertBlockPresent(Blocks.END_STONE, new BlockPos(x, 6, 3 + wood.ordinal() * 8));
                }
                var source = ((RotatingBlockEntity) level.getBlockEntity(motorPos)).getRotationNode().rotation();
                for (int offset = 1; offset <= 4; offset++) {
                    var part = (RotatingBlockEntity) level.getBlockEntity(motorPos.east(offset));
                    var rotation = part.getRotationNode().rotation();
                    helper.assertTrue(!part.isInvalidInNetwork() && rotation != null && rotation.speed() > 0
                        && rotation.speed() == source.speed(), "Rotation must pass through Nether axle, gear box and clutch");
                }
            }
            helper.succeed();
        });
    }

    @GameTest(template = "nether_tree", batch = "climate")
    public static void netherTrees(GameTestHelper helper) throws IOException {
        for (OverdimsWood wood : OverdimsWood.values()) {
            String name = wood.getSerializedName();
            int count = wood == OverdimsWood.CRIMSON ? 16 : 17;
            var registries = helper.getLevel().registryAccess();
            var placedRegistry = registries.registryOrThrow(Registries.PLACED_FEATURE);
            ResourceLocation treeId = ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID, "tree/" + name);
            var placed = placedRegistry.get(treeId);
            helper.assertTrue(placed != null && placed.placement().getFirst() instanceof CountOnEveryLayerPlacement,
                "Tree placement must count on every layer");
            var placement = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, placed.placement().getFirst()).getOrThrow().getAsJsonObject();
            helper.assertTrue(placement.get("count").getAsInt() == 8, "Every-layer count must be eight");
            var config = (RandomTreeConfig) placed.feature().value().config();
            helper.assertTrue(config.structureNames().size() == count, "Wrong tree candidate count");
            helper.assertTrue(config.trunk().orElseThrow().minHeight() == 3
                && config.trunk().orElseThrow().maxHeight() == 5, "Wrong trunk height");
            var biome = registries.registryOrThrow(Registries.BIOME).get(ResourceLocation.withDefaultNamespace(name + "_forest"));
            var carvers = registries.registryOrThrow(Registries.CONFIGURED_CARVER).asLookup();
            var vanilla = wood == OverdimsWood.CRIMSON ? NetherBiomes.crimsonForest(placedRegistry.asLookup(), carvers)
                : NetherBiomes.warpedForest(placedRegistry.asLookup(), carvers);
            var actual = biome.getGenerationSettings().features();
            var original = vanilla.getGenerationSettings().features();
            boolean found = false;
            for (int step = 0; step < Math.max(actual.size(), original.size()); step++) {
                List<ResourceLocation> current = step < actual.size() ? actual.get(step).stream()
                    .map(holder -> placedRegistry.getKey(holder.value())).toList() : List.of();
                List<ResourceLocation> baseline = step < original.size() ? original.get(step).stream()
                    .map(holder -> placedRegistry.getKey(holder.value()))
                    .filter(id -> !id.equals(ResourceLocation.withDefaultNamespace(name + "_fungi"))).toList() : List.of();
                helper.assertTrue(!current.contains(ResourceLocation.withDefaultNamespace(name + "_fungi")),
                    "Vanilla giant fungus feature must be removed");
                found |= current.contains(treeId);
                var unchanged = current.stream().filter(id -> !id.equals(treeId)
                    && !id.equals(ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID, "loose_netherrack_patch"))
                    && !id.equals(ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID, "loose_blackstone_patch"))
                    && !id.equals(ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID, "harden_nether_ceiling"))).toList();
                helper.assertTrue(unchanged.equals(baseline), "Other Nether features must remain unchanged at step " + step);
            }
            helper.assertTrue(found, "Modified forest must include its Nether tree");
            for (int i = 1; i <= count; i++) {
                var nbt = NbtIo.readCompressed(resource("data/" + TFCOverdims.MOD_ID + "/structure/" + name + "/" + i + ".nbt"),
                    NbtAccounter.unlimitedHeap());
                var palette = nbt.getList("palette", 10);
                for (int entry = 0; entry < palette.size(); entry++) {
                    String block = palette.getCompound(entry).getString("Name");
                    helper.assertTrue(List.of(TFCOverdims.MOD_ID + ":wood/log/" + name,
                        TFCOverdims.MOD_ID + ":wood/wood/" + name, TFCOverdims.MOD_ID + ":wood/leaves/" + name).contains(block),
                        "Tree templates must contain only this species' logs, branches and leaves");
                }
            }
            BlockPos tree = prepareTreeSite(helper, wood);
            ConfiguredFeature<?, ?> configured = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE).get(ResourceLocation.fromNamespaceAndPath(
                    TFCOverdims.MOD_ID, "tree/" + name));
            helper.assertTrue(configured != null && configured.place(helper.getLevel(),
                helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(43), tree),
                "Nether tree feature must place on nylium");
            int logs = 0;
            int leaves = 0;
            for (BlockPos scan : BlockPos.betweenClosed(tree.offset(-8, 0, -8), tree.offset(8, 29, 8))) {
                Block block = helper.getLevel().getBlockState(scan).getBlock();
                if (block == OverdimsWoodBlocks.WOODS.get(wood).get(Wood.BlockType.LOG).get()) logs++;
                if (block == OverdimsWoodBlocks.WOODS.get(wood).get(Wood.BlockType.LEAVES).get()) leaves++;
                helper.assertTrue(!isGiantFungusBlock(block),
                    "Nether tree must not contain Vanilla giant fungus blocks");
            }
            helper.assertTrue(logs >= 3 && leaves > 0, "Tree must contain TFC logs and leaves");
        }
        helper.succeed();
    }

    @GameTest(template = "nether_tree", batch = "climate")
    public static void saplingAndFungus(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000);
        helper.getLevel().updateSkyBrightness();
        for (OverdimsWood wood : OverdimsWood.values()) {
            var sapling = (TFCSaplingBlock) OverdimsWoodBlocks.WOODS.get(wood).get(Wood.BlockType.SAPLING).get();
            helper.assertTrue(sapling.getTicksToGrow() == 8L * ICalendar.CALENDAR_TICKS_IN_DAY,
                "Sapling must take eight TFC days");
            BlockPos pos = prepareTreeSite(helper, wood);
            helper.getLevel().setBlock(pos, sapling.defaultBlockState(), 3);
            var counter = (TickCounterBlockEntity) helper.getLevel().getBlockEntity(pos);
            helper.assertTrue(counter != null, "Sapling needs TFC calendar counter");
            counter.setLastUpdateTick(Calendars.SERVER.getTicks() - 9L * ICalendar.CALENDAR_TICKS_IN_DAY);
            for (int attempt = 0; attempt < 100 && helper.getLevel().getBlockState(pos).is(sapling); attempt++) {
                helper.getLevel().getBlockState(pos).randomTick(helper.getLevel(), pos, RandomSource.create(attempt));
            }
            int logs = 0;
            int leaves = 0;
            for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-8, 0, -8), pos.offset(8, 29, 8))) {
                Block block = helper.getLevel().getBlockState(scan).getBlock();
                if (block == OverdimsWoodBlocks.WOODS.get(wood).get(Wood.BlockType.LOG).get()) logs++;
                if (block == OverdimsWoodBlocks.WOODS.get(wood).get(Wood.BlockType.LEAVES).get()) leaves++;
            }
            helper.assertTrue(logs >= 3 && leaves > 0, "Mature TFC sapling must grow a Nether tree");
        }
        for (OverdimsWood wood : OverdimsWood.values()) {
            BlockPos pos = prepareTreeSite(helper, wood);
            Block fungus = wood == OverdimsWood.CRIMSON ? Blocks.CRIMSON_FUNGUS : Blocks.WARPED_FUNGUS;
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack plant = new ItemStack(fungus);
            player.setItemInHand(InteractionHand.MAIN_HAND, plant);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()),
                Direction.UP, pos.below(), false);
            plant.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(fungus), "Small fungus must remain placeable");
            ItemStack meal = new ItemStack(Items.BONE_MEAL, 16);
            for (int attempt = 0; attempt < 16; attempt++) {
                BoneMealItem.applyBonemeal(meal, helper.getLevel(), pos, player);
            }
            helper.assertTrue(meal.getCount() == 16 && helper.getLevel().getBlockState(pos).is(fungus),
                "Bone meal must neither grow nor consume on Vanilla fungus");
            for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-8, 0, -8), pos.offset(8, 29, 8))) {
                helper.assertTrue(!isGiantFungusBlock(helper.getLevel().getBlockState(scan).getBlock()),
                    "Bone meal must not generate giant fungus blocks");
            }
            helper.getLevel().destroyBlock(pos, true);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).stream()
                .anyMatch(entity -> entity.getItem().is(fungus.asItem())), "Small fungus must drop itself");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void woodProcessing(GameTestHelper helper) {
        for (var reference : helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!reference.id().getNamespace().equals("tfc") || !reference.id().getPath().startsWith("crafting/wood/")
                || !reference.id().getPath().contains("/oak")) continue;
            for (OverdimsWood wood : OverdimsWood.values()) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID,
                    reference.id().getPath().replace("crafting/", "").replace("/oak", "/" + wood.getSerializedName()));
                var target = helper.getLevel().getRecipeManager().byKey(id);
                if (target.isEmpty()) continue; // Boats and metal hanging signs are outside Wood.BlockType.
                List<ItemStack> oakItems = new ArrayList<>();
                List<ItemStack> woodItems = new ArrayList<>();
                for (var ingredient : reference.value().getIngredients()) {
                    ItemStack oak = ItemStack.EMPTY;
                    if (!ingredient.isEmpty()) {
                        oak = ingredient.getItems()[0].copyWithCount(1);
                        for (ItemStack candidate : ingredient.getItems()) {
                            String itemId = BuiltInRegistries.ITEM.getKey(candidate.getItem()).toString();
                            if (itemId.contains("/oak") || itemId.equals("tfc:metal/saw/copper")) {
                                oak = candidate.copyWithCount(1);
                                break;
                            }
                        }
                    }
                    if (oak.isDamageableItem()) oak.setDamageValue(7);
                    oakItems.add(oak);
                    ResourceLocation oakId = BuiltInRegistries.ITEM.getKey(oak.getItem());
                    woodItems.add(oakId.getNamespace().equals("tfc") && oakId.getPath().contains("/oak")
                        ? new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID,
                            oakId.getPath().replace("/oak", "/" + wood.getSerializedName())))) : oak.copy());
                }
                int width = reference.value() instanceof ShapedRecipe shaped ? shaped.getWidth() : Math.min(3, oakItems.size());
                int height = reference.value() instanceof ShapedRecipe shaped ? shaped.getHeight() : (oakItems.size() + width - 1) / width;
                while (oakItems.size() < width * height) {
                    oakItems.add(ItemStack.EMPTY);
                    woodItems.add(ItemStack.EMPTY);
                }
                CraftingInput oakInput = CraftingInput.of(width, height, oakItems);
                CraftingInput woodInput = CraftingInput.of(width, height, woodItems);
                helper.assertTrue(reference.value().matches(oakInput, helper.getLevel()), "Reference input must craft " + reference.id());
                var crafted = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, woodInput, helper.getLevel());
                helper.assertTrue(crafted.isPresent() && crafted.get().id().equals(id), "Equivalent Nether ingredients must craft " + id);
                var expected = reference.value().assemble(oakInput, helper.getLevel().registryAccess());
                var result = crafted.orElseThrow().value().assemble(woodInput, helper.getLevel().registryAccess());
                helper.assertTrue(result.getCount() == expected.getCount()
                    && BuiltInRegistries.ITEM.getKey(result.getItem()).getPath().equals(
                        BuiltInRegistries.ITEM.getKey(expected.getItem()).getPath().replace("/oak", "/" + wood.getSerializedName())),
                    "Product and yield must match oak: " + id);
                var oakRemainder = reference.value().getRemainingItems(oakInput);
                var remainder = crafted.get().value().getRemainingItems(woodInput);
                for (int slot = 0; slot < woodInput.size(); slot++) {
                    helper.assertTrue(ItemStack.matches(remainder.get(slot), oakRemainder.get(slot)),
                        "Tool wear and consumed materials must match oak: " + id);
                    if (woodInput.getItem(slot).is(TagKey.create(Registries.ITEM, ResourceLocation.parse("c:tools/saw")))) {
                        helper.assertTrue(remainder.get(slot).getDamageValue() == 8, "Saw must lose one durability: " + id);
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate", timeoutTicks = 120)
    public static void woodFuel(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        FirepitBlockEntity firepit = new FirepitBlockEntity(pos, TFCBlocks.FIREPIT.get().defaultBlockState());
        LogPileBlockEntity pile = new LogPileBlockEntity(pos, TFCBlocks.LOG_PILE.get().defaultBlockState());
        for (OverdimsWood wood : OverdimsWood.values()) {
            for (Wood.BlockType type : Wood.BlockType.values()) {
                Block block = OverdimsWoodBlocks.WOODS.get(wood).get(type).get();
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    for (Direction face : Direction.values()) {
                        helper.assertTrue(!state.isFlammable(helper.getLevel(), pos, face)
                            && state.getFireSpreadSpeed(helper.getLevel(), pos, face) == 0,
                            "Every Nether wood state must resist fire: " + state);
                    }
                }
            }
        }
        for (var item : BuiltInRegistries.ITEM.entrySet()) {
            if (item.getKey().location().getNamespace().equals(TFCOverdims.MOD_ID)
                && item.getKey().location().getPath().startsWith("wood/")) {
                ItemStack stack = new ItemStack(item.getValue());
                helper.assertTrue(Fuel.get(stack) == null && !stack.is(ItemTags.LOGS_THAT_BURN)
                    && stack.getBurnTime(RecipeType.SMELTING) == 0
                    && !firepit.isItemValid(AbstractFirepitBlockEntity.SLOT_FUEL_INPUT, stack)
                    && !pile.isItemValid(0, stack), "Nether wood must be rejected by firepit and charcoal log pile: " + stack);
            }
        }
        for (OverdimsWood wood : OverdimsWood.values()) {
            for (Wood.BlockType type : List.of(Wood.BlockType.LOG, Wood.BlockType.PLANKS)) {
                BlockPos target = pos.offset(wood.ordinal() * 4, 0, type == Wood.BlockType.LOG ? 0 : 3);
                helper.getLevel().setBlock(target, wood.getBlock(type).get().defaultBlockState(), 3);
                helper.getLevel().setBlock(target.east().below(), Blocks.NETHERRACK.defaultBlockState(), 3);
                helper.getLevel().setBlock(target.east(), Blocks.FIRE.defaultBlockState(), 3);
            }
        }
        helper.runAfterDelay(80, () -> {
            for (OverdimsWood wood : OverdimsWood.values()) {
                for (Wood.BlockType type : List.of(Wood.BlockType.LOG, Wood.BlockType.PLANKS)) {
                    BlockPos target = pos.offset(wood.ordinal() * 4, 0, type == Wood.BlockType.LOG ? 0 : 3);
                    helper.assertTrue(helper.getLevel().getBlockState(target).is(wood.getBlock(type).get()), "Ignited wood must remain intact");
                }
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "climate")
    public static void vanillaWoodRecipes(GameTestHelper helper) {
        for (String wood : List.of("crimson", "warped")) {
            for (String suffix : List.of("button", "door", "fence", "fence_gate", "hanging_sign", "hyphae",
                "planks", "pressure_plate", "sign", "slab", "stairs", "trapdoor")) {
                helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.withDefaultNamespace(
                    wood + "_" + suffix)).isEmpty(), "Vanilla wood recipe must be disabled");
            }
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.withDefaultNamespace(
                "stripped_" + wood + "_hyphae")).isEmpty(), "Stripped hyphae recipe must be disabled");
            OverdimsWood type = wood.equals("crimson") ? OverdimsWood.CRIMSON : OverdimsWood.WARPED;
            for (Wood.BlockType blockType : List.of(Wood.BlockType.LOG, Wood.BlockType.WOOD,
                Wood.BlockType.PLANKS)) {
                var input = CraftingInput.of(1, 1, List.of(new ItemStack(
                    OverdimsWoodBlocks.WOODS.get(type).get(blockType).get())));
                var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input,
                    helper.getLevel());
                helper.assertTrue(recipe.isEmpty() || !recipe.orElseThrow().value().assemble(input,
                    helper.getLevel().registryAccess()).is(wood.equals("crimson") ? Blocks.CRIMSON_PLANKS.asItem()
                    : Blocks.WARPED_PLANKS.asItem()), "TFC wood must not convert directly to Vanilla planks");
            }
        }
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.withDefaultNamespace(
            "warped_fungus_on_a_stick")).isPresent(), "Non-wood recipe must remain");
        helper.succeed();
    }

    private static boolean isGiantFungusBlock(Block block) {
        return List.of(Blocks.CRIMSON_STEM, Blocks.WARPED_STEM, Blocks.STRIPPED_CRIMSON_STEM,
            Blocks.STRIPPED_WARPED_STEM, Blocks.CRIMSON_HYPHAE, Blocks.WARPED_HYPHAE,
            Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE,
            Blocks.NETHER_WART_BLOCK, Blocks.WARPED_WART_BLOCK, Blocks.SHROOMLIGHT).contains(block);
    }

    private static Path resource(String path) {
        return ModList.get().getModFileById(TFCOverdims.MOD_ID).getFile().findResource(path);
    }

    private static BlockPos prepareTreeSite(GameTestHelper helper, OverdimsWood wood) {
        BlockPos pos = helper.absolutePos(new BlockPos(9, 2, 9));
        for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-8, 0, -8), pos.offset(8, 29, 8))) {
            helper.getLevel().setBlock(scan, Blocks.AIR.defaultBlockState(), 3);
        }
        helper.getLevel().setBlock(pos.below(),
            (wood == OverdimsWood.CRIMSON ? Blocks.CRIMSON_NYLIUM : Blocks.WARPED_NYLIUM).defaultBlockState(), 3);
        return pos;
    }

    private static void assertSize(List<String> errors, ItemStack actual, String referenceId) {
        ItemStack reference = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(referenceId)));
        var size = ItemSizeManager.get(actual);
        var referenceSize = ItemSizeManager.get(reference);
        if (size.getSize(actual) != referenceSize.getSize(reference) || size.getWeight(actual) != referenceSize.getWeight(reference)) {
            errors.add(actual + " must match " + referenceId + ": " + referenceSize.getSize(reference) + "/" + referenceSize.getWeight(reference));
        }
        long definitions = ItemSizeManager.MANAGER.getValues().stream().filter(definition -> definition.ingredient().test(actual)).count();
        if (definitions != 1) errors.add(actual + " needs one size definition, found " + definitions);
    }
}
