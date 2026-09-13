package com.sighs.sophisticatedsorter.utils;

import com.sighs.sophisticatedsorter.common.PinyinOrdering;
import com.sighs.sophisticatedsorter.common.SortBackend;
import com.sighs.sophisticatedsorter.common.SortComparatorProvider;
import com.sighs.sophisticatedsorter.common.SortComparatorSelection;
import com.sighs.sophisticatedsorter.common.SortCriterion;
import com.sighs.sophisticatedsorter.common.SortRequest;
import com.sighs.sophisticatedsorter.common.SortSlot;
import com.sighs.sophisticatedsorter.common.SorterService;
import com.sighs.sophisticatedsorter.common.TransferRequest;
import com.sighs.sophisticatedsorter.mixin.AbstractContainerMenuAccessor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.InventorySorter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Minecraft-facing facade backed by the loader-neutral sorter service. */
public final class CoreUtils {
    public static final Comparator<Map.Entry<ItemStackKey, Integer>> BY_PINYIN =
            PinyinOrdering.<Map.Entry<ItemStackKey, Integer>>byName(
                    entry -> entry.getKey().getStack().getHoverName().getString());
    private static final SortComparatorProvider<Map.Entry<ItemStackKey, Integer>> COMPARATORS =
            SortComparatorProvider.of(InventorySorter.BY_NAME, InventorySorter.BY_MOD,
                    InventorySorter.BY_COUNT, InventorySorter.BY_TAGS, BY_PINYIN);

    private static volatile SortPlatform platform;
    private static volatile SorterService<Player, ItemStack, Item> sorter;

    private CoreUtils() {
    }

    public static void installPlatform(SortPlatform sortPlatform) {
        if (sortPlatform == null) {
            throw new IllegalArgumentException("sortPlatform");
        }
        platform = sortPlatform;
        sorter = new SorterService<Player, ItemStack, Item>(new MinecraftSortBackend());
    }

    public static Comparator<Map.Entry<ItemStackKey, Integer>> getComparator(SortBy sortBy, boolean pinyin) {
        SortCriterion criterion = sortBy == null
                ? SortCriterion.NAME
                : SortCriterion.fromWireName(sortBy.getSerializedName());
        return SortComparatorSelection.select(criterion, pinyin, COMPARATORS);
    }

    public static boolean isSlotInvalid(Slot slot) {
        return !slot.mayPlace(new ItemStack(Items.BARRIER)) || slot instanceof ResultSlot;
    }

    public static void executeSort(Player player, SortRequest request) {
        if (isSophisticatedMenu(player)) {
            return;
        }
        sorter().sort(player, request);
    }

    public static void executeTransfer(Player player, TransferRequest request) {
        if (isSophisticatedMenu(player)) {
            return;
        }
        sorter().transfer(player, request);
    }

    /**
     * 精妙背包 / 精妙储存的容器菜单由精妙自己管理排序，且支持高堆叠（堆叠升级）；
     * 本模组的通用排序会把高堆叠压回原版一组，格数不足时还会丢失溢出物品，因此必须完全跳过。
     * 这是服务端纵深防御：即使客户端（版本差异或第三方客户端）未拦截，服务端也不会误排精妙容器。
     */
    private static boolean isSophisticatedMenu(Player player) {
        AbstractContainerMenu menu = player.containerMenu;
        return menu instanceof StorageContainerMenuBase || menu instanceof SettingsContainerMenu;
    }

    private static SorterService<Player, ItemStack, Item> sorter() {
        SorterService<Player, ItemStack, Item> current = sorter;
        if (current == null) {
            throw new IllegalStateException("CoreUtils platform has not been installed");
        }
        return current;
    }

    private static final class MinecraftSortBackend implements SortBackend<Player, ItemStack, Item> {
        @Override
        public List<? extends SortSlot<ItemStack, Item>> containerSlots(Player player) {
            AbstractContainerMenu menu = player.containerMenu;
            Set<Slot> quickcraftSlots = ((AbstractContainerMenuAccessor) menu)
                    .sophisticatedSorter$getQuickcraftSlots();
            List<SortSlot<ItemStack, Item>> slots = new ArrayList<SortSlot<ItemStack, Item>>(menu.slots.size());
            for (Slot slot : menu.slots) {
                slots.add(new MinecraftSortSlot(slot, quickcraftSlots.contains(slot)));
            }
            return slots;
        }

