package com.sighs.sophisticatedsorter.network;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OptionalClientRegistrySyncTest {
    private static ResourceLocation id(String id) { return ResourceLocation.parse(id); }

    private Map<ResourceLocation, Object2IntMap<ResourceLocation>> registries() {
        var menus = new Object2IntLinkedOpenHashMap<ResourceLocation>();
        menus.put(id("minecraft:generic_9x3"), 2);
        menus.put(OptionalClientRegistrySync.SETTINGS_MENU, 31);
        menus.put(id("othermod:menu"), 32);
        var items = new Object2IntLinkedOpenHashMap<ResourceLocation>();
        items.put(id("othermod:item"), 700);
        var map = new LinkedHashMap<ResourceLocation, Object2IntMap<ResourceLocation>>();
        map.put(OptionalClientRegistrySync.MENU_REGISTRY, menus);
        map.put(id("minecraft:item"), items);
        return map;
    }

    @Test void stockClientRetainsExactRegistryIds() {
        var original = registries();
        assertSame(original, OptionalClientRegistrySync.forClient(original, true));
        assertEquals(31, original.get(OptionalClientRegistrySync.MENU_REGISTRY).getInt(OptionalClientRegistrySync.SETTINGS_MENU));
    }

    @Test void absentClientOmitsOnlySorterAndDoesNotMutateOtherConnections() {
        var original = registries();
        var filtered = OptionalClientRegistrySync.forClient(original, false);
        var menus = filtered.get(OptionalClientRegistrySync.MENU_REGISTRY);
        assertFalse(menus.containsKey(OptionalClientRegistrySync.SETTINGS_MENU));
        assertEquals(2, menus.size());
        assertEquals(2, menus.getInt(id("minecraft:generic_9x3")));
        assertEquals(32, menus.getInt(id("othermod:menu")));
        assertEquals(original.get(id("minecraft:item")), filtered.get(id("minecraft:item")));
        assertTrue(original.get(OptionalClientRegistrySync.MENU_REGISTRY).containsKey(OptionalClientRegistrySync.SETTINGS_MENU));
        assertSame(original, OptionalClientRegistrySync.forClient(original, true));
    }

    @Test void serverMovesSorterLastSoAbsentClientsHaveNoRawIdHoles() {
        var original = registries();
        var ordered = OptionalClientRegistrySync.sorterLast(original.get(OptionalClientRegistrySync.MENU_REGISTRY));
        assertEquals(0, ordered.getInt(id("minecraft:generic_9x3")));
        assertEquals(1, ordered.getInt(id("othermod:menu")));
        assertEquals(2, ordered.getInt(OptionalClientRegistrySync.SETTINGS_MENU));
        assertEquals(31, original.get(OptionalClientRegistrySync.MENU_REGISTRY).getInt(OptionalClientRegistrySync.SETTINGS_MENU));
        original.put(OptionalClientRegistrySync.MENU_REGISTRY, ordered);
        var filtered = OptionalClientRegistrySync.forClient(original, false).get(OptionalClientRegistrySync.MENU_REGISTRY);
        assertEquals(Set.of(0, 1), Set.copyOf(filtered.values()));
    }

    @Test void missingMenuRegistryNeedsNoFiltering() {
        Map<ResourceLocation, Object2IntMap<ResourceLocation>> empty = Map.of();
        assertSame(empty, OptionalClientRegistrySync.forClient(empty, false));
    }

    @Test void requiresBothUpstreamReceivingChannels() {
        var tracked = id("sophisticatedsorter:tracked_container_key");
        var settings = id("sophisticatedsorter:container_settings_contents");
        assertFalse(OptionalClientSupport.supportsSorter(Set.of()));
        assertFalse(OptionalClientSupport.supportsSorter(Set.of(tracked)));
        assertFalse(OptionalClientSupport.supportsSorter(Set.of(settings)));
        assertTrue(OptionalClientSupport.supportsSorter(Set.of(tracked, settings)));
    }
}
