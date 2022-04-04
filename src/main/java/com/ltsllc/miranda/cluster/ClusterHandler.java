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
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;

import java.io.IOException;
import java.util.*;

import static com.ltsllc.miranda.cluster.ClusterConnectionStates.*;

/**
 * A connection to another node in the cluster
 *
 * <p>
 * This class implements the Cluster protocol (see the package documentation).
 * </P>
 */
public class ClusterHandler implements IoHandler {
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
    public static final String GET_MESSAGE = "GET MESSAGE";
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
    public static final String SYNCHRONIZE = "SYNCHRONIZE";
    public static final String SYNCHRONIZE_START = "SYNCHRONIZE START";
    public static final String TIMEOUT = "TIMEOUT";

    protected static ImprovedRandom ourRandom = new ImprovedRandom();

    /**
     * The connection state.  The connection starts in the start state.
     */
    protected ClusterConnectionStates state = ClusterConnectionStates.START;

    /**
     * A message cache
     * <p>
     * Other nodes could ask for a message we have.
     * </P>
     */
    protected LoggingCache cache = null;

    /**
     * A stack that contains the state to goto once we are done looking up a message
     * <p>
     * If we are currently not looking up a message, then this stack should be empty.
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

    public static ImprovedRandom getOurRandom() {
        return ourRandom;
    }


    public UUID getPartnerID() {
        return partnerID;
    }

    public void setPartnerID(UUID partnerID) {
        this.partnerID = partnerID;
    }

    public static void setOurRandom(ImprovedRandom r) {
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

    public ClusterHandler(Node node, LoggingCache cache)
            throws LtsllcException {
        logger.debug("entering constructor with node = " + node + ", and cache = " + cache);
        this.node = node;
        this.cache = cache;
    }

    @Override
    public void sessionCreated(IoSession session) {
    }

    @Override
    public void sessionOpened(IoSession ioSession) {
        node.setIoSession(ioSession);
        node.setConnected(true);

        if (Miranda.getInstance().getSynchronizationFlag()) {
            synchronize(ioSession);
        }
    }

    @Override
    public void sessionClosed(IoSession ioSession) throws Exception {
        logger.debug("IoSession closed, removing instance from cluster");
        Cluster.getInstance().removeNode(node);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
    }

    /**
     * An exception occurred in this ClusterHandler
     * <p>
     * The response to this happening is to send an error to the other node and to return to the start state.
     *
     * @param ioSession The session where this occurred
     * @param throwable The exception
     */
    @Override
    public void exceptionCaught(IoSession ioSession, Throwable throwable) {
        logger.error("in ClusterHandler when encountered exception", throwable);
        logger.error("sending error and going back to start state");
        ioSession.write(ERROR);
        state = ClusterConnectionStates.START;
    }

