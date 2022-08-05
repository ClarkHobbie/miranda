package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.UncheckedLtsllcException;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.alarm.AlarmClock;
import com.ltsllc.miranda.alarm.Alarmable;
import com.ltsllc.miranda.alarm.Alarms;
import com.ltsllc.miranda.netty.ClientChannelToNodeDecoder;
import com.ltsllc.miranda.netty.HeartBeatHandler;
import com.ltsllc.miranda.netty.ServerChannelToNodeDecoder;
import com.ltsllc.miranda.netty.StringEncoder;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.message.MessageLog;
import com.ltsllc.miranda.properties.Properties;
import com.ltsllc.miranda.properties.PropertyChangedEvent;
import com.ltsllc.miranda.properties.PropertyListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * A Miranda cluster
 * <p>
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
 * <p>
 * When a new node joins the cluster it announces itself to all the cluster members via:
 * <PRE>
 * NEW NODE &lt;UUID of new node&gt;
 * </PRE>
 * The other nodes have a configurable amount of time to respond with:
 * <PRE>
 * CONFIRM NEW NODE &lt;UUID of new node&gt;
 * </PRE>
 * <p>
 * When a node creates a new message it informs the other node via:
 * <PRE>
 * NEW MESSAGE &lt;UUID of new message&gt; STATUS: &lt;URL of status URL&gt; DELIVERY: &lt;URL of delivery URL&gt;
 * CONTENTS:
 * &lt;Contents of the message&gt;
 * </PRE>
 * The other nodes make no reply.
 * <p>
 * When a node delivers a message, it tells the rest of the cluster via:
 * <PRE>
 * MESSAGE DELIVERED &lt;UUID of message&gt;
 * </PRE>
 * The other nodes make no reply.
 */
public class Cluster implements Alarmable, PropertyListener {
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


    protected Bootstrap bootstrap;

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    protected ServerBootstrap serverBootstrap;

    public ServerBootstrap getServerBootstrap() {
        return serverBootstrap;
    }

    public void setServerBootstrap(ServerBootstrap serverBootstrap) {
        this.serverBootstrap = serverBootstrap;
    }

    protected Election election;

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Is the cluster bound?  For debugging
     */
    protected boolean bound = false;

    public boolean isBound() {
        return bound;
    }

    public void setBound(boolean bound) {
        this.bound = bound;
    }

    public static Cluster getInstance() {
        return instance;
    }

    public static void setInstance(Cluster instance) {
        Cluster.instance = instance;
    }

    public Cluster() {
        initialize();
    }

    public synchronized List<Node> getNodes() {
        return nodes;
    }

    public synchronized void setNodes(List<Node> n) {
        nodes = n;
    }

    protected Map<HeartBeatHandler, UUID> heartBeatHandlerToUUIDMap = new HashMap<>();
    /**
     * Set the random number generator that this node uses
     *
     * <p>
     * The random number generator is used mostly in bidding
     * </P>
     */
    public static void setRandomNumberGenerator(ImprovedRandom randomNumberGenerator) {
        Cluster.randomNumberGenerator = randomNumberGenerator;
    }

    public boolean allVotesIn() throws LtsllcException {
        if (null == election) {
            throw new LtsllcException("null election");
        }

        return election.allVotesIn();
    }


