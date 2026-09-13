package com.sighs.sophisticatedsorter.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

/** Shared accessor for the field present in every supported menu mapping. */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @Accessor("quickcraftSlots")
    Set<Slot> sophisticatedSorter$getQuickcraftSlots();

    /** Vanilla slot-range move, used to honor the configured main-inventory/hotbar order. */
    @Invoker("moveItemStackTo")
    boolean sophisticatedSorter$moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverse);
}
