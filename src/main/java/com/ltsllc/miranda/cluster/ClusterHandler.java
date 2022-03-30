package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.commons.util.Utils;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.MessageLog;
import com.ltsllc.miranda.MessageType;
import com.ltsllc.miranda.logging.LoggingCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;

import java.io.IOException;
import java.util.*;

/**
 * A connection to another node in the cluster
 *
 * <P>
 * This class implements the Cluster protocol (see the package documentation).
 * </P>
 */
public class ClusterHandler implements IoHandler {
    public static final Logger logger = LogManager.getLogger();

    /*
     * message name constants
     */
    public static final String AUCTION = "AUCTION";
    public static final String AUCTION_OVER = "AUCTION OVER";
    public static final String BID = "BID";
    public static final String DEAD_NODE = "DEAD NODE";
    public static final String ERROR = "ERROR";
    public static final String GET_MESSAGE = "GET MESSAGE";
    public static final String HEART_BEAT = "HEART BEAT";
    public static final String MESSAGE = "MESSAGE";
    public static final String MESSAGE_DELIVERED = "MESSAGE DELIVERED";
    public static final String MESSAGE_NOT_FOUND = "MESSAGE NOT FOUND";
    public static final String NEW_MESSAGE = "MESSAGE CREATED";
    public static final String NEW_NODE = "NEW NODE";
    public static final String NEW_NODE_CONFIRMED = "NEW NODE CONFIRMED";
    public static final String NEW_NODE_OVER = "NEW NODE OVER";
    public static final String START = "START";
    public static final String TIMEOUT = "TIMEOUT";

    protected static ImprovedRandom ourRandom = new ImprovedRandom();

    /**
     * The connection state.  The connection starts in the start state.
     */
    protected ClusterConnectionStates state = ClusterConnectionStates.START;

    /**
     * A message cache
     * <P>
     *     Other nodes could ask for a message we have.
     * </P>
     */
    protected LoggingCache cache = null;

    /**
     * A stack that contains the state to goto once we are done looking up a message
     * <P>
     *     If we are currently not looking up a message, then this stack should be empty.
     * </P>
     */
    protected Stack<ClusterConnectionStates> stateStack = new Stack<>();

    /**
     * The uuid of this node
     */
    protected UUID uuid = UUID.randomUUID(); // our UUID

    /**
     * The uuid of the node we are connected to, or null if we don't know
     */
    protected UUID partnerID;

    /**
     * The node that represents the other part of this connection
     */
    protected Node node = null;

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public static ImprovedRandom getOurRandom () {
        return ourRandom;
    }


    public UUID getPartnerID() {
        return partnerID;
    }

    public void setPartnerID(UUID partnerID) {
        this.partnerID = partnerID;
    }

    public static void setOurRandom (ImprovedRandom r) {
        ourRandom = r;
    }

    public Stack<ClusterConnectionStates> getStatesStack() {
        return stateStack;
    }

    public void setStatesStack(Stack<ClusterConnectionStates> statesStack) {
        this.stateStack = statesStack;
    }

