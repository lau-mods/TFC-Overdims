package net.claustra01.tfcoverdims.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.dries007.tfc.common.blocks.wood.Wood;
import net.dries007.tfc.common.component.size.ItemSizeManager;

import net.claustra01.tfcoverdims.TFCOverdims;
import net.claustra01.tfcoverdims.common.wood.OverdimsWood;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AssetSizeGameTests {
    @GameTest(template = "empty", batch = "climate")
    public static void allItemSizes(GameTestHelper helper) {
        Set<ResourceLocation> expected = new HashSet<>();
        for (String rock : List.of("loose_netherrack", "loose_blackstone", "loose_end_stone")) {
            compareSize(helper, expected, rock, "rock/loose/granite");
        }
        for (String brick : List.of("blackstone_brick", "end_stone_brick")) {
            compareSize(helper, expected, brick, "brick/granite");
        }
        for (String rock : List.of("netherrack", "end_stone")) {
            compareSize(helper, expected, "cobble_" + rock, "rock/cobble/granite");
            compareSize(helper, expected, "hardened_" + rock, "rock/hardened/granite");
        }
        for (OverdimsWood wood : OverdimsWood.values()) {
            String name = wood.getSerializedName();
            compareSize(helper, expected, "wood/lumber/" + name, "wood/lumber/oak");
            compareSize(helper, expected, "wood/support/" + name, "wood/support/oak");
            compareSize(helper, expected, "wood/sign/" + name, "wood/sign/oak");
            for (Wood.BlockType type : Wood.BlockType.values()) {
                if (type.needsItem()) {
                    compareSize(helper, expected, type.nameFor(wood), type.nameFor(Wood.OAK));
                }
            }
        }
        helper.succeed();
    }

    private static void compareSize(GameTestHelper helper, Set<ResourceLocation> expected,
        String path, String referencePath) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID, path);
        ResourceLocation referenceId = ResourceLocation.fromNamespaceAndPath("tfc", referencePath);
        expected.add(id);
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id), "Missing item: " + id);
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(referenceId), "Missing TFC reference: " + referenceId);
        ItemStack actual = new ItemStack(BuiltInRegistries.ITEM.get(id));
        ItemStack reference = new ItemStack(BuiltInRegistries.ITEM.get(referenceId));
        var size = ItemSizeManager.get(actual);
        var referenceSize = ItemSizeManager.get(reference);
        helper.assertTrue(size.getSize(actual) == referenceSize.getSize(reference)
            && size.getWeight(actual) == referenceSize.getWeight(reference),
            "Size and weight differ from " + referenceId + ": " + id);
        helper.assertTrue(ItemSizeManager.MANAGER.getValues().stream()
            .filter(definition -> definition.ingredient().test(actual)).count() == 1,
            "Item needs exactly one size definition: " + id);
    }

    @GameTest(template = "empty", batch = "climate")
    public static void allItemNames(GameTestHelper helper) throws IOException {
        var modFile = ModList.get().getModFileById(TFCOverdims.MOD_ID).getFile();
        var tfcFile = ModList.get().getModFileById("tfc").getFile();
        JsonObject english = JsonParser.parseString(Files.readString(modFile.findResource(
            "assets/tfcoverdims/lang/en_us.json"))).getAsJsonObject();
        JsonObject japanese = JsonParser.parseString(Files.readString(modFile.findResource(
            "assets/tfcoverdims/lang/ja_jp.json"))).getAsJsonObject();
        helper.assertTrue(english.keySet().equals(japanese.keySet()), "English and Japanese keys differ");
        for (JsonObject names : List.of(english, japanese)) {
            for (var entry : names.entrySet()) {
                helper.assertTrue(!entry.getValue().getAsString().isBlank(), "Blank translation: " + entry.getKey());
            }
            for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
                if (!id.getNamespace().equals(TFCOverdims.MOD_ID)) continue;
                String key = BuiltInRegistries.ITEM.get(id).getDescriptionId();
                helper.assertTrue(names.has(key), "Missing item name: " + key);
                helper.assertTrue(Files.exists(modFile.findResource(
                    "assets/tfcoverdims/models/item/" + id.getPath() + ".json")),
                    "Missing item model for " + id);
            }
            for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
                if (!id.getNamespace().equals(TFCOverdims.MOD_ID)) continue;
                String key = BuiltInRegistries.BLOCK.get(id).getDescriptionId();
                helper.assertTrue(names.has(key), "Missing block name: " + key);
            }
        }
        for (String language : List.of("en_us", "ja_jp")) {
            JsonObject names = language.equals("en_us") ? english : japanese;
            JsonObject tfcNames = JsonParser.parseString(Files.readString(tfcFile.findResource(
                "assets/tfc/lang/" + language + ".json"))).getAsJsonObject();
            for (String basalt : List.of("basalt", "polished_basalt", "smooth_basalt")) {
                helper.assertTrue(names.has("block.minecraft." + basalt), "Missing Vanilla basalt name: " + basalt);
                helper.assertTrue(!names.get("block.minecraft." + basalt).getAsString().equals(
                    tfcNames.get("block.tfc.rock.raw.basalt").getAsString()),
                    "Vanilla and TFC basalt names must differ in " + language);
            }
        }
        helper.succeed();
    }
}
