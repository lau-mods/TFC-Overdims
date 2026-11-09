package net.claustra01.tfcoverdims.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

/** Converts one already rolled structure chest stack into TFC resources and equipment. */
public final class TfcLootRules {
    private static final List<String> COMMON_GEMS = List.of("tfc:gem/amethyst", "tfc:gem/diamond", "tfc:gem/emerald",
        "tfc:gem/lapis_lazuli", "tfc:gem/opal", "tfc:gem/pyrite", "tfc:gem/ruby", "tfc:gem/sapphire",
        "tfc:gem/topaz");
    private static final List<String> COAL_LIKE = List.of("minecraft:charcoal", "tfc:ore/bituminous_coal",
        "tfc:ore/lignite", "tfc:ore/graphite");
    private static final List<String> UTILITY = List.of("tfc:powder/flux", "minecraft:glass", "minecraft:glass_pane",
        "tfc:kaolin_clay", "tfc:powder/wood_ash", "tfc:powder/saltpeter", "tfc:powder/sylvite");
    private static final List<String> IRON_NUGGET = List.of("tfc:ore/small_hematite", "tfc:ore/small_magnetite",
        "tfc:ore/small_limonite");
    private static final Map<String, Group> GROUPS = new HashMap<>();
    private static final Map<String, Family> FAMILIES = new HashMap<>();

    static {
        for (String id : List.of("minecraft:diamond", "minecraft:emerald", "minecraft:lapis_lazuli",
            "minecraft:amethyst_shard")) {
            GROUPS.put(id, Group.GEM);
        }
        for (String id : List.of("minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:copper_ingot")) {
            GROUPS.put(id, Group.INGOT);
        }
        GROUPS.put("minecraft:iron_nugget", Group.IRON_NUGGET);
        GROUPS.put("minecraft:gold_nugget", Group.GOLD_NUGGET);
        GROUPS.put("minecraft:coal", Group.COAL_LIKE);
        GROUPS.put("minecraft:charcoal", Group.COAL_LIKE);
        for (String id : List.of("minecraft:obsidian", "minecraft:crying_obsidian", "minecraft:quartz",
            "minecraft:glowstone_dust")) {
            GROUPS.put(id, Group.UTILITY);
        }

        for (String material : List.of("wooden", "stone", "golden", "iron", "diamond", "netherite")) {
            FAMILIES.put("minecraft:" + material + "_pickaxe", Family.PICKAXE);
            FAMILIES.put("minecraft:" + material + "_axe", Family.AXE);
            FAMILIES.put("minecraft:" + material + "_hoe", Family.HOE);
            FAMILIES.put("minecraft:" + material + "_sword", Family.SWORD);
            FAMILIES.put("minecraft:" + material + "_shovel", Family.SHOVEL);
        }
        for (String material : List.of("leather", "chainmail", "golden", "iron", "diamond", "netherite")) {
            FAMILIES.put("minecraft:" + material + "_helmet", Family.HELMET);
            FAMILIES.put("minecraft:" + material + "_chestplate", Family.CHESTPLATE);
            FAMILIES.put("minecraft:" + material + "_leggings", Family.LEGGINGS);
            FAMILIES.put("minecraft:" + material + "_boots", Family.BOOTS);
        }
        FAMILIES.put("minecraft:shears", Family.SHEARS);
        FAMILIES.put("minecraft:shield", Family.SHIELD);
        for (String material : List.of("iron", "golden", "diamond")) {
            FAMILIES.put("minecraft:" + material + "_horse_armor", Family.HORSE_ARMOR);
        }
    }