    public ClusterConnectionStates getState() {
        return state;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setState(ClusterConnectionStates state) {
        this.state = state;
    }

    public LoggingCache getCache() {
        return cache;
    }

    public void setCache(LoggingCache cache) {
        this.cache = cache;
    }

    public ClusterHandler (Node node, LoggingCache cache)
            throws LtsllcException
    {
        logger.debug("entering constructor with node = " + node + ", and cache = " + cache);
        this.node = node;
        this.cache = cache;
    }

    @Override
    public void sessionCreated(IoSession session) {
    }

    @Override
    public void sessionOpened(IoSession ioSession) throws Exception {
    }

    @Override
    public void sessionClosed(IoSession ioSession) throws Exception {
        logger.debug("IoSession closed, removing instance from cluster");
        Cluster.getInstance().removeNode(node);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {}

    /**
     * An exception occurred in this ClusterHandler
     *
     * The response to this happening is to send an error to the other node and to return to the start state.
     *
     * @param ioSession The session where this occurred
     * @param throwable The exception
     */
    @Override
    public void exceptionCaught(IoSession ioSession, Throwable throwable){
        logger.error ("in ClusterHandler when encountered exception", throwable);
        logger.error ("sending error and going back to start state");
        ioSession.write(ERROR);
        state = ClusterConnectionStates.START;
    }

    /**
     * The node received a message
     *
     * <P>
     *     This method is really the "heart" of this class, and a thorough understanding of this method is required to
     *     understand this class.
     *
     * <P>
     *     The general state is where a connection usually exists while it is awaiting the next message.
     *
     * @param ioSession  The sender node
     * @param o          The message
     */
    @Override
    public void messageReceived(IoSession ioSession, Object o) throws IOException, LtsllcException {
        logger.debug("entering messageReceived with state = " + state + " and message = " + o);
        String s = (String) o;
        s = s.toUpperCase();
        MessageType messageType = determineMessageType(s);

        switch (state) {
            case START : {
                switch (messageType) {
                    case START: {
                        handleStart(s, ioSession);
                        break;
                    }

                    case NEW_NODE: {
                        handleNewNode(s, ioSession);
                        state =  ClusterConnectionStates.NEW_NODE;
                        break;
                    }

                    case ERROR: {
                        handleError(s, ioSession);
                        break;
                    }

                    default: {
                        logger.error ("protocol violation, sending an error and returning to start state");
                        ioSession.write(ERROR);
                        state = ClusterConnectionStates.START;
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
                        ioSession.write(ClusterHandler.AUCTION_OVER);
                        break;
                    }

                    default: {
                        ioSession.write(ERROR);
                        logger.error("protocol error, returning to START state");
                        state = ClusterConnectionStates.START;
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

                    default: {
                        ioSession.write(ERROR);
                        logger.error("protocol error, returning to START state");
                        state = ClusterConnectionStates.START;
                        break;
                    }
                }
                break;
            }

            case NEW_NODE: {
                switch (messageType) {
                    case MESSAGE: {
                        Message message = Message.readLongFormat(s);
                        if (message == null) {
                            logger.debug("lost bid for message, staying in auction state");
                        } else {
                            logger.debug("won bid for message, adding message to send queue and staying in auction state");
                            cache.add(message);
                        }
                        break;
                    }

                    case NEW_NODE_OVER:
                        state = ClusterConnectionStates.START;
                        break;

                    default:
                        ioSession.write(ERROR);
                        logger.error("Protocol error while in the new node state, sending error and returning to start state");
                        state = ClusterConnectionStates.START;
                        break;
                }
                break;
            }

            case GENERAL : {
                switch (messageType) {
                    case AUCTION: {
                        ioSession.write(AUCTION);
                        state = ClusterConnectionStates.AUCTION;
                        break;
                    }

                    case DEAD_NODE: {
                        ioSession.write(s);
                        break;
                    }

                    case GET_MESSAGE: {
                        handleGetMessage(s, ioSession);
                        break;
                    }

                    case HEART_BEAT: {
                        ioSession.write(s);
                        break;
                    }

                    case MESSAGE_DELIVERED: {
                        handleMessageDelivered(s);
                        break;
                    }

                    case NEW_MESSAGE: {
                        handleNewMessage(s,partnerID);
                        break;
                    }

                    case ERROR: {
                        handleError(s, ioSession);
                        break;
                    }

                    case START: {
                        break;
                    }

                    default: {
                        ioSession.write(ERROR);
                        handleError(s,ioSession);
                        break;
                    }
                }
                break;
            }

        }
        logger.debug("leaving messageReceived with state = " + state);
    }

    @Override
    public void messageSent(IoSession session, Object message)  {}

    /**
     * An IoSession closed - remove it from the cluster
     *
     * @param ioSession The node that closed the session
     */
    @Override
    public void inputClosed(IoSession ioSession)  {}

    @Override
    public void event(IoSession session, FilterEvent event)  {}


    /**
     * Convert a string to a MessageType
     *
     * NOTE: the method uses the various constants like AUCTION to identify the input.
     *
     * @param s The string to convert.  This is assumed to be in upper case.
     * @return The MessageType that the string represents or MessageType.UNKNOWN if the method doesn't recognize it.
     */
    protected MessageType determineMessageType (String s) {
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
        } else if (s.startsWith(ERROR)) {
            messageType = MessageType.ERROR;
        } else if (s.startsWith(GET_MESSAGE)) {
            messageType = MessageType.GET_MESSAGE;
        } else if (s.startsWith(HEART_BEAT)) {
            messageType = MessageType.HEART_BEAT;
        } else if (s.startsWith(MESSAGE_DELIVERED)) {
            messageType = MessageType.MESSAGE_DELIVERED;
        } else if (s.startsWith(NEW_MESSAGE)) {
            messageType = MessageType.NEW_MESSAGE;
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
        } else if (s.startsWith(START)) {
            messageType = MessageType.START;
        } else if (s.startsWith(TIMEOUT)) {
            messageType = MessageType.TIMEOUT;
        }

        logger.debug ("leaving determineMessageType with messageType = " + messageType);

        return messageType;
    }

    /**
     * "push" a state onto the state stack
     *
     * This method does not set the state.
     *
     * @param state The state that the caller wants to push.
     */
    protected void pushState (ClusterConnectionStates state) {
        stateStack.push(state);
    }

    /**
     * Pop a state off the state stack.
     * <P>
     *     NOTE: this method assumes that the stateStack is not empty.
     * </P>
     * <P>
     *     This method does not set the state.
     * </P>
     * @return A state
     * @throws LtsllcException This method throws this exception if the stateStack is empty.
     */
    protected ClusterConnectionStates popState () throws LtsllcException {
        if (stateStack.empty()) {
            logger.error("popState when the stateStack is empty");
            throw new LtsllcException("popState when the state is empty");
        }

        return stateStack.pop();
    }

    /**
     * Send a Message over an IoSession
     * <P>
     *     This method sends a com.ltsllc.miranda.Message over an IoSession.
     * </P>
     * @param uuid The Message to send.  NOTE: the uuid must exist in the message cache.
     * @param ioSession The IoSession over which to send it.
     */
    protected void sendMessage (UUID uuid, IoSession ioSession) throws IOException, LtsllcException {
        Message message = cache.get(uuid);
        String strMessage = message.longToString();
        ioSession.write (strMessage);
    }

    /**
     * Convert a message message into a Message
     *
     * @param inputMessage The string to convert
     * @return The message
     */
    protected Message readMessage (String inputMessage) {
        Message message = Message.readLongFormat(inputMessage);

        return message;
    }

    /**
     * Send a message message over an IoSession
     *
     * @param message The message to send.
     * @param ioSession The IoSession to send it on.
     */
    protected void sendMessage (Message message, IoSession ioSession) {
        String strMessage = message.longToString();

        ioSession.write(strMessage);
    }

    /**
     * Handle a bid
     *
     * This method takes care of a bid.  It reads in the message UUID and bid and sends out a bid of it's own.
     *
     * @param input A string containing a bid message.
     * @param ioSession The IoSession to respond to.
     * @param partnerID The uuid of the other node for this ioSession
     */
    protected void handleBid (String input, IoSession ioSession, UUID partnerID) throws IOException {
        logger.debug("entering handleBid with input = " + input + " and ioSession = " + ioSession + " and partnerID = " + partnerID);
        Scanner scanner = new Scanner(input);
        scanner.skip(BID);
        UUID messageID = UUID.fromString(scanner.next());
        int theirBid = Integer.parseInt(scanner.next());
        int myBid = ourRandom.nextInt();
        while (myBid == theirBid) {
            logger.debug ("a tie");
            myBid = ourRandom.nextInt();
        }
        logger.debug("myBid = " + myBid + " theirBid = " + theirBid);

        String strMessage = BID;
        strMessage += " ";
        strMessage += messageID;
        strMessage += " ";
        strMessage += myBid;
        ioSession.write(strMessage);
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

                pushState(state);
                state = ClusterConnectionStates.MESSAGE;
            }
        }

        MessageLog.getInstance().setOwner(messageID, owner);
        logger.debug("leaving handleBid");
    }

