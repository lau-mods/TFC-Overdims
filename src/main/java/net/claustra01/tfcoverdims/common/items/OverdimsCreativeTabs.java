package net.claustra01.tfcoverdims.common.items;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.claustra01.tfcoverdims.TFCOverdims;

public final class OverdimsCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TFCOverdims.MOD_ID);

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tfcoverdims"))
            .icon(() -> new ItemStack(OverdimsItems.LOOSE_NETHERRACK.get()))
            .displayItems((parameters, output) -> OverdimsItems.ITEMS.getEntries()
                .forEach(item -> output.accept(item.get())))
            .build());
    }

    private OverdimsCreativeTabs() {}
}
