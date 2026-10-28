package net.claustra01.tfcoverdims.client;

import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.dries007.tfc.client.render.blockentity.TFCSignBlockEntityRenderer;
import net.claustra01.tfcoverdims.TFCOverdims;
import net.claustra01.tfcoverdims.common.wood.OverdimsWood;

@EventBusSubscriber(modid = TFCOverdims.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WoodClientEvents {
    @SubscribeEvent
    public static void registerSignModels(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (OverdimsWood wood : OverdimsWood.values()) {
            var type = wood.getVanillaWoodType();
            TFCSignBlockEntityRenderer.MODELS.put(type,
                context -> SignRenderer.createSignModel(context.getModelSet(), type));
        }
    }
}
