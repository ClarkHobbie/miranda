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
    public static final String SYNCHRONIZE = "SYNCHRONIZE";
    public static final String SYNCHRONIZE_START = "SYNCHRONIZE START";
    public static final String TIMEOUT = "TIMEOUT";



    protected Map<IoSession, Node> ioSessionToNode = new HashMap<>();

    public Map<IoSession, Node> getIoSessionToNode() {
        return ioSessionToNode;
    }

    public void setIoSessionToNode(Map<IoSession, Node> ioSessionToNode) {
        this.ioSessionToNode = ioSessionToNode;
    }


    public ClusterHandler() {
    }

    @Override
    public void sessionCreated(IoSession session) {
    }

    @Override
    public void sessionOpened(IoSession ioSession) throws LtsllcException {
        Node node = ioSessionToNode.get(ioSession);
        if (node == null) {
            node = new Node("Unknown", -1);
        }

        node.sessionOpened(ioSession);
    }

    @Override
    public void sessionClosed(IoSession ioSession) throws LtsllcException {
        Node node = ioSessionToNode.get(ioSession);
        if (null == node) {
            throw new LtsllcException("null node");
        }

        node.sessionClosed(ioSession);
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
    public void exceptionCaught(IoSession ioSession, Throwable throwable) throws LtsllcException {
        Node node = ioSessionToNode.get (ioSession);
        if (node == null) {
            throw new LtsllcException("null node");
        }

        node.exceptionCaught (ioSession, throwable);
    }

    /**
     * The node received a message
     *
     * @param ioSession The sender node
     * @param o         The message
     */
    @Override
    public void messageReceived(IoSession ioSession, Object o) throws IOException, LtsllcException {
        logger.debug("entering messageReceived with session = " + ioSession + ", and message = " + o );
        Node node = ioSessionToNode.get(ioSession);
        if (null == node) {
            throw new LtsllcException("null node");
        }

        node.messageReceived(ioSession, o);

        logger.debug("leaving messageReceived.");
    }

    /**
     * The node sent a message
     *
     * @param session The session over which the message was sent.
     * @param message The message.
     */
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
}