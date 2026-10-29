package net.claustra01.tfcoverdims.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.claustra01.tfcoverdims.TFCOverdims;
import net.claustra01.tfcoverdims.common.items.OverdimsItems;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AssetReferenceGameTests {
    @GameTest(template = "empty", batch = "climate")
    public static void creativeTab(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TFCOverdims.MOD_ID, "main");
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
        helper.assertTrue(tab != null && BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).equals(id),
            "The mod creative tab must be registered");
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
            FeatureFlags.DEFAULT_FLAGS, true, helper.getLevel().registryAccess()));
        Set<Item> expected = new HashSet<>();
        OverdimsItems.ITEMS.getEntries().forEach(holder -> expected.add(holder.get()));
        Set<Item> displayed = new HashSet<>();
        tab.getDisplayItems().forEach(stack -> displayed.add(stack.getItem()));
        helper.assertTrue(displayed.equals(expected) && tab.getDisplayItems().size() == expected.size(),
            "The creative tab must show each registered item exactly once");
        helper.assertTrue(tab.getIconItem().is(OverdimsItems.LOOSE_NETHERRACK.get()),
            "The creative tab must have the loose netherrack icon");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void assetReferences(GameTestHelper helper) throws IOException {
        Path root = ModList.get().getModFileById(TFCOverdims.MOD_ID).getFile().findResource("assets");
        Path modAssets = root.resolve(TFCOverdims.MOD_ID);
        try (var files = Files.walk(modAssets.resolve("models"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject model = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                checkModel(helper, model, root, new HashSet<>());
            }
        }
        try (var files = Files.walk(modAssets.resolve("blockstates"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                checkBlockstate(helper, JsonParser.parseString(Files.readString(file)), root);
            }
        }
        helper.succeed();
    }

    private static void checkModel(GameTestHelper helper, JsonElement element, Path root, Set<String> textures) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) checkModel(helper, child, root, textures);
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("parent")) checkReference(helper, root, object.get("parent").getAsString(), "models", ".json");
            if (object.has("textures")) {
                JsonObject declared = object.getAsJsonObject("textures");
                textures.addAll(declared.keySet());
                for (JsonElement value : declared.asMap().values()) {
                    String reference = value.getAsString();
                    if (!reference.startsWith("#")) checkReference(helper, root, reference, "textures", ".png");
                }
            }
            for (var entry : object.entrySet()) {
                if (entry.getKey().equals("parent") || entry.getKey().equals("textures")) continue;
                if (entry.getKey().equals("texture") && entry.getValue().isJsonPrimitive()) {
                    String reference = entry.getValue().getAsString();
                    if (reference.startsWith("#")) {
                        helper.assertTrue(textures.contains(reference.substring(1)) || object.has("parent"),
                            "Undefined texture variable: " + reference);
                    } else checkReference(helper, root, reference, "textures", ".png");
                } else checkModel(helper, entry.getValue(), root, textures);
            }
        }
    }

    private static void checkBlockstate(GameTestHelper helper, JsonElement element, Path root) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) checkBlockstate(helper, child, root);
        } else if (element.isJsonObject()) {
            for (var entry : element.getAsJsonObject().entrySet()) {
                if (entry.getKey().equals("model")) {
                    checkReference(helper, root, entry.getValue().getAsString(), "models", ".json");
                } else checkBlockstate(helper, entry.getValue(), root);
            }
        }
    }

    private static void checkReference(GameTestHelper helper, Path root, String value, String directory, String extension) {
        ResourceLocation id = ResourceLocation.parse(value);
        Path path = root.resolve(id.getNamespace()).resolve(directory).resolve(id.getPath() + extension);
        boolean present = Files.exists(path) || (id.getNamespace().equals("minecraft") || id.getNamespace().equals("tfc"))
            && AssetReferenceGameTests.class.getClassLoader().getResource(
                "assets/" + id.getNamespace() + "/" + directory + "/" + id.getPath() + extension) != null;
        helper.assertTrue(present, "Missing " + directory + " reference: " + value);
    }
}
