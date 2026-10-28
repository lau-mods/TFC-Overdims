package net.claustra01.tfcoverdims.common.growth;

import java.util.ArrayDeque;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;

import net.claustra01.tfcoverdims.TFCOverdims;

public final class CalendarGrowth {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TFCOverdims.MOD_ID);
    public static final Supplier<AttachmentType<CompoundTag>> LAST_UPDATE = ATTACHMENTS.register("plant_growth",
        () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC).build());

    private static final ThreadLocal<Replay> REPLAY = new ThreadLocal<>();

    public static void placed(Level level, BlockPos pos, BlockState state) {
        if (state.isRandomlyTicking()) {
            Replay replay = REPLAY.get();
            var chunk = level.getChunkAt(pos);
            chunk.getData(LAST_UPDATE).putLong(Long.toString(pos.asLong()),
                replay == null ? Calendars.get(level).getCalendarTicks() : replay.tick);
            chunk.setUnsaved(true);
            if (replay != null) {
                replay.positions.add(pos.immutable());
            }
        }
    }

    public static void removed(Level level, BlockPos pos) {
        var chunk = level.getChunkAt(pos);
        if (chunk.hasData(LAST_UPDATE)) {
            chunk.getData(LAST_UPDATE).remove(Long.toString(pos.asLong()));
            chunk.setUnsaved(true);
        }
    }

    public static long interval(ICalendar calendar) {
        // Vanilla selects each block once per 4096 / 3 game ticks on average.
        return Math.max(1, calendar.getFixedCalendarTicksFromTick(1366));
    }

    public static boolean tick(ServerLevel level, BlockPos pos, RandomSource random) {
        if (REPLAY.get() != null) {
            return false;
        }
        var calendar = Calendars.get(level);
        long now = calendar.getCalendarTicks();
        long interval = interval(calendar);
        Replay replay = new Replay();
        replay.positions.add(pos.immutable());
        REPLAY.set(replay);
        try {
            // Preserve unprocessed time in the attachment when a large jump needs another random tick.
            int remaining = 4096;
            while (!replay.positions.isEmpty() && remaining > 0) {
                BlockPos current = replay.positions.remove();
                var chunk = level.getChunkAt(current);
                var times = chunk.getData(LAST_UPDATE);
                String key = Long.toString(current.asLong());
                BlockState state = level.getBlockState(current);
                if (!growing(state)) {
                    continue;
                }
                if (!times.contains(key) || times.getLong(key) > now) {
                    times.putLong(key, now);
                    chunk.setUnsaved(true);
                }
                long last = times.getLong(key);
                while (growing(state) && now - last >= interval && remaining > 0) {
                    last += interval;
                    replay.tick = last;
                    times.putLong(key, last);
                    chunk.setUnsaved(true);
                    state.randomTick(level, current, random);
                    state = level.getBlockState(current);
                    remaining--;
                }
            }
        } finally {
            REPLAY.remove();
        }
        return true;
    }

    private static boolean growing(BlockState state) {
        return (state.is(Blocks.NETHER_WART) || state.is(Blocks.CHORUS_FLOWER)) && state.isRandomlyTicking();
    }

    private static final class Replay {
        private final ArrayDeque<BlockPos> positions = new ArrayDeque<>();
        private long tick;
    }
}