    public void initialize() {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        "LENGTH", new LengthFieldBasedFrameDecoder(2048, 0, 4, 0, 4));
                ch.pipeline().addLast("STRING",new StringEncoder());
                ch.pipeline().addLast("HEARTBEAT", new HeartBeatHandler(ch));
                ch.pipeline().addLast("DECODER", new ClientChannelToNodeDecoder());
            }
        });
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                   public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                "LENGTH", new LengthFieldBasedFrameDecoder(2048, 0, 4, 0, 4)
                                );
                        ch.pipeline().addLast("ENCODER",new StringEncoder());
                        ch.pipeline().addLast("HEARTBEAT",new HeartBeatHandler(ch));
                        ch.pipeline().addLast("DECODER", new ServerChannelToNodeDecoder());
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
    }

    /**
     * Add another node to the list of nodes in the cluster
     *
     * <p>
     * If the node is a duplicate of another node in the cluster (i.e. if it has the same uuid) then the node is
     * not added and the node's session is closed.
     * </P>
     *
     * @param node      The node to add.
     */
    public synchronized void addNode(Node node) {
        logger.debug("adding node " + node + " to nodes");

        boolean alreadyPresent = false;
        for (Node node2 : nodes) {
            if ((node.getUuid() != null) && (node2.getUuid() != null) && (node2.getUuid().equals(node.getUuid()))) {
                alreadyPresent = true;
                logger.debug("node already present discarding");
                node.closeChannel();
            }
        }

        if (!alreadyPresent) {
            nodes.add(node);
        }
    }

    /**
     * remove a node from the cluster
     * <p>
     * This method does so synchronously, so it is thread safe.
     * </P>
     */
    public synchronized void removeNode(Node node) {
        logger.debug("removing node, " + node + " from nodes");
        nodes.remove(node);
        node.closeChannel();
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
            if (node.getChannel() == null) {
                continue;
            }
            ChannelFuture channelFuture = node.getChannel().writeAndFlush(contents);
            try {
                if (!channelFuture.await(IONM_TIMEOUT, IONM_TIMEOUT_UNITS)) {
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
     * <p>
     * When another node is told of a message delivery, it can free up memory it was using to record the message.
     * </P>
     *
     * @param message The message that we delivered.
     */
    public void informOfDelivery(Message message) {
        logger.debug("entering informOfDelivery with message = " + message);

        String contents = "MESSAGE DELIVERED " + message.getMessageID();
        logger.debug("session contents = " + contents);
        for (Node node : nodes) {
            if (node.getChannel() == null) {
                continue;
            }

            ChannelFuture channelFuture = node.getChannel().writeAndFlush(contents);
            try {
                channelFuture.await(IOD_TIMEOUT, IOD_TIMEOUT_UNITS);
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
    public void start(List<SpecNode> list) throws LtsllcException, CloneNotSupportedException {
        Miranda.getProperties().listen(this, Properties.cluster);
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

        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        Miranda.getProperties().listen(this, Properties.cluster);

        SocketAddress socketAddress = new InetSocketAddress(port);

        logger.debug("listening at port " + port);
        serverBootstrap.bind(socketAddress);
        bound = true;

        logger.debug("leaving listen");
    }

    /**
     * Connect to the other nodes in the cluster
     *
     * @param list The list of nodes to connect to.
     * @throws LtsllcException If there is a problem with the ClusterHandler for the connection.
     */
    public synchronized void connectNodes(List<SpecNode> list) throws LtsllcException, CloneNotSupportedException {
        logger.debug("entering connectNodes");
        events.info("Trying to connect to other nodes");

        Miranda.getProperties().listen(this, Properties.cluster);
        if (Miranda.getProperties().getProperty(Miranda.PROPERTY_CLUSTER).equals("off")) {
            logger.debug("clustering is off returning");
            events.info("Clustering is off, aborting connect");
            return;
        }

        for (SpecNode specNode : list) {
            Node node = new Node(null, specNode.getHost(), specNode.getPort(), null);
            nodes.add(node);
        }

        boolean tempAllNodesFailed = true;


        for (Node node : nodes) {
            if (node.getChannel() != null) {
                tempAllNodesFailed = false;
            } else {
                tempAllNodesFailed = !connectToNode(node);
            }
        }

        if (tempAllNodesFailed) {
            allNodesFailed = true;
        }
    }

    /**
     * Try to connect to a Node
     *
     * @param node The node to try and connect to.
     * @return true if we were able to connect to the node, false otherwise.
     */
    public boolean connectToNode(Node node) throws LtsllcException, CloneNotSupportedException {
        boolean returnValue = true;
        logger.debug("entering connectToNode with " + node.getHost() + ":" + node.getPort());
        if ((node.getHost() == null) || (node.getPort() == -1)) {
            logger.error("connectToNode called with null host or -1 port, returning");
            return false;
        }

        InetSocketAddress addrRemote = new InetSocketAddress(node.getHost(), node.getPort());
        ChannelFuture channelFuture = bootstrap.connect(addrRemote);
        try {
            channelFuture.await();
            node.setChannel(channelFuture.channel());
            Channel channel = channelFuture.channel();
            ChannelPipeline pipeline = channel.pipeline();
            ChannelHandler channelHandler = pipeline.get("DECODER");
            if (channelHandler instanceof ClientChannelToNodeDecoder) {
                ClientChannelToNodeDecoder decoder = (ClientChannelToNodeDecoder) channelHandler;
                decoder.setNode(node);
            } else {
                throw new LtsllcException("decoder is not an instance of ClientChannelToNodeDecoder");
            }
            channelHandler = pipeline.get("HEARTBEAT");
            if (channelHandler instanceof HeartBeatHandler) {
                HeartBeatHandler heartBeatHandler = (HeartBeatHandler) channelHandler;
                heartBeatHandler.setUuid(node.getUuid());
                heartBeatHandler.setNode(node);
            }
        } catch (InterruptedException e) {
            logger.debug("failed to connect to " + node.getHost() + ":" + node.getPort());
            returnValue = false;
        }

        if (returnValue) {
            events.info("Connected to " + node.getHost() + ":" + node.getPort());
        }

        node.sendStart(true);

        coalesce();

        logger.debug("leaving connectToNode with " + returnValue);
        return returnValue;
    }

    /**
     * Initialize the instance
     */
    public synchronized static void defineStatics() {
        instance = new Cluster();
        instance.setUuid(Miranda.getInstance().getMyUuid());

        AlarmClock.getInstance().schedule(getInstance(), Alarms.CLUSTER,
                Miranda.getProperties().getLongProperty(Miranda.PROPERTY_CLUSTER_RETRY));
        AlarmClock.getInstance().scheduleOnce(getInstance(), Alarms.COALESCE,
                Miranda.getProperties().getIntProperty(Miranda.PROPERTY_COALESCE_PERIOD));
    }

    /**
     * try to connect to all nodes that we're not already connected to.
     *
     * @return Whether we are connected to all the other nodes.
     */
    public synchronized boolean reconnect() throws LtsllcException, CloneNotSupportedException {
        logger.debug("entering reconnect");
        boolean returnValue = true;

        //
        // if a node is not connected then try and connect it
        //
        for (Node node : nodes) {
            if (node.getChannel() == null) {
                if (!connectToNode(node)) {
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
     * <p>
     * This method checks to see if we are connected to every node in the cluster.  If we are then it returns
     * without doing anything.  If we are not, then it tries reconnect to the offline nodes.
     * </P>
     *
     * @throws LtsllcException If there is a problem reconnecting.
     */
    public void reconnectIfNecessary() throws LtsllcException, CloneNotSupportedException {
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
        for (Node node : nodes) {
            if (node.getChannel() == null) {
                returnValue = false;
            }
        }

        return returnValue;
    }

    /**
     * Merge nodes that point to the same thing.
     *
     * <p>
     * Two nodes point to the same thing when the hosts of the two nodes are the (without case) same, and the ports
     * are the same.
     * </P>
     */
    public synchronized void coalesce() throws LtsllcException, CloneNotSupportedException {
        if (nodes.size() < 2)
            return;
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                Node node1 = nodes.get(i);
                Node node2 = nodes.get(j);
                if (node1.pointsToTheSameThing(node2)) {
                    node1.merge(node2);
                    removeNode(node2);
                }
            }
        }
    }


    /**
     * The alarm method required by the alarmable interface
     *
     * @param alarm The alarm.
     * @throws Throwable If there is a problem reconnecting.
     */
    @Override
    public void alarm(Alarms alarm) throws Throwable {
        switch (alarm) {
            case CLUSTER: {
                reconnectIfNecessary();
                break;
            }

            case COALESCE: {
                coalesce();
                break;
            }

            case SCAN: {
                scan();
                break;
            }

            default: {
                String msg = "unrecognized alarm: " + alarm;
                logger.error(msg);
                throw new LtsllcException(msg);
            }
        }
    }

    /**
     * Are we connected to all nodes in the cluster?
     *
     * @return If we are connected to all the nodes in the cluster: true if we are false otherwise
     */
    public synchronized boolean allConnected() {
        for (Node node : nodes) {
            if (node.getChannel() == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Divide the list up into portions
     *
     * <p>
     * As far as this method is concerned all portions except the last one are of equal size. The last portion
     * contains the regular number plus the modulus of the size of the list modulo the number of portions.
     * </P>
     */
    public void divideUpMessages() throws LtsllcException, IOException {
        if (null == election) {
            throw new LtsllcException("null election");
        }

        election.divideUpNodesMessages();
    }

    /**
     * Transfer the ownership of a message
     *
     * @param newOwner The new owner of the message.
     * @param message  The message whose ownership is to be transferred
     */
    public synchronized void takeOwnershipOf(UUID newOwner, UUID message) {
        for (Node node : nodes) {
            if (node.getChannel() != null) {
                if (node.getChannel() != null) {
                    node.sendTakeOwnershipOf(newOwner, message);
                }
            }
        }
    }

    /**
     * A node has been declared dead --- divide up its messages among the connected survivors
     *
     * @param uuid The uuid of the node being declared dead.
     * @throws IOException If there is a problem transferring ownership.
     */
    public synchronized void deadNode(UUID uuid, Node node) throws IOException {
        election = new Election(nodes);
        election.setDeadNode(uuid);

        for (Node n : nodes) {
            if (n.getChannel() != null) {
                n.sendDeadNode(uuid);
            } else if (n.getChannel() != null && null != n.getUuid() && !n.getUuid().equals(uuid)) {
                n.sendDeadNodeStart(uuid);
            }
        }

        if (election.getVoters().size() == 1) {
            List<UUID> messages = MessageLog.getInstance().getAllMessagesOwnedBy(uuid);
            for (UUID message: messages) {
                MessageLog.getInstance().setOwner(message, Miranda.getInstance().getMyUuid());
            }
        }
    }

    /**
     * Notify the cluster of a message delivery
     * <p>
     * This consists of passing the message along to the nodes of the cluster.  This is to let the clients know that
     * they can stop tracking the message.
     * </P>
     *
     * @param message The message that has been delivered
     */
    public synchronized void notifyOfDelivery(Message message) {
        for (Node node : nodes) {
            if (null != node.getChannel()) {
                node.notifyOfDelivery(message);
            }
        }
    }

    /**
     * Get the number of nodes that are connected to other nodes.
     * <H>
     * This returns the number of nodes in the custer with a non-null channel.
     * </H>
     *
     * @return The number of nodes in the cluster with non-null channels..
     */
    public int getNumberOfConnections() {
        int count = 0;
        for (Node node : nodes) {
            if (null != node.getChannel()) {
                count++;
            }
        }

        return count;
    }

    /**
     * A property changed that we are watching.
     * <H>
     * The properties that we watch are the custer port and the nodes which make up the cluster.
     * </H>
     *
     * @param propertyChangedEvent The propertyChanged event that kicked off this whole process.
     */
    @Override
    public void propertyChanged(PropertyChangedEvent propertyChangedEvent) throws CloneNotSupportedException {
        try {
            switch (propertyChangedEvent.getProperty()) {
                case clusterPort: {
                    listen();
                    break;
                }

                case cluster: {
                    start(Miranda.getInstance().getSpecNodes());
                    break;
                }

                default: {
                    throw new UncheckedLtsllcException("propertyChanged called with " + propertyChangedEvent.getProperty());
                }
            }
        } catch (LtsllcException e) {
            throw new UncheckedLtsllcException(e);
        }
    }


    /**
     * Return the number of nodes in the cluster
     *
     * @return The number of nodes in the cluster.
     */
    public int getNumberOfNodes() {
        return nodes.size();
    }

    public boolean containsNode(Node node) {
        return nodes.contains(node);
    }

    /**
     * Assign a message to a node.
     *
     * @param receiverUuid The assignee of the message
     * @param messageUuid  The message to be assigned
     * @throws LtsllcException If there is a problem finding the assignee
     */
    public void assignMessageTo(UUID receiverUuid, UUID messageUuid) throws LtsllcException, IOException {
        Node node = findNode(receiverUuid);
        node.assignMessage(messageUuid);
    }

    /**
     * Find a node amongst all the nodes in the cluster
     *
     * @param uuid The node we are trying to find
     * @return The node
     * @throws LtsllcException If we couldn't find the node
     */
    public synchronized Node findNode(UUID uuid) throws LtsllcException {
        for (Node node : nodes) {
            if (uuid.equals(node.getUuid())) {
                return node;
            }
        }

        throw new LtsllcException("could not find node for: " + uuid);
    }

    public void divideUpNodesMessagesOnlyNode(UUID uuid) throws IOException {
        List<UUID> list = MessageLog.getInstance().getAllMessagesOwnedBy(uuid);
        for (UUID uuidOfMessage : list) {
            MessageLog.getInstance().setOwner(uuidOfMessage, Miranda.getInstance().getMyUuid());
        }
    }

    public void setupScan() {
        AlarmClock.getInstance().scheduleOnce(this, Alarms.SCAN,
                Miranda.getProperties().getLongProperty(Miranda.PROPERTY_SCAN_PERIOD));
    }

    public synchronized void scan() {
        setupScan();
        for (Node node : nodes) {
            if ((node.getChannel() != null) && (node.getState() == ClusterConnectionStates.START)) {
                node.sendStart(true);
            }
        }
    }

    public synchronized void register(HeartBeatHandler heartBeatHandler, UUID uuid) {
        heartBeatHandlerToUUIDMap.put(heartBeatHandler, uuid);
    }


    public void vote (UUID voter, int vote) throws LtsllcException {
        if (null == election) {
            throw new LtsllcException("null election in vote");
        }

        election.vote(voter, vote);
    }

    public synchronized void sendLeader () throws LtsllcException {
        for (Node node : nodes) {
            if (node.getChannel() != null) {
                node.sendLeader();
            }
        }
    }

    public UUID getLeaderUuid() throws LtsllcException {
        if (null == election) {
            throw new LtsllcException("null election");
        }

        return election.getLeader().getNode().getUuid();
    }

    public void countVotes() throws LtsllcException {
        if (null == election) {
            throw new LtsllcException("null election");
        }

        election.countVotes();
    }
}
