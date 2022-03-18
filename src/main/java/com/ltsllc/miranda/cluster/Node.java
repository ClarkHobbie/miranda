package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.session.IoSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A node in the cluster
 */
public class Node {
    public static final Logger logger = LogManager.getLogger();


    public Node (UUID myUUID, UUID partnerID, IoSession ioSession){
        this.ioSession = ioSession;
        this.uuid = myUUID;
        this.partnerID = partnerID;
    }

    public Node () {
    }


    /**
     * for bids
     */
    protected static ImprovedRandom ourRandom = new ImprovedRandom();

    public static ImprovedRandom getOurRandom() {
        return ourRandom;
    }

    public static void setOurRandom(ImprovedRandom ourRandom) {
        Node.ourRandom = ourRandom;
    }

    /**
     * The IoSession for the node
     */
    protected IoSession ioSession;

    public IoSession getIoSession() {
        return ioSession;
    }

    public void setIoSession(IoSession ioSession) {
        this.ioSession = ioSession;
    }

    /**
     * The ClusterHandler for the node
     */
    protected ClusterHandler clusterHandler;

    public ClusterHandler getClusterHandler() {
        return clusterHandler;
    }

    public void setClusterHandler(ClusterHandler clusterHandler) {
        this.clusterHandler = clusterHandler;
    }

    /**
     * id of this node
     */
    protected UUID uuid;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * The UUID of the node on the other side of the ioSession
     */
    protected UUID partnerID;

    public UUID getPartnerID() {
        return partnerID;
    }

    public void setPartnerID(UUID partnerID) {
        this.partnerID = partnerID;
    }

    /**
     * Map of owners
     */
    protected static Map<UUID, UUID> uuidToOwner = new HashMap ();

    public static Map<UUID, UUID> getUuidToOwner() {
        return uuidToOwner;
    }

    public static void setUuidToOwner(Map<UUID, UUID> uuidToOwner) {
        Node.uuidToOwner = uuidToOwner;
    }

    /**
     * Are we connected?
     */
    protected boolean connected = false;

    /**
     * The IP address of the node
     */
    protected String ipAddr = null;

    protected int port = -1;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public static UUID getOwnerFor (UUID messageID) {
        UUID returnValue = null;
        returnValue = uuidToOwner.get(messageID);
        return returnValue;
    }

    public static void setOwnerFor (UUID messageID, UUID owner) {
        uuidToOwner.put(messageID, owner);
    }

    /**
     * Tell the node that we are starting an auction.
     *
     * This method returns after the node acknowledges that we are starting an auction.
     *
     * @param uuid The node whose messages we are going to auction.
     */
    public void informOfStartOfAuction (UUID uuid) {
        logger.debug("entering informOfStartOfAuction with uuid = " + uuid);
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append(ClusterHandler.AUCTION);
        stringBuffer.append(" ");
        stringBuffer.append(uuid);

        ioSession.write(stringBuffer.toString());
        logger.debug("wrote " + stringBuffer.toString());
        logger.debug("leaving informOfStartOfAuction");
    }

    public void informOfAuctionEnd () {
        ioSession.write(ClusterHandler.AUCTION_OVER);
    }

    /**
     * Tell the node that we are ending an auction
     *
     * This method returns after the node has
     */
    public void informOfEndOfAuction () {
        logger.debug("entering informOfEndOfAuction");
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append(ClusterHandler.AUCTION_OVER);
        ioSession.write(stringBuffer.toString());
        logger.debug("wrote "  + stringBuffer.toString());
        logger.debug("leaving informOfEndOfAuction");
    }

    /**
     * Auction a message with this node
     *
     * This method takes care of sending out a bid for the message and takes care of the response
     *
     * @param message The message to be auctioned.
     * @return True if we "won" the auction false otherwise
     */
    public synchronized boolean auctionMessage (Message message) throws InterruptedException {
        logger.debug("entering auctionMessage with " + message);

        boolean returnValue = true;
        int myBid = ourRandom.nextInt();

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ClusterHandler.BID);
        stringBuffer.append(" ");
        stringBuffer.append(message.getMessageID());
        stringBuffer.append(" ");
        stringBuffer.append(myBid);

        ioSession.write(stringBuffer.toString());
        logger.debug("wrote " + stringBuffer.toString());

        ReadFuture readFuture = ioSession.read();
        long timeout = Miranda.getProperties().getLongProperty(Miranda.PROPERTY_BID_TIMEOUT);
        logger.debug("waiting " + timeout + " milliseconds for reply");
        String reply = null;

        if (readFuture.await(timeout, TimeUnit.MILLISECONDS)) {
            logger.debug("timed out waiting for reply, keeping the message, sending timeout and setting the state to start");
            ioSession.write(ClusterHandler.TIMEOUT);
            clusterHandler.state = ClusterConnectionStates.START;
            uuidToOwner.put(message.getMessageID(), uuid);
            returnValue = true;
        } else {
            reply = (String) readFuture.getMessage();
            logger.debug("got a reply before the timeout, reply = " + reply);
            Scanner scanner = new Scanner((String) readFuture.getMessage());
            scanner.skip("BID");
            UUID uuidOfMessage = UUID.fromString(scanner.next());
            int theirBid = Integer.parseInt(scanner.next());

            logger.debug("myBid = " + myBid + " theirBid = " + theirBid);
            if (myBid > theirBid) {
                logger.debug("won the bid, assigning the message to us");
                uuidToOwner.put (uuidOfMessage, uuid);
                returnValue = true;
            } else if (myBid == theirBid) {
                logger.debug("a tie, reissuing bid");
                returnValue = auctionMessage(message);
            } else {
                returnValue = false;
                logger.debug("lost bid, turning over message to the other node");
                clusterHandler.undefineMessage(message,partnerID);
                uuidToOwner.put(uuidOfMessage, partnerID);
            }
        }

        logger.debug("leaving auctionMessage, returnValue = " + returnValue);
        return returnValue;
    }

    /**
     * Send a message to the node informing it that we created a message
     *
     * This is so that if we go down, someone has a copy of the message.
     *
     * @param message The message that we created.
     */
    public void informOfMessageCreation (Message message) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ClusterHandler.NEW_MESSAGE);
        stringBuffer.append(" ");
        stringBuffer.append(message.internalsToString());

        ioSession.write(stringBuffer.toString());

        setOwnerFor(message.getMessageID(), uuid);
    }

    /**
     * Inform the partner node that we delivered this message
     *
     * This is so that if we crash, the partner node knows that the message does not need to be delivered.
     *
     * @param message The message that we delivered.
     */
    public void informOfMessageDelivery (Message message) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ClusterHandler.MESSAGE_DELIVERED);
        stringBuffer.append (" ");
        stringBuffer.append(message.getMessageID());

        ioSession.write(stringBuffer.toString());

        setOwnerFor(message.getMessageID(), null);

    }

}