        @Override
        public int inventorySize(Player player) {
            return player.getInventory().items.size();
        }

        @Override
        public ItemStack inventoryStack(Player player, int slotIndex) {
            return player.getInventory().items.get(slotIndex);
        }

        @Override
        public void setInventoryStack(Player player, int slotIndex, ItemStack stack) {
            player.getInventory().setItem(slotIndex, stack);
        }

        @Override
        public void sortStacks(List<ItemStack> stacks, SortCriterion criterion, boolean pinyinOrder) {
            platform().sortStacks(stacks,
                    SortComparatorSelection.select(criterion, pinyinOrder, COMPARATORS));
        }

        @Override
        public void quickMove(Player player, int slotIndex) {
            player.containerMenu.quickMoveStack(player, slotIndex);
        }

        @Override
        public boolean moveIntoPlayerInventory(Player player, int slotIndex, boolean mainInventoryFirst) {
            AbstractContainerMenu menu = player.containerMenu;
            Slot source = menu.slots.get(slotIndex);
            if (source.getItem().isEmpty()) {
                return false;
            }
            int playerStart = playerInventoryBlockStart(menu, player.getInventory());
            if (playerStart < 0) {
                // Unrecognized layout: fall back to vanilla quick move (hotbar first).
                quickMove(player, slotIndex);
                return true;
            }
            // The player block is laid out [27 main inventory][9 hotbar], so reverse=false fills the
            // main inventory first and reverse=true fills the hotbar first. Vanilla quickMoveStack
            // uses the latter; Core's own transfer fills the main inventory first.
            ItemStack stack = source.getItem();
            boolean moved = ((AbstractContainerMenuAccessor) menu).sophisticatedSorter$moveItemStackTo(
                    stack, playerStart, menu.slots.size(), !mainInventoryFirst);
            if (stack.isEmpty()) {
                source.setByPlayer(ItemStack.EMPTY);
            } else if (moved) {
                source.setChanged();
            }
            return moved;
        }

        @Override
        public void broadcastChanges(Player player) {
            player.containerMenu.broadcastChanges();
        }
    }

    private static SortPlatform platform() {
        SortPlatform current = platform;
        if (current == null) {
            throw new IllegalStateException("CoreUtils platform has not been installed");
        }
        return current;
    }

    /**
     * Index of the menu's trailing 36-slot player block, or -1 when the menu does not expose the
     * standard layout. The block must be the last 36 slots, all backed by the player inventory, with
     * the 27 main-inventory slots (container index 9-35) first and the 9 hotbar slots (0-8) last -
     * the order the transfer order flag relies on.
     */
    private static int playerInventoryBlockStart(AbstractContainerMenu menu, Inventory inventory) {
        int size = menu.slots.size();
        if (size < 36) {
            return -1;
        }
        int start = size - 36;
        for (int i = 0; i < 36; i++) {
            Slot slot = menu.slots.get(start + i);
            if (slot.container != inventory) {
                return -1;
            }
            int containerSlot = slot.getContainerSlot();
            boolean mainSlot = containerSlot >= 9 && containerSlot <= 35;
            boolean hotbarSlot = containerSlot >= 0 && containerSlot <= 8;
            if (i < 27 ? !mainSlot : !hotbarSlot) {
                return -1;
            }
        }
        return start;
    }

    private static final class MinecraftSortSlot implements SortSlot<ItemStack, Item> {
        private final Slot slot;
        private final boolean quickcraftSlot;

        private MinecraftSortSlot(Slot slot, boolean quickcraftSlot) {
            this.slot = slot;
            this.quickcraftSlot = quickcraftSlot;
        }

        @Override
        public int index() {
            return slot.index;
        }

        @Override
        public boolean isQuickcraftSlot() {
            return quickcraftSlot;
        }

        @Override
        public boolean isInvalid() {
            return CoreUtils.isSlotInvalid(slot);
        }

        @Override
        public boolean isPlayerInventorySlot() {
            return slot.container instanceof Inventory;
        }

        @Override
        public ItemStack stack() {
            return slot.getItem();
        }

        @Override
        public void setStack(ItemStack stack) {
            slot.set(stack);
        }

        @Override
        public Item item() {
            return slot.getItem().getItem();
        }
    }

}
