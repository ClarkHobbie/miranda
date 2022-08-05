package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.commons.util.Utils;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.alarm.AlarmClock;
import com.ltsllc.miranda.alarm.Alarmable;
import com.ltsllc.miranda.alarm.Alarms;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.message.MessageLog;
import com.ltsllc.miranda.message.MessageType;
import com.ltsllc.miranda.netty.HeartBeatHandler;
import com.ltsllc.miranda.properties.PropertyChangedEvent;
import com.ltsllc.miranda.properties.PropertyListener;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

import static com.ltsllc.miranda.cluster.ClusterConnectionStates.GENERAL;
import static com.ltsllc.miranda.cluster.ClusterConnectionStates.SYNCHRONIZING;


/**
 * A node in the cluster
 *
 * <p>
 * This object represents a node in the cluster.  It contains a host, a port and the uuid of that node.  It also
 * represents the state of the connection as in have we just connected or are we in the middle of something.
 * Finally, the class also serves as the container for many constants.  Generally these are constants that have to do
 * with the connection such as "what does a bid look like?"
 * </P>
 */
public class Node implements Cloneable, Alarmable, PropertyListener {
    public static final Logger logger = LogManager.getLogger();
    public static final Logger events = LogManager.getLogger("events");

    /*
     * message name constants
     */
    public static final String ASSIGN_MESSAGE = "ASSIGN MESSAGE";
    public static final String BID = "BID";
    public static final String DEAD_NODE = "DEAD NODE";
    public static final String DEAD_NODE_START = "DEAD NODE START";
    public static final String ERROR = "ERROR";
    public static final String ERROR_START = "ERROR_START";
    public static final String GET_MESSAGE = "GET MESSAGE";
    public static final String HEART_BEAT_START = "HEART BEAT START";
    public static final String HEART_BEAT = "HEART BEAT";
    public static final String LEADER = "LEADER";
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
    public static final String TAKE = "TAKE";
    public static final String TIMEOUT = "TIMEOUT";

    public Node(UUID myUUID, String host, int port, Channel channel) {
        this.channel = channel;
        this.uuid = myUUID;
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
     * Has a timeout been met?
     */
    protected HashMap<Alarms, Boolean> timeoutsMet = new HashMap<>();

    public HashMap<Alarms, Boolean> getTimeoutsMet() {
        return timeoutsMet;
    }

    protected Map<Channel, Boolean> channelToSentStart = new HashMap<>();


    public Map<Channel, Boolean> getChannelToSentStart() {
        return channelToSentStart;
    }

    public void setChannelToSentStart(Map<Channel, Boolean> channelToSentStart) {
        this.channelToSentStart = channelToSentStart;
    }

    /**
     * The IoSession that we talk to our partner with or null if we are not currently connected
     */
    protected Channel channel;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * id of that this node represents
     */
    protected UUID uuid;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * The hostname or IP address of our partner node
     */
    protected String host = null;

    /**
     * The port number that our partner is waiting for connections on
     */
    protected int port = -1;


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
     * When did the node start?
     */
    protected long nodeStart = -1;

    public long getNodeStart() {
        return nodeStart;
    }

    public void setNodeStart(long nodeStart) {
        this.nodeStart = nodeStart;
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
     * Send a message to the node informing it that we created a message
     * <p>
     * This is so that if we go down, someone has a copy of the message.
     *
     * @param message The message that we created.
     */
    public void informOfMessageCreation(Message message) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.NEW_MESSAGE);
        stringBuffer.append(" ");
        stringBuffer.append(message.internalsToString());

        channel.writeAndFlush(stringBuffer.toString());
    }

