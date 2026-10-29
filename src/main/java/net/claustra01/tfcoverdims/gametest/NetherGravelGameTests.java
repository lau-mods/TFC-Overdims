package net.claustra01.tfcoverdims.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.claustra01.tfcoverdims.TFCOverdims;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetherGravelGameTests {
    private static final Block BASALT_GRAVEL = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:rock/gravel/basalt"));

    @GameTest(template = "empty", batch = "climate", timeoutTicks = 200)
    public static void netherGravelOre(GameTestHelper helper) throws IOException {
        ResourceLocation id = ResourceLocation.withDefaultNamespace("ore_gravel_nether");
        var registries = helper.getLevel().registryAccess();
        ConfiguredFeature<?, ?> configured = registries.registryOrThrow(Registries.CONFIGURED_FEATURE).get(id);
        PlacedFeature placed = registries.registryOrThrow(Registries.PLACED_FEATURE).get(id);
        helper.assertTrue(configured != null && placed != null && placed.feature().value() == configured,
            "Vanilla placed feature must still use the overridden configured feature");
        assertGravelReplacement(helper, "worldgen/configured_feature/ore_gravel_nether.json", 1);
        assertUnchanged(helper, "worldgen/placed_feature/ore_gravel_nether.json");
        helper.assertTrue(BASALT_GRAVEL != Blocks.AIR, "TFC basalt gravel must exist");

        ConfiguredFeature<?, ?> vanilla = ConfiguredFeature.DIRECT_CODEC.parse(
            RegistryOps.create(JsonOps.INSTANCE, registries), vanillaJson(helper, "worldgen/configured_feature/ore_gravel_nether.json"))
            .getOrThrow();
        // The registered feature remains in the biome; the baseline omits only its final, random-free biome filter.
        PlacedFeature baseline = new PlacedFeature(Holder.direct(vanilla), placed.placement().stream()
            .filter(modifier -> !(modifier instanceof BiomeFilter)).toList());
        var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        var generator = nether.getChunkSource().getGenerator();
        int cx = 710;
        int cz = -710;
        while (cx < 750) {
            boolean eligible = true;
            for (int x = 0; x < 4 && eligible; x++) {
                for (int z = 0; z < 4 && eligible; z++) {
                    for (int y = 1; y <= 10; y++) {
                        if (!generator.getBiomeGenerationSettings(generator.getBiomeSource().getNoiseBiome(
                            cx * 4 + x, y, cz * 4 + z, nether.getChunkSource().randomState().sampler()))
                            .hasFeature(placed)) {
                            eligible = false;
                            break;
                        }
                    }
                }
            }
            if (eligible) break;
            cx++;
        }
        helper.assertTrue(cx < 750, "A fixed Nether sample must allow the Vanilla gravel feature");
        helper.assertTrue(!nether.getChunkSource().hasChunk(cx, cz), "The ore sample must use new Nether chunks");
        nether.getChunk(cx, cz);
        BlockPos origin = new BlockPos(cx * 16, 0, cz * 16);
        Set<BlockPos> existing = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-16, 0, -16), origin.offset(31, 55, 31))) {
            if (nether.getBlockState(pos).is(BASALT_GRAVEL)) existing.add(pos.immutable());
        }
        Set<BlockPos> expected = new HashSet<>();
        long seed = 18;
        for (; seed < 26 && expected.isEmpty(); seed++) {
            baseline.place(nether, generator, RandomSource.create(seed), origin);
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-16, 0, -16), origin.offset(31, 55, 31))) {
                if (nether.getBlockState(pos).is(Blocks.GRAVEL)) {
                    expected.add(pos.immutable());
                    nether.setBlock(pos, Blocks.NETHERRACK.defaultBlockState(), 2);
                }
            }
        }
        helper.assertTrue(!expected.isEmpty(), "Vanilla placement must produce a gravel vein in the fixed-seed sample");
        placed.placeWithBiomeCheck(nether, generator, RandomSource.create(seed - 1), origin);
        Set<BlockPos> actual = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-16, 0, -16), origin.offset(31, 55, 31))) {
            var state = nether.getBlockState(pos);
            helper.assertTrue(!state.is(Blocks.GRAVEL), "The placed feature must not produce vanilla gravel");
            if (state.is(BASALT_GRAVEL) && !existing.contains(pos)) actual.add(pos.immutable());
        }
        helper.assertTrue(actual.equals(expected), "The placed feature must retain Vanilla's positions and vein size");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate", timeoutTicks = 200)
    public static void netherGravelSurface(GameTestHelper helper) throws IOException {
        assertGravelReplacement(helper, "worldgen/noise_settings/nether.json", 4);
        assertUnchanged(helper, "worldgen/noise_settings/overworld.json");
        assertUnchanged(helper, "worldgen/configured_feature/ore_gravel.json");
        assertUnchanged(helper, "worldgen/placed_feature/ore_gravel.json");
        assertUnchanged(helper, "worldgen/configured_feature/basalt_blobs.json");
        assertUnchanged(helper, "worldgen/placed_feature/basalt_blobs.json");
        assertUnchanged(helper, "worldgen/configured_feature/basalt_pillar.json");
        assertUnchanged(helper, "worldgen/placed_feature/basalt_pillar.json");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.NOISE_SETTINGS)
            .containsKey(ResourceLocation.withDefaultNamespace("nether")),
            "The overridden Nether noise settings must load");
        var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        int surfaceGravel = 0;
        Map<String, Integer> sampled = new HashMap<>();
        Set<String> biomes = Set.of("nether_wastes", "soul_sand_valley", "basalt_deltas");
        var generator = nether.getChunkSource().getGenerator();
        for (int cx = 600; cx < 640 && (sampled.size() < biomes.size() || surfaceGravel == 0); cx++) {
            for (int cz = -640; cz < -600; cz++) {
                String biome = generator.getBiomeSource().getNoiseBiome(cx * 4 + 2, 8, cz * 4 + 2,
                    nether.getChunkSource().randomState().sampler()).unwrapKey().orElseThrow().location().getPath();
                if (!biomes.contains(biome) || sampled.getOrDefault(biome, 0) >= 8) continue;
                helper.assertTrue(!nether.getChunkSource().hasChunk(cx, cz), "The surface sample must use new Nether chunks");
                // SURFACE is before ore placement, so every gravel block here comes from the Surface Rule.
                var chunk = nether.getChunkSource().getChunk(cx, cz, ChunkStatus.SURFACE, true);
                sampled.merge(biome, 1, Integer::sum);
                for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
                    for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                        for (int y = 0; y < 128; y++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            var state = chunk.getBlockState(pos);
                            helper.assertTrue(!state.is(Blocks.GRAVEL), "Fresh Nether terrain must not contain vanilla gravel");
                            if (y >= 30 && y <= 35 && state.is(BASALT_GRAVEL)
                                && chunk.getBlockState(pos.above()).isAir()) surfaceGravel++;
                        }
                    }
                }
            }
        }
        helper.assertTrue(sampled.keySet().equals(biomes), "All three Nether surface biomes must be sampled");
        helper.assertTrue(surfaceGravel > 0, "Fresh Nether terrain must contain exposed basalt gravel at lava-shore height");
        helper.succeed();
    }

    private static void assertGravelReplacement(GameTestHelper helper, String path, int count) throws IOException {
        JsonElement original = vanillaJson(helper, path);
        JsonElement modified = activeJson(helper, path);
        String vanillaState = "\"Name\":\"minecraft:gravel\"";
        String basaltState = "\"Name\":\"tfc:rock/gravel/basalt\"";
        String json = original.toString();
        helper.assertTrue(json.split(vanillaState, -1).length - 1 == count,
            "Unexpected number of Vanilla gravel states in " + path);
        helper.assertTrue(modified.equals(JsonParser.parseString(json.replace(vanillaState, basaltState))),
            "Only gravel block states may differ from Vanilla in " + path);
    }

    private static void assertUnchanged(GameTestHelper helper, String path) throws IOException {
        helper.assertTrue(activeJson(helper, path).equals(vanillaJson(helper, path)),
            "Vanilla world generation must remain unchanged in " + path);
    }

    private static JsonElement vanillaJson(GameTestHelper helper, String path) throws IOException {
        var resources = helper.getLevel().getServer().getResourceManager()
            .getResourceStack(ResourceLocation.withDefaultNamespace(path));
        var vanilla = resources.stream().filter(resource -> resource.sourcePackId().equals("vanilla"))
            .findFirst().orElseThrow();
        try (var reader = vanilla.openAsReader()) {
            return JsonParser.parseReader(reader);
        }
    }

    private static JsonElement activeJson(GameTestHelper helper, String path) throws IOException {
        var active = helper.getLevel().getServer().getResourceManager()
            .getResourceOrThrow(ResourceLocation.withDefaultNamespace(path));
        try (var reader = active.openAsReader()) {
            return JsonParser.parseReader(reader);
        }
    }
}
