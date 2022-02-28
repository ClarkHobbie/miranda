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

    protected static final ImprovedRandom ourRandom = new ImprovedRandom();

    /**
     * The connection state.  The connection starts in the start state.
     */
    protected ClusterConnectionStates state = ClusterConnectionStates.START;
    protected MessageCache cache = new MessageCache();
    protected Stack<ClusterConnectionStates> stateStack = new Stack<>();
    protected UUID uuid;


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

    }

    @Override
    public void sessionClosed(IoSession ioSession) throws Exception {
        logger.debug("removing instance, " + ioSession + " from cluster");
        Cluster.removeNode(ioSession);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {

    }

    @Override
    public void exceptionCaught(IoSession ioSession, Throwable throwable) throws Exception {
        logger.error ("in ClusterHandler when encountered exception", throwable);
        logger.error ("going back to start state");
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
                        String strMessage = NEW_NODE_CONFIRMED;
                        strMessage += " ";
                        strMessage += this.uuid;
                        ioSession.write(strMessage);

                        synchronized (cache) {
                            Set<UUID> set = cache.getUuidToOnline().keySet();
                            Iterator<UUID> iterator = set.iterator();
                            while (iterator.hasNext()) {
                                UUID uuid = iterator.next();

                                Message message = cache.get(uuid);

                                strMessage = MESSAGE;
                                strMessage += " ";
                                strMessage += "ID: ";
                                strMessage += uuid;
                                strMessage += " STATUS: ";
                                strMessage += message.getStatusURL();
                                strMessage += " DELIVERY: ";
                                strMessage += message.getDeliveryURL();
                                strMessage += " CONTENTS: ";
                                strMessage += Utils.hexEncode(message.getContents());

                                ioSession.write(strMessage);
                            }
                        }
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
                        Scanner scanner = new Scanner(s);
                        scanner.skip(BID);
                        String strMessageID = scanner.next();
                        String strBid = scanner.next();
                        int theirBid = Integer.parseInt(strBid);
                        int myBid = ourRandom.nextInt();
                        while (myBid == theirBid) {
                            myBid = ourRandom.nextInt();
                        }
                        String strMessage = BID;
                        strMessage += strMessageID;
                        strMessage += " ";
                        strMessage += myBid;
                        ioSession.write(strMessage);

                        //
                        // if we "won" the bid
                        //
                        if (myBid > theirBid) {
                            if (!cache.contains(UUID.fromString(strMessageID))) {
                                strMessage = GET_MESSAGE;
                                strMessage += strMessageID;
                                strMessage += " ";
                                strMessage += myBid;
                                ioSession.write(strMessage);

                                pushState (state);
                                state = ClusterConnectionStates.MESSAGE;
                            }
                        }
                        break;
                    }

                }
            }
            case MESSAGE: {
                switch (messageType) {
                    case MESSAGE: {
                        Message message = new Message();

                        Scanner scanner = new Scanner(s);
                        scanner.skip(MESSAGE);
                        scanner.skip("ID:");
                        message.setMessageID(UUID.fromString(scanner.next()));
                        scanner.skip("STATUS:");
                        message.setStatusURL(scanner.next());
                        scanner.skip("DELIVERY:");
                        message.setDeliveryURL(scanner.next());
                        scanner.skip("CONTENTS:");
                        message.setContents(Utils.hexStringToBytes(scanner.next()));

                        cache.add(message);
                        state = ClusterConnectionStates.GENERAL;

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
                        Scanner scanner = new Scanner(s);
                        scanner.skip(MESSAGE);
                        scanner.skip("ID:");
                        Message message = new Message();
                        String strUuid = scanner.next();
                        message.setMessageID(UUID.fromString(strUuid));
                        scanner.skip("STATUS:");
                        message.setStatusURL(scanner.next());
                        scanner.skip("DELIVERY:");
                        message.setDeliveryURL(scanner.next());
                        scanner.skip("CONTENTS:");
                        message.setContents(Utils.hexStringToBytes(scanner.next()));

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
                        Scanner scanner = new Scanner(s);
                        scanner.skip(GET_MESSAGE);
                        String strUuid = scanner.next();
                        UUID uuid = UUID.fromString(strUuid);
                        if (!cache.contains(uuid)) {
                            ioSession.write(MESSAGE_NOT_FOUND + strUuid);
                        } else {
                            sendMessage(uuid, ioSession);
                        }
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
                        Scanner scanner = new Scanner(s);

                        Message newMessage = new Message();
                        scanner.skip(NEW_MESSAGE);
                        newMessage.setStatusURL(scanner.next());
                        scanner.skip("DELIVERY:");
                        newMessage.setDeliveryURL(scanner.next());
                        scanner.skip("CONTENT:");
                        String strContents = scanner.next();
                        newMessage.setContents(Utils.hexDecode(strContents));
                        cache.add(newMessage);
                        break;
                    }

                    default: {
                        ioSession.write(ERROR);
                        logger.error ("protocol error, returning to START");
                        state = popState();
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

        state = stateStack.pop();
        return state;
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
        String strMessage = MESSAGE + " STATUS: " + message.getStatusURL() + " DELIVERY: " + message.getDeliveryURL();
        strMessage += " CONTENTS: " + Utils.hexEncode(message.getContents());
        ioSession.write (strMessage);
    }


}
