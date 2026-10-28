package net.claustra01.tfcoverdims.common;

import net.dries007.tfc.common.blockentities.TFCChestBlockEntity;
import net.minecraft.world.item.ItemStack;

public final class ContainerRules {
    private ContainerRules() {
    }

    public static boolean fitsTfcChest(ItemStack stack) {
        return TFCChestBlockEntity.isValid(stack);
    }
}
