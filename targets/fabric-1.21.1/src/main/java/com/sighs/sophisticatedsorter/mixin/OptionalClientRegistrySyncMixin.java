package com.sighs.sophisticatedsorter.mixin;

import com.sighs.sophisticatedsorter.network.OptionalClientRegistrySync;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replace only Fabric's server-side registry task scheduling, never client validation. */
@Mixin(value = RegistrySyncManager.class, remap = false)
public abstract class OptionalClientRegistrySyncMixin {
    @Inject(method = "configureClient", at = @At("HEAD"), cancellable = true)
    private static void sophisticatedSorter$optionalClients(ServerConfigurationPacketListenerImpl handler,
            MinecraftServer server, CallbackInfo ci) {
        OptionalClientRegistrySync.configureClient(handler, server);
        ci.cancel();
    }
}