    /**
     * The node received a message
     *
     * <p>
     * This method is really the "heart" of this class, and a thorough understanding of this method is required to
     * understand this class.
     *
     * <p>
     * The general state is where a connection usually exists while it is awaiting the next message.
     *
     * @param ioSession The sender node
     * @param o         The message
     */
    @Override
    public void messageReceived(IoSession ioSession, Object o) throws IOException, LtsllcException {
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

                    case SYNCHRONIZE_START: {
                        incomingSynchronize(ioSession);
                        break;
                    }

                    case ERROR: {
                        handleError(ioSession);
                        break;
                    }

                    default: {
                        logger.error("protocol violation, sending an error and returning to start state");
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


            case GENERAL: {
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
                        handleNewMessage(s, partnerID);
                        break;
                    }

                    case ERROR: {
                        handleError(ioSession);
                        break;
                    }

                    case START: {
                        break;
                    }

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
                        sendOwners();
                        break;
                    }

                    case OWNERS: {
                        handleSendOwners(ioSession);
                        break;
                    }

                    case OWNER: {
                        handleReceiveOwner(s);
                        break;
                    }

                    case OWNERS_END: {
                        sendMessages();
                        break;
                    }

                    case MESSAGES: {
                        handleSendMessages(ioSession);
                        break;
                    }

                    case MESSAGE: {
                        handleReceiveMessage(s);
                        break;
                    }

                    case MESSAGES_END: {
                        sendStart(ioSession);
                        state = GENERAL;
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

    @Override
    public void messageSent(IoSession session, Object message) {
        int i = 5;
        i++;
    }

    /**
     * An IoSession closed - remove it from the cluster
     *
     * @param ioSession The node that closed the session
     */
    @Override
    public void inputClosed(IoSession ioSession) {
    }

    @Override
    public void event(IoSession session, FilterEvent event) {
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
     * Send a Message over an IoSession
     * <p>
     * This method sends a com.ltsllc.miranda.Message over an IoSession.
     * </P>
     *
     * @param uuid      The Message to send.  NOTE: the uuid must exist in the message cache.
     * @param ioSession The IoSession over which to send it.
     */
    protected void sendMessage(UUID uuid, IoSession ioSession) throws IOException, LtsllcException {
        Message message = cache.get(uuid);
        String strMessage = message.longToString();
        ioSession.write(strMessage);
    }

    /**
     * Convert a message message into a Message
     *
     * @param inputMessage The string to convert
     * @return The message
     */
    protected Message readMessage(String inputMessage) {
        Message message = Message.readLongFormat(inputMessage);

        return message;
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
     * <p>
     * This method takes care of the new node's UUID and "filling it in" on the messages that this node knows about.
     *
     * @param input     A string containing the message.
     * @param ioSession The session for the new node.
     */
    protected void handleNewNode(String input, IoSession ioSession) throws IOException, LtsllcException {
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
     * @param owner   The owner.
     */
    public synchronized void undefineMessage(Message message, UUID owner) {
        cache.undefine(message.getMessageID());
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
    protected synchronized void handleStart(String input, IoSession ioSession) {
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
    public synchronized void handleMessageDelivered(String input) throws LtsllcException, IOException {
        Scanner scanner = new Scanner(input);
        scanner.skip(MESSAGE_DELIVERED);
        String strUuid = scanner.next();
        UUID uuid = UUID.fromString(strUuid);
        cache.remove(uuid);
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
        }

        logger.debug("leaving handleGetMessageAuction");
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

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ClusterHandler.START);
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyUuid());
        ioSession.write(stringBuffer.toString());
        state = ClusterConnectionStates.START;
        logger.debug("wrote " + stringBuffer.toString());
        logger.debug("leaving handleError");
    }

    /**
     * Send the owners message to our peer
     */
    public void sendOwners() {
        logger.debug("entering sendOwners");
        node.getIoSession().write(OWNERS);
        logger.debug("Sent " + OWNERS);
        logger.debug("leaving sendOwners");
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
        }
        ioSession.write(OWNERS_END);
        logger.debug("leaving handleSendOwners");
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
        }
        ioSession.write(MESSAGES_END);
        logger.debug("leaving handleSendMessages");
    }

    /**
     * Receive a message
     */
    public void handleReceiveMessage(String input) throws IOException {
        Message message = Message.readLongFormat(input);
        MessageLog.getInstance().add(message, null);
    }

    public void sendMessages() {
        logger.debug("entering sendMessages");
        node.getIoSession().write(ClusterHandler.MESSAGES);
        logger.debug("wrote " + ClusterHandler.MESSAGES);
        logger.debug("leaving sendMessages");
    }

    public void synchronize(IoSession ioSession) {
        Miranda.getInstance().setSynchronizationFlag(false);
        ImprovedProperties p = Miranda.getProperties();
        ImprovedFile messages = new ImprovedFile(p.getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        ImprovedFile owners = new ImprovedFile(p.getProperty(Miranda.PROPERTY_OWNER_FILE));
        int loadLimit = p.getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT);
        MessageLog.defineStatics(messages, loadLimit, owners);
        state = SYNCHRONIZING;

        sendSynchronizeStart(ioSession);
    }

    public void sendSynchronize () {
        logger.debug("entering sendSynchronize");

        node.getIoSession().write (ClusterHandler.SYNCHRONIZE);
        logger.debug("wrote " + ClusterHandler.SYNCHRONIZE);

        logger.debug("leaving sendSynchronize");
    }

    public void start () {
        logger.debug("entering start");

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ClusterHandler.START);
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyUuid());

        node.getIoSession().write(stringBuffer.toString());
        logger.debug("wrote " + stringBuffer.toString());

        logger.debug("leaving start");
    }

    public void incomingSynchronize (IoSession ioSession) {
        state = SYNCHRONIZING;
        ioSession.write(ClusterHandler.SYNCHRONIZE);
    }

    public void sendSynchronizeStart (IoSession ioSession) {
        ioSession.write(SYNCHRONIZE_START);
    }

    public void sendStart(IoSession ioSession) {
        logger.debug("entering sendStart");
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(START);
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyUuid());

        ioSession.write(stringBuffer.toString());
        logger.debug("leaving sendStart");
    }
}