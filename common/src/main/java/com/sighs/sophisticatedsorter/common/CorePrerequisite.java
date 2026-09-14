package com.sighs.sophisticatedsorter.common;

/**
 * Prerequisite check for Sophisticated Core.
 *
 * <p>This mod is an extension of Sophisticated Core and cannot work without it. The dependency is
 * declared in every target's mod metadata (Forge/NeoForge {@code mandatory}, Fabric
 * {@code depends.sophisticatedcore}), so a loader normally refuses to start when Core is missing.
 * Some launchers, however, only surface that as an unspecific "mod loading failed" on the loading
 * screen, and if the check is ever bypassed the mod would instead fail deep inside class
 * initialisation with a bare {@code NoClassDefFoundError}. This class lets each entry point fail
 * early with an explicit, greppable message that names the missing mod.</p>
 *
 * <p>It is plain Java with no Core or Minecraft types in its signatures, so it can be called before
 * any Core-referencing class is touched.</p>
 */
public final class CorePrerequisite {
    /** Mod id of the required prerequisite. */
    public static final String CORE_MOD_ID = "sophisticatedcore";

    /** A Core class present in every supported Core build; used only as a presence probe. */
    private static final String PROBE_CLASS = "net.p3pp3rf1y.sophisticatedcore.util.InventorySorter";

    private CorePrerequisite() {
    }

    /**
     * Whether Sophisticated Core is present on the class path. Uses {@code initialize = false} so the
     * probe never triggers Core's own static initialisation (or the class-not-found cascade we are
     * guarding against).
     */
    public static boolean isCorePresent() {
        try {
            Class.forName(PROBE_CLASS, false, CorePrerequisite.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Message used when the prerequisite is missing; names the mod so the cause is obvious. */
    public static String missingCoreMessage() {
        return "Sophisticated Sorter requires Sophisticated Core (" + CORE_MOD_ID
                + ") to be installed. Install Sophisticated Core for this Minecraft version and restart. "
                + "精妙整理需要前置模组 Sophisticated Core（" + CORE_MOD_ID + "），请先安装对应版本的 Sophisticated Core 再启动。";
    }

    /**
     * Verifies the prerequisite, throwing a descriptive {@link IllegalStateException} when absent.
     * Call this at the very start of a mod entry point, before touching any Core-referencing class.
     */
    public static void requireCore() {
        if (!isCorePresent()) {
            throw new IllegalStateException(missingCoreMessage());
        }
    }
}
