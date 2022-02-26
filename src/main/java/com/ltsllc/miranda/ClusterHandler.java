package com.ltsllc.miranda;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;

/**
 * A connection to another node in the cluster
 *
 * This class implements the Cluster protocol (see the package documentation).
 */
public class ClusterHandler implements IoHandler {
    public static final Logger logger = LogManager.getLogger();

    /**
     * The connection state.  The connection starts in the start state.
     */
    protected ClusterConnectionStates state = ClusterConnectionStates.STATE_START;


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
    public void sessionIdle(IoSession ioSession, IdleStatus idleStatus) throws Exception {

    }

    @Override
    public void exceptionCaught(IoSession ioSession, Throwable throwable) throws Exception {

    }

    /**
     * The node received a message
     *
     * <P>
     *     This method is really the "heart" of this class, and a through understanding of this method is required to
     *     understand this class.
     * <P>
     *     The types of unannounced messages that a node can receive depends on the current state:
     *     <UL>
     *         <LI>general: heart beat message, get message, new message, message delivered, auction, and dead node</LI>
     *         <LI>start: start message, new node message</LI>
     *         <LI>auction: bid message, auction over message</LI>
     *         <LI>new node: message message, new node over message</LI>
     *     </UL>
     *
     * <P>
     *     The general state is where a connection usually exists while it is awaiting the next message.
     *
     * @param ioSession  The sender node
     * @param o          The message
     * @throws Exception
     */
    @Override
    public void messageReceived(IoSession ioSession, Object o) throws Exception {
        determineMessageType(o);
        switch (state)
        }
    }

    @Override
    public void messageSent(IoSession ioSession, Object o) throws Exception {

    }

    @Override
    public void inputClosed(IoSession ioSession) throws Exception {

    }

    @Override
    public void event(IoSession ioSession, FilterEvent filterEvent) throws Exception {

    }

    protected MessageType determineMessageType(Object o) {
        MessageType messageType = MessageType.MSG_TYPE_UNKNOWN;

        String s = (String) o;

        switch ()

        return messageType;
    }

}
