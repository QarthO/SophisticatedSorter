package com.sighs.sophisticatedsorter.utils;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Guards the copy-based generic sort against silently dropping items.
 *
 * <p>The mod's generic sort feeds the container's stacks through Sophisticated Core's
 * {@code InventorySorter} and writes the result back into the real container. That round trip can
 * destroy items in two ways, both of which this class detects:</p>
 * <ul>
 * <li><b>Overflow dropped:</b> Core compacts everything into item-&gt;count pools and places them
 * back at the sorter's per-slot limit. A high-stack item (for example a stack of 1000 from a
 * Sophisticated Storage stack upgrade) then needs many slots; when the target handler has fewer
 * slots than that, Core's {@code sortIntoOtherSlots} simply stops and the leftover is gone.</li>
 * <li><b>Oversized stack truncated:</b> if the per-slot limit the sorter uses is larger than the
 * item's real limit, max-stack-1 items (axes, leggings, shovels, ...) get merged into a single
 * "stack of N" that the container truncates back to N=1 on write-back - the surplus is destroyed.</li>
 * </ul>
 *
 * <p>Both are caught by comparing the sort result against a snapshot taken <b>before</b> the sort
 * (so in-place mutation of the input during sorting cannot weaken the check): the per-item totals
 * must be unchanged (a correct sort is a permutation) and no result stack may exceed both the item's
 * own limit and the largest stack that item legitimately had in the input. A pre-existing oversized
 * stack therefore still passes, while the two corruption modes above are rejected and the caller
 * keeps the originals.</p>
 */
public final class SortIntegrity {
    private SortIntegrity() {
    }

    /** Per-item totals of the given stacks, taken before a sort. */
    public static Map<Item, Integer> totals(List<ItemStack> stacks) {
        Map<Item, Integer> totals = new HashMap<Item, Integer>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                totals.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return totals;
    }

    /** Largest single stack per item in the given stacks, taken before a sort. */
    public static Map<Item, Integer> maxCounts(List<ItemStack> stacks) {
        Map<Item, Integer> maxCounts = new HashMap<Item, Integer>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                maxCounts.merge(stack.getItem(), stack.getCount(), Math::max);
            }
        }
        return maxCounts;
    }

    /** Whether the stacks still hold exactly the totals recorded in {@code beforeTotals}. */
    public static boolean preserved(Map<Item, Integer> beforeTotals, List<ItemStack> stacks) {
        return beforeTotals.equals(totals(stacks));
    }

    /**
     * Whether the sort result may be written back.
     *
     * @param beforeTotals    per-item totals of the input, taken before the sort
     * @param beforeMaxCounts largest single stack per item of the input, taken before the sort
     * @param result          the stacks the sorter produced
     */
    public static boolean isSafeResult(Map<Item, Integer> beforeTotals,
                                       Map<Item, Integer> beforeMaxCounts,
                                       List<ItemStack> result) {
        if (!preserved(beforeTotals, result)) {
            return false;
        }
        for (ItemStack stack : result) {
            if (stack.isEmpty()) {
                continue;
            }
            int allowed = Math.max(stack.getMaxStackSize(),
                    beforeMaxCounts.getOrDefault(stack.getItem(), 0));
            if (stack.getCount() > allowed) {
                return false;
            }
        }
        return true;
    }
}
