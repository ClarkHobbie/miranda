package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.MessageLog;
import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.session.IoSession;

import java.io.IOException;
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

    public Node (UUID myUuid, String host, int myPort) {
        this.uuid = myUuid;
        this.host = host;
        this.port = myPort;
    }

    public Node (String host, int port) {
        this.host = host;
        this.port = port;
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
     * The IoSession that we talk to our partner with or null if we are not currently connected
     */
    protected IoSession ioSession = null;

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
     * Are we connected?
     */
    protected boolean connected = false;

    /**
     * The hostname or IP address of our partner node
     */
    protected String host = null;

    /**
     * The port number that our partner is waiting for connections on
     */
    protected int port = -1;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }


    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
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
    public synchronized boolean auctionMessage (Message message) throws InterruptedException, IOException {
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
            MessageLog.getInstance().setOwner(message.getMessageID(), uuid);
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
                MessageLog.getInstance().setOwner (uuidOfMessage, uuid);
                returnValue = true;
            } else if (myBid == theirBid) {
                logger.debug("a tie, reissuing bid");
                returnValue = auctionMessage(message);
            } else {
                returnValue = false;
                logger.debug("lost bid, turning over message to the other node");
                clusterHandler.undefineMessage(message,partnerID);
                MessageLog.getInstance().setOwner(uuidOfMessage, partnerID);
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
    }

}
