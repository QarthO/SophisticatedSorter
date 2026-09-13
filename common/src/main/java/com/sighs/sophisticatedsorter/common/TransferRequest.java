package com.sighs.sophisticatedsorter.common;

/** Loader-neutral transfer command. */
public final class TransferRequest {
    private final boolean toContainer;
    private final boolean filterByDestination;
    private final boolean mainInventoryFirst;

    public TransferRequest(boolean toContainer, boolean filterByDestination) {
        this(toContainer, filterByDestination, true);
    }

    public TransferRequest(boolean toContainer, boolean filterByDestination, boolean mainInventoryFirst) {
        this.toContainer = toContainer;
        this.filterByDestination = filterByDestination;
        this.mainInventoryFirst = mainInventoryFirst;
    }

    public boolean toContainer() { return toContainer; }

    public boolean filterByDestination() { return filterByDestination; }

    /**
     * When transferring into the player inventory, whether the 27-slot main inventory is filled
     * before the 9-slot hotbar ({@code true}, matching Sophisticated Core's own transfer) or the
     * hotbar is filled first ({@code false}, vanilla {@code quickMoveStack} behavior).
     */
    public boolean mainInventoryFirst() { return mainInventoryFirst; }
}
