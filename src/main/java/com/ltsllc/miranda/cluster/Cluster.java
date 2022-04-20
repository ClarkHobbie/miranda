package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.MessageLog;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.logging.LoggingCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;




/**
 * A Miranda cluster
 *
 * A group of nodes exchanging heart beet messages. A heart beet message consists of:
 * <PRE>
 * HEARTBEET &lt;node UUID&gt;
 * </PRE>
 * A node that doesn't respond back with a heart beet in a configurable period of time is considered
 * dead. A dead node is announced via:
 * <PRE>
 * DEAD &lt;UUID of dead node&gt;
 * </PRE>
 * All other nodes must respond in a configurable period of time with:
 * <PRE>
 * DEAD &lt;UUID of dead node&gt;
 * </PRE>
 * The survivors then auction off the dead node's messages.  An auction consists of the survivors
 * creating bids for each message. A bid consists of:
 * <PRE>
 * BID &lt;UUID of message&gt; &lt;the writer's randomly generated integer&gt;
 * </PRE>
 * In the case of a tie the surviving nodes bid again until there is no tie. In the case where
 * there is no tie the winning node doesn't respond with a message: it merely takes possession
 * of the message and goes onto the next bid.  When the survivors have auctioned off all the
 * messages, they go back to heart beets.
 * <P>
 * When a new node joins the cluster it announces itself to all the cluster members via:
 * <PRE>
 * NEW NODE &lt;UUID of new node&gt;
 * </PRE>
 * The other nodes have a configurable amount of time to respond with:
 * <PRE>
 * CONFIRM NEW NODE &lt;UUID of new node&gt;
 * </PRE>
 * <P>
 * When a node creates a new message it informs the other node via:
 * <PRE>
 * NEW MESSAGE &lt;UUID of new message&gt; STATUS: &lt;URL of status URL&gt; DELIVERY: &lt;URL of delivery URL&gt;
 * CONTENTS:
 * &lt;Contents of the message&gt;
 * </PRE>
 * The other nodes make no reply.
 * <P>
 * When a node delivers a message, it tells the rest of the cluster via:
 * <PRE>
 * MESSAGE DELIVERED &lt;UUID of message&gt;
 * </PRE>
 * The other nodes make no reply.
 *
 */
public class Cluster {
    public static final long BID_WRITE_TIMEOUT = 1000;
    public static final TimeUnit BID_WRITE_TIMEOUT_UNITS = TimeUnit.MILLISECONDS;
    public static final long BID_READ_TIMEOUT = 1000;
    public static final TimeUnit BID_READ_TIMEOUT_UNITS = TimeUnit.MILLISECONDS;
    public static final long IOD_TIMEOUT = 1000;
    public static final TimeUnit IOD_TIMEOUT_UNITS = TimeUnit.MILLISECONDS;
    public static final long IONM_TIMEOUT = 1000;
    public static final TimeUnit IONM_TIMEOUT_UNITS = TimeUnit.MILLISECONDS;

    /**
     * The random number generator.  Usually swapped out when you want to win (or lose) a bid.
     */
    protected static ImprovedRandom randomNumberGenerator = new ImprovedRandom();

    protected static final Logger logger = LogManager.getLogger();
    public static final Logger events = LogManager.getLogger("events");
    protected static Cluster instance = null;

    /**
     * Did all the nodes fail to connect?  Is the node disconnected from all other nodes in the cluster?
     */
    protected boolean allNodesFailed = false;

    public boolean getAllNodesFailed() {
        return allNodesFailed;
    }

    public void setAllNodesFailed(boolean allNodesFailed) {
        this.allNodesFailed = allNodesFailed;
    }

    /**
     * A list of the other nodes in the cluster
     */
    protected List<Node> nodes = new ArrayList<>();

    /**
     * This node's UUID
     */
    protected UUID uuid = null;

    /**
     * The IoConnector to use when trying to connect to other nodes
     */
    protected IoConnector ioConnector = null;

    /**
     * The IoAcceptor to use when other nodes try and connect to us.
     */
    protected IoAcceptor ioAcceptor = null;

    public IoAcceptor getIoAcceptor() {
        if (null == ioAcceptor) {
            ioAcceptor = new NioSocketAcceptor();
        }

        return ioAcceptor;
    }

    public void setIoAcceptor(IoAcceptor ioAcceptor) {
        this.ioAcceptor = ioAcceptor;
    }

    /**
     * The IoHandler for the acceptor and the connector
     */
    protected ClusterHandler clusterHandler;

    public ClusterHandler getClusterHandler() {
        return clusterHandler;
    }

    public void setClusterHandler(ClusterHandler clusterHandler) {
        this.clusterHandler = clusterHandler;
    }

    public IoConnector getIoConnector() {
        if (ioConnector == null) {
            ioConnector = new NioSocketConnector();
        }

        return ioConnector;
    }

    public void setIoConnector(IoConnector ioConnector) {
        this.ioConnector = ioConnector;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public static Cluster getInstance() {
        return instance;
    }

    public static void setInstance(Cluster instance) {
        Cluster.instance = instance;
    }

    public Cluster() {
        clusterHandler = new ClusterHandler();
        ioConnector = new NioSocketConnector();
        ioConnector.setHandler(clusterHandler);
        ioAcceptor = new NioSocketAcceptor();
        ioAcceptor.setHandler(clusterHandler);
    }

    public synchronized List<Node> getNodes() {
        return nodes;
    }

    public synchronized void setNodes(List<Node> n) {
        nodes = n;
    }

    public static ImprovedRandom getRandomNumberGenerator() {
        return randomNumberGenerator;
    }

    /**
     * Set the random number generator that this node uses
     *
     * <P>
     *     The random number generator is used mostly in bidding
     * </P>
     */
    public static void setRandomNumberGenerator(ImprovedRandom randomNumberGenerator) {
        Cluster.randomNumberGenerator = randomNumberGenerator;
    }

    public synchronized void addNode (Node node, IoSession ioSession) {
        logger.debug("adding node " + node + " to nodes");
        boolean alreadyPresent = false;
        for (Node node2 : nodes) {
            if (node2.getHost().equalsIgnoreCase(node.getHost()) &&
                    (node2.getPort() == node.getPort()) ) {
                alreadyPresent = true;
                logger.debug("node already present discarding");
                if (node.isConnected()) {
                    node2.setIoSession(node.getIoSession());
                }
            }
        }
        if (!alreadyPresent) {
            nodes.add(node);
        }

        for (Node node2 : nodes) {
            clusterHandler.getIoSessionToNode().put(ioSession, node);
            clusterHandler.getIoSessionToNode().put(ioSession, node);
        }
    }

    /**
     * remove a node from the cluster
     * <P>
     *     This method does so synchronously, so it is thread safe.
     * </P>
     */
    public synchronized void removeNode(Node node, IoSession ioSession) {
        logger.debug("removing node, " + node + " from nodes");
        nodes.remove(node);
        for (Node node2 : nodes) {
            clusterHandler.getIoSessionToNode().remove(ioSession);
        }
    }



    /**
     * Inform the cluster of this node receiving a new message
     *
     * @param message The message we received.
     */
    public void informOfNewMessage(Message message) {
        logger.debug("entering informOfNewMessage with message = " + message);
        String contents = "MESSAGE CREATED " + message.longToString();

        logger.debug("POST contents = " + contents);
        for (Node node : nodes) {
            WriteFuture future = node.getIoSession().write(contents);
            try {
                if (!future.await(IONM_TIMEOUT, IONM_TIMEOUT_UNITS)) {
                    logger.error("write timed out informing another node of new message");
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted during wait in informOfNewMessage", e);
            }
        }
        logger.debug("leaving informOfNewMessage");
    }


    /**
     * Tell the cluster that we delivered a message
     * <P>
     *     When another node is told of a message delivery, it can free up memory it was using to record the message.
     * </P>
     *
     * @param message The message that we delivered.
     */
    public void informOfDelivery(Message message) {
        logger.debug("entering informOfDelivery with message = " + message);

        String contents = "MESSAGE DELIVERED " + message.getMessageID();
        logger.debug("session contents = " + contents);
        for (Node node : nodes) {
            if (!node.isConnected()) {
                continue;
            }
            WriteFuture future = node.getIoSession().write(contents);
            try {
                future.await(IOD_TIMEOUT, IOD_TIMEOUT_UNITS);
            } catch (InterruptedException e) {
                logger.error("Interrupted during wait for write to complete for some in informOfDelivery", e);
            }
        }

        logger.debug("leaving informOfDelivery");
    }

    /**
     * Connect to the other nodes in the cluster
     *
     * @param list A list of nodes that make up the cluster.
     * @throws LtsllcException Both listen and connectNode, which this method calls, throw this exception.
     * @see #listen()
     * @see #connectNodes(List)
     */
    public void connect(List<SpecNode> list) throws LtsllcException {
        listen();
        connectNodes(list);
    }

    /**
     * Listen at the cluster port
     *
     * @throws LtsllcException If there is a problem listening
     */
    public void listen() throws LtsllcException {
        logger.debug("entering listen ");
        if (nodes == null) {
            nodes = new ArrayList<>();
        }

        IoAcceptor ioAcceptor = getIoAcceptor();
        ioAcceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));

        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);

