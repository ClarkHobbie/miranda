package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.commons.util.Utils;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.MessageLog;
import com.ltsllc.miranda.MessageType;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.logging.LoggingCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.session.IoSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.ltsllc.miranda.cluster.ClusterConnectionStates.GENERAL;
import static com.ltsllc.miranda.cluster.ClusterConnectionStates.SYNCHRONIZING;

/**
 * A node in the cluster
 */
public class Node implements Cloneable {
    public static final Logger logger = LogManager.getLogger();
    public static final Logger events = LogManager.getLogger("events");

    /*
     * message name constants
     */
    public static final String AUCTION = "AUCTION";
    public static final String AUCTION_OVER = "AUCTION OVER";
    public static final String BID = "BID";
    public static final String DEAD_NODE = "DEAD NODE";
    public static final String ERROR = "ERROR";
    public static final String ERROR_START = "ERROR_START";
    public static final String GET_MESSAGE = "GET MESSAGE";
    public static final String HEART_BEAT_START = "HEART BEAT START";
    public static final String HEART_BEAT = "HEART BEAT";
    public static final String MESSAGE = "MESSAGE";
    public static final String MESSAGES = "MESSAGES";
    public static final String MESSAGES_END = "MESSAGES END";
    public static final String MESSAGE_DELIVERED = "MESSAGE DELIVERED";
    public static final String MESSAGE_NOT_FOUND = "MESSAGE NOT FOUND";
    public static final String NEW_MESSAGE = "MESSAGE CREATED";
    public static final String NEW_NODE = "NEW NODE";
    public static final String NEW_NODE_CONFIRMED = "NEW NODE CONFIRMED";
    public static final String NEW_NODE_OVER = "NEW NODE OVER";
    public static final String OWNER = "OWNER";
    public static final String OWNERS = "OWNERS";
    public static final String OWNERS_END = "OWNERS END";
    public static final String START = "START";
    public static final String START_ACKNOWLEDGED = "START ACKNOWLEDGED";
    public static final String SYNCHRONIZE = "SYNCHRONIZE";
    public static final String SYNCHRONIZE_START = "SYNCHRONIZE START";
    public static final String TIMEOUT = "TIMEOUT";

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
     * Used for heart beats
     */
    protected Timer timer = new Timer();

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
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
     * The hostname or IP address of our partner node
     */
    protected String host = null;

    /**
     * The port number that our partner is waiting for connections on
     */
    protected int port = -1;

    protected LoggingCache cache;

    public LoggingCache getCache() {
        return cache;
    }

    public void setCache(LoggingCache cache) {
        this.cache = cache;
    }


    /**
     * The state of the connection
     */
    protected ClusterConnectionStates state = ClusterConnectionStates.START;

    public ClusterConnectionStates getState() {
        return state;
    }

    public void setState(ClusterConnectionStates state) {
        this.state = state;
    }

    /**
     * When the last time we sent anything or received anything
     */
    protected Long timeOfLastActivity = System.currentTimeMillis();

    public Long getTimeOfLastActivity() {
        return timeOfLastActivity;
    }

    public void setTimeOfLastActivity(Long timeOfLastActivity) {
        this.timeOfLastActivity = timeOfLastActivity;
    }

    /**
     * The heart beat associated with the node
     */
    protected HeartBeatTimerTask heartBeatTimerTask = null;

    public HeartBeatTimerTask getHeartBeatTimerTask() {
        return heartBeatTimerTask;
    }

    public void setHeartBeatTimerTask(HeartBeatTimerTask heartBeatTimerTask) {
        this.heartBeatTimerTask = heartBeatTimerTask;
    }

    /**
     * Do we already have a heart beat?
     */
    protected boolean alreadyHaveAHeartBeat = false;

    public boolean isAlreadyHaveAHeartBeat() {
        return alreadyHaveAHeartBeat;
    }

    public void setAlreadyHaveAHeartBeat(boolean alreadyHaveAHeartBeat) {
        this.alreadyHaveAHeartBeat = alreadyHaveAHeartBeat;
    }

    /**
     * Have we already cancelled the heart beat?
     */
    protected boolean alreadyCancelledHeartBeat = false;

    public boolean isAlreadyCancelledHeartBeat() {
        return alreadyCancelledHeartBeat;
    }

