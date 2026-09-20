package com.sighs.sophisticatedsorter.network;

import java.util.LinkedHashMap;
import java.util.Comparator;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.impl.registry.sync.RemappableRegistry;
import net.fabricmc.fabric.impl.registry.sync.RemapException;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map;
import java.util.function.Consumer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.impl.networking.CommonPacketsImpl;
import net.fabricmc.fabric.impl.networking.CommonRegisterPayload;
import net.fabricmc.fabric.impl.networking.CommonVersionPayload;
import net.fabricmc.fabric.impl.networking.server.ServerNetworkingImpl;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

/** Fabric 1.21.1 adapter; keep registry sync before vanilla configuration data. */
public final class OptionalClientRegistrySync {
    static final ResourceLocation MENU_REGISTRY = ResourceLocation.withDefaultNamespace("menu");
    static final ResourceLocation SETTINGS_MENU = ResourceLocation.fromNamespaceAndPath("sophisticatedsorter", "container_settings");

    private OptionalClientRegistrySync() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            var menus = new Object2IntLinkedOpenHashMap<ResourceLocation>();
            for (var menu : BuiltInRegistries.MENU) {
                menus.put(BuiltInRegistries.MENU.getKey(menu), BuiltInRegistries.MENU.getId(menu));
            }
            try {
                // Removing a middle entry would leave a raw-ID hole on unmodded clients.
                // Fabric's unmap on disconnect cannot restore registries with holes. Move
                // our menu to the tail once, using Fabric's remap events for cached IDs.
                ((RemappableRegistry) BuiltInRegistries.MENU).remap(
                        MENU_REGISTRY.toString(), sorterLast(menus), RemappableRegistry.RemapMode.EXACT);
            } catch (RemapException e) {
                throw new IllegalStateException("Unable to prepare optional Sorter menu registry", e);
            }
        });
    }

    static Object2IntMap<ResourceLocation> sorterLast(Object2IntMap<ResourceLocation> original) {
        var ordered = new Object2IntLinkedOpenHashMap<ResourceLocation>();
        original.keySet().stream().filter(id -> !id.equals(SETTINGS_MENU))
                .sorted(Comparator.comparingInt(original::getInt))
                .forEach(id -> ordered.put(id, ordered.size()));
        if (original.containsKey(SETTINGS_MENU)) {
            ordered.put(SETTINGS_MENU, ordered.size());
        }
        return ordered;
    }

    public static void configureClient(ServerConfigurationPacketListenerImpl handler, MinecraftServer server) {
        if (!RegistrySyncManager.DEBUG && server.isSingleplayerOwner(handler.getOwner())) {
            return;
        }
        if (!ServerConfigurationNetworking.canSend(handler, RegistrySyncManager.DIRECT_PACKET_HANDLER.getPacketId())) {
            return;
        }
        var map = RegistrySyncManager.createAndPopulateRegistryMap();
        if (map == null) {
            return;
        }
        // Fabric normally negotiates play channels AFTER registry sync. Do the standard
        // exchange early, using Fabric's existing receivers/task keys, so stock clients
        // identify themselves before the optional menu is included in their registry map.
        if (ServerConfigurationNetworking.canSend(handler, CommonVersionPayload.ID)
                && ServerConfigurationNetworking.canSend(handler, CommonRegisterPayload.ID)) {
            handler.addTask(new VersionTask(handler));
            handler.addTask(new ChannelsTask(handler));
        }
        handler.addTask(new RegistryTask(handler, map));
    }

    static Map<ResourceLocation, Object2IntMap<ResourceLocation>> forClient(
            Map<ResourceLocation, Object2IntMap<ResourceLocation>> original, boolean supportsSorter) {
        if (supportsSorter || !original.containsKey(MENU_REGISTRY)) {
            return original;
        }
        // Copy both maps: Fabric may reuse its map for other connections. Preserve every
        // other entry and its raw ID, so all other mods retain validation.
        // SERVER_STARTING places our menu last, so removing it leaves no holes.
        var filtered = new LinkedHashMap<>(original);
        var menus = new Object2IntLinkedOpenHashMap<>(original.get(MENU_REGISTRY));
        menus.removeInt(SETTINGS_MENU);
        filtered.put(MENU_REGISTRY, menus);
        return filtered;
    }

    private record VersionTask(ServerConfigurationPacketListenerImpl handler) implements ConfigurationTask {
        @Override public Type type() { return new Type(CommonVersionPayload.ID.id().toString()); }
        @Override public void start(Consumer<Packet<?>> sender) {
            ServerConfigurationNetworking.send(handler, new CommonVersionPayload(CommonPacketsImpl.SUPPORTED_COMMON_PACKET_VERSIONS));
        }
    }

    private record ChannelsTask(ServerConfigurationPacketListenerImpl handler) implements ConfigurationTask {
        @Override public Type type() { return new Type(CommonRegisterPayload.ID.id().toString()); }
        @Override public void start(Consumer<Packet<?>> sender) {
            var addon = ServerNetworkingImpl.getAddon(handler);
            ServerConfigurationNetworking.send(handler, new CommonRegisterPayload(
                    addon.getNegotiatedVersion(), CommonRegisterPayload.PLAY_PHASE, ServerPlayNetworking.getGlobalReceivers()));
        }
    }

    private record RegistryTask(ServerConfigurationPacketListenerImpl handler,
            Map<ResourceLocation, Object2IntMap<ResourceLocation>> map) implements ConfigurationTask {
        @Override public Type type() { return RegistrySyncManager.SyncConfigurationTask.KEY; }
        @Override public void start(Consumer<Packet<?>> sender) {
            var channels = ServerNetworkingImpl.getAddon(handler).getChannelInfoHolder()
                    .fabric_getPendingChannelsNames(ConnectionProtocol.PLAY);
            var clientMap = forClient(map, OptionalClientSupport.supportsSorter(channels));
            RegistrySyncManager.DIRECT_PACKET_HANDLER.sendPacket(
                    payload -> ServerConfigurationNetworking.send(handler, payload), clientMap);
        }
    }
}
