package net.claustra01.tfcoverdims.client;

import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.dries007.tfc.client.extensions.ItemRendererExtension;
import net.dries007.tfc.client.render.blockentity.ChestItemRenderer;
import net.dries007.tfc.client.render.blockentity.TFCSignBlockEntityRenderer;
import net.dries007.tfc.common.blocks.wood.Wood;
import net.dries007.tfc.common.items.ChestBlockItem;
import net.claustra01.tfcoverdims.TFCOverdims;
import net.claustra01.tfcoverdims.common.wood.OverdimsWood;
import net.claustra01.tfcoverdims.common.wood.OverdimsWoodBlocks;

@EventBusSubscriber(modid = TFCOverdims.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WoodClientEvents {
    @SubscribeEvent
    public static void registerChestItemRenderers(RegisterClientExtensionsEvent event) {
        for (OverdimsWood wood : OverdimsWood.values()) {
            registerChestItemRenderer(event, wood, Wood.BlockType.CHEST);
            registerChestItemRenderer(event, wood, Wood.BlockType.TRAPPED_CHEST);
        }
    }

    private static void registerChestItemRenderer(RegisterClientExtensionsEvent event, OverdimsWood wood, Wood.BlockType type) {
        Item item = OverdimsWoodBlocks.WOODS.get(wood).get(type).get().asItem();
        ChestBlockItem chestItem = (ChestBlockItem) item;
        event.registerItem(ItemRendererExtension.cached(() -> new ChestItemRenderer(chestItem)), item);
    }

    @SubscribeEvent
    public static void registerSignModels(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (OverdimsWood wood : OverdimsWood.values()) {
            var type = wood.getVanillaWoodType();
            TFCSignBlockEntityRenderer.MODELS.put(type,
                context -> SignRenderer.createSignModel(context.getModelSet(), type));
        }
    }
}
