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
 * The IoHandler for an IoSession.
 *
 * <P>
 *     This classes main responsibility is to select the proper Node for an event, and then to pass the event on to that
 *     Node.  To accomplish this the class maintains a mapping from IoSessions to their respective Node, that must be
 *     maintained by others.
 * </P>
 *
 * @see Node
 * @see IoHandler
 */
public class ClusterHandler implements IoHandler {
    public static final Logger logger = LogManager.getLogger();
    public static final Logger events = LogManager.getLogger("events");


    protected Map<IoSession, Node> ioSessionToNode = new HashMap<>();

    public Map<IoSession, Node> getIoSessionToNode() {
        return ioSessionToNode;
    }

    public void setIoSessionToNode(Map<IoSession, Node> ioSessionToNode) {
        this.ioSessionToNode = ioSessionToNode;
    }


    public ClusterHandler() {}

    /**
     * Ignore the sessionCreated event
     *
     * @param session The new session
     */
    @Override
    public void sessionCreated(IoSession session) {
    }

    /**
     * Pass the sessionOpened event onto the node
     * <P>
     *    The interesting part of the method is the is when a node is trying to connect to this node.  In that case, a
     *    Node does not exist for the connecting node so the method creates a new one.
     * </P>
     *
     * @param ioSession
     * @throws LtsllcException
     */
    @Override
    public void sessionOpened(IoSession ioSession) throws LtsllcException {
        Node node = ioSessionToNode.get(ioSession);

        //
        // This is a new connection add it to the cluster
        //
        if (node == null) {
            node = new Node(Miranda.getInstance().getMyUuid(), "Unknown", -1);
            node.setConnected(true);
            node.setupHeartBeat();
            node.setIoSession(ioSession);

            Cluster.getInstance().addNode(node, ioSession);
        }

        node.sessionOpened(ioSession);
    }

    /**
     * The session is closing --- pass the event along to the Node
     *
     * <P>
     *     If no mapping exists for this IoSession then this represents an error - throw an exception.
     * </P>
     *
     * @param ioSession The closing IoSession.
     * @throws LtsllcException If no mapping exists for the session
     */
    @Override
    public void sessionClosed(IoSession ioSession) throws LtsllcException {
        Node node = ioSessionToNode.get(ioSession);
        if (null == node) {
            throw new LtsllcException("null node");
        }

        node.sessionClosed(ioSession);
    }

    /**
     * Ignore the sessionIdle event.
     *
     * @param session The session
     * @param status The idle status
     */
    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
    }

    /**
     * An exception occurred in this ClusterHandler
     *
     * @param ioSession The session where this occurred
     * @param throwable The exception
     * @throws LtsllcException If no mapping exists for this IoSession
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
     * @throws IOException If the underlying Node throws this exception
     * @throws LtsllcException If the underlying Node throws this exception; but this is also thrown by this method in
     * the case where no mapping exists for the IoSession.
     */
    @Override
    public void messageReceived(IoSession ioSession, Object o) throws IOException, LtsllcException, CloneNotSupportedException {
        logger.debug("entering messageReceived with session = " + ioSession + ", and message = " + o );

        Node node = ioSessionToNode.get(ioSession);
        if (null == node) {
            throw new LtsllcException("null node");
        }

        node.messageReceived(ioSession, o);

        logger.debug("leaving messageReceived.");
    }

    /**
     * Ignore the messageSent event
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
     * Ignore the inputClosed event
     *
     * @param ioSession The node that closed the session
     */
    @Override
    public void inputClosed(IoSession ioSession) {
    }

    /**
     * Ignore the event event.
     *
     * @param session The IoSession on which the event took place.
     * @param event The event/
     */
    @Override
    public void event(IoSession session, FilterEvent event) {
    }
}