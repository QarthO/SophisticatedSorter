package com.sighs.sophisticatedsorter.mixin;

import com.sighs.sophisticatedsorter.common.SortExecutionState;
import com.sighs.sophisticatedsorter.common.SortStackLimitPolicy;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.InventorySorter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = InventorySorter.class, remap = false)
public class InventorySorterMixin {
    /**
     * {@code remap = true} is required here: the injection target embeds a vanilla type
     * ({@code ItemVariant}/{@code ItemStack}) that is obfuscated on Fabric production runtimes
     * (intermediary {@code class_1799}), while the Core class itself is not remapped. Without it the
     * injector is simply never found in production - which silently re-enables the vanilla 64-per-slot
     * limit and makes the sorter merge max-stack-1 items into one invalid stack, losing the rest.
     */
    @ModifyVariable(
            method = "placeStack(Lnet/p3pp3rf1y/sophisticatedcore/inventory/ItemStackKey;IIZLnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$IStackLimitGetter;Lnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$ISlotStackGetter;Lnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$ISlotStackSetter;)I",
            at = @At(value = "INVOKE", target = "Lnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$IStackLimitGetter;getStackLimit(ILnet/fabricmc/fabric/api/transfer/v1/item/ItemVariant;)I", shift = At.Shift.AFTER, remap = true),
            ordinal = 0, argsOnly = true, require = 0, remap = false)
    private static int modifyFabricSlotLimit(int slotLimit, ItemStackKey current) {
        return applySlotLimit(slotLimit, current);
    }

    @ModifyVariable(
            method = "placeStack(Lnet/p3pp3rf1y/sophisticatedcore/inventory/ItemStackKey;IIZLnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$IStackLimitGetter;Lnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$ISlotStackGetter;Lnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$ISlotStackSetter;)I",
            at = @At(value = "INVOKE", target = "Lnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$IStackLimitGetter;getStackLimit(ILnet/minecraft/world/item/ItemStack;)I", shift = At.Shift.AFTER, remap = true),
            ordinal = 0, argsOnly = true, require = 0, remap = false)
    private static int modifyItemStackSlotLimit(int slotLimit, ItemStackKey current) {
        return applySlotLimit(slotLimit, current);
    }

    private static int applySlotLimit(int slotLimit, ItemStackKey current) {
        return SortStackLimitPolicy.apply(slotLimit, current.getStack().getMaxStackSize(),
                SortExecutionState.shouldLimitToItemMaxStackSize());
    }
}