    public void setAlreadyCancelledHeartBeat(boolean alreadyCancelledHeartBeat) {
        this.alreadyCancelledHeartBeat = alreadyCancelledHeartBeat;
    }

    protected Stack<ClusterConnectionStates> stateStack = new Stack<>();

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

    /**
     * Are we connected?
     */
    protected boolean connected = false;

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

        stringBuffer.append(AUCTION);
        stringBuffer.append(" ");
        stringBuffer.append(uuid);

        ioSession.write(stringBuffer.toString());
        logger.debug("wrote " + stringBuffer.toString());
        logger.debug("leaving informOfStartOfAuction");
    }

    public void informOfAuctionEnd () {
        ioSession.write(Node.AUCTION_OVER);
    }

    /**
     * Tell the node that we are ending an auction
     *
     * This method returns after the node has
     */
    public void informOfEndOfAuction () {
        logger.debug("entering informOfEndOfAuction");
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append(Node.AUCTION_OVER);
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
        stringBuffer.append(Node.BID);
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
            ioSession.write(Node.TIMEOUT);
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
                undefineMessage(message);
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
        stringBuffer.append(Node.NEW_MESSAGE);
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
        stringBuffer.append(Node.MESSAGE_DELIVERED);
        stringBuffer.append (" ");
        stringBuffer.append(message.getMessageID());

        ioSession.write(stringBuffer.toString());
    }

    public void messageReceived (IoSession ioSession, Object o) throws IOException, LtsllcException, CloneNotSupportedException {
        setTimeOfLastActivity();
        logger.debug("entering messageReceived with state = " + state + " and message = " + o);
        String s = (String) o;
        s = s.toUpperCase();

        MessageType messageType = determineMessageType(s);

        switch (state) {
            case START: {
                switch (messageType) {
                    case START: {
                        handleStart(s, ioSession);
                        break;
                    }

                    case START_ACKNOWLEDGED:
                        state = GENERAL;
                        break;

                    case SYNCHRONIZE_START: {
                        handleSynchronizeStart(s,ioSession);
                        break;
                    }

                    case SYNCHRONIZE: {
                        handleSynchronize(s,ioSession);
                        state = SYNCHRONIZING;
                        break;
                    }

                    case HEART_BEAT_START: {
                        handleHeartBeatStart(ioSession);
                        break;
                    }

                    case HEART_BEAT: {
                        break;
                    }

                    case ERROR_START: {
                        handleErrorStart(ioSession);
                        break;
                    }

                    case ERROR: {
                        break;
                    }

                    default: {
                        handleError(ioSession);
                        break;
                    }
                }
                break;
            }

            case AUCTION: {
                switch (messageType) {
                    case BID: {
                        handleBid(s, ioSession, partnerID);
                        break;
                    }

                    case GET_MESSAGE: {
                        handleGetMessageAuction(s, ioSession);
                        break;
                    }

                    case AUCTION_OVER: {
                        state = ClusterConnectionStates.GENERAL;
                        ioSession.write(Node.AUCTION_OVER);
                        setTimeOfLastActivity();
                        break;
                    }

                    case ERROR_START: {
                        handleErrorStart(ioSession);
                        break;
                    }

                    case ERROR: {
                        break;
                    }

                    default: {
                        handleError(ioSession);
                        break;
                    }
                }
                break;
            }

            case MESSAGE: {
                switch (messageType) {
                    case MESSAGE: {
                        handleMessage(s, ioSession);
                        break;
                    }

                    case MESSAGE_NOT_FOUND: {
                        state = popState();
                        break;
                    }

                    case HEART_BEAT_START: {
                        handleHeartBeatStart (ioSession);
                        break;
                    }

                    case HEART_BEAT: {
                        break;
                    }

                    case ERROR_START: {
                        handleErrorStart(ioSession);
                        break;
                    }

                    case ERROR: {
                        break;
                    }

                    default: {
                        handleError(ioSession);
                        logger.error("protocol error, returning to START state");
                        break;
                    }
                }
                break;
            }


            case GENERAL: {
                switch (messageType) {
                    case AUCTION: {
                        ioSession.write(AUCTION);
                        setTimeOfLastActivity();
                        state = ClusterConnectionStates.AUCTION;
                        break;
                    }

                    case DEAD_NODE: {
                        ioSession.write(s);
                        setTimeOfLastActivity();
                        break;
                    }

                    case GET_MESSAGE: {
                        handleGetMessage(s, ioSession);
                        break;
                    }

                    case HEART_BEAT_START: {
                        ioSession.write(HEART_BEAT);
                        setTimeOfLastActivity();
                        break;
                    }

                    case HEART_BEAT: {
                        break;
                    }

                    case MESSAGE_DELIVERED: {
                        handleMessageDelivered(s);
                        break;
                    }

                    case NEW_MESSAGE: {
                        handleNewMessage(s, partnerID);
                        break;
                    }

                    case ERROR_START: {
                        handleErrorStart(ioSession);
                        break;
                    }

                    case ERROR: {
                        state = ClusterConnectionStates.START;
                        break;
                    }

                    case START: {
                        handleStartGeneral(s, ioSession);
                        break;
                    }

                    case START_ACKNOWLEDGED:
                        break;

                    default: {
                        handleError(ioSession);
                        break;
                    }
                }
                break;
            }

            case SYNCHRONIZING: {
                switch (messageType) {
                    case SYNCHRONIZE: {
                        handleSynchronize(s,ioSession);
                        break;
                    }

                    case OWNERS: {
                        sendAllOwners(ioSession);
                        break;
                    }

                    case OWNER: {
                        handleReceiveOwner(s);
                        break;
                    }

                    case OWNERS_END: {
                        sendMessages(ioSession);
                        break;
                    }

                    case MESSAGE: {
                        handleReceiveMessage(s);
                        break;
                    }

                    case MESSAGES: {
                        sendAllMessages(ioSession);
                        sendStart(ioSession);
                        state = ClusterConnectionStates.START;
                        break;
                    }

                    case MESSAGES_END: {
                        sendStart(ioSession);
                        state = ClusterConnectionStates.START;
                        break;
                    }

                    case HEART_BEAT_START: {
                        handleHeartBeatStart(ioSession);
                        break;
                    }

                    case ERROR_START: {
                        handleErrorStart(ioSession);
                        break;
                    }

                    case ERROR: {
                        break;
                    }

                    default: {
                        handleError(ioSession);
                        break;
                    }
                }

                break;
            }

        }
        logger.debug("leaving messageReceived with state = " + state);
    }
    /**
     * Convert a string to a MessageType
     * <p>
     * NOTE: the method uses the various constants like AUCTION to identify the input.
     *
     * @param s The string to convert.  This is assumed to be in upper case.
     * @return The MessageType that the string represents or MessageType.UNKNOWN if the method doesn't recognize it.
     */
    protected MessageType determineMessageType(String s) {
        logger.debug("entering determineMessageType with " + s);
        MessageType messageType = MessageType.UNKNOWN;

        if (s.startsWith(AUCTION_OVER)) {
            messageType = MessageType.AUCTION_OVER;
        } else if (s.startsWith(AUCTION)) {
            messageType = MessageType.AUCTION;
        } else if (s.startsWith(BID)) {
            messageType = MessageType.BID;
        } else if (s.startsWith(DEAD_NODE)) {
            messageType = MessageType.DEAD_NODE;
        } else if (s.startsWith(ERROR_START)) {
            messageType = MessageType.ERROR_START;
        } else if (s.startsWith(ERROR)) {
            messageType = MessageType.ERROR;
        } else if (s.startsWith(GET_MESSAGE)) {
            messageType = MessageType.GET_MESSAGE;
        } else if (s.startsWith(HEART_BEAT_START)) {
            messageType = MessageType.HEART_BEAT_START;
        } else if (s.startsWith(HEART_BEAT)) {
            messageType = MessageType.HEART_BEAT;
        } else if (s.startsWith(MESSAGE_DELIVERED)) {
            messageType = MessageType.MESSAGE_DELIVERED;
        } else if (s.startsWith(NEW_MESSAGE)) {
            messageType = MessageType.NEW_MESSAGE;
        } else if (s.startsWith(MESSAGES_END)) {
            messageType = MessageType.MESSAGES_END;
        } else if (s.startsWith(MESSAGES)) {
            messageType = MessageType.MESSAGES;
        } else if (s.startsWith(MESSAGE_NOT_FOUND)) {
            messageType = MessageType.MESSAGE_NOT_FOUND;
        } else if (s.startsWith(MESSAGE)) {
            messageType = MessageType.MESSAGE;
        } else if (s.startsWith(NEW_NODE_CONFIRMED)) {
            messageType = MessageType.NEW_NODE_CONFIRMED;
        } else if (s.startsWith(NEW_NODE_OVER)) {
            messageType = MessageType.NEW_NODE_OVER;
        } else if (s.startsWith(NEW_NODE)) {
            messageType = MessageType.NEW_NODE;
        } else if (s.startsWith(OWNERS_END)) {
            messageType = MessageType.OWNERS_END;
        } else if (s.startsWith(OWNERS)) {
            messageType = MessageType.OWNERS;
        } else if (s.startsWith(OWNER)) {
            messageType = MessageType.OWNER;
        } else if (s.startsWith(START_ACKNOWLEDGED)) {
            messageType = MessageType.START_ACKNOWLEDGED;
        } else if (s.startsWith(START)) {
            messageType = MessageType.START;
        } else if (s.startsWith(SYNCHRONIZE_START)) {
            messageType = MessageType.SYNCHRONIZE_START;
        } else if (s.startsWith(SYNCHRONIZE)) {
            messageType = MessageType.SYNCHRONIZE;
        } else if (s.startsWith(TIMEOUT)) {
            messageType = MessageType.TIMEOUT;
        }

        logger.debug("leaving determineMessageType with messageType = " + messageType);

        return messageType;
    }

    public void setTimeOfLastActivity () {
        timeOfLastActivity = System.currentTimeMillis();
    }

    /**
     * Handle a start message.
     * <p>
     * This method sets the partnerID and the state by side effect.  The response to this is to send a start of our own,
     * with our UUID and to go into the general state.
     *
     * @param input     The string that came to us.
     * @param ioSession The IoSession associated with this connection.
     */
    protected synchronized void handleStart(String input, IoSession ioSession) throws LtsllcException, CloneNotSupportedException {
        logger.debug("entering handleStart");
        Scanner scanner = new Scanner(input);
        scanner.skip(START);
        partnerID = UUID.fromString(scanner.next());

        host = scanner.next();

        port = Integer.parseInt(scanner.next());

        ioSession.getConfig().setUseReadOperation(true);
        connected = true;
        ioSession.write(Node.START_ACKNOWLEDGED);
        sendStart(ioSession);

        Cluster.getInstance().coalesce();

        logger.debug("leaving handleStart");
    }

    public void sendStart(IoSession ioSession) {
        logger.debug("entering sendStart");
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(START);
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyUuid());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyHost());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyPort());

        ioSession.write(stringBuffer.toString());
        logger.debug("wrote " + stringBuffer.toString());
        setTimeOfLastActivity();
        logger.debug("leaving sendStart");
    }

    public void handleSynchronizeStart (String input,IoSession ioSession) {
        state = SYNCHRONIZING;

        Scanner scanner = new Scanner(input);
        scanner.next();scanner.next(); // SYNCHRONIZE START
        UUID partnerID = UUID.fromString(scanner.next());
        String host = scanner.next();
        int port = scanner.nextInt();
        this.partnerID = partnerID;
        this.host = host;
        this.port = port;

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(SYNCHRONIZE);
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyUuid());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyHost());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyPort());
        ioSession.write(stringBuffer.toString());
        setTimeOfLastActivity();

        logger.debug("wrote " + stringBuffer);
    }

    public void handleHeartBeatStart(IoSession ioSession) {
        logger.debug("entering handleHeartBeatStart");
        ioSession.write(HEART_BEAT);
        setTimeOfLastActivity();
        logger.debug("leaving handleHeartBeatStart");
    }

    public void handleErrorStart (IoSession ioSession) {
        logger.debug("entering handleErrorStart");

        ioSession.write(ERROR);

        sendStart(ioSession);
        state = ClusterConnectionStates.START;

        logger.debug("leaving handleErrorStart");
    }


    /**
     * Handle a bid
     * <p>
     * This method takes care of a bid.  It reads in the message UUID and bid and sends out a bid of it's own.
     *
     * @param input     A string containing a bid message.
     * @param ioSession The IoSession to respond to.
     * @param partnerID The uuid of the other node for this ioSession
     */
    protected void handleBid(String input, IoSession ioSession, UUID partnerID) throws IOException {
        logger.debug("entering handleBid with input = " + input + " and ioSession = " + ioSession + " and partnerID = " + partnerID);
        Scanner scanner = new Scanner(input);
        scanner.skip(BID);
        UUID messageID = UUID.fromString(scanner.next());
        int theirBid = Integer.parseInt(scanner.next());
        int myBid = ourRandom.nextInt();
        while (myBid == theirBid) {
            logger.debug("a tie");
            myBid = ourRandom.nextInt();
        }
        logger.debug("myBid = " + myBid + " theirBid = " + theirBid);

        String strMessage = BID;
        strMessage += " ";
        strMessage += messageID;
        strMessage += " ";
        strMessage += myBid;
        ioSession.write(strMessage);
        setTimeOfLastActivity();
        logger.debug("wrote " + strMessage);

        UUID owner = null;
        //
        // if we lost the bid, then set the owner to the partner
        //
        if (myBid < theirBid) {
            owner = partnerID;
        } else {
            //
            // otherwise, we won the bid, set the owner to us
            //
            owner = uuid;

            //
            // watch out for the situation where we don't have the message
            //
            if (!cache.contains(messageID)) {
                logger.debug("cache miss");
                strMessage = GET_MESSAGE;
                strMessage += " ";
                strMessage += messageID;
                ioSession.write(strMessage);
                setTimeOfLastActivity();

                pushState(state);
                state = ClusterConnectionStates.MESSAGE;
            }
        }

        MessageLog.getInstance().setOwner(messageID, owner);
        logger.debug("leaving handleBid");
    }

    /**
     * "push" a state onto the state stack
     * <p>
     * This method does not set the state.
     *
     * @param state The state that the caller wants to push.
     */
    protected void pushState(ClusterConnectionStates state) {
        stateStack.push(state);
    }


    /**
     * Pop a state off the state stack.
     * <p>
     * NOTE: this method assumes that the stateStack is not empty.
     * </P>
     * <p>
     * This method does not set the state.
     * </P>
     *
     * @return A state
     * @throws LtsllcException This method throws this exception if the stateStack is empty.
     */
    protected ClusterConnectionStates popState() throws LtsllcException {
        if (stateStack.empty()) {
            logger.error("popState when the stateStack is empty");
            throw new LtsllcException("popState when the state is empty");
        }

        return stateStack.pop();
    }


    /**
     * Handle a get message message while in the auction state
     *
     * @param input     The message we received.
     * @param ioSession The IoSession over which we received it
     * @throws IOException If there is a problem sending the message
     */
    public synchronized void handleGetMessageAuction(String input, IoSession ioSession) throws IOException, LtsllcException {
        logger.debug("entering the handleGetMessageAuction method with input = " + input + " and ioSession = " + ioSession);
        Scanner scanner = new Scanner(input);
        scanner.skip(GET_MESSAGE);
        UUID uuid = UUID.fromString(scanner.next());

        if (cache.contains(uuid)) {
            logger.debug("cache hit");
            sendMessage(uuid, ioSession);
        } else {
            logger.error("in an auction where we were asked for a message we don't have: " + uuid);
            ioSession.write(MESSAGE_NOT_FOUND);
            setTimeOfLastActivity();
        }

        logger.debug("leaving handleGetMessageAuction");
    }

    /**
     * Send a Message over an IoSession
     * <p>
     * This method sends a com.ltsllc.miranda.Message over an IoSession.
     * </P>
     *
     * @param uuid      The Message to send.  NOTE: the uuid must exist in the message cache.
     * @param ioSession The IoSession over which to send it.
     */
    public void sendMessage(UUID uuid, IoSession ioSession) throws IOException, LtsllcException {
        Message message = cache.get(uuid);
        String strMessage = message.longToString();
        ioSession.write(strMessage);
        setTimeOfLastActivity();
    }

    /**
     * Handle a message message
     *
     * @param input     The message we received;
     * @param ioSession The ioSession over which we received the message.
     * @throws LtsllcException If there is a problem looking up the message.
     */
    public synchronized void handleMessage(String input, IoSession ioSession) throws LtsllcException, IOException {
        logger.debug("entering handleMessage with input = " + input + " and ioSession = " + ioSession);
        Message message = Message.readLongFormat(input);
        cache.add(message);

        state = popState();

        String strMessage = "got message with message ID: " + message.getMessageID();
        strMessage += ", and status URL: " + message.getStatusURL();
        strMessage += ", and delivery URL: " + message.getDeliveryURL();
        strMessage += ", and content: " + Utils.hexEncode(message.getContents());
        strMessage += ", and pushed state: " + state;
        logger.debug(strMessage);

        logger.debug("leaving handleMessage");
    }

    /**
     * Handle a get message message
     * <p>
     * This method takes care of reading in the message ID and sending back the requested message.
     *
     * @param input     A string containing the new message message.  Note that this method assumes that this string has
     *                  been capitalized.
     * @param ioSession The session over which to respond.
     * @throws IOException If there is a problem looking up the message in the cache.
     */
    protected void handleGetMessage(String input, IoSession ioSession) throws IOException, LtsllcException {
        logger.debug("entering handleGetMessage with input = " + input + " and ioSession = " + ioSession);
        Scanner scanner = new Scanner(input);
        scanner.skip(GET_MESSAGE);
        UUID uuid = UUID.fromString(scanner.next());
        if (!cache.contains(uuid)) {
            logger.debug("cache miss, sending MESSAGE NOT FOUND");
            ioSession.write(MESSAGE_NOT_FOUND);
            setTimeOfLastActivity();
        } else {
            Message message = cache.get(uuid);
            logger.debug("cache hit, message = " + message.longToString() + "; sending message");
            sendMessage(message, ioSession);
        }
        logger.debug("leaving handleGetMessage");
    }

    /**
     * Send a message message over an IoSession
     *
     * @param message   The message to send.
     * @param ioSession The IoSession to send it on.
     */
    protected void sendMessage(Message message, IoSession ioSession) {
        String strMessage = message.longToString();

        ioSession.write(strMessage);
        setTimeOfLastActivity();
    }

    /**
     * Handle an error by sending a start message
     *
     * <p>
     * This method first sends an error, and then a start message.
     * </P>
     *
     * @param ioSession The session we should send the start over.
     */
    public void handleError(IoSession ioSession) {
        logger.debug("entering handleError");

        ioSession.write(ERROR_START);
        logger.debug("wrote " + ERROR_START);
        state = ClusterConnectionStates.START;

        sendStart(ioSession);

        logger.debug("leaving handleError");
    }

    /**
     * Handle a message delivered message
     *
     * @param input The string we received.
     * @throws LtsllcException If the cache has problems removing the message
     */
    public synchronized void handleMessageDelivered(String input) throws LtsllcException, IOException {
        Scanner scanner = new Scanner(input);
        scanner.skip(MESSAGE_DELIVERED);
        String strUuid = scanner.next();
        UUID uuid = UUID.fromString(strUuid);
        cache.remove(uuid);
    }

    /**
     * Process the new message message
     * <p>
     * This method reads in the new message message and updates the cache
     * <p>
     * NOTE: java.util.Scanner has a bug where skip(&lt;pattern&gt;) will not find a pattern after first calling
     * skip(&lt;pattern&gt;) but next() will.  For that reason, the method uses next()
     * instead of skip(&lt;pattern&gt;).
     *
     * @param input       A string containing the message.
     * @param partnerUuid The UUID of our partner
     * @throws LtsllcException If there is a problem adding the message.
     */
    protected void handleNewMessage(String input, UUID partnerUuid) throws LtsllcException, IOException {
        logger.debug("entering handleNewMessage with " + input);

        Scanner scanner = new Scanner(input);
        scanner.next(); // MESSAGE
        scanner.next(); // CREATED

        Message message = Message.readLongFormat(scanner);

        MessageLog.getInstance().setOwner(message.getMessageID(), partnerUuid);
        MessageLog.getInstance().add(message, partnerID);

        logger.debug("leaving handleNewMessage");
    }


    /**
     * Send all the owner information in the system
     * <p>
     * This information is written as OWNER &lt;message UUID&gt; &lt;owner UUID&gt; to the ioChannel.
     * </P>
     *
     * @param ioSession The ioSession to write the owner information to.
     */
    public void handleSendOwners(IoSession ioSession) {
        logger.debug("entering handleSendOwners");
        Collection<UUID> ownerKeys = MessageLog.getInstance().getAllOwnerKeys();
        Iterator<UUID> iterator = ownerKeys.iterator();
        while (iterator.hasNext()) {
            UUID message = iterator.next();
            UUID owner = MessageLog.getInstance().getOwnerOf(message);

            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(OWNER);
            stringBuffer.append(" ");
            stringBuffer.append(message);
            stringBuffer.append(" ");
            stringBuffer.append(owner);
            stringBuffer.append(" ");

            ioSession.write(stringBuffer.toString());
            setTimeOfLastActivity();
        }
        ioSession.write(OWNERS_END);
        setTimeOfLastActivity();
        logger.debug("leaving handleSendOwners");
    }

    /**
     * Receive some owner information
     * <p>
     * The owner information is expected to be in the form "OWNER &lt;message UUID&gt; &lt;owner UUID&gt;."
     * </P>
     *
     * @param input
     * @throws IOException
     */
    public void handleReceiveOwner(String input) throws IOException {
        logger.debug("entering handleOwner");
        Scanner scanner = new Scanner(input);
        scanner.next(); // OWNER
        UUID message = UUID.fromString(scanner.next()); // message UUID
        UUID owner = UUID.fromString(scanner.next()); // owner UUID
        MessageLog.getInstance().setOwner(message, owner);
        logger.debug("leaving handelOwner");
    }

    /**
     * Send the messages message
     *
     * <P>
     *     This method does not send out any messages, it merely tells the node on the other side that we will be
     *     sending messages.  This is part of the synchronization process.
     * </P>
     */
    public void sendMessages(IoSession ioSession) {
        ioSession.write(MESSAGES);
        setTimeOfLastActivity();

        logger.debug("wrote " + MESSAGES);
    }

    public void sendAllMessages(IoSession ioSession) throws IOException {
        logger.debug("entering sendAllMessages");

        for (Message message : MessageLog.getInstance().copyAllMessages()) {
            String msg = message.longToString();

            ioSession.write(msg);
            setTimeOfLastActivity();
            logger.debug("wrote " + msg);
        }

        ioSession.write(MESSAGES_END);
        setTimeOfLastActivity();
        logger.debug("wrote " + MESSAGES_END);

        logger.debug("leaving sendMessages");
    }

    /**
     * Send all the messages that we have
     * <p>
     * The message information is in the form "MESSAGE ID: &lt;message UUID&gt; STATUS: &lt;message status URL&gt;
     * DELIVERY: &lt;message delivery URL&gt; CONTENT: &lt;message content as a hexadecimal string&gt;
     * </P>
     */
    public void handleSendMessages(IoSession ioSession) throws IOException {
        logger.debug("entering handleSendMessages");
        Collection<Message> messages = MessageLog.getInstance().copyAllMessages();
        Iterator<Message> iterator = messages.iterator();
        while (iterator.hasNext()) {
            ioSession.write(iterator.next().longToString() + " ");
            setTimeOfLastActivity();
        }
        ioSession.write(MESSAGES_END);
        setTimeOfLastActivity();
        logger.debug("leaving handleSendMessages");
    }

    /**
     * Receive a message message
     *
     * <P>
     *     This method expects a message, in long format, is in the input string.  The method responds by adding the
     *     message to the message log.
     * </P>
     *
     * @throws IOException If there is a problem adding the message to the message log.
     */
    public void handleReceiveMessage(String input) throws IOException {
        Message message = Message.readLongFormat(input);
        MessageLog.getInstance().add(message, null);
    }

    /**
     * The IoSession has been opened --- start synchronizing if the Miranda synchronization flag has been set.
     *
     * @param ioSession The newly opened session
     */
    public void sessionOpened(IoSession ioSession) {
        this.ioSession = ioSession;
        connected = true;

        if (Miranda.getInstance().getSynchronizationFlag()) {
            synchronize(ioSession);
        }
    }


    /**
     * Go into the synchronization state.
     *
     * <P>
     *     This starts the synchronization process by going into the synchronization state and sending a synchronization
     *     start message
     * </P>
     * @param ioSession The IoSession over which to send the synchronization message
     */
    public void synchronize(IoSession ioSession) {
        Miranda.getInstance().setSynchronizationFlag(false);

        sendSynchronizationStart(ioSession);
        state = SYNCHRONIZING;
    }

    /**
     * An exception was caught on the IoSession --- send an error then send a start message
     *
     * @param ioSession The IoSession for which the exception was caught.
     * @param throwable The exception.
     */
    public void exceptionCaught (IoSession ioSession, Throwable throwable) {
        logger.error("caught exception starting over", throwable);
        ioSession.write(ERROR_START);
        sendStart(ioSession);
    }

    /**
     * The IoSession for this node has closed --- remove it from the cluster.
     *
     * @param ioSession The closing IoSession.
     */
    public void sessionClosed(IoSession ioSession) {
        stopHeartBeat();
        connected = false;
        Cluster.getInstance().removeNode(this,ioSession);
    }

    /**
     * Stop sending heart beat messages.
     */
    public void stopHeartBeat () {
        if (alreadyCancelledHeartBeat) {
            return;
        }
        alreadyCancelledHeartBeat = true;

        timer.cancel();
    }

    /**
     * "Forget" about a message
     *
     * @param message The message to forget about.
     */
    public void undefineMessage (Message message) {
        cache.undefine(message.getMessageID());
    }

    /**
     * A node was created.  Add it to the cluster
     *
     * @param ioSession The new IoSession.
     */
    public void sessionCreated (IoSession ioSession) {
        connected = true;
        this.ioSession = ioSession;
        setupHeartBeat();

        Cluster.getInstance().addNode(this, ioSession);
    }

    /**
     * Setup a timer and a TimerTask to perform heart beats
     */
    public void setupHeartBeat () {
        if (alreadyHaveAHeartBeat) {
            return;
        }
        alreadyHaveAHeartBeat = true;

        //
        // clear any old heart beats
        //
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        //
        // create a new Timer and TimerTask
        //
        timer = new Timer();

        if (heartBeatTimerTask == null) {
            heartBeatTimerTask = new HeartBeatTimerTask(this, timer);
        }

        //
        // schedule the heart beat
        //
        long period = Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL);
        timer.schedule(heartBeatTimerTask, period, period);
    }

    public void sendHeartBeat () {
        ioSession.write(Node.HEART_BEAT_START);
        logger.debug("wrote " + Node.HEART_BEAT_START);
        setTimeOfLastActivity();
    }

    /**
     * Record the information from the START message, acknowledge the START message, but don't send a START of owr own
     *
     * @param input The START message.
     * @param ioSession The IoSession we should respond with.
     * @throws LtsllcException If there there is a problem coalescing the resulting cluster
     */
    public void handleStartGeneral (String input, IoSession ioSession) throws LtsllcException, CloneNotSupportedException {
        Scanner scanner = new Scanner(input);
        scanner.next(); // START
        partnerID = UUID.fromString(scanner.next());
        host = scanner.next();
        port = scanner.nextInt();

        ioSession.write(START_ACKNOWLEDGED);

        Cluster.getInstance().coalesce();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void sendOwners (IoSession ioSession) throws IOException {
        ioSession.write(OWNERS);
        setTimeOfLastActivity();
        logger.debug("wrote " + OWNERS);
    }

    public void sendAllOwners(IoSession ioSession) {
        for (UUID messageID : MessageLog.getInstance().getUuidToOwner().getAllKeys()) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(OWNER);
            stringBuffer.append(" ");
            stringBuffer.append(messageID);
            stringBuffer.append(" ");
            stringBuffer.append(MessageLog.getInstance().getOwnerOf(messageID));

            ioSession.write(stringBuffer.toString());
            setTimeOfLastActivity();
            logger.debug("wrote " + stringBuffer);
        }

        ioSession.write(OWNERS_END);
        setTimeOfLastActivity();
        logger.debug("wrote " + OWNERS_END);

    }

    public void handleSynchronize (String input, IoSession ioSession) {
        Scanner scanner = new Scanner(input);

        scanner.next(); // SYNCHRONIZE
        UUID partnerID = UUID.fromString(scanner.next());
        String host = scanner.next();
        int port = scanner.nextInt();

        this.partnerID = partnerID;
        this.host = host;
        this.port = port;

        ioSession.write(OWNERS);
        setTimeOfLastActivity();

        logger.debug("wrote " + OWNERS);
    }


    public void sendSynchronizationStart(IoSession ioSession) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(SYNCHRONIZE_START);
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyUuid());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyHost());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyPort());

        ioSession.write(stringBuffer.toString());
        setTimeOfLastActivity();
        logger.debug("wrote " + stringBuffer);
    }
}
