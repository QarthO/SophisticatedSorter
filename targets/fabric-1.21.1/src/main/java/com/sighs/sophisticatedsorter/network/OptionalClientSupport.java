package com.sighs.sophisticatedsorter.network;

import java.util.Collection;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Uses channels already advertised by the unmodified upstream 1.1.1 client. */
public final class OptionalClientSupport {
    private OptionalClientSupport() {}

    public static boolean supportsSorter(Collection<ResourceLocation> channels) {
        return channels.contains(ClientboundTrackedContainerKeyPayload.TYPE.id())
                && channels.contains(ClientboundContainerSettingsPayload.TYPE.id());
    }

    public static boolean supportsSorter(ServerPlayer player) {
        return supportsSorter(ServerPlayNetworking.getSendable(player));
    }

    public static void sendIfSupported(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
