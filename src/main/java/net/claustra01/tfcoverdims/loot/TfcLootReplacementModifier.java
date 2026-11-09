package net.claustra01.tfcoverdims.loot;

import java.util.Map;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.claustra01.tfcoverdims.TFCOverdims;

/** Replaces stacks rolled by an unmodified Vanilla chest loot table according to {@link TfcLootRules}. */
public final class TfcLootReplacementModifier extends LootModifier {
    public static final MapCodec<TfcLootReplacementModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
        codecStart(instance).and(instance.group(
            TfcLootRules.Profile.CODEC.fieldOf("profile").forGetter(modifier -> modifier.profile),
            Codec.unboundedMap(ResourceLocation.CODEC, LootItemFunctions.ROOT_CODEC)
                .optionalFieldOf("enchantments", Map.of()).forGetter(modifier -> modifier.enchantments)))
            .apply(instance, TfcLootReplacementModifier::new));
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TFCOverdims.MOD_ID);
    public static final Supplier<MapCodec<TfcLootReplacementModifier>> TYPE =
        SERIALIZERS.register("tfc_loot_replacement", () -> CODEC);

    private final TfcLootRules.Profile profile;
    /** The source entry's enchantment function per source item, re-run on finished equipment only. */
    private final Map<ResourceLocation, LootItemFunction> enchantments;

    public TfcLootReplacementModifier(LootItemCondition[] conditions, TfcLootRules.Profile profile,
        Map<ResourceLocation, LootItemFunction> enchantments) {
        super(conditions);
        this.profile = profile;
        this.enchantments = Map.copyOf(enchantments);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ObjectArrayList<ItemStack> result = new ObjectArrayList<>(generatedLoot.size());
        for (ItemStack stack : generatedLoot) {
            result.addAll(TfcLootRules.replace(stack, profile, enchantments, context));
        }
        return result;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return TYPE.get();
    }
}