    /**
     * Read and handle a new node message
     *
     * This method takes care of the new node's UUID and "filling it in" on the messages that this node knows about.
     *
     * @param input A string containing the message.
     * @param ioSession The session for the new node.
     */
    protected void handleNewNode (String input, IoSession ioSession) throws IOException, LtsllcException {
        Scanner scanner = new Scanner(input);
        scanner.skip(NEW_NODE);
        UUID uuid = UUID.fromString(scanner.next());
        String strMessage = NEW_NODE_CONFIRMED;
        strMessage += " ";
        strMessage += this.uuid;
        ioSession.write(strMessage);

        synchronized (cache) {
            Set<UUID> set = cache.getUuidToInMemory().keySet();
            Iterator<UUID> iterator = set.iterator();
            while (iterator.hasNext()) {
                uuid = iterator.next();

                Message message = cache.get(uuid);
                sendMessage(uuid, ioSession);
            }
        }
    }

    /**
     * Process the new message message
     *
     * This method reads in the new message message and updates the cache
     *
     * NOTE: java.util.Scanner has a bug where skip(&lt;pattern&gt;) will not find a pattern after first calling
     * skip(&lt;pattern&gt;) but next() will.  For that reason, the method uses next()
     * instead of skip(&lt;pattern&gt;).
     *
     * @param input A string containing the message.
     * @param partnerUuid The UUID of our partner
     * @throws LtsllcException If there is a problem adding the message.
     *
     */
    protected void handleNewMessage (String input, UUID partnerUuid) throws LtsllcException, IOException {
        logger.debug("entering handleNewMessage with " + input);

        Scanner scanner = new Scanner(input);
        scanner.next(); // MESSAGE
        scanner.next(); // CREATED

        Message message = Message.readLongFormat(scanner);

        MessageLog.getInstance().setOwner(message.getMessageID(), partnerUuid);
        MessageLog.getInstance().add(message,partnerID);

        logger.debug("leaving handleNewMessage");
    }

