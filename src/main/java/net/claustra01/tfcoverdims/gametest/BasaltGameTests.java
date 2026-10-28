package net.claustra01.tfcoverdims.gametest;

import java.util.List;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.recipes.BlockRecipe;
import net.dries007.tfc.common.recipes.CollapseRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.world.settings.RockSettings;

import net.claustra01.tfcoverdims.TFCOverdims;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BasaltGameTests {
    @GameTest(template = "empty", batch = "climate")
    public static void basaltIdentities(GameTestHelper helper) {
        List<Block> blocks = List.of(Blocks.BASALT, Blocks.POLISHED_BASALT, Blocks.SMOOTH_BASALT);
        List<String> ids = List.of("basalt", "polished_basalt", "smooth_basalt");
        List<String> names = List.of("Nether Basalt", "Polished Nether Basalt", "Smooth Nether Basalt");
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            ResourceLocation id = ResourceLocation.withDefaultNamespace(ids.get(i));
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(block).equals(id)
                && BuiltInRegistries.ITEM.getKey(block.asItem()).equals(id), "Vanilla block and item IDs must remain intact");
            helper.assertTrue(block.getDescriptionId().equals("block.minecraft." + ids.get(i))
                && new ItemStack(block).getDescriptionId().equals(block.getDescriptionId()), "Item and block must use the original translation key");
            helper.assertTrue(Language.getInstance().getOrDefault(block.getDescriptionId()).equals(names.get(i))
                && new ItemStack(block).getHoverName().getString().equals(names.get(i)),
                "Minecraft must resolve the specified English block and item names");
        }
        Block tfcBasalt = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:rock/raw/basalt"));
        helper.assertTrue(Language.getInstance().getOrDefault(tfcBasalt.getDescriptionId()).equals("Raw Basalt")
            && new ItemStack(tfcBasalt).getHoverName().getString().equals("Raw Basalt"),
            "TFC basalt must retain its own English name");
        helper.succeed();
    }

    @GameTest(template = "collapse", batch = "climate")
    public static void basaltGeology(GameTestHelper helper) {
        var level = helper.getLevel();
        Block raw = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:rock/raw/basalt"));
        Block loose = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:rock/loose/basalt"));
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.BASALT);
        helper.setBlock(new BlockPos(5, 3, 2), raw);
        helper.assertTrue(raw.defaultBlockState().is(TFCTags.Blocks.STONES_RAW)
            && raw.defaultBlockState().is(TFCTags.Blocks.BREAKS_WHEN_ISOLATED), "TFC basalt must retain its geological and mining tags");
        var rocks = level.registryAccess().registryOrThrow(RockSettings.KEY);
        var basalt = rocks.get(ResourceLocation.parse("tfc:basalt"));
        helper.assertTrue(basalt != null && basalt.raw() == raw && basalt.loose().orElseThrow() == loose,
            "TFC basalt must retain its existing raw rock and loose rock mapping");
        for (Block vanilla : List.of(Blocks.BASALT, Blocks.POLISHED_BASALT, Blocks.SMOOTH_BASALT)) {
            var state = vanilla.defaultBlockState();
            ItemStack stack = new ItemStack(vanilla);
            helper.assertTrue(!stack.is(TFCTags.Items.ROCK_KNAPPING) && stack.getTags().noneMatch(tag ->
                tag.location().getNamespace().equals("tfc") && tag.location().getPath().startsWith("stones/")),
                "Vanilla basalt items must remain outside TFC rock and loose rock tags");
            helper.assertTrue(level.getRecipeManager().getAllRecipesFor(TFCRecipeTypes.KNAPPING.get()).stream()
                .noneMatch(holder -> holder.value().knappingType().get().inputItem().ingredient().test(stack)
                    && holder.value().matchesItem(stack)),
                "No TFC knapping recipe may accept Vanilla basalt items");
            helper.assertTrue(!state.is(TFCTags.Blocks.STONES_RAW) && !state.is(TFCTags.Blocks.STONES_HARDENED)
                && !state.is(TFCTags.Blocks.BREAKS_WHEN_ISOLATED) && CollapseRecipe.getRecipe(state) == null,
                "Vanilla basalt must remain outside TFC geology and collapse");
            helper.assertTrue(rocks.stream().noneMatch(rock -> rock.raw() == vanilla || rock.hardened() == vanilla
                || rock.cobble() == vanilla || rock.loose().orElse(null) == vanilla), "Vanilla basalt must not be a registered TFC rock");
            helper.assertTrue(!BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("tfcoverdims:loose_" + BuiltInRegistries.BLOCK.getKey(vanilla).getPath())),
                "Vanilla basalt must not acquire a dedicated loose rock");
            for (var holder : level.getRecipeManager().getRecipes()) {
                var recipe = holder.value();
                ItemStack result = recipe.getResultItem(level.registryAccess());
                ResourceLocation output = BuiltInRegistries.ITEM.getKey(result.getItem());
                if (output.getNamespace().equals("tfc") && output.getPath().endsWith("/basalt")) {
                    helper.assertTrue(recipe.getIngredients().stream().noneMatch(ingredient -> ingredient.test(new ItemStack(vanilla))),
                        "Vanilla basalt must not feed TFC basalt conversion recipes: " + holder.id());
                }
                if (recipe instanceof BlockRecipe blockRecipe && holder.id().getNamespace().equals("tfc")) {
                    helper.assertTrue(!blockRecipe.matches(state), "Vanilla basalt must not match TFC geological block recipes: " + holder.id());
                }
            }
        }
        var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        boolean basaltHost = false;
        for (var entry : level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).entrySet()) {
            if (entry.getKey().location().getNamespace().equals("tfc") && entry.getKey().location().getPath().startsWith("vein/")) {
                String json = ConfiguredFeature.DIRECT_CODEC.encodeStart(ops, entry.getValue()).getOrThrow().toString();
                helper.assertTrue(!json.contains("\"minecraft:basalt\""), "Vanilla basalt must not become a TFC ore host");
                basaltHost |= json.contains("tfc:rock/raw/basalt");
            }
        }
        helper.assertTrue(basaltHost, "Existing TFC veins must still use TFC basalt as an ore host");
        BlockPos featurePos = helper.absolutePos(new BlockPos(8, 4, 8));
        level.setBlockAndUpdate(featurePos.below(), Blocks.BASALT.defaultBlockState());
        for (String rock : List.of("netherrack", "blackstone", "end_stone")) {
            var feature = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .get(ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID, "loose_" + rock));
            for (int seed = 0; seed < 16; seed++) {
                feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), featurePos);
                helper.assertTrue(level.getBlockState(featurePos).isAir(), "M02 loose rocks must not generate on Vanilla basalt");
            }
        }
        assertMining(helper, Blocks.BASALT, Blocks.BASALT, 1, 1);
        assertMining(helper, raw, loose, 1, 4);
        helper.succeed();
    }

    private static void assertMining(GameTestHelper helper, Block block, Block drop, int min, int max) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 4, 12));
        var player = FakePlayerFactory.getMinecraft(level);
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        level.setBlockAndUpdate(pos.east(), block.defaultBlockState());
        for (int seed = 0; seed < 16; seed++) {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            level.random.setSeed(seed);
            helper.assertTrue(player.gameMode.destroyBlock(pos), "Survival mining must break the basalt block");
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
            int count = drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
            helper.assertTrue(!drops.isEmpty() && drops.stream().allMatch(entity -> entity.getItem().is(drop.asItem()))
                && count >= min && count <= max, "Basalt must retain its original mining drops");
            drops.forEach(ItemEntity::discard);
        }
        level.removeBlock(pos.east(), false);
    }
}
