package net.claustra01.tfcoverdims.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class EnchantingLapis {
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("tfcoverdims", "enchanting_lapis"));
    private static final ResourceLocation VANILLA_LAPIS = ResourceLocation.withDefaultNamespace("lapis_lazuli");
    private static final ResourceLocation TFC_LAPIS = ResourceLocation.fromNamespaceAndPath("tfc", "gem/lapis_lazuli");

    private EnchantingLapis() {}

    public static boolean accepts(ItemStack stack) {
        if (stack.is(TAG)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.equals(VANILLA_LAPIS) || id.equals(TFC_LAPIS);
    }
}