        SocketAddress socketAddress = new InetSocketAddress(port);

        try {
            logger.debug("listening at port " + port);
            ioAcceptor.bind(socketAddress);
        } catch (IOException e) {
            logger.error("exception binding to cluster port, " + Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT), e);
            throw new LtsllcException(
                    "exception binding to cluster port, "
                            + Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT),
                    e
            );
        }
        logger.debug("leaving listen");
    }

    /**
     * Connect to the other nodes in the cluster
     *
     * @param list The list of nodes to connect to.
     * @throws LtsllcException If there is a problem with the ClusterHandler for the connection.
     */
    public synchronized void connectNodes(List<SpecNode> list) throws LtsllcException {
        logger.debug("entering connectNodes");
        events.info("Trying to connect to other nodes");

        if (Miranda.getProperties().getProperty(Miranda.PROPERTY_CLUSTER).equals("off")) {
            logger.debug("clustering is off returning");
            events.info("Clustering is off, aborting connect");
            return;
        }

        ioConnector = getIoConnector();
        ioConnector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory()));

        for (SpecNode specNode: list) {
            Node node = new Node(Miranda.getInstance().getMyUuid(), specNode.getHost(), specNode.getPort());
            nodes.add(node);
        }

        for (Node node : nodes) {
            if (node.isConnected()) {
                continue;
            }

            connectToNode(node,ioConnector);
        }

        boolean tempAllNodesFailed = false;
        for (Node node : nodes) {
            tempAllNodesFailed = !node.isConnected();
        }

        if (tempAllNodesFailed) {
            allNodesFailed = true;
        }
    }

    /**
     * Try to connect to a Node
     *
     * <P>
     *     This method updates the node's connected attribute to reflect whether it is connected.
     * </P>
     * @param node The node to try and connect to.
     * @param ioConnector Over what to make the connection.
     * @return true if we were able to connect to the node, false otherwise.
     */
    public boolean connectToNode(Node node, IoConnector ioConnector) throws LtsllcException {
        boolean returnValue = true;
        logger.debug("entering connectToNode with node = " + node.getHost() + ":" + node.getPort() + ", and ioConnector = " + ioConnector);
        InetSocketAddress addrRemote = new InetSocketAddress(node.getHost(), node.getPort());

        ConnectFuture future = getIoConnector().connect(addrRemote);
        future.awaitUninterruptibly();
        IoSession ioSession = null;
        try {
            if (future.getException() == null) {
                ioSession = future.getSession();
                ioSession.getConfig().setUseReadOperation(true);
                node.setIoSession(ioSession);
                node.setConnected(true);
                node.setupHeartBeat();
                clusterHandler.getIoSessionToNode().put(ioSession, node);
                if (Miranda.getInstance().getSynchronizationFlag()) {
                    Miranda.getInstance().setSynchronizationFlag(false);
                    node.sendSynchronizationStart(ioSession);
                } else {
                    node.sendStart(ioSession);
                }
            } else {
                logger.debug("failed to connect to " + node.getHost() + ":" + node.getPort());
                returnValue = false;
                node.setConnected(false);
                node.stopHeartBeat();
                Miranda.getInstance().setClusterAlarm(System.currentTimeMillis());
            }
        } catch (RuntimeIoException e) {
            logger.error("encountered exception", e);
            returnValue = false;
            Miranda.getInstance().setClusterAlarm(System.currentTimeMillis() + Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_RETRY));
        }
        if (returnValue) {
            events.info ("connected to " + node.getHost() + ":" + node.getPort());
        }
        logger.debug("leaving connectToNode with " + returnValue);
        return returnValue;
    }

    public synchronized static void defineStatics (UUID myID) {
        instance = new Cluster();
        instance.setUuid(myID);
    }

    /**
     * try to connect to all nodes that we're not already connected to.
     *
     * @return Whether we are connected to all the other nodes.
     */
    public synchronized boolean reconnect () throws LtsllcException {
        logger.debug("entering reconnect");
        boolean returnValue = true;

        //
        // if we're already connected to everyone then there is nothing to do
        //
        if (connectedToAll()){
            Miranda.getInstance().setClusterAlarm(-1);
            logger.debug("we're all connected, returning true");
            return true;
        }

        for (Node node : nodes) {
            if (!node.isConnected()) {
                if (!connectToNode(node,ioConnector)) {
                    Miranda.getInstance().setClusterAlarm(System.currentTimeMillis() + Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_RETRY));
                    returnValue = false;
                }
            }
        }
        logger.debug("leaving reconnect with " + returnValue);
        return returnValue;
    }

    /**
     * Return whether we are connected to all of the other nodes in the cluster
     *
     * @return Whether we are connected to all of the other nodes in the cluster.  True if we're connected false
     * otherwise.
     */
    public boolean connectedToAll() {
        boolean returnValue = false;

        //
        // create a list of the other nodes we know about
        //
        for (Node node : nodes ) {
            if (!node.isConnected()) {
                returnValue = false;
            }
        }
        return returnValue;
    }

    /**
     * Merge nodes that point to the same thing.
     *
     * <P>
     *     Two nodes point to the same thing when the hosts of the two nodes are the (without case), and the ports are
     *     the same.
     * </P>
     */
    public synchronized void coalesce () throws LtsllcException, CloneNotSupportedException {
        List<Node> results = new ArrayList<>(nodes.size());

        for (Node node : nodes) {
            Node node2 = (Node) node.clone();
            results.add(node2);
        }

        for (int i = 0; i  < nodes.size(); i++) {
            for (int j = 1; j < nodes.size(); j++) {
                if (i == j) {
                    continue;
                }

                Node node1 = nodes.get(i);
                Node node2 = nodes.get(j);
                if ((node1.getHost().equalsIgnoreCase(node2.getHost())) &&
                        (node1.getPort() == node2.getPort())) {
                    Node mergedNode = mergeNodes(i, j, nodes);
                }
            }
        }

        nodes = results;
    }

    /**
     * Merge two nodes
     */
    public Node mergeNodes (int index1, int index2, List<Node> list) throws LtsllcException {
        Node node1 = list.get(index1);
        Node node2 = list.get(index2);

        boolean connected = node1.isConnected() || node2.isConnected();
        node1.setConnected(connected);

        if ((node2.getUuid() != null) && (node1.getUuid() !=null) && (!node1.getUuid().equals(node2.getUuid()))) {
            throw new LtsllcException("nodes have different UUIDs");
        } else if ((node1.getUuid() == null) && (node2.getUuid() == null)) {
            throw new LtsllcException("both nodes have null UUIDs");
        } else if (node1.getUuid() == null) {
            node1.setUuid(node2.getUuid());
        }
        //
        // if it gets to this point node2.getUuid may be null, in which case we should use node1's UUID, but we are
        // returning node1 so there is nothing to do
        //

        if ((node1.getPartnerID() != null) && (node2.getPartnerID() != null) &&
                (!node1.getPartnerID().equals(node2.getPartnerID()))) {
            throw new LtsllcException("nodes have different partner UUIDs");
        } else if ((node1.getPartnerID() == null) && (node2.getPartnerID() == null)) {
            throw new LtsllcException("both nodes have null partner UUIDs");
        } else if (node1.getPartnerID() == null) {
            node1.setPartnerID(node2.getPartnerID());
        }
        //
        // Once again there is nothing to do
        //

        if ((!node1.isAlreadyHaveAHeartBeat()) && (node2.isAlreadyHaveAHeartBeat())) {
            node1.setupHeartBeat();
        } else if (!node1.isAlreadyHaveAHeartBeat()) {
            node1.setupHeartBeat();
        }
        //
        // it is possible for node1 to have a heart beat and node2 doesn't but there is nothing to do in that case
        //

        if (node1.getTimeOfLastActivity() < node2.getTimeOfLastActivity()) {
            node1.setTimeOfLastActivity(node2.getTimeOfLastActivity());
        } else if ((node1.getTimeOfLastActivity() == null) && (node2.getTimeOfLastActivity() != null)) {
            node1.setTimeOfLastActivity(node2.getTimeOfLastActivity());
        }
        //
        // many other cases that boil down to just using node1's value
        //

        if ((node1.getIoSession() == null) && (node2.getIoSession() != null)) {
            throw new LtsllcException("node1 has a null ioSession while node2 does not");
        }

        if (node2.getIoSession() != null) {
            node2.getIoSession().closeNow();
        }

        if (node1.getCache() == null) {
            node1.setCache(MessageLog.getInstance().getCache());
        }

        if (node2.getIoSession() != null) {
            Cluster.getInstance().getClusterHandler().getIoSessionToNode().remove(node2.getIoSession());
        }

        return node1;
    }

}
