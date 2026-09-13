package com.sighs.sophisticatedsorter.network;

import com.sighs.sophisticatedsorter.common.TransferRequest;
import com.sighs.sophisticatedsorter.platform.FabricSorterCommands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class ServerTransferPacket implements Packet{
    private final boolean transferToContainer;
    private final boolean filter;
    private final boolean mainInventoryFirst;

    public ServerTransferPacket(boolean transferToContainer, boolean filter, boolean mainInventoryFirst) {
        this.transferToContainer = transferToContainer;
        this.filter = filter;
        this.mainInventoryFirst = mainInventoryFirst;
    }

    public ServerTransferPacket(TransferRequest request) {
        this(request.toContainer(), request.filterByDestination(), request.mainInventoryFirst());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(transferToContainer);
        buf.writeBoolean(filter);
        buf.writeBoolean(mainInventoryFirst);
    }

    public static ServerTransferPacket decode(FriendlyByteBuf buf) {
        return new ServerTransferPacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public void handle(ServerPlayer player) {
        if (player != null) {
            FabricSorterCommands.INSTANCE.transfer(player,
                    new TransferRequest(transferToContainer, filter, mainInventoryFirst));
        }
    }
}
