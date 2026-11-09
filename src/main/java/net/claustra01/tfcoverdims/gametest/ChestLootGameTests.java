package net.claustra01.tfcoverdims.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.claustra01.tfcoverdims.TFCOverdims;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChestLootGameTests {
    private static final List<String> UTILITY = List.of("tfc:powder/flux", "minecraft:glass", "minecraft:glass_pane",
        "tfc:kaolin_clay", "tfc:powder/wood_ash", "tfc:powder/saltpeter", "tfc:powder/sylvite");

    @GameTest(template = "empty", batch = "climate")
    public static void netherChestLoot(GameTestHelper helper) throws Exception {
        for (String name : List.of("nether_bridge", "bastion_treasure", "bastion_other", "bastion_bridge", "bastion_hoglin_stable")) {
            checkChest(helper, name, List.of("wrought_iron", "steel"), List.of("wrought_iron"));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void portalAndEndChestLoot(GameTestHelper helper) throws Exception {
        checkChest(helper, "ruined_portal", List.of("copper", "bronze", "bismuth_bronze", "black_bronze", "wrought_iron"),
            List.of("copper", "bronze", "bismuth_bronze", "black_bronze"));
        checkChest(helper, "end_city_treasure", List.of("steel", "black_steel"), List.of("steel"));
        helper.succeed();
    }

    /** Compares the Vanilla roll ({@code getRandomItemsRaw} skips GLMs) with the GLM result of the same seed. */
    @SuppressWarnings("deprecation")
    private static void checkChest(GameTestHelper helper, String name, List<String> ingots, List<String> metals) throws Exception {
        helper.assertTrue(MobLootGameTests.lootJson(helper, "chests/" + name, false)
            .equals(MobLootGameTests.lootJson(helper, "chests/" + name, true)), "Vanilla loot table is not overridden: " + name);
        var level = helper.getLevel();
        var key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/" + name));
        var table = level.getServer().reloadableRegistries().getLootTable(key);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
        Set<String> expected = new HashSet<>();
        Set<String> seen = new HashSet<>();
        for (long seed = 1; seed <= 2000; seed++) {
            List<ItemStack> raw = new ArrayList<>();
            table.getRandomItemsRaw(new LootContext.Builder(params).withOptionalRandomSeed(seed).create(Optional.empty()),
                LootTable.createStackSplitter(level, raw::add));
            List<ItemStack> modified = table.getRandomItems(params, seed);
            int index = 0;
            for (ItemStack source : raw) {
                Set<String> allowed = allowedOutputs(id(source), ingots, metals);
                ItemStack first = modified.get(index);
                if (allowed.isEmpty()) {
                    helper.assertTrue(ItemStack.matches(source, first), "Unmapped Vanilla reward remains: " + id(source));
                    index++;
                    continue;
                }
                expected.addAll(allowed);
                seen.add(id(first));
                helper.assertTrue(allowed.contains(id(first)), id(source) + " must not become " + id(first));
                boolean unfinished = id(first).matches("tfc:metal/(.*_head|.*_blade|unfinished_.*)/.*");
                helper.assertTrue(first.getTagEnchantments().isEmpty()
                    || !source.getTagEnchantments().isEmpty() && !unfinished, "Only finished replacements of enchanted rewards are enchanted");
                int total = 0;
                while (total < source.getCount()) {
                    helper.assertTrue(modified.get(index).is(first.getItem()), "Split stacks keep one replacement item");
                    total += modified.get(index++).getCount();
                }
                helper.assertTrue(total == source.getCount(), "Replacement keeps the rolled count: " + id(source));
            }
            helper.assertTrue(index == modified.size(), "Replacement adds no stacks: " + name);
        }
        helper.assertTrue(seen.containsAll(expected), "Fixed seeds exercise every replacement in " + name + ": " + seen);
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.CHEST);
        var chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(pos));
        for (long seed : new long[] {17, 42, 256, 8191}) {
            chest.clearContent();
            chest.setLootTable(key, seed);
            chest.unpackLootTable(null);
            helper.assertTrue(!chest.isEmpty(), "Opening generated chest fills inventory: " + name);
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                String id = id(chest.getItem(slot));
                helper.assertTrue(!id.matches("minecraft:(iron|gold)_(ingot|nugget)|minecraft:(diamond|emerald|obsidian|crying_obsidian|quartz|chain)")
                    && equipmentShapes(id) == null, "Chest inventory contains replaced rewards: " + id);
            }
        }
    }

    private static Set<String> allowedOutputs(String source, List<String> ingots, List<String> metals) {
        Set<String> values = new HashSet<>();
        switch (source) {
            case "minecraft:diamond", "minecraft:emerald" -> List.of("amethyst", "diamond", "emerald", "lapis_lazuli", "opal",
                "pyrite", "ruby", "sapphire", "topaz").forEach(gem -> values.add("tfc:gem/" + gem));
            case "minecraft:iron_ingot", "minecraft:gold_ingot" -> ingots.forEach(metal -> values.add("tfc:metal/ingot/" + metal));
            case "minecraft:iron_nugget" -> List.of("hematite", "magnetite", "limonite").forEach(ore -> values.add("tfc:ore/small_" + ore));
            case "minecraft:gold_nugget" -> values.add("tfc:ore/small_native_gold");
            case "minecraft:chain" -> values.add("tfc:metal/chain/wrought_iron");
            case "minecraft:obsidian", "minecraft:crying_obsidian", "minecraft:quartz" -> values.addAll(UTILITY);
            default -> {
                List<String> shapes = equipmentShapes(source);
                if (shapes != null) {
                    shapes.forEach(shape -> metals.forEach(metal -> values.add("tfc:metal/" + shape + "/" + metal)));
                }
            }
        }
        return values;
    }

    private static List<String> equipmentShapes(String source) {
        if (source.matches("minecraft:(iron|golden|diamond)_horse_armor")) {
            return List.of("horse_armor");
        }
        if (!source.matches("minecraft:(golden|iron|diamond)_(sword|axe|hoe|pickaxe|shovel|helmet|chestplate|leggings|boots)")) {
            return null;
        }
        return switch (source.substring(source.lastIndexOf('_') + 1)) {
            case "sword" -> List.of("sword_blade", "javelin_head", "mace_head", "sword", "javelin", "mace");
            case "pickaxe" -> List.of("pickaxe_head", "propick_head", "pickaxe", "propick");
            case "axe" -> List.of("axe_head", "saw_blade", "axe", "saw");
            case "hoe" -> List.of("hoe_head", "scythe_blade", "hoe", "scythe");
            case "shovel" -> List.of("shovel_head", "shovel");
            case "leggings" -> List.of("unfinished_greaves", "greaves");
            default -> List.of("unfinished_" + source.substring(source.lastIndexOf('_') + 1), source.substring(source.lastIndexOf('_') + 1));
        };
    }

    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
