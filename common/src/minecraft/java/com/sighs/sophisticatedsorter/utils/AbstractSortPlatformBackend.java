package com.sighs.sophisticatedsorter.utils;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Shared adapter plumbing around the one loader-specific Core storage type. */
public abstract class AbstractSortPlatformBackend<H> implements SortPlatform {
    protected abstract H createHandler(int size);

    protected abstract void setStack(H handler, int slot, ItemStack stack);

    protected abstract ItemStack getStack(H handler, int slot);

    protected abstract void sortHandler(H handler,
                                        Comparator<Map.Entry<ItemStackKey, Integer>> comparator);

    @Override
    public final void sortStacks(List<ItemStack> stacks,
                                 Comparator<Map.Entry<ItemStackKey, Integer>> comparator) {
        // Sorting must be a permutation, and every result stack must respect its item's own limit.
        // Two failure modes are guarded here: Core's sorter drops the overflow when the handler has
        // too few slots for a high-stack item, and an oversized result stack (its per-slot limit hook
        // not applied) would be truncated by the container on write-back, destroying the surplus -
        // which is exactly how max-stack-1 items used to be merged into one and lost on Fabric.
        // Snapshot before sorting, then keep the originals whenever the result is not safe.
        Map<Item, Integer> beforeTotals = SortIntegrity.totals(stacks);
        Map<Item, Integer> beforeMaxCounts = SortIntegrity.maxCounts(stacks);
        H handler = createHandler(stacks.size());
        for (int index = 0; index < stacks.size(); index++) {
            setStack(handler, index, stacks.get(index));
        }
        com.sighs.sophisticatedsorter.common.SortExecutionState.withItemMaxStackSizeLimit(
                () -> sortHandler(handler, comparator));
        List<ItemStack> sorted = new ArrayList<ItemStack>(stacks.size());
        for (int index = 0; index < stacks.size(); index++) {
            sorted.add(getStack(handler, index));
        }
        if (SortIntegrity.isSafeResult(beforeTotals, beforeMaxCounts, sorted)) {
            for (int index = 0; index < stacks.size(); index++) {
                stacks.set(index, sorted.get(index));
            }
        }
    }
}