    /**
     * Returns the stacks that replace {@code stack}, or a list holding only {@code stack} when no rule applies.
     *
     * @param enchantments the source entry's enchantment function per source item. An enchanted source is replaced
     *                     only when it has one; the function is then re-run on finished equipment only.
     */
    public static List<ItemStack> replace(ItemStack stack, Profile profile,
        Map<ResourceLocation, LootItemFunction> enchantments, LootContext context) {
        if (stack.isEmpty()) {
            return List.of(stack);
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = key.toString();
        Family family = FAMILIES.get(id);
        LootItemFunction enchantment = family != null ? enchantments.get(key) : null;
        if (!hasOnlyKnownComponents(stack, enchantment != null)) {
            return List.of(stack);
        }
        boolean enchanted = !stack.getTagEnchantments().isEmpty();
        RandomSource random = context.getRandom();
        String target;
        boolean finished = false;
        if (id.equals("minecraft:chain")) {
            target = "tfc:metal/chain/wrought_iron";
        } else if (family != null) {
            boolean unfinished = !family.unfinished.isEmpty() && random.nextInt(3) < 2;
            String shape = pick(unfinished ? family.unfinished : family.finished, random);
            target = "tfc:metal/" + shape + "/" + pick(profile.equipment, random);
            finished = !unfinished;
        } else {
            Group group = GROUPS.get(id);
            if (group == null) {
                return List.of(stack);
            }
            target = switch (group) {
                case GEM -> pick(COMMON_GEMS, random);
                case INGOT -> "tfc:metal/ingot/" + pick(profile.ingots, random);
                case IRON_NUGGET -> pick(IRON_NUGGET, random);
                case GOLD_NUGGET -> "tfc:ore/small_native_gold";
                case COAL_LIKE -> pick(COAL_LIKE, random);
                case UTILITY -> pick(UTILITY, random);
            };
        }
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(target));
        if (item == Items.AIR || item == stack.getItem()) {
            return List.of(stack);
        }
        List<ItemStack> result = new ArrayList<>();
        int remaining = stack.getCount();
        while (remaining > 0) {
            ItemStack output = new ItemStack(item);
            int count = Math.min(remaining, output.getMaxStackSize());
            output.setCount(count);
            if (finished && enchanted) {
                output = enchantment.apply(output, context);
            }
            result.add(output);
            remaining -= count;
        }
        return result;
    }

    /**
     * Rejects stacks carrying components that a freshly created stack of the same item lacks. Components attached
     * to every new stack, such as TFC heat and food, are defaults rather than special loot data. Damage is not
     * carried over to a different item, and enchantments are known only when the source entry's function is given.
     */
    private static boolean hasOnlyKnownComponents(ItemStack stack, boolean enchantmentsKnown) {
        DataComponentPatch fresh = new ItemStack(stack.getItem()).getComponentsPatch();
        for (var entry : stack.getComponentsPatch().entrySet()) {
            DataComponentType<?> type = entry.getKey();
            if (fresh.get(type) == null && type != DataComponents.DAMAGE
                && !(enchantmentsKnown && type == DataComponents.ENCHANTMENTS)) {
                return false;
            }
        }
        return true;
    }

    private static String pick(List<String> values, RandomSource random) {
        return values.get(random.nextInt(values.size()));
    }

    private TfcLootRules() {
    }

    private enum Group {
        GEM, INGOT, IRON_NUGGET, GOLD_NUGGET, COAL_LIKE, UTILITY
    }

    private enum Family {
        PICKAXE(List.of("pickaxe_head", "propick_head"), List.of("pickaxe", "propick")),
        AXE(List.of("axe_head", "saw_blade"), List.of("axe", "saw")),
        HOE(List.of("hoe_head", "scythe_blade"), List.of("hoe", "scythe")),
        SWORD(List.of("sword_blade", "javelin_head", "mace_head"), List.of("sword", "javelin", "mace")),
        SHOVEL(List.of("shovel_head"), List.of("shovel")),
        HELMET(List.of("unfinished_helmet"), List.of("helmet")),
        CHESTPLATE(List.of("unfinished_chestplate"), List.of("chestplate")),
        LEGGINGS(List.of("unfinished_greaves"), List.of("greaves")),
        BOOTS(List.of("unfinished_boots"), List.of("boots")),
        SHEARS(List.of(), List.of("shears")),
        SHIELD(List.of(), List.of("shield")),
        HORSE_ARMOR(List.of(), List.of("horse_armor"));

        private final List<String> unfinished;
        private final List<String> finished;

        Family(List<String> unfinished, List<String> finished) {
            this.unfinished = unfinished;
            this.finished = finished;
        }
    }

    public enum Profile implements StringRepresentable {
        ORDINARY("ordinary", List.of("copper", "bronze", "bismuth_bronze", "black_bronze", "wrought_iron"),
            List.of("copper", "bronze", "bismuth_bronze", "black_bronze")),
        DANGEROUS("dangerous", List.of("wrought_iron", "steel"), List.of("wrought_iron")),
        LATEGAME("lategame", List.of("steel", "black_steel"), List.of("steel"));

        public static final Codec<Profile> CODEC = StringRepresentable.fromEnum(Profile::values);

        private final String name;
        private final List<String> ingots;
        private final List<String> equipment;

        Profile(String name, List<String> ingots, List<String> equipment) {
            this.name = name;
            this.ingots = ingots;
            this.equipment = equipment;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
