package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.Message;
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
     * remove a node from the cluster
     * <P>
     *     This method does so synchronously, so it is thread safe.
     * </P>
     */
    public synchronized void removeNode(Node node) {
        logger.debug("removing node, " + node + " from nodes");
        nodes.remove(node);
    }

    /*
    */

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
                    logger.error("write timed out informing another node of message delivery");
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
        uuid = UUID.randomUUID();
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
        ImprovedProperties p = Miranda.getProperties();
        Node node = new Node(UUID.randomUUID(), p.getProperty(Miranda.PROPERTY_HOST), p.getIntProperty(Miranda.PROPERTY_CLUSTER_PORT));

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);

        ioAcceptor.setHandler(clusterHandler);

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
            return;
        }

        ioConnector = getIoConnector();
        ImprovedProperties p = Miranda.getProperties();
        Node newNode = new Node(UUID.randomUUID(), p.getProperty(Miranda.PROPERTY_HOST), p.getIntProperty(Miranda.PROPERTY_CLUSTER_PORT));

        ImprovedFile messages = new ImprovedFile(Miranda.PROPERTY_MESSAGE_LOG);
        int loadLimit = p.getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT);
        LoggingCache cache = new LoggingCache(messages,loadLimit);
        ClusterHandler clusterHandler = new ClusterHandler(newNode, cache);

        ioConnector.setHandler(clusterHandler);
        ioConnector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory()));

        for (SpecNode specNode: list) {
            Node node = new Node(specNode.getHost(), specNode.getPort());
            node.setConnected(false);
            nodes.add(node);
        }

        for (Node node : nodes) {
            if (node.isConnected()) {
                continue;
            }

            node.setConnected(connectToNode(node,ioConnector));
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
        getIoConnector().setHandler(new SimpleIoHandler());
        ConnectFuture future = getIoConnector().connect(addrRemote);
        future.awaitUninterruptibly();
        IoSession ioSession = null;
        try {
            ioSession = future.getSession();
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(ClusterHandler.START);
            stringBuffer.append(" ");
            stringBuffer.append(uuid);

            WriteFuture writeFuture = ioSession.write(stringBuffer.toString());
            try {
                writeFuture.await();
            } catch (InterruptedException e) {
            }

            writeFuture.getSession().getConfig().setUseReadOperation(true);
            node.setIoSession(writeFuture.getSession());
            node.setConnected(true);
            logger.debug("wrote " + stringBuffer.toString());
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
    public boolean reconnect () throws LtsllcException {
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
                if (connectToNode(node,ioConnector)) {
                    node.setConnected(true);
                } else {
                    node.setConnected(false);
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
 }
