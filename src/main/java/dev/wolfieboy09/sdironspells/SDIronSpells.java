package dev.wolfieboy09.sdironspells;

import com.mojang.logging.LogUtils;
import dev.wolfieboy09.streamdecked.event.DeckLifecycleEvent;
import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(SDIronSpells.MOD_ID)
@EventBusSubscriber(modid = SDIronSpells.MOD_ID)
public class SDIronSpells {
    public static final String MOD_ID = "streamiss";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SDIronSpells(IEventBus modEventBus, ModContainer modContainer) {
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }

    @SubscribeEvent
    public static void onInscribe(InscribeSpellEvent event) {
        if (event.isCanceled()) return;
        SDIronSpellsPlugin.refresh(event.getEntity());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        SDIronSpellsPlugin.refresh(Minecraft.getInstance().player);
    }

    @SubscribeEvent
    public static void onDeckReady(DeckLifecycleEvent.@NotNull Ready event) {
        SDIronSpellsPlugin.onDeckReady(event.getSurface(), Minecraft.getInstance().player);
    }

    @SubscribeEvent
    public static void onDeckDisconnected(DeckLifecycleEvent.Disconnected event) {
        SDIronSpellsPlugin.onDeckDisconnected();
    }
}
