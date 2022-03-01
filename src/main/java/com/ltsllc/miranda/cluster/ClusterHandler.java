package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.commons.util.Utils;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.MessageType;
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
 * This class implements the Cluster protocol (see the package documentation).
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
    public static final String NEW_MESSAGE = "NEW MESSAGE";
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
    protected MessageCache cache = new MessageCache();
    protected Stack<ClusterConnectionStates> stateStack = new Stack<>();
    protected UUID uuid; // our UUID
    protected Map<UUID, IoSession> uuidToNode = new HashMap<>();


    public static ImprovedRandom getOurRandom () {
        return ourRandom;
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

    public MessageCache getCache() {
        return cache;
    }

    public void setCache(MessageCache cache) {
        this.cache = cache;
    }

    public ClusterHandler () {
        logger.debug("New instance of ClusterHandler");
    }

    public void sessionCreated (IoSession ioSession) {
        logger.debug("adding new node, " + ioSession + " to cluster");
        Cluster.addNode(ioSession);
    }

    @Override
    public void sessionOpened(IoSession ioSession) throws Exception {
        //
        // the START state handles this
        //
        Cluster.addNode(ioSession);
    }

    @Override
    public void sessionClosed(IoSession ioSession) throws Exception {
        logger.debug("IoSessiob closed, removing instance, " + ioSession + " from cluster");
        Cluster.removeNode(ioSession);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {}

    @Override
    public void exceptionCaught(IoSession ioSession, Throwable throwable) throws Exception {
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
        s.toUpperCase();
        MessageType messageType = determineMessageType(s);

        switch (state) {
            case START : {
                switch (messageType) {
                    case START:
                        ioSession.write(START);
                        state = ClusterConnectionStates.GENERAL;
                        break;

                    case NEW_NODE: {
                        handleNewNode(s, ioSession);
                        state =  ClusterConnectionStates.NEW_NODE;
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
                        handleBid(s, ioSession);
                        break;
                    }

                    case GET_MESSAGE: {
                        Scanner scanner = new Scanner(s);
                        scanner.skip(GET_MESSAGE);
                        UUID uuid = UUID.fromString(scanner.next());

                        if (cache.contains(uuid)) {
                            sendMessage(uuid, ioSession);
                        } else {
                            logger.error("in an auction where we were asked for a message we don't have: " + uuid);
                            ioSession.write(MESSAGE_NOT_FOUND);
                        }
                        break;
                    }

                    case AUCTION_OVER: {
                        Scanner scanner = new Scanner(s);
                        scanner.skip(AUCTION_OVER);
                        state = ClusterConnectionStates.GENERAL;
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
                        Message message = readMessage(s);
                        cache.add(message);

                        ClusterConnectionStates pushedState = popState();
                        String strMessage = "got message with message ID: " + message.getMessageID();
                        strMessage += ", and status URL: " + message.getStatusURL();
                        strMessage += ", and delivery URL: " + message.getDeliveryURL();
                        strMessage += ", and content: " + Utils.hexEncode(message.getContents());
                        strMessage += ", and pushed state: " + pushedState;
                        logger.debug (strMessage);

                        state = pushedState;

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
                        Message message = readMessage(s);
                        cache.add(message);
                        break;
                    }

                    case NEW_NODE_OVER:
                        state = ClusterConnectionStates.START;
                        break;

                    default:
                        ioSession.write(ERROR);
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
                        Scanner scanner = new Scanner(s);
                        scanner.skip(MESSAGE_DELIVERED);
                        String strUuid = scanner.next();
                        UUID uuid = UUID.fromString(strUuid);
                        cache.remove(uuid);
                        break;
                    }

                    case NEW_MESSAGE: {
                        handleNewMessage(s);
                        break;
                    }

                    default: {
                        ioSession.write(ERROR);
                        logger.error ("protocol error, returning to START");
                        state = ClusterConnectionStates.START;
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
    public void inputClosed(IoSession ioSession)  {
        Cluster.removeNode(ioSession);
    }

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
        } else if (s.startsWith(MESSAGE_NOT_FOUND)) {
            messageType = MessageType.MESSAGE_NOT_FOUND;
        } else if (s.startsWith(MESSAGE)) {
            messageType = MessageType.MESSAGE;
        } else if (s.startsWith(NEW_MESSAGE)) {
            messageType = MessageType.NEW_MESSAGE;
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
     *
     * NOTE: this method assumes that the stateStack is not empty.
     *
     * This method does not set the state.
     *
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
     *
     * This method sends a com.ltsllc.miranda.Message over an IoSession.
     *
     * @param uuid The Message to send.  NOTE: the uuid must exist in the message cache.
     * @param ioSession The IoSession over which to send it.
     */
    protected void sendMessage (UUID uuid, IoSession ioSession) throws IOException {
        Message message = cache.get(uuid);
        String strMessage = MESSAGE;
        strMessage += " ID: ";
        strMessage += uuid;
        strMessage += " STATUS: ";
        strMessage += message.getStatusURL();
        strMessage += " DELIVERY: ";
        strMessage += message.getDeliveryURL();
        strMessage += " CONTENTS: ";
        strMessage += Utils.hexEncode(message.getContents());
        ioSession.write (strMessage);
    }

    /**
     * Convert a message message into a Message
     *
     * @param inputMessage The string to convert
     * @return The message
     */
    protected Message readMessage (String inputMessage) {
        Message message = new Message();

        Scanner scanner = new Scanner(inputMessage);
        scanner.skip(MESSAGE);
        scanner.skip(" ID: ");
        message.setMessageID(UUID.fromString(scanner.next()));
        scanner.skip(" STATUS: ");
        message.setStatusURL(scanner.next());
        scanner.skip(" DELIVERY: ");
        message.setDeliveryURL(scanner.next());
        scanner.skip(" CONTENTS: ");
        message.setContents(Utils.hexDecode(scanner.next()));

        return message;
    }

    /**
     * Send a message message over an IoSession
     *
     * @param message The message to send.
     * @param ioSession The IoSession to send it on.
     */
    protected void sendMessage (Message message, IoSession ioSession) {
        String strMessage = MESSAGE;
        strMessage += " ";
        strMessage += "ID: ";
        strMessage += message.getMessageID();
        strMessage += " STATUS: ";
        strMessage += message.getStatusURL();
        strMessage += " DELIVERY: ";
        strMessage += message.getDeliveryURL();
        strMessage += " CONTENTS: ";
        strMessage += Utils.hexEncode(message.getContents());

        ioSession.write(strMessage);
    }

    /**
     * Handle a bid
     *
     * This method takes care of a bid.  It reads in the message UUID and bid and sends out a bid of it's own.
     *
     * @param input A string containing a bid message.
     * @param ioSession The IoSession to respond to.
     */
    protected void handleBid (String input, IoSession ioSession) {
        Scanner scanner = new Scanner(input);
        scanner.skip(BID);
        String strMessageID = scanner.next();
        int theirBid = Integer.parseInt(scanner.next());
        int myBid = ourRandom.nextInt();
        while (myBid == theirBid) {
            myBid = ourRandom.nextInt();
        }
        String strMessage = BID;
        strMessage += " ";
        strMessage += strMessageID;
        strMessage += " ";
        strMessage += myBid;
        ioSession.write(strMessage);

        //
        // if we "won" the bid watch out for the situation where we don't have the message
        //
        if (myBid > theirBid) {
            if (!cache.contains(UUID.fromString(strMessageID))) {
                strMessage = GET_MESSAGE;
                strMessage += " ";
                strMessage += strMessageID;
                ioSession.write(strMessage);

                pushState(state);
                state = ClusterConnectionStates.MESSAGE;
            }
        }
    }

    /**
     * Read and handle a new node message
     *
     * This method takes care of the new node's UUID and "filling it in" on the messages that this node knows about.
     *
     * @param input A string containing the message.
     * @param ioSession The session for the new node.
     */
    protected void handleNewNode (String input, IoSession ioSession) throws IOException {
        Scanner scanner = new Scanner(input);
        scanner.skip(NEW_NODE);
        UUID uuid = UUID.fromString(scanner.next());
        String strMessage = NEW_NODE_CONFIRMED;
        strMessage += " ";
        strMessage += this.uuid;
        ioSession.write(strMessage);
        uuidToNode.put (uuid, ioSession);
        synchronized (cache) {
            Set<UUID> set = cache.getUuidToOnline().keySet();
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
     * NOTE: java.util.Scanner has a bug where skip(&lt;pattern&gt;) will not find pattern after first calling
     * skip(&lt;pattern&gt;) but next(&lt;Pattern&gt;) will.  For that reason, the method uses next(&lt;pattern&gt;)
     * instead of skip(&lt;pattern&gt;).
     *
     * @param input A string containing the message.
     * @throws LtsllcException If there is a problem adding the message.
     */
    protected void handleNewMessage (String input) throws LtsllcException {
        Scanner scanner = new Scanner(input);

        Message newMessage = new Message();
        scanner.skip(NEW_MESSAGE);
        scanner.next("ID:"); // avoid a bug in Scanner
        String strUuid = scanner.next();
        newMessage.setMessageID(UUID.fromString(strUuid));
        scanner.next("STATUS:");
        newMessage.setStatusURL(scanner.next());
        scanner.next("DELIVERY:");
        newMessage.setDeliveryURL(scanner.next());
        scanner.next("CONTENTS:");
        String strContents = scanner.next();
        newMessage.setContents(Utils.hexDecode(strContents));
        cache.add(newMessage);
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
    protected void handleGetMessage (String input, IoSession ioSession) throws IOException {
        Scanner scanner = new Scanner(input);
        scanner.skip(GET_MESSAGE);
        String strUuid = scanner.next();
        UUID uuid = UUID.fromString(strUuid);
        if (!cache.contains(uuid)) {
            ioSession.write(MESSAGE_NOT_FOUND);
        } else {
            Message message = cache.get(uuid);
            sendMessage(message, ioSession);
        }

    }
}

