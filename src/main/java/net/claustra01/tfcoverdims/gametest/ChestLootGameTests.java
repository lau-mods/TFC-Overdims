package net.claustra01.tfcoverdims.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.claustra01.tfcoverdims.TFCOverdims;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChestLootGameTests {
    @GameTest(template = "empty", batch = "climate")
    public static void netherChestLoot(GameTestHelper helper) throws Exception {
        for (String name : List.of("nether_bridge", "bastion_treasure", "bastion_other", "bastion_bridge", "bastion_hoglin_stable")) {
            checkChest(helper, name);
        }
        var fortress = MobLootGameTests.lootJson(helper, "chests/nether_bridge", false);
        for (String item : List.of("tfc:gem/diamond", "tfc:metal/ingot/wrought_iron", "tfc:metal/ingot/gold",
            "tfc:metal/sword/wrought_iron", "tfc:metal/chestplate/wrought_iron")) {
            assertWeight(helper, fortress, item, 5);
        }
        assertWeight(helper, fortress, "minecraft:saddle", 10);
        assertWeight(helper, fortress, "tfc:metal/horse_armor/bismuth_bronze", 8);
        var treasure = MobLootGameTests.lootJson(helper, "chests/bastion_treasure", false);
        for (String piece : List.of("helmet", "chestplate", "greaves", "boots")) {
            assertWeight(helper, treasure, "tfc:metal/" + piece + "/black_steel", 6);
        }
        assertWeight(helper, treasure, "tfc:metal/sword/black_steel", 7);
        for (String gem : List.of("diamond", "opal", "ruby")) assertWeight(helper, treasure, "tfc:gem/" + gem, 10);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void portalAndEndChestLoot(GameTestHelper helper) throws Exception {
        checkChest(helper, "ruined_portal");
        checkChest(helper, "end_city_treasure");
        helper.succeed();
    }

    private static void checkChest(GameTestHelper helper, String name) throws Exception {
        var original = MobLootGameTests.lootJson(helper, "chests/" + name, true);
        var actual = MobLootGameTests.lootJson(helper, "chests/" + name, false);
        Set<String> overrideItems = MobLootGameTests.itemNames(actual);
        for (String id : overrideItems) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)), "Registered chest reward: " + id);
            helper.assertTrue(id.startsWith("minecraft:") || id.startsWith("tfc:"), "No independent progression rewards: " + id);
            helper.assertTrue(!id.matches("minecraft:(golden|iron|diamond)_(sword|axe|hoe|pickaxe|shovel|helmet|chestplate|leggings|boots|horse_armor)")
                && !List.of("minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:diamond", "minecraft:emerald",
                    "minecraft:porkchop", "minecraft:cooked_porkchop", "minecraft:golden_apple", "minecraft:enchanted_golden_apple",
                    "minecraft:golden_carrot").contains(id), "No Vanilla equipment, ingot or food bypass: " + id);
        }
        var oldPools = original.getAsJsonArray("pools");
        var pools = actual.getAsJsonArray("pools");
        helper.assertTrue(pools.size() == oldPools.size(), "All Vanilla reward pools remain: " + name);
        for (int i = 0; i < pools.size(); i++) {
            var pool = pools.get(i).getAsJsonObject();
            var oldPool = oldPools.get(i).getAsJsonObject();
            helper.assertTrue(pool.get("rolls").equals(oldPool.get("rolls")), "Roll counts remain correct: " + name);
            for (var entry : oldPool.getAsJsonArray("entries")) {
                var object = entry.getAsJsonObject();
                String id = object.has("name") ? object.get("name").getAsString() : "";
                // Retained Vanilla entries include all Netherite, template and dimension rewards.
                if (id.isEmpty() || overrideItems.contains(id)) {
                    helper.assertTrue(pool.getAsJsonArray("entries").contains(entry), "Original entry, weight and functions remain: " + id);
                }
            }
        }
        var level = helper.getLevel();
        var key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/" + name));
        var table = level.getServer().reloadableRegistries().getLootTable(key);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
        Set<String> outputs = MobLootGameTests.rewardNames(actual);
        Set<String> seen = new HashSet<>();
        for (long seed = 1; seed <= 1024; seed++) {
            for (var stack : table.getRandomItems(params, seed * 0x9E3779B97F4A7C15L)) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                helper.assertTrue(outputs.contains(id), "Loaded table must only yield declared rewards: " + id);
                seen.add(id);
            }
        }
        helper.assertTrue(seen.containsAll(outputs), "Fixed seeds exercise every candidate in " + name + ": " + seen);
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.CHEST);
        var chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(pos));
        for (long seed : new long[] {17, 42, 256, 8191}) {
            chest.clearContent();
            chest.setLootTable(key, seed);
            chest.unpackLootTable(null);
            helper.assertTrue(!chest.isEmpty(), "Opening generated chest fills inventory: " + name);
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                var stack = chest.getItem(slot);
                helper.assertTrue(stack.isEmpty() || outputs.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()),
                    "Chest inventory contains compatible rewards");
            }
        }
    }

    private static void assertWeight(GameTestHelper helper, JsonObject table, String id, int weight) {
        int matches = 0;
        for (var entry : table.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")) {
            var object = entry.getAsJsonObject();
            if (object.has("name") && object.get("name").getAsString().equals(id)) {
                matches++;
                helper.assertTrue(object.get("weight").getAsInt() == weight, "Reference weight for " + id);
            }
        }
        helper.assertTrue(matches == 1, "Exactly one weighted candidate for " + id);
    }
}
