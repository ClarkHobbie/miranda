package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.*;
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
public class Cluster implements Alarmable {
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

    /**
     * Add another node to the list of nodes in the cluster
     *
     * <P>
     *     If the node is a duplicate of another node in the cluster (i.e. if it has the same uuid) then the node is
     *     not added and the node's session is closed.
     * </P>
     * @param node The node to add.
     * @param ioSession The session the new node should use.
     */
    public synchronized void addNode (Node node, IoSession ioSession) {
        logger.debug("adding node " + node + " to nodes");

        node.setIoSession(ioSession);

        boolean alreadyPresent = false;
        for (Node node2 : nodes) {
            if ((node.getUuid() != null) && (node2.getUuid() != null) && (node2.getUuid().equals(node.getUuid()))){
                alreadyPresent = true;
                logger.debug("node already present discarding");
                node.closeSession();
            }
        }

        if (!alreadyPresent) {
            nodes.add(node);
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
        node.closeSession();
        clusterHandler.getIoSessionToNode().remove(ioSession);
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
            if (node.getIoSession() == null) {
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
    public void start(List<SpecNode> list) throws LtsllcException {
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
            Node node = new Node(specNode.getHost(), specNode.getPort());
            nodes.add(node);
        }

        boolean tempAllNodesFailed = true;

        for (Node node : nodes) {
            if (node.getIoSession() != null) {
                tempAllNodesFailed = false;
            }
            tempAllNodesFailed = !connectToNode(node,ioConnector);
        }

        if (tempAllNodesFailed) {
            allNodesFailed = true;
        }
    }

    /**
     * Try to connect to a Node
     *
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
            }
        } catch (RuntimeIoException e) {
            logger.error("encountered exception", e);
            returnValue = false;
        }
        if (returnValue) {
            events.info ("Connected to " + node.getHost() + ":" + node.getPort());
        }
        logger.debug("leaving connectToNode with " + returnValue);
        return returnValue;
    }

    /**
     * Initialize the instance
     */
    public synchronized static void defineStatics () {
        instance = new Cluster();
        instance.setUuid(Miranda.getInstance().getMyUuid());

        AlarmClock.getInstance().schedule(getInstance(), Alarms.CLUSTER,
                Miranda.getProperties().getLongProperty(Miranda.PROPERTY_CLUSTER_RETRY));
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
        // if a node is not connected then try and connect it
        //
        for (Node node : nodes) {
            if (node.getIoSession() == null) {
                if (!connectToNode(node,ioConnector)) {
                    returnValue = false;
                }
            }
        }
        logger.debug("leaving reconnect with " + returnValue);
        return returnValue;
    }

    /**
     * Reconnect if any nodes are offline
     *
     * <P>
     *     This method checks to see if we are connected to every node in the cluster.  If we are then it returns
     *     without doing anything.  If we are not, then it tries reconnect to the offline nodes.
     * </P>
     * @throws LtsllcException If there is a problem reconnecting.
     */
    public void reconnectIfNecessary () throws LtsllcException{
        if (connectedToAll()) {
            return;
        } else {
            reconnect();
        }
    }

    /**
     * Return whether we are connected to all of the other nodes in the cluster
     *
     * @return Whether we are connected to all of the other nodes in the cluster.  True if we're connected false
     * otherwise.
     */
    public boolean connectedToAll() {
        boolean returnValue = true;

        //
        // create a list of the other nodes we know about
        //
        for (Node node : nodes ) {
            if (node.getIoSession() == null) {
                returnValue = false;
            }
        }

        return returnValue;
    }

    /**
     * Merge nodes that point to the same thing.
     *
     * <P>
     *     Two nodes point to the same thing when the hosts of the two nodes are the (without case) same, and the ports
     *     are the same.
     * </P>
     */
    public synchronized void coalesce () throws LtsllcException, CloneNotSupportedException {
        List<Node> copy = new ArrayList<>(nodes.size());
        List<Node> results = new ArrayList<>(nodes.size());

        for (Node node : nodes) {
            if ((node.getUuid() != null) || ((node.getHost() != null) && (node.getPort() != -1))){
                Node node2 = (Node) node.clone();
                copy.add(node2);
            }
        }

        while (!copy.isEmpty()) {
            Node node1 = copy.get(0);
            copy.remove(node1);

            while (!copy.isEmpty()) {
                Node node2 = copy.get(0);
                copy.remove(node2);
                if (node1.pointsToTheSameThing(node2)) {
                    node1.merge(node2);
                    node2.closeSession();
                } else {
                    results.add(node2);
                }
            }

            results.add(node1);
        }


        nodes = results;
    }


    /**
     * The alarm method required by the alarmable interface
     *
     * @param alarm The alarm.
     * @throws Throwable If there is a problem reconnecting.
     */
    @Override
    public void alarm(Alarms alarm) throws Throwable {
        if (alarm == Alarms.CLUSTER) {
            reconnectIfNecessary();
        }
    }

    /**
     * Divide up a node's messages among the still-connected nodes
     *
     * @param uuid The node whose messages we are dividing up
     * @throws IOException If there is a problem taking ownership.
     */
    public synchronized void divideUpNodesMessages (UUID uuid) throws IOException {
        if (uuid == null) {
            return;
        }

        //
        // how many connected nodes do we have?
        //
        List<Node> connectedNodes = new ArrayList<>();
        for (Node node : nodes) {
            if (node.getIoSession() != null) {
                connectedNodes.add(node);
            }
        }

        //
        // dived up the messages among the still-connected nodes
        //
        List<UUID> list = MessageLog.getInstance().getAllMessagesOwnedBy(uuid);
        if (connectedNodes.size() == 1 || connectedNodes.size() == 0) {
            Node node = connectedNodes.get(0);
            node.takeOwnershipOf(list);
        } else {
            for (int i = 0; i < connectedNodes.size(); i++) {
                Node node = connectedNodes.get(i);
                List<UUID> nodeMessages = divideUpMessages(connectedNodes.size(), i, list);
                node.takeOwnershipOf(nodeMessages);
            }
        }
    }

    /**
     * Are we connected to all nodes in the cluster?
     *
     * @return If we are connected to all the nodes in the cluster: true if we are false otherwise
     */
    public synchronized boolean allConnected () {
        for (Node node : nodes) {
            if (node.getIoSession() == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Divide the list up into portions
     *
     * <P>
     *     As far as this method is concerned all portions except the last one are of equal size. The last portion
     *     contains the regular number plus the modulus of the size of the list modulo the number of portions.
     * </P>
     *
     * @param numberOfPortions The number of ways messages will be split.
     * @param myOrder Which portion we want.  Note that this is zero-relative so the order will be 0 for the first
     *                portion, 1 for the second, and so on.
     * @param messages The list to be divided into portions
     * @return The portion of messages that "belong to" myOrder
     */
    public List<UUID> divideUpMessages(int numberOfPortions, int myOrder, List<UUID> messages) {
        int numberOfMessages = messages.size()/numberOfPortions;
        List<UUID> myPortion = new ArrayList<>();

        int remainder = messages.size() % numberOfPortions;

        int startIndex = myOrder * numberOfMessages;
        if ((myOrder + 1) == numberOfPortions) {
            numberOfMessages += remainder;
        }

        for (int i = 0; i < numberOfMessages; i++) {
            UUID newUuid = new UUID(messages.get(i + startIndex).getMostSignificantBits(),
                    messages.get(i + startIndex).getLeastSignificantBits());
            myPortion.add(newUuid);
        }

        return myPortion;
    }

    /**
     * Transfer the ownership of a message
     *
     * @param newOwner The new owner of the message.
     * @param message The message whose ownership is to be transferred
     */
    public synchronized void takeOwnershipOf (UUID newOwner, UUID message) {
        for (Node node : nodes) {
            if (node.getIoSession() != null) {
                node.sendTakeOwnershipOf(newOwner, message);
            }
        }
    }

    /**
     * A node has been declared dead --- divide up its message among the connected survivors
     *
     * @param uuid The uuid of the node being declared dead.
     * @throws IOException If there is a problem transferring ownership.
     */
    public synchronized void deadNode (UUID uuid) throws IOException {

        List<Node> connectedNodes = new ArrayList<>();
        for (Node node: nodes) {
            if (node.getUuid().equals(uuid)) {
                node.setIoSession(null);
            }

            if (node.getIoSession() != null) {
                connectedNodes.add(node);
            }
        }

        List<UUID> messages = MessageLog.getInstance().getAllMessagesOwnedBy(uuid);
        for (int i = 0; i < connectedNodes.size(); i++) {
            List<UUID> aPortion = divideUpMessages(connectedNodes.size(), i, messages);
            Node node = connectedNodes.get(i);
            node.takeOwnershipOf(aPortion);
        }
    }

    /**
     * Notify the cluster of a message delivery
     * <P>
     *     This consists of passing the message along to the nodes of the cluster.  This is to let the clients know that
     *     they can stop tracking the message.
     * </P>
     * @param message The message that has been delivered
     */
    public synchronized void notifyOfDelivery (Message message) {
        for (Node node : nodes) {
            if (null != node.getIoSession()) {
                node.notifyOfDelivery(message);
            }
        }
    }
}