    /**
     * Inform the partner node that we delivered this message
     * <p>
     * This is so that if we crash, the partner node knows that the message does not need to be delivered.
     *
     * @param message The message that we delivered.
     */
    public void informOfMessageDelivery(Message message) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.MESSAGE_DELIVERED);
        stringBuffer.append(" ");
        stringBuffer.append(message.getMessageID());

        channel.write(stringBuffer.toString());
    }

    /**
     * Process a message
     * <p>
     * This method is really the "heart" of this class.  It is where most of the application logic lives.
     * </P>
     *
     * @param s         The message.
     * @throws IOException                If there is a problem opening or closing a file.
     * @throws LtsllcException            If there is an application error.
     */
    public void messageReceived(String s) throws IOException, LtsllcException {
        logger.debug("entering messageReceived with state = " + state + " and message = " + s);
        s = s.toUpperCase();

        MessageType messageType = determineMessageType(s);

        switch (state) {
            case START: {
                switch (messageType) {
                    case START: {
                        handleStart(s);
                        break;
                    }

                    case START_ACKNOWLEDGED: {
                        handleStartAcknowledged(s);
                        break;
                    }

                    case SYNCHRONIZE_START: {
                        handleSynchronizeStartInStart(s);
                        break;
                    }

                    case SYNCHRONIZE: {
                        handleSynchronize(s);
                        break;
                    }

                    case DEAD_NODE: {
                        handleError(); // this would mean we had issued a heart beat start
                        break;
                    }

                    case DEAD_NODE_START: {
                        handleDeadNodeStart(s);
                        break;
                    }

                    case ERROR_START: {
                        handleErrorStart();
                        break;
                    }

                    case ERROR: {
                        break;
                    }

                    case LEADER: {
                        handleLeader(s);
                        break;
                    }
                    case TAKE: {
                        handleTake(s);
                        break;
                    }

                    case MESSAGE_DELIVERED: {
                        handleMessageDelivered(s);
                        break;
                    }



                    default: {
                        handleError();
                        break;
                    }
                }
                break;
            }


            case MESSAGE: {
                switch (messageType) {
                    case MESSAGE: {
                        handleMessage(s);
                        break;
                    }

                    case MESSAGE_NOT_FOUND: {
                        setState(popState());
                        break;
                    }

                    case DEAD_NODE: {
                        handleDeadNode(s);
                        break;
                    }

                    case DEAD_NODE_START: {
                        handleDeadNodeStart(s);
                        break;
                    }

                    case ERROR_START: {
                        handleErrorStart();
                        break;
                    }

                    case ERROR: {
                        break;
                    }

                    case MESSAGE_DELIVERED: {
                        break;
                    }

                    case TAKE: {
                        handleTake(s);
                        break;
                    }

                    default: {
                        handleError();
                        logger.error("protocol error, returning to START state");
                        break;
                    }
                }
                break;
            }


            case GENERAL: {
                switch (messageType) {
                    case ASSIGN: {
                        handleAssign(s);
                        break;
                    }

                    case DEAD_NODE: {
                        handleDeadNode(s);
                        break;
                    }

                    case DEAD_NODE_START: {
                        handleDeadNodeStart(s);
                        break;
                    }

                    case GET_MESSAGE: {
                        handleGetMessage(s);
                        break;
                    }

                    case MESSAGE_DELIVERED: {
                        handleMessageDelivered(s);
                        break;
                    }

                    case NEW_MESSAGE: {
                        handleNewMessage(s, uuid);
                        break;
                    }

                    case ERROR_START: {
                        handleErrorStart();
                        break;
                    }

                    case ERROR: {
                        handleErrorGeneral();
                        break;
                    }

                    case START: {
                        handleStartGeneral(s);
                        break;
                    }

                    case START_ACKNOWLEDGED: {
                        handleStartAcknowledgedGeneral(s);
                        break;
                    }

                    case SYNCHRONIZE: {
                        handleSynchronize(s);
                        break;
                    }

                    case SYNCHRONIZE_START: {
                        handleSynchronizationStartInGeneral(s);
                        break;
                    }

                    case TAKE: {
                        handleTake(s);
                        break;
                    }

                    default: {
                        handleError();
                        break;
                    }
                }
                break;
            }

            case SYNCHRONIZING: {
                switch (messageType) {
                    case SYNCHRONIZE: {
                        handleSynchronize(s);
                        break;
                    }

                    case OWNERS: {
                        break;
                    }

                    case OWNER: {
                        handleReceiveOwner(s);
                        break;
                    }

                    case OWNERS_END: {
                        break;
                    }

                    case MESSAGE: {
                        handleReceiveMessage(s);
                        break;
                    }

                    case MESSAGES: {
                        break;
                    }

                    case MESSAGES_END: {
                        handleMessagesEnd();
                        break;
                    }

                    case ERROR_START: {
                        handleErrorStart();
                        break;
                    }

                    case DEAD_NODE: {
                        handleDeadNode(s);
                        break;
                    }

                    case DEAD_NODE_START: {
                        handleDeadNodeStart(s);
                        break;
                    }

                    case ERROR: {
                        break;
                    }

                    case START: {
                        handleStartSynchronizing(s);
                        break;
                    }

                    case TAKE: {
                        handleTake(s);
                        break;
                    }

                    case START_ACKNOWLEDGED: {
                        break;
                    }

                    default: {
                        handleError();
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

        if (s.equals(DEAD_NODE_START)) {
            messageType = MessageType.DEAD_NODE_START;
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
        } else if (s.equals(LEADER)) {
            messageType = MessageType.LEADER;
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
        } else if (s.startsWith(TAKE)) {
            messageType = MessageType.TAKE;
        } else if (s.startsWith(TIMEOUT)) {
            messageType = MessageType.TIMEOUT;
        }

        logger.debug("leaving determineMessageType with messageType = " + messageType);

        return messageType;
    }

    /**
     * Handle a start message.
     * <p>
     * This method sets the partnerID and the state by side effect.  The response to this is to send a start of our own,
     * with our UUID and to go into the general state.
     *
     * @param input     The string that came to us.
     */
    protected synchronized void handleStart(String input)  {
        logger.debug("entering handleStart");
        Scanner scanner = new Scanner(input);
        scanner.skip(START);
        uuid = UUID.fromString(scanner.next());
        registerUuid(uuid);
        host = scanner.next();
        port = scanner.nextInt();
        nodeStart = scanner.nextLong();

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.START_ACKNOWLEDGED);
        stringBuffer.append(" ");
        addId(stringBuffer);

        channel.writeAndFlush(stringBuffer.toString());
        logger.debug("wrote " + stringBuffer);

        //
        // if the node is eldest, then synchronize with it
        //
        if ((Miranda.getInstance().getMyStart() == -1) || (Miranda.getInstance().getEldest() > nodeStart)) {
            Miranda.getInstance().setEldest(nodeStart);
            sendSynchronizationStart();
            pushState(state);
            setState(SYNCHRONIZING);
        }
        //
        // otherwise just treat it like any other node
        //
        else {
            sendStart(true);
        }

        logger.debug("leaving handleStart");
    }

    /**
     * Send a start message complete with the uuid, host and port
     */
    public void sendStart(boolean force) {
        if (channelToSentStart.get(channel) != null && channelToSentStart.get(channel).booleanValue()
                    && !force) {
            return;
        }

        if (channel == null) {
            logger.warn("channel is null in sendStart");
            return;
        }

        channelToSentStart.put(channel, new Boolean(true));
        logger.debug("entering sendStart");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START);
        stringBuilder.append(" ");
        stringBuilder.append(Miranda.getInstance().getMyUuid());
        stringBuilder.append(" ");
        stringBuilder.append(Miranda.getInstance().getMyHost());
        stringBuilder.append(" ");
        stringBuilder.append(Miranda.getInstance().getMyPort());
        stringBuilder.append(" ");
        stringBuilder.append(Miranda.getInstance().getMyStart());

        channel.writeAndFlush(stringBuilder.toString());
        logger.debug("wrote " + stringBuilder.toString());

        //AlarmClock.getInstance().scheduleOnce(this, Alarms.START,
        //        Miranda.getProperties().getLongProperty(Miranda.PROPERTY_START_TIMEOUT));
        timeoutsMet.put(Alarms.START, false);

        logger.debug("leaving sendStart");
    }

    /**
     * Handle a synchronization start message
     *
     * <p>
     * This consists of reading the uuid, host and port from the message sending a synchronize message of out own,
     * with our own uuid, host and port and switching to state.
     * </P>
     *
     * @param input     The synchronization start message
     */
    public void handleSynchronizeStartInStart(String input) throws LtsllcException, IOException {
        logger.debug("entering handleSynchronizeStart with input = " + input);

        pushState(state);
        setState(SYNCHRONIZING);

        Scanner scanner = new Scanner(input);
        scanner.next();
        scanner.next(); // SYNCHRONIZE START
        UUID uuid = UUID.fromString(scanner.next());
        registerUuid(uuid);
        String host = scanner.next();
        int port = scanner.nextInt();
        long start = scanner.nextLong();

        this.uuid = uuid;
        this.host = host;
        this.port = port;
        this.nodeStart = start;

        sendSynchronize();

        sendAllOwners();
        sendAllMessages();

        setState(popState());
        if (state == ClusterConnectionStates.START) {
            sendStart(false);
        }

        logger.debug("leaving handleSynchronizeStart");
    }

    /**
     * Handle an error start message by sending out an error message returning to the start state, and sending a new
     * start message
     *
     */
    public void handleErrorStart() {
        logger.debug("entering handleErrorStart");

        channel.writeAndFlush(ERROR);

        sendStart(true);

        logger.debug("leaving handleErrorStart");
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
     * Send a Message over a channel
     * <p>
     * This method sends a com.ltsllc.miranda.Message over an IoSession.
     * </P>
     *
     * @param uuid      The Message to send.  NOTE: the uuid must exist in the message cache.
     */
    public void sendMessage(UUID uuid) throws IOException, LtsllcException {
        Message message = MessageLog.getInstance().get(uuid);
        String strMessage = message.longToString();
    }

    /**
     * Handle a message message
     *
     * @param input     The message we received;
     * @throws LtsllcException If there is a problem looking up the message.
     */
    public synchronized void handleMessage(String input) throws LtsllcException, IOException {
        logger.debug("entering handleMessage with input = " + input);
        Message message = Message.readLongFormat(input);
        MessageLog.getInstance().add(message,Miranda.getInstance().getMyUuid());

        setState(popState());

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
     * @throws IOException If there is a problem looking up the message in the cache.
     */
    protected void handleGetMessage(String input) throws IOException, LtsllcException {
        logger.debug("entering handleGetMessage with input = " + input);
        Scanner scanner = new Scanner(input);
        scanner.skip(GET_MESSAGE);
        UUID uuid = UUID.fromString(scanner.next());
        registerUuid(uuid);
        if (!MessageLog.getInstance().contains(uuid)) {
            logger.debug("cache miss, sending MESSAGE NOT FOUND");
            channel.writeAndFlush(MESSAGE_NOT_FOUND);
        } else {
            Message message = MessageLog.getInstance().get(uuid);
            logger.debug("cache hit, message = " + message.longToString() + "; sending message");
            sendMessage(message);
        }
        logger.debug("leaving handleGetMessage");
    }

    /**
     * Send a message message
     *
     * @param message   The message to send.
     */
    protected void sendMessage(Message message) {
        String strMessage = message.longToString();

        channel.writeAndFlush(strMessage);
    }

    /**
     * Handle an error by sending a start message
     *
     * <p>
     * This method first sends an error, and then a start message.
     * </P>
     */
    public void handleError() {
        logger.debug("entering handleError");

        channel.writeAndFlush(ERROR_START);
        logger.debug("wrote " + ERROR_START);
        setState(ClusterConnectionStates.START);

        sendStart(true);

        logger.debug("leaving handleError");
    }

    /**
     * Handle a message delivered message
     *
     * @param input The string we received.
     */
    public synchronized void handleMessageDelivered(String input) throws IOException {
        Scanner scanner = new Scanner(input);
        scanner.skip(MESSAGE_DELIVERED);
        String strUuid = scanner.next();
        UUID uuid = UUID.fromString(strUuid);
        registerUuid(uuid);
        MessageLog.getInstance().remove(uuid);
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
     */
    protected void handleNewMessage(String input, UUID partnerUuid) throws IOException {
        logger.debug("entering handleNewMessage with " + input);

        Scanner scanner = new Scanner(input);
        scanner.next(); // MESSAGE
        scanner.next(); // CREATED

        Message message = Message.readLongFormat(scanner);

        MessageLog.getInstance().add(message, partnerUuid);

        logger.debug("leaving handleNewMessage");
    }


    /**
     * Send all the owner information in the system
     * <p>
     * This information is written as OWNER &lt;message UUID&gt; &lt;owner UUID&gt; to the ioChannel.
     * </P>
     */
    public void handleSendOwners() {
        logger.debug("entering handleSendOwners");
        channel.writeAndFlush("OWNERS");
        Collection<UUID> ownerKeys = MessageLog.getInstance().getAllOwnerKeys();
        Iterator<UUID> iterator = ownerKeys.iterator();
        while (iterator.hasNext()) {
            UUID message = iterator.next();
            UUID owner = MessageLog.getInstance().getOwnerOf(message);

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(OWNER);
            stringBuilder.append(" ");
            stringBuilder.append(message);
            stringBuilder.append(" ");
            stringBuilder.append(owner);
            stringBuilder.append(" ");

            channel.writeAndFlush(stringBuilder.toString());
        }
        channel.writeAndFlush(OWNERS_END);
        logger.debug("leaving handleSendOwners");
    }

    /**
     * Receive some owner information
     * <p>
     * The owner information is expected to be in the form "OWNER &lt;message UUID&gt; &lt;owner UUID&gt;."
     * </P>
     *
     * @param input The owner information
     * @throws IOException If there is a problem setting the owner
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

    public void sendAllMessages() throws IOException {
        logger.debug("entering sendAllMessages");

        channel.writeAndFlush(MESSAGES);

        for (Message message : MessageLog.getInstance().copyAllMessages()) {
            String msg = message.longToString();

            channel.writeAndFlush(msg);
            logger.debug("wrote " + msg);
        }

        channel.writeAndFlush(MESSAGES_END);
        logger.debug("wrote " + MESSAGES_END);

        logger.debug("leaving sendMessages");
    }

    /**
     * Receive a message message
     *
     * <p>
     * This method expects a message, in long format, is in the input string.  The method responds by adding the
     * message to the message log.
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
     */
    public void sessionOpened() {
        if (Miranda.getInstance().getSynchronizationFlag()) {
            synchronize();
        }
    }


    /**
     * Go into the synchronization state.
     *
     * <p>
     * This starts the synchronization process by going into the synchronization state and sending a synchronization
     * start message
     * </P>
     */
    public void synchronize() {
        Miranda.getInstance().setSynchronizationFlag(false);

        sendSynchronizationStart();
        pushState(state);
        setState(SYNCHRONIZING);
    }

    /**
     * An exception was caught on the IoSession --- send an error then send a start message
     *
     * @param throwable The exception.
     */
    public void exceptionCaught(Throwable throwable) {
        logger.error("caught exception starting over", throwable);
        channel.writeAndFlush(ERROR_START);
        sendStart(true);
    }

    /**
     * The IoSession for this node has closed --- remove it from the cluster.
     */
    public void sessionClosed() {
        closeChannel();
        Cluster.getInstance().removeNode(this);
    }

    /**
     * A node was created.  Add it to the cluster
     */
    public void sessionCreated() {
        Cluster.getInstance().addNode(this);
    }

    /**
     * Add the uuid host and port to a StringBuffer
     *
     * @param stringBuffer The StringBuffer to add the id to.
     */
    public void addId(StringBuffer stringBuffer) {
        stringBuffer.append(Miranda.getInstance().getMyUuid());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyHost());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyPort());
    }

    /**
     * Record the information from the START message, acknowledge the START message, but don't send a START of owr own
     *
     * @param input     The START message.
     */
    public void handleStartGeneral(String input) {
        logger.debug("entering handleStartGeneral");
        Scanner scanner = new Scanner(input);
        scanner.next(); // START
        uuid = UUID.fromString(scanner.next());
        registerUuid(uuid);
        host = scanner.next();
        port = scanner.nextInt();
        long nodeStart = scanner.nextLong();
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(START_ACKNOWLEDGED);
        stringBuffer.append(" ");
        addId(stringBuffer);
        channel.writeAndFlush(stringBuffer.toString());

        if ((Miranda.getInstance().getEldest() == -1) || (Miranda.getInstance().getEldest() > nodeStart)) {
            Miranda.getInstance().setEldest(nodeStart);
            sendSynchronizationStart();
            pushState(state);
            setState(SYNCHRONIZING);
        }

        logger.debug("leaving handleStartGeneral");
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Send all this object's owner information
     *
     * <p>
     * Owner's information consists or a message UUID, and the owner UUID.  The method terminates the info with an
     * owners end message.  This method uses the MessagesLog's owners information as the info on this host.
     * </P>
     */
    public void sendAllOwners() {
        channel.writeAndFlush(OWNERS);

        for (UUID messageID : MessageLog.getInstance().getUuidToOwner().getAllKeys()) {
            StringBuilder stringBuilder  = new StringBuilder();
            stringBuilder.append(OWNER);
            stringBuilder.append(" ");
            stringBuilder.append(messageID);
            stringBuilder.append(" ");
            stringBuilder.append(MessageLog.getInstance().getOwnerOf(messageID));

            channel.writeAndFlush(stringBuilder.toString());
            logger.debug("wrote " + stringBuilder);
        }

        channel.writeAndFlush(OWNERS_END);
        logger.debug("wrote " + OWNERS_END);

    }

    /**
     * Take care of recording the data in a synchronize message
     *
     * <p>
     * This method sends the proper response to a synchronize message - to whit a owners message.
     * </P>
     *
     * @param input     A String containing the synchronize message
     */
    public void handleSynchronize(String input) {
        Scanner scanner = new Scanner(input);

        scanner.next(); // SYNCHRONIZE
        UUID uuid = UUID.fromString(scanner.next());
        registerUuid(uuid);
        String host = scanner.next();
        int port = scanner.nextInt();
        long start = scanner.nextLong();

        this.uuid = uuid;
        this.host = host;
        this.port = port;
        this.nodeStart = start;
    }


    /**
     * Send a synchronization start message, that includes the UUID of this node, the host its on and the port where
     * it is listening
     *
     * <p>
     * This method takes care of setting the time of last activity and the contents of the synchronization message.
     * </P>
     */
    public void sendSynchronizationStart() {
        logger.debug("entering sendSynchronizationStart");

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(SYNCHRONIZE_START);
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyUuid());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyHost());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyPort());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyStart());

        channel.writeAndFlush(stringBuffer.toString());
        logger.debug("wrote " + stringBuffer);

        logger.debug("leaving sendSynchronizationStart");
    }

    /**
     * Close the session associated with this node
     */
    public void closeChannel() {
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }

    /**
     * Merge this node with another node
     *
     * @param other The other node to merge with
     */
    public void merge(Node other) throws LtsllcException {
        logger.debug("entering merge");

        if ((null == uuid) && (null != other.uuid)) {
            uuid = other.uuid;
        }

        if ((timeOfLastActivity != null && other.timeOfLastActivity != null) && (timeOfLastActivity < other.timeOfLastActivity)) {
            timeOfLastActivity = other.timeOfLastActivity;
        } else if ((timeOfLastActivity == null) && (other.timeOfLastActivity != null)) {
            timeOfLastActivity = other.timeOfLastActivity;
        }
        //
        // many other cases that boil down to just using the node's value
        //

        if ((channel == null) && (other.channel != null)) {
            throw new LtsllcException("node1 has a null ioSession while node2 does not");
        }

        other.closeChannel();

        logger.debug("leaving merge");
    }

    /**
     * Handle a start message
     *
     * <p>
     * This consists of sending a start acknowledged message, and the sending a start of our own.
     * </P>
     *
     * @param input     A string containing the start message.
     * @throws LtsllcException            Coalesce throws this.
     * @throws CloneNotSupportedException Coalesce also throws this.
     */
    public void handleStartSynchronizing(String input)   {
        logger.debug("entering handleStartSynchronizing");

        Scanner scanner = new Scanner(input);
        scanner.next(); // START
        uuid = UUID.fromString(scanner.next());
        registerUuid(uuid);
        host = scanner.next();
        port = scanner.nextInt();

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(START_ACKNOWLEDGED);
        stringBuffer.append(" ");
        addId(stringBuffer);
        channel.writeAndFlush(stringBuffer.toString());
        logger.debug("wrote " + stringBuffer);

        logger.debug("leaving handleStartSynchronizing");
    }

    /**
     * This method returns true if two nodes "point" to the same thing
     *
     * <p>
     * Two nodes are considered to point to the same thing if their uuids are the same or their host and ports are
     * equal.
     * </P>
     *
     * @param other The other node to compare with.
     * @return true if both nodes point to the same thing, false otherwise.
     */
    public boolean pointsToTheSameThing(Node other) {
        if ((uuid != null) && (other.uuid != null) && uuid.equals(other.uuid)) {
            return true;
        }

        if ((host == null) || (port == -1)) {
            return false;
        }

        if ((other.host == null) || (other.port == -1)) {
            return false;
        }

        if ((host.equalsIgnoreCase(other.host)) && (port == other.port)) {
            return true;
        }

        return false;
    }

    /**
     * The node timed out waiting for a reply to its start message
     */
    public void startTimeout() {
        logger.debug("entering startTimeout");

        setState(ClusterConnectionStates.START);
        if (channel != null) {
            sendStart(true);
        }

        logger.debug("leaving startTimeout");
    }

    /**
     * The node received some sort of alarm
     *
     * @param alarm The alarm.
     */
    @Override
    public void alarm(Alarms alarm)  {
        switch (alarm) {
            case START: {
                if (!timeoutsMet.get(Alarms.START)) {
                    startTimeout();
                } else {
                    timeoutsMet.put(Alarms.START, false);
                }
                break;
            }

            case DEAD_NODE: {
                if (!timeoutsMet.get(Alarms.DEAD_NODE)) {
                    deadNodeTimeout();
                } else {
                    timeoutsMet.put(Alarms.DEAD_NODE, false);
                }
                break;
            }

            default: {
                logger.error("received an unrecognized alarm:" + alarm);
                break;
            }
        }
    }

    /**
     * Handles a dead node start message by sending back a dead node message of it own
     *
     * @param input     The dead node start message.
     */
    public void handleDeadNodeStart(String input) throws LtsllcException, IOException {
        logger.debug("entering handleDeadNodeStart");

        Scanner scanner = new Scanner(input);
        scanner.next();
        scanner.next();
        scanner.next(); // DEAD NODE START
        String deadNodeStr = scanner.next();

        int vote = scanner.nextInt();

        Cluster.getInstance().vote(Miranda.getInstance().getMyUuid(), vote);

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(DEAD_NODE);
        stringBuffer.append(" ");
        stringBuffer.append(deadNodeStr);
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyUuid());
        stringBuffer.append(" ");
        stringBuffer.append(ourRandom.nextInt());

        channel.writeAndFlush(stringBuffer.toString());

        logger.debug("leaving handleDeadNodeStart");
    }

    /**
     * Handle a dead node message
     *
     * <p>
     * This consists of setting a timeoutsMet flag.
     * </P>
     */
    public void handleDeadNode(String input) throws LtsllcException {
        logger.debug("entering handleDeadNode");

        timeoutsMet.put(Alarms.DEAD_NODE, true);
        Scanner scanner = new Scanner(input);

        scanner.next();
        scanner.next(); // DEAD NODE
        scanner.next(); // uuid
        int vote = scanner.nextInt();

        Cluster.getInstance().vote(uuid, vote);
        if (Cluster.getInstance().allVotesIn()) {
            Cluster.getInstance().countVotes();
            Cluster.getInstance().sendLeader();
        }

        logger.debug("leaving handleDeadNode");
    }

    /**
     * Send a DEAD NODE START message
     *
     * @param uuid The uuid of the dead node
     */
    public void sendDeadNodeStart(UUID uuid) {
        logger.debug("entering sendDeadNode with " + uuid);

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(DEAD_NODE_START);
        stringBuffer.append(" ");
        stringBuffer.append(uuid);
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyUuid());
        stringBuffer.append(" ");
        stringBuffer.append(ourRandom.nextInt());

        channel.writeAndFlush(stringBuffer.toString());
        logger.debug("wrote " + stringBuffer);
        AlarmClock.getInstance().scheduleOnce(this, Alarms.DEAD_NODE,
                Miranda.getProperties().getLongProperty(Miranda.PROPERTY_DEAD_NODE_TIMEOUT));
        timeoutsMet.put(Alarms.DEAD_NODE, false);

        logger.debug("leaving sendDeadNode");
    }

    /**
     * The node has timed out waiting for a reply to a dead node start message
     */
    public void deadNodeTimeout() {
        logger.debug("entering deadNodeTimeout");

        setState(ClusterConnectionStates.START);
        sendStart(true);

        logger.debug("leaving deadNodeTimeout");
    }

    /**
     * We have been asked to send a dead node message
     */
    public void sendDeadNode(UUID uuid) {
        logger.debug("entering sendDeadNode");

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(DEAD_NODE);
        stringBuffer.append(" ");
        stringBuffer.append(uuid);

        channel.writeAndFlush(stringBuffer.toString());

        logger.debug("leaving sendDeadNode");
    }

    /**
     * Take ownership of the specified messages
     *
     * @param messages A list of uuids that the node "owns."
     * @throws IOException If the MessageLog throws this exception
     */
    public void takeOwnershipOf(List<UUID> messages) throws IOException {
        logger.debug("entering takeOwnershipOf");

        for (UUID uuid : messages) {
            MessageLog.getInstance().setOwner(uuid, this.uuid);
            Cluster.getInstance().takeOwnershipOf(Miranda.getInstance().getMyUuid(), uuid);
        }

        logger.debug("leaving takeOwnershipOf");
    }

    /**
     * Send a take message to the node
     *
     * @param owner   The (new) owner of the message.
     * @param message The message whose ownership will be changed.
     */
    public void sendTakeOwnershipOf(UUID owner, UUID message) {
        logger.debug("entering sendTakeOwnershipOf with owner = " + owner + " and message = " + message);

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(TAKE);
        stringBuffer.append(" ");
        stringBuffer.append(owner);
        stringBuffer.append(" ");
        stringBuffer.append(message);

        channel.writeAndFlush(stringBuffer.toString());
        logger.debug("wrote " + stringBuffer);

        logger.debug("leaving sendTakeOwnershipOf");
    }

    /**
     * Handle a take message
     *
     * @param input     A string containing the take message.
     * @throws IOException If there is a problem transferring ownership
     */
    public void handleTake(String input) throws IOException {
        logger.debug("entering handleTake");

        Scanner scanner = new Scanner(input);
        scanner.next(); // TAKE

        UUID owner = UUID.fromString(scanner.next());
        UUID message = UUID.fromString(scanner.next());

        MessageLog.getInstance().setOwner(message, owner);

        logger.debug("leaving handleTake");
    }

    /**
     * Tell the node about a message delivery
     *
     * @param message The message that was delivered
     */
    public void notifyOfDelivery(Message message) {
        logger.debug("entering notifyOfDelivery");

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(MESSAGE_DELIVERED);
        stringBuffer.append(" ");
        stringBuffer.append(message.getMessageID());

        channel.writeAndFlush(stringBuffer.toString());

        logger.debug("wrote " + stringBuffer);

        logger.debug("leaving notifyOfDelivery");
    }

    /**
     * A property we are watching changed.
     * <H>
     * The property we care about is the heart beat.
     * </H>
     *
     * @param propertyChangedEvent The property changed event.
     * @throws Throwable If we don't recognize the property.
     */
    @Override
    public void propertyChanged(PropertyChangedEvent propertyChangedEvent) throws Throwable {
        switch (propertyChangedEvent.getProperty()) {
            default: {
                throw new LtsllcException("propertyChanged call for " + propertyChangedEvent.getProperty());
            }
        }
    }


    /**
     * The end of the messages has been encountered.  This is basically the end of the synchronization process.
     *
     * <H>
     *     In reply to this event the node goes into the start state and sends a start message.
     * </H>
     */
    public void handleMessagesEnd() throws LtsllcException {
        logger.debug("entering handleMessagesEnd");

        Miranda.getInstance().setMyStart(nodeStart);
        setState(popState());

        logger.debug("leaving handleMessagesEnd");
    }

    /**
     * Send a synchronization message and start synchronizing with the node.
     *
     * <H>
     *     A synchronization message consists of more than a request to synchronize it includes the uuid of this node,
     *     the host of this node, the port it is listening on and the time when this node started.
     * </H>
     */
    public void sendSynchronize() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(SYNCHRONIZE);
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyUuid());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyHost());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyPort());
        stringBuffer.append(" ");
        stringBuffer.append(Miranda.getInstance().getMyStart());
        channel.writeAndFlush(stringBuffer.toString());

        logger.debug("wrote " + stringBuffer);
    }

    /**
     * Another node wants to synchronize with us.  Send all of our ownership and message information.
     *
     * @param input The synchronization message.  This includes the uuid of the other node its host its port and the
     *              time it was started.
     * @throws IOException If there is a problem sending the messages.
     */
    public void handleSynchronizationStartInGeneral(String input) throws IOException, LtsllcException {
        sendSynchronize();

        sendAllOwners();
        sendAllMessages();
    }

    /**
     * Tell another node to take possession of a message
     *
     * @param uuid The id of the message we are assigning.
     */
    public void assignMessage(UUID uuid) throws IOException {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ASSIGN_MESSAGE);
        stringBuffer.append(" ");
        stringBuffer.append(uuid);
        channel.writeAndFlush(stringBuffer.toString());
    }

    /**
     * Take possession of a message
     * <H>
     *     This method tells the cluster that this node is taking possession of the message.
     * </H>
     *
     * @param input The assignment message.
     */
    public void handleAssign(String input) {
        Scanner scanner = new Scanner(input);
        scanner.next();
        scanner.next(); // ASSIGN MESSAGE
        UUID uuid = UUID.fromString(scanner.next());

        Cluster.getInstance().takeOwnershipOf (Miranda.getInstance().getMyUuid(), uuid);
    }

    public void handleStartAcknowledged (String input) {
        Scanner scanner = new Scanner(input);
        scanner.next(); scanner.next(); // START ACKNOWLEDGED

        uuid = UUID.fromString(scanner.next());
        registerUuid(uuid);
        host = scanner.next();
        port = scanner.nextInt();

        setState(GENERAL);
        timeoutsMet.put(Alarms.START, true);
    }

    public void handleStartAcknowledgedGeneral (String input) {
        Scanner scanner = new Scanner(input);
        scanner.next(); scanner.next(); // START ACKNOWLEDGED

        uuid = UUID.fromString(scanner.next());
        registerUuid(uuid);
        host = scanner.next();
        port = scanner.nextInt();

        timeoutsMet.put(Alarms.START, true);
    }

    public void handleErrorGeneral () {
        sendStart(true);
        setState(ClusterConnectionStates.START);
    }

    public void registerUuid (UUID uuid) {
        ChannelHandler channelHandler = channel.pipeline().get("HEARTBEAT");
        HeartBeatHandler heartBeatHandler = (HeartBeatHandler) channelHandler;
        heartBeatHandler.setUuid(uuid);
        heartBeatHandler.setNode(this);
    }

    public void sendLeader () throws LtsllcException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(LEADER);
        stringBuilder.append(" ");
        stringBuilder.append(Cluster.getInstance().getLeaderUuid());

        channel.writeAndFlush(stringBuilder);
    }

    public void handleLeader (String input) throws LtsllcException, IOException {
        Scanner scanner = new Scanner(input);
        scanner.next(); // LEADER
        UUID uuid = UUID.fromString(scanner.next());
        if (uuid.equals(Miranda.getInstance().getMyUuid())) {
            Cluster.getInstance().divideUpMessages();
        }
    }

}

