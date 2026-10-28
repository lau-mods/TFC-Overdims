package net.claustra01.tfcoverdims.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.PiglinSpecificSensor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.size.Size;
import net.dries007.tfc.common.component.size.Weight;

import net.claustra01.tfcoverdims.TFCOverdims;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MobLootGameTests {
    @GameTest(template = "empty", batch = "climate")
    public static void piglinCurrency(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        var previousPosition = player.position();
        var piglin = EntityType.PIGLIN.create(level);
        piglin.setBaby(false);
        piglin.setPos(helper.absoluteVec(new Vec3(2, 3, 2)));
        level.getChunkAt(piglin.blockPosition());
        level.addFreshEntity(piglin);
        var finish = PiglinAi.class.getDeclaredMethod("stopHoldingOffHandItem", Piglin.class, boolean.class);
        finish.setAccessible(true);
        var pickup = PiglinAi.class.getDeclaredMethod("pickUpItem", Piglin.class, ItemEntity.class);
        pickup.setAccessible(true);
        var wants = PiglinAi.class.getDeclaredMethod("wantsToPickup", Piglin.class, ItemStack.class);
        wants.setAccessible(true);
        try {
            for (String id : List.of("minecraft:gold_ingot", "tfc:metal/ingot/gold")) {
                for (boolean ground : new boolean[] {false, true}) {
                    piglin.getBrain().eraseMemory(MemoryModuleType.ADMIRING_ITEM);
                    ItemStack currency = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)), 2);
                    if (ground) {
                        helper.assertTrue((boolean) wants.invoke(null, piglin, currency), "Piglin must want dropped " + id);
                        var entity = new ItemEntity(level, piglin.getX(), piglin.getY(), piglin.getZ(), currency);
                        level.addFreshEntity(entity);
                        pickup.invoke(null, piglin, entity);
                        helper.assertTrue(entity.getItem().getCount() == 1, "Pickup consumes one ingot");
                        entity.discard();
                    } else {
                        player.setItemInHand(InteractionHand.MAIN_HAND, currency);
                        helper.assertTrue(PiglinAi.mobInteract(piglin, player, InteractionHand.MAIN_HAND).consumesAction(),
                            "Hand interaction must accept " + id);
                        helper.assertTrue(currency.getCount() == 1, "Interaction consumes one ingot");
                    }
                    helper.assertTrue(piglin.getOffhandItem().is(currency.getItem())
                        && piglin.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_ITEM), "Piglin must admire currency");
                    helper.assertTrue(!piglin.isPersistenceRequired(), "Currency must not prevent despawning: " + id
                        + (ground ? " by pickup" : " by hand"));
                    finish.invoke(null, piglin, true);
                    var rewards = level.getEntitiesOfClass(ItemEntity.class, piglin.getBoundingBox().inflate(5));
                    helper.assertTrue(piglin.getOffhandItem().isEmpty() && !rewards.isEmpty(), "Bartering must produce rewards");
                    Set<String> allowed = rewardNames(lootJson(helper, "gameplay/piglin_bartering", true));
                    for (var reward : rewards) {
                        helper.assertTrue(allowed.contains(BuiltInRegistries.ITEM.getKey(reward.getItem().getItem()).toString()),
                            "Bartering retains Vanilla rewards");
                        reward.discard();
                    }
                }
            }
            piglin.getBrain().eraseMemory(MemoryModuleType.ADMIRING_ITEM);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT));
            helper.assertTrue(!PiglinAi.mobInteract(piglin, player, InteractionHand.MAIN_HAND).consumesAction(), "Iron is not currency");
            piglin.setBaby(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT));
            helper.assertTrue(!PiglinAi.mobInteract(piglin, player, InteractionHand.MAIN_HAND).consumesAction(), "Babies cannot barter");
            for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
            piglin.setBaby(false);
            player.setPos(piglin.position().add(1, 0, 0));
            var sensor = new PiglinSpecificSensor();
            var sense = PiglinSpecificSensor.class.getDeclaredMethod("doTick", ServerLevel.class, LivingEntity.class);
            sense.setAccessible(true);
            piglin.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                new NearestVisibleLivingEntities(piglin, List.of(player)));
            sense.invoke(sensor, level, piglin);
            helper.assertTrue(!PiglinAi.isWearingGold(player)
                && piglin.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD),
                "Unarmored players are selected as hostile targets");
            var armor = List.of(Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);
            var slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
            for (int i = 0; i < armor.size(); i++) {
                player.setItemSlot(slots.get(i), new ItemStack(armor.get(i)));
                sense.invoke(sensor, level, piglin);
                helper.assertTrue(PiglinAi.isWearingGold(player)
                    && !piglin.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD),
                    "Every Vanilla gold armor piece suppresses hostile target selection");
                player.setItemSlot(slots.get(i), ItemStack.EMPTY);
            }
            helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.keySet().stream().noneMatch(id -> id.getNamespace().equals("tfcoverdims")),
                "No new mobs");
            helper.assertTrue(BuiltInRegistries.ITEM.keySet().stream().noneMatch(id -> id.getNamespace().equals("tfcoverdims")
                && id.getPath().contains("gold")), "No new gold equipment");
            helper.assertTrue(lootJson(helper, "gameplay/piglin_bartering", false)
                .equals(lootJson(helper, "gameplay/piglin_bartering", true)), "Bartering table is unchanged");
        } finally {
            piglin.discard();
            player.setPos(previousPosition);
            for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void mobDrops(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        for (var type : List.of(EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.ENDERMAN,
            EntityType.SHULKER, EntityType.BLAZE, EntityType.GHAST)) {
            var mob = type.create(level);
            String path = "entities/" + BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
            var original = lootJson(helper, path, true);
            if (type == EntityType.ZOMBIFIED_PIGLIN) {
                original.getAsJsonArray("pools").get(2).getAsJsonObject().getAsJsonArray("entries")
                    .get(0).getAsJsonObject().addProperty("name", "tfc:metal/ingot/gold");
            }
            helper.assertTrue(original.equals(lootJson(helper, path, false)), "Only the existing gold ingot drop changes: " + path);
            var params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player))
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                .withParameter(LootContextParams.ATTACKING_ENTITY, player)
                .create(LootContextParamSets.ENTITY);
            Set<String> seen = new HashSet<>();
            var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.withDefaultNamespace(path)));
            for (long seed = 1; seed <= 1024; seed++) {
                for (var stack : table.getRandomItems(params, seed * 0x9E3779B97F4A7C15L)) {
                    if (stack.isEmpty()) continue;
                    String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    helper.assertTrue(itemNames(original).contains(id), "Unexpected mob drop " + id);
                    var size = ItemSizeManager.get(stack);
                    Size expectedSize = switch (id) {
                        case "tfc:metal/ingot/gold" -> Size.LARGE;
                        case "minecraft:rotten_flesh" -> Size.SMALL;
                        case "minecraft:blaze_rod" -> Size.NORMAL;
                        case "minecraft:gunpowder" -> Size.TINY;
                        default -> Size.VERY_SMALL;
                    };
                    Weight expectedWeight = switch (id) {
                        case "tfc:metal/ingot/gold" -> Weight.MEDIUM;
                        case "minecraft:rotten_flesh", "minecraft:blaze_rod" -> Weight.LIGHT;
                        default -> Weight.VERY_LIGHT;
                    };
                    helper.assertTrue(size.getSize(stack) == expectedSize && size.getWeight(stack) == expectedWeight,
                        "Drop retains TFC ingot, food, rod, powder or default item size rules: " + id);
                    seen.add(id);
                }
            }
            helper.assertTrue(seen.containsAll(itemNames(original)), "Seeds must exercise every mob drop: " + path + " " + seen);
            mob.setPos(helper.absoluteVec(new Vec3(2, 3, 2)));
            level.addFreshEntity(mob);
            var killedDrops = new ArrayList<ItemEntity>();
            Consumer<LivingDropsEvent> capture = event -> {
                if (event.getEntity() == mob) killedDrops.addAll(event.getDrops());
            };
            NeoForge.EVENT_BUS.addListener(capture);
            try {
                mob.hurt(level.damageSources().playerAttack(player), 1000);
                helper.assertTrue(mob.isDeadOrDying(), "The target mob must be killed: " + path);
                for (var drop : killedDrops) {
                    helper.assertTrue(itemNames(original).contains(BuiltInRegistries.ITEM.getKey(drop.getItem().getItem()).toString()),
                        "Actual kills retain the same compatible drops: " + path);
                }
            } finally {
                NeoForge.EVENT_BUS.unregister(capture);
                killedDrops.forEach(ItemEntity::discard);
                mob.discard();
            }
        }
        helper.succeed();
    }

    static JsonObject lootJson(GameTestHelper helper, String path, boolean vanilla) throws Exception {
        var manager = helper.getLevel().getServer().getResourceManager();
        var id = ResourceLocation.withDefaultNamespace("loot_table/" + path + ".json");
        var resource = vanilla ? manager.getResourceStack(id).stream().filter(r -> r.sourcePackId().equals("vanilla"))
            .findFirst().orElseThrow() : manager.getResourceOrThrow(id);
        try (var reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    static Set<String> rewardNames(JsonObject table) {
        Set<String> names = itemNames(table);
        for (var pool : table.getAsJsonArray("pools")) {
            for (var entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                var object = entry.getAsJsonObject();
                if (object.has("name") && object.get("name").getAsString().equals("minecraft:book")
                    && object.has("functions") && object.get("functions").toString().contains("minecraft:enchant_")) {
                    names.remove("minecraft:book");
                    names.add("minecraft:enchanted_book");
                }
            }
        }
        return names;
    }

    static Set<String> itemNames(JsonObject table) {
        Set<String> names = new HashSet<>();
        if (table.has("pools")) {
            for (var pool : table.getAsJsonArray("pools")) {
                for (var entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                    var object = entry.getAsJsonObject();
                    if (object.has("name") && object.get("type").getAsString().equals("minecraft:item")) {
                        names.add(object.get("name").getAsString());
                    }
                }
            }
        }
        return names;
    }
}
