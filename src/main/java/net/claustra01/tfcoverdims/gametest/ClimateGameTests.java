package net.claustra01.tfcoverdims.gametest;

import java.io.File;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.BiomeBasedClimateModel;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateModel;
import net.dries007.tfc.util.climate.ClimateModelType;
import net.dries007.tfc.util.climate.ClimateModels;
import net.dries007.tfc.util.events.SelectClimateModelEvent;

import net.claustra01.tfcoverdims.TFCOverdims;
import net.claustra01.tfcoverdims.climate.EndClimateModel;
import net.claustra01.tfcoverdims.climate.NetherClimateModel;
import net.claustra01.tfcoverdims.climate.OverdimsClimateModels;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ClimateGameTests {
    @BeforeBatch(batch = "climate")
    public static void configureReport(ServerLevel level) throws ParserConfigurationException {
        String report = System.getProperty("tfcoverdims.gametest.report");
        if (report != null) {
            GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new File(report)));
        }
    }

    @GameTest(template = "empty", batch = "climate")
    public static void netherClimate(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(Climate.get(nether) == NetherClimateModel.INSTANCE, "Nether must select its climate on level load");
        assertCodec(helper, NetherClimateModel.INSTANCE, OverdimsClimateModels.NETHER.get(), "nether");

        List<ResourceKey<Biome>> biomes = List.of(Biomes.NETHER_WASTES, Biomes.BASALT_DELTAS,
            Biomes.CRIMSON_FOREST, Biomes.WARPED_FOREST, Biomes.SOUL_SAND_VALLEY);
        float[] temperatures = {70f, 80f, 60f, 60f, 55f};
        for (int i = 0; i < biomes.size(); i++) {
            prepareBiome(helper, nether, new BlockPos(8 + 32 * i, 80, 8), biomes.get(i));
        }

        for (int day = 0; day < 3; day++) {
            for (int i = 0; i < biomes.size(); i++) {
                assertClimate(helper, nether, new BlockPos(8 + 32 * i, 80, 8), temperatures[i]);
            }
            assertClimate(helper, nether, new BlockPos(8, 32, 8), 75f);
            assertClimate(helper, nether, new BlockPos(8, 128, 8), 65f);
            assertClimate(helper, nether, new BlockPos(8, 0, 8), 75f);
            assertClimate(helper, nether, new BlockPos(8, 255, 8), 65f);
            if (day < 2) {
                advanceCalendar(helper);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void endClimate(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(Climate.get(end) == EndClimateModel.INSTANCE, "End must select its climate on level load");
        assertCodec(helper, EndClimateModel.INSTANCE, OverdimsClimateModels.END.get(), "end");

        List<ResourceKey<Biome>> biomes = List.of(Biomes.THE_END, Biomes.END_HIGHLANDS,
            Biomes.END_MIDLANDS, Biomes.SMALL_END_ISLANDS, Biomes.END_BARRENS);
        for (int i = 0; i < biomes.size(); i++) {
            prepareBiome(helper, end, new BlockPos(8 + 32 * i, 80, 8), biomes.get(i));
        }

        for (int day = 0; day < 3; day++) {
            for (int i = 0; i < biomes.size(); i++) {
                for (int y : new int[] {0, 80, 255}) {
                    assertClimate(helper, end, new BlockPos(8 + 32 * i, y, 8), 20f);
                }
            }
            if (day < 2) {
                advanceCalendar(helper);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void overworldUnchanged(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ClimateModel original = Climate.get(overworld);
        helper.assertTrue(original == BiomeBasedClimateModel.INSTANCE,
            "Flat GameTest worlds must keep TFC's biome-based climate");
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        helper.assertTrue(Climate.getAverageTemperature(overworld, pos)
            == BiomeBasedClimateModel.INSTANCE.getAverageTemperature(overworld, pos), "Overworld temperature must stay with TFC");
        helper.assertTrue(Climate.getAverageRainfall(overworld, pos)
            == BiomeBasedClimateModel.INSTANCE.getAverageRainfall(overworld, pos), "Overworld rainfall must stay with TFC");

        SelectClimateModelEvent event = new SelectClimateModelEvent(overworld);
        event.setModel(original);
        OverdimsClimateModels.selectClimateModel(event);
        helper.assertTrue(event.getModel() == original, "Overworld model selection must leave the current model untouched");
        Climate.chooseModelForWorld(overworld);
        helper.assertTrue(Climate.get(overworld) == original, "TFC must keep selecting the same Overworld climate");
        helper.succeed();
    }

    private static void prepareBiome(GameTestHelper helper, ServerLevel level, BlockPos pos, ResourceKey<Biome> biome) {
        var holder = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biome);
        var chunk = level.getChunkAt(pos);
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> holder, level.getChunkSource().randomState().sampler());
        helper.assertTrue(level.getBiome(pos).is(biome), "The inspection point must have biome " + biome.location());
    }

    private static void assertClimate(GameTestHelper helper, ServerLevel level, BlockPos pos, float temperature) {
        String point = level.dimension().location() + " at " + pos.toShortString();
        helper.assertTrue(Climate.getAverageTemperature(level, pos) == temperature, point + ": wrong average temperature");
        helper.assertTrue(Climate.getInstantTemperature(level, pos) == temperature, point + ": wrong instant temperature");
        helper.assertTrue(Climate.getAverageRainfall(level, pos) == 0f, point + ": average rainfall must be zero");
        helper.assertTrue(Climate.get(level).getInstantRainfall(level, pos) == 0f, point + ": instant rainfall must be zero");
        helper.assertTrue(Climate.getRainfallVariance(level, pos) == 0f, point + ": rainfall must have no seasonal variance");
    }

    private static void advanceCalendar(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (ServerLevel level : server.getAllLevels()) {
            helper.assertTrue(Calendars.get(level) == Calendars.SERVER, "Every dimension must share TFC's calendar");
        }
        long before = Calendars.SERVER.getCalendarTicks();
        Calendars.SERVER.skipForwardBy(40L * ICalendar.CALENDAR_TICKS_IN_DAY + 6L * ICalendar.CALENDAR_TICKS_IN_HOUR);
        helper.assertTrue(Calendars.SERVER.getCalendarTicks() >= before + 40L * ICalendar.CALENDAR_TICKS_IN_DAY,
            "The TFC calendar must advance by multiple days and seasons");
    }

    private static <T extends ClimateModel> void assertCodec(GameTestHelper helper, T model, ClimateModelType<T> type, String id) {
        helper.assertTrue(model.type() == type, "Climate model must expose its registered type");
        helper.assertTrue(ClimateModels.REGISTRY.getKey(type).toString().equals(TFCOverdims.MOD_ID + ":" + id),
            "Climate type must use the mod namespace");
        var buffer = Unpooled.buffer();
        try {
            type.codec().encode(buffer, model);
            helper.assertTrue(type.codec().decode(buffer) == model, "Climate model must round-trip through TFC's network codec");
        } finally {
            buffer.release();
        }
    }
}
