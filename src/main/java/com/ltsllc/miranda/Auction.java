package com.ltsllc.miranda;

import java.util.List;
import java.util.UUID;

/**
 * A class that "remembers" all the details of an auction
 */
public class Auction {
    public UUID uuid; // the uuid of the node being auctioned
    public List<UUID> messages; // a list of the messages owned by the node being auctioned
    public int index; // an index into the list of messages of the next message to be auctioned

    public Auction (UUID uuid) {
        this.uuid = uuid;
        this.messages = MessageLog.getInstance().getAllMessagesOwnedBy (uuid);
        this.index = 0;
    }
}
