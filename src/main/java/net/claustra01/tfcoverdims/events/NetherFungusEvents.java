package net.claustra01.tfcoverdims.events;

import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;

public final class NetherFungusEvents {
    public static void bonemeal(BonemealEvent event) {
        if (event.getState().is(Blocks.CRIMSON_FUNGUS) || event.getState().is(Blocks.WARPED_FUNGUS)) {
            event.setCanceled(true);
        }
    }
}
