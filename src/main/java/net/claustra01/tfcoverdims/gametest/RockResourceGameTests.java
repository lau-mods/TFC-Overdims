package net.claustra01.tfcoverdims.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.claustra01.tfcoverdims.TFCOverdims;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RockResourceGameTests {
    @GameTest(template = "empty", batch = "climate")
    public static void rockResources(GameTestHelper helper) throws IOException {
        Path root = resource("tfcoverdims", "");
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".json")).toList()) {
                JsonElement json = JsonParser.parseString(Files.readString(path));
                helper.assertTrue(json.isJsonObject(), "Resources must contain JSON objects: " + root.relativize(path));
                String relative = root.relativize(path).toString().replace('\\', '/');
                if (relative.startsWith("assets/tfcoverdims/models/")) {
                    assertModel(helper, json.getAsJsonObject(), relative.startsWith("assets/tfcoverdims/models/block/loose_"));
                } else if (relative.startsWith("assets/tfcoverdims/blockstates/")) {
                    assertBlockstateModels(helper, json);
                } else if (relative.contains("/tags/item/") || relative.contains("/tags/block/")) {
                    assertTag(helper, json.getAsJsonObject(), relative.contains("/tags/block/"));
                }
            }
        }
        for (String language : List.of("en_us", "ja_jp")) {
            JsonObject names = JsonParser.parseString(Files.readString(resource("tfcoverdims", "assets/tfcoverdims/lang/" + language + ".json"))).getAsJsonObject();
            for (String rock : List.of("netherrack", "blackstone", "end_stone")) {
                helper.assertTrue(names.has("block.tfcoverdims.loose_" + rock), "Loose rock must have a localized name");
                for (int count = 1; count <= 4; count++) {
                    assertModel(helper, JsonParser.parseString(Files.readString(resource("tfcoverdims",
                        "assets/tfcoverdims/models/block/loose_" + rock + "_" + count + ".json"))).getAsJsonObject(), true);
                }
            }
            for (String brick : List.of("blackstone_brick", "end_stone_brick")) {
                helper.assertTrue(names.has("item.tfcoverdims." + brick), "Bricks must have a localized name");
            }
        }
        helper.succeed();
    }

    private static void assertModel(GameTestHelper helper, JsonObject model, boolean looseRock) {
        if (model.has("parent")) {
            String parent = model.get("parent").getAsString();
            helper.assertTrue(ResourceLocation.parse(parent).getNamespace().equals("minecraft") || Files.exists(asset(parent, "models", ".json")), "Model parent must resolve: " + parent);
        }
        if (model.has("textures")) {
            for (JsonElement value : model.getAsJsonObject("textures").asMap().values()) {
                String texture = value.getAsString();
                helper.assertTrue(texture.startsWith("#") || ResourceLocation.parse(texture).getNamespace().equals("minecraft") || Files.exists(asset(texture, "textures", ".png")), "Texture must resolve: " + texture);
            }
        }
        for (JsonElement child : model.asMap().values()) {
            if (child.isJsonObject()) {
                assertModel(helper, child.getAsJsonObject(), false);
            } else if (child.isJsonArray()) {
                for (JsonElement entry : child.getAsJsonArray()) {
                    if (entry.isJsonObject()) assertModel(helper, entry.getAsJsonObject(), false);
                }
            }
        }
        if (looseRock && model.has("elements")) {
            for (JsonElement value : model.getAsJsonArray("elements")) {
                JsonObject element = value.getAsJsonObject();
                for (String bound : List.of("from", "to")) {
                    for (JsonElement coordinate : element.getAsJsonArray(bound)) {
                        helper.assertTrue(coordinate.getAsDouble() >= 0 && coordinate.getAsDouble() <= 16, "Loose geometry must fit within its block");
                    }
                }
                for (JsonElement face : element.getAsJsonObject("faces").asMap().values()) {
                    for (JsonElement uv : face.getAsJsonObject().getAsJsonArray("uv")) {
                        helper.assertTrue(uv.getAsDouble() >= 0 && uv.getAsDouble() <= 16, "Loose rock UVs must stay inside the original texture");
                    }
                }
            }
        }
    }

    private static void assertBlockstateModels(GameTestHelper helper, JsonElement element) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                assertBlockstateModels(helper, child);
            }
        } else if (element.isJsonObject()) {
            for (var entry : element.getAsJsonObject().entrySet()) {
                if (entry.getKey().equals("model")) {
                    String model = entry.getValue().getAsString();
                    helper.assertTrue(ResourceLocation.parse(model).getNamespace().equals("minecraft") || Files.exists(asset(model, "models", ".json")),
                        "Blockstate model must resolve: " + model);
                } else {
                    assertBlockstateModels(helper, entry.getValue());
                }
            }
        }
    }

    private static void assertTag(GameTestHelper helper, JsonObject tag, boolean block) {
        for (JsonElement value : tag.getAsJsonArray("values")) {
            String id = value.getAsString();
            ResourceLocation key = ResourceLocation.parse(id);
            helper.assertTrue(block ? BuiltInRegistries.BLOCK.containsKey(key) : BuiltInRegistries.ITEM.containsKey(key),
                "Tag entry must reference a registered object: " + id);
        }
    }

    private static Path asset(String id, String directory, String extension) {
        ResourceLocation location = ResourceLocation.parse(id);
        return resource(location.getNamespace(), "assets/" + location.getNamespace() + "/" + directory + "/" + location.getPath() + extension);
    }

    private static Path resource(String mod, String path) {
        return ModList.get().getModFileById(mod).getFile().findResource(path);
    }
}