    /**
     * Handle a get message message
     *
     * This method takes care of reading in the message ID and sending back the requested message.
     *
     * @param input A string containing the new message message.  Note that this method assumes that this string has
     *              been capitalized.
     * @param ioSession The session over which to respond.
     * @throws IOException If there is a problem looking up the message in the cache.
     */
    protected void handleGetMessage (String input, IoSession ioSession) throws IOException, LtsllcException {
        logger.debug("entering handleGetMessage with input = " + input + " and ioSession = " + ioSession);
        Scanner scanner = new Scanner(input);
        scanner.skip(GET_MESSAGE);
        UUID uuid = UUID.fromString(scanner.next());
        if (!cache.contains(uuid)) {
            logger.debug("cache miss, sending MESSAGE NOT FOUND");
            ioSession.write(MESSAGE_NOT_FOUND);
        } else {
            Message message = cache.get(uuid);
            logger.debug("cache hit, message = " + message.longToString() + "; sending message");
            sendMessage(message, ioSession);
        }
        logger.debug("leaving handleGetMessage");
    }

    /**
     * Tell the cache to discard the message, and set the owner of a message to the specified owner
     *
     * @param message The message.
     * @param owner The owner.
     */
    public synchronized void undefineMessage(Message message, UUID owner) {
        cache.undefine(message.getMessageID());
    }

    /**
     * Handle a start message.
     *
     * This method sets the partnerID and the state by side effect.  The response to this is to send a start of our own,
     * with our UUID and to go into the general state.
     *
     * @param input The string that came to us.
     * @param ioSession The IoSession associated with this connection.
     */
    protected synchronized void handleStart (String input, IoSession ioSession) {
        Scanner scanner = new Scanner(input);
        scanner.skip(START);
        partnerID = UUID.fromString(scanner.next());

        ioSession.getConfig().setUseReadOperation(true);
        node.setIoSession(ioSession);
        node.setPartnerID(partnerID);

        state = ClusterConnectionStates.GENERAL;
    }

    /**
     * Handle a message delivered message
     *
     * @param input The string we received.
     * @throws LtsllcException If the cache has problems removing the message
     */
    public synchronized void handleMessageDelivered (String input) throws LtsllcException, IOException {
        Scanner scanner = new Scanner(input);
        scanner.skip(MESSAGE_DELIVERED);
        String strUuid = scanner.next();
        UUID uuid = UUID.fromString(strUuid);
        cache.remove(uuid);
    }

    /**
     * Handle a get message message while in the auction state
     *
     * @param input The message we received.
     * @param ioSession The IoSession over which we received it
     * @throws IOException If there is a problem sending the message
     */
    public synchronized void handleGetMessageAuction (String input, IoSession ioSession) throws IOException, LtsllcException {
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
        }

        logger.debug("leaving handleGetMessageAuction");
    }

    /**
     * Handle a message message
     *
     * @param input The message we received;
     * @param ioSession The ioSession over which we received the message.
     * @throws LtsllcException If there is a problem looking up the message.
     */
    public synchronized void handleMessage (String input, IoSession ioSession) throws LtsllcException, IOException {
        logger.debug("entering handleMessage with input = " + input +" and ioSession = " + ioSession);
        Message message = Message.readLongFormat(input);
        cache.add(message);

        state = popState();

        String strMessage = "got message with message ID: " + message.getMessageID();
        strMessage += ", and status URL: " + message.getStatusURL();
        strMessage += ", and delivery URL: " + message.getDeliveryURL();
        strMessage += ", and content: " + Utils.hexEncode(message.getContents());
        strMessage += ", and pushed state: " + state;
        logger.debug (strMessage);

        logger.debug("leaving handleMessage");
    }

    /**
     * Handle an error by sending a start message
     *
     * <P>
     *     This method first sends an error, and then a start message.
     * </P>
     * @param s The error message (unused).
     * @param ioSession The session we should send the start over.
     */
    public void handleError (String s, IoSession ioSession) {
        logger.debug("entering handleError");

        ioSession.write(ClusterHandler.ERROR);
        state = ClusterConnectionStates.START;
        logger.debug("wrote " + ClusterHandler.ERROR);
        logger.debug("leaving handleError");
    }
}