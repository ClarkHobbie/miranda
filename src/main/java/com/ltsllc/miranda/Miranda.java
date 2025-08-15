package com.ltsllc.miranda;


import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.UncheckedLtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.cluster.ClusterThread;
import com.ltsllc.miranda.cluster.Node;
import com.ltsllc.miranda.cluster.SpecNode;
import com.ltsllc.miranda.logging.LoggingCache;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.message.MessageEventLogger;
import com.ltsllc.miranda.message.MessageLog;
import com.ltsllc.miranda.netty.ChannelMonitor;
import com.ltsllc.miranda.netty.HeartBeatHandler;
import com.ltsllc.miranda.netty.ServerChannelToNodeDecoder;
import com.ltsllc.miranda.netty.StringEncoder;
import com.ltsllc.miranda.properties.PropertiesHolder;
import com.ltsllc.miranda.properties.PropertyChangedEvent;
import com.ltsllc.miranda.properties.PropertyListener;
import com.ltsllc.miranda.servlets.*;
import com.ltsllc.miranda.servlets.Properties;
import com.ltsllc.miranda.servlets.Queue;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetectorFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.*;
import java.util.*;



public class Miranda implements PropertyListener {
    public static InputStream in = System.in;
    public static PrintStream out = System.out;
    public static PrintStream err = System.err;
    public static int exitCode = 0;

    /**
     * The property for specifying what port number to use for cluster connections
     */
    public static final String PROPERTY_CLUSTER_PORT = com.ltsllc.miranda.properties.Properties.clusterPort.toString();

    /**
     * The port number to use if it's not set.  The default is 2020
     */
    public static final String PROPERTY_DEFAULT_CLUSTER_PORT = "2021";
    public static final String PROPERTY_PROPERTIES_FILE = "properties";

    /**
     * The default properties file.  It's miranda.properties
     */
    public static final String PROPERTY_DEFAULT_PROPERTIES_FILE = "miranda.properties";

    public static final String PROPERTY_LONG_LOGGING_LEVEL = com.ltsllc.miranda.properties.Properties.loggingLevel.toString();
    public static final String PROPERTY_SHORT_LOGGING_LEVEL = "l";

    public static final String PROPERTY_LOGGING_LEVEL_DEBUG = "debug";

    /**
     * The default Logging level to use if the user doesn't specify one.  The default is ERROR.
     */
    public static final String PROPERTY_DEFAULT_LOGGING_LEVEL = "error";

    /**
     * How long the system, in milliseconds, will wait for another node to make a bid
     */
    public static final String PROPERTY_BID_TIMEOUT = com.ltsllc.miranda.properties.Properties.bidTimeout.toString();

    /**
     * The default bid timeout to use if the user doesn't specify one.  The default is 500msecs.
     */
    public static final String PROPERTY_DEFAULT_BID_TIMEOUT = "500";

    /**
     * The TCP port at which the system will listen for messages from a client
     */
    public static final String PROPERTY_MESSAGE_PORT = com.ltsllc.miranda.properties.Properties.messagePort.toString();

    /**
     * The default TCP port to listen to if the user doesn't specify one.  The default is 3030
     */
    public static final String PROPERTY_DEFAULT_MESSAGE_PORT = "3030";

    /**
     * The value that is used for the message cache.
     *
     * <p>
     * This is the size, in two character bytes, for the message cache.
     * </P>
     */
    public static final String PROPERTY_CACHE_LOAD_LIMIT = com.ltsllc.miranda.properties.Properties.cacheLoadLimit.toString();

    /**
     * The default size of the message cache.   The default is 100Megabytes.
     */
    public static final String PROPERTY_DEFAULT_CACHE_LOAD_LIMIT = "104856700"; // 100 megabytes

    /**
     * The name of the owner file
     */
    public static final String PROPERTY_OWNER_FILE = "ownerFile";

    /**
     * The default name to use for the owner file.  The default is "owners.log."
     */
    public static final String PROPERTY_DEFAULT_OWNER_FILE = "owners.log";

    /**
     * Whether clustering is on or not.
     */
    public static final String PROPERTY_CLUSTER = "cluster";

    /**
     * Whether clustering is on by default.  The default is "off."
     */
    public static final String PROPERTY_DEFAULT_CLUSTER = "off";

    /**
     * The host we are on
     */
    public static final String PROPERTY_THIS_HOST = com.ltsllc.miranda.properties.Properties.thisHost.toString();

    /**
     * The port we are listening to for the cluster
     */
    public static final String PROPERTY_THIS_PORT = com.ltsllc.miranda.properties.Properties.thisPort.toString();

    /**
     * The name of the property to look at to decide if clustering is on or off.  If the user doesn't supply an
     * alternative value then the system assumes that clustering is off.
     */
    public static final String PROPERTY_CLUSTER_1 = com.ltsllc.miranda.properties.Properties.cluster1.toString();

    /**
     * The amount of time, in milliseconds that the system waits after an attempt to contact another node
     */
    public static final String PROPERTY_CLUSTER_RETRY = com.ltsllc.miranda.properties.Properties.clusterRetry.toString();

    /**
     * The default time to wait between calls to another node to see if it's up.  The default is 10,000 milliseconds (10
     * seconds).
     */
    public static final String PROPERTY_DEFAULT_CLUSTER_RETRY = "10000";

    /**
     * The name of the file that the node will log new messages to.
     */
    public static final String PROPERTY_MESSAGE_LOG = com.ltsllc.miranda.properties.Properties.messageLogfile.toString();

    /**
     * The default logfile name to use.  The default is "messages.log."
     */
    public static final String PROPERTY_DEFAULT_MESSAGE_LOG = "messages.log";

    /**
     * The timeout (in milliseconds) that miranda will wait for a START_ACKNOWLEDGED message after issuing a START
     * message.
     */
    public static final String PROPERTY_START_TIMEOUT = com.ltsllc.miranda.properties.Properties.startTimeout.toString();

    /**
     * The default timeout for a start message (1000 milliseconds).
     */
    public static final String PROPERTY_DEFAULT_START_TIMEOUT = "1000";

    /**
     * The universally unique identifier (UUID) of this node.
     *
     * <p>
     * Note that there is no default value for this property the user MUST supply one to avoid system shutdown.
     * </P>
     */
    public static final String PROPERTY_UUID = "UUID";

    /**
     * When to do the next compaction; using milliseconds
     */
    public static final String PROPERTY_COMPACTION_TIME = com.ltsllc.miranda.properties.Properties.compaction.toString();

    /**
     * The default value for PROPERTY_COMPACTION_TIME is 10 seconds
     */
    public static final String PROPERTY_DEFAULT_COMPACTION_TIME = "100000";

    /**
     * The amount of time to wait in between heart beat start messages
     */
    public static final String PROPERTY_HEART_BEAT_INTERVAL = com.ltsllc.miranda.properties.Properties.heartBeatTimeout.toString();

    /**
     * The default period to wait between heart beat start messages is 5 sec
     */
    public static final String PROPERTY_DEFAULT_HEART_BEAT_INTERVAL = "5000";

    /**
     * The timeout (in milliseconds) for another node to respond to an auction message
     */
    public static final String PROPERTY_AUCTION_TIMEOUT = com.ltsllc.miranda.properties.Properties.auctionTimeout.toString();

    /**
     * The period for a heart beat to timeout (in milliseconds)
     */
    public static final String PROPERTY_HEART_BEAT_TIMEOUT = com.ltsllc.miranda.properties.Properties.heartBeatTimeout.toString();

    /**
     * The default timeout for a heart beat (1/2 a second)
     */
    public static final String PROPERTY_DEFAULT_HEART_BEAT_TIMEOUT = "5000";

    /**
     * The timeout for responding to a DEAD NODE START message.
     */
    public static final String PROPERTY_DEAD_NODE_TIMEOUT = com.ltsllc.miranda.properties.Properties.deadNodeTimeout.toString();

    /**
     * The default timeout for a DEAD NODE START message is 1/2 a second
     */
    public static final String PROPERTY_DEFAULT_DEAD_NODE_TIMEOUT = "1000";

    /**
     * The period to wait to coalesce the cluster
     */
    public static final String PROPERTY_COALESCE_PERIOD = com.ltsllc.miranda.properties.Properties.coalescePeriod.toString();

    /**
     * Defaults to 10 minuets
     */
    public static final String PROPERTY_DEFAULT_COALESCE_PERIOD = "600000";

    /**
     * The period of time between scans
     */
    public static final String PROPERTY_SCAN_PERIOD = com.ltsllc.miranda.properties.Properties.scanPeriod.toString();

    /**
     * The default period for scans
     */
    public static final String PROPERTY_DEFAULT_SCAN_PERIOD = "3000";

    /**
     * whether the system should send heartbeats
     */
    public static final String PROPERTY_USE_HEARTBEATS = com.ltsllc.miranda.properties.Properties.useHeartbeats.toString();

    /**
     * default is to send heartbeat messages
     */
    public static final String PROPERTY_DEFAULT_USE_HEART_BEATS = "true";


    /**
     * The logger to use
     */
    protected static final Logger logger = LogManager.getLogger(Miranda.class);

    /**
     * The logger to use for system events like system startup, shutdown, message creation, delivery, etc.
     */
    public static final Logger event = LogManager.getLogger("events");

    /**
     * The one instance of miranda that exists.
     */
    public static Miranda instance = new Miranda();

    /**
     * Should miranda keep running?
     */
    protected boolean keepRunning = true;

    /**
     * The system properties
     */
    protected static PropertiesHolder properties;

    /**
     * The jetty server that receives new messages
     */
    protected Server server;

    /**
     * A list of node specifications that make up the cluster
     */
    static protected List<SpecNode> specNodes = new ArrayList<>();

    /**
     * The set of messages that we have sent out, but haven't heard back from
     */
    protected Set<Message> inflight = new HashSet<>();

    protected String myHost;

    public String getMyHost() {
        return myHost;
    }

    public void setMyHost(String myHost) {
        this.myHost = myHost;
    }

    protected int myPort;

    public int getMyPort() {
        return myPort;
    }

    public void setMyPort(int myPort) {
        this.myPort = myPort;
    }

    /**
     * The getOwnerFlag signifies that we need to get owner and message information from our peers
     */
    protected boolean synchronizationFlag = false;

    public boolean getSynchronizationFlag() {
        return synchronizationFlag;
    }

    public void setSynchronizationFlag(boolean synchronizationFlag) {
        this.synchronizationFlag = synchronizationFlag;
    }

    /**
     * The UUID of this node
     */
    protected UUID myUuid = null;

    public UUID getMyUuid() {
        return myUuid;
    }

    public void setMyUuid(UUID myUuid) {
        this.myUuid = myUuid;
    }

    protected long iterations = 0;

    public long getIterations() {
        return iterations;
    }

    public void setIterations(long iterations) {
        this.iterations = iterations;
    }

    /**
     * The time when the node started
     */
    protected long myStart = System.currentTimeMillis();

    public long getMyStart() {
        return myStart;
    }

    public void setMyStart(long myStart) {
        this.myStart = myStart;
    }

    /**
     * What was the time when the oldest node started?
     *
     * <H>
     * This is used when first starting to determine if the node should sync with another node.  A value of -1
     * signifies that we're not currently connected to any other nodes.
     * </H>
     */
    protected long eldest = -1;

    public long getEldest() {
        return eldest;
    }

    public void setEldest(long eldest) {
        this.eldest = eldest;
    }

    public boolean isKeepRunning() {
        return keepRunning;
    }

    public void setKeepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
    }

    public Set<Message> getInflight() {
        return inflight;
    }

    public void setInflight(Set<Message> inflight) {
        this.inflight = inflight;
    }

    public static List<SpecNode> getSpecNodes() {
        return specNodes;
    }

    public void setSpecNodes(List<SpecNode> specNodes) {
        this.specNodes = specNodes;
    }

    protected static ResourceLeakDetector rld;

    public static ResourceLeakDetector getRld() {
        return rld;
    }

    public static void setRld(ResourceLeakDetector rld) {
        Miranda.rld = rld;
    }

    /**
     * where we should pick up
     */
    protected int restartIndex = 0;

    public int getRestartIndex() {
        return restartIndex;
    }

    public void setRestartIndex(int restartIndex) {
        this.restartIndex = restartIndex;
    }


    /**
     * The constructor for the class
     *
     * <p>
     * This sets the static instance to the newly created instance and tries to load the properties for that instance.  If
     * there is a problem loading the properties the method throws an UncheckedLtsllcException.  It checks for the
     * existence of the myUuid, the myHost and the myPort properties and throws an UncheckedLtsllcException if the
     * property is not defined.
     * </P>
     */
    public Miranda() {
        Miranda.instance = this;

        properties = new PropertiesHolder();

        try {
            loadProperties();
        } catch (LtsllcException e) {
            throw new UncheckedLtsllcException(e);
        }
        properties.listen(this, com.ltsllc.miranda.properties.Properties.uuid);
        if (null == properties.getProperty(PROPERTY_UUID)) {
            String msg = "Please specify a UUID for this node by setting the " + PROPERTY_UUID + " property";
            logger.error(msg);
            event.error(msg);
            throw new UncheckedLtsllcException(msg);
        } else {
            myUuid = UUID.fromString(properties.getProperty(PROPERTY_UUID));
        }

        properties.listen(this, com.ltsllc.miranda.properties.Properties.thisHost);
        if (null == properties.getProperty(PROPERTY_THIS_HOST)) {
            String msg = "Please specify a host for this node by setting the " + PROPERTY_THIS_HOST + " property";
            logger.error(msg);
            event.error(msg);
            throw new UncheckedLtsllcException(msg);
        } else {
            myHost = properties.getProperty(PROPERTY_THIS_HOST);
        }

        setupClusterPort(Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT));

        if (null == properties.getProperty(PROPERTY_THIS_PORT)) {
            String msg = "Please specify a port for this node by setting the " + PROPERTY_THIS_PORT + " property";
            logger.error(msg);
            event.error(msg);
            throw new UncheckedLtsllcException(msg);
        } else {
            myPort = properties.getIntProperty(PROPERTY_THIS_PORT);
        }
        rld = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(ByteBuf.class);
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);


    }

    /**
     * Setup the port for other nodes in the network
     *
     * @param port The port number to listen at.
     */
    public void setupClusterPort(int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(Cluster.STRING_ENCODER, new StringEncoder());
                        //new LengthFieldBasedFrameDecoder(2048, 0, 4, 0, 4),
                ch.pipeline().addLast(Cluster.DECODER, new ServerChannelToNodeDecoder("spam"));
                ch.pipeline().addLast(Cluster.HEART_BEAT, new HeartBeatHandler(ch));
                ch.pipeline().addFirst("whatever", new ChannelMonitor());
            }
        });
    }


    /**
     * Get the jetty server
     *
     * <p>
     * The system uses a Jetty sever for messages.
     * </P>
     *
     * @return The Jetty server
     */
    public Server getServer() {
        return server;
    }

    /**
     * Set the Jetty server for the system
     *
     * @param server The Jetty server
     */
    public void setServer(Server server) {
        this.server = server;
    }

    public static Miranda getInstance() {
        return instance;
    }

    public static void setInstance(Miranda instance) {
        Miranda.instance = instance;
    }

    public static PropertiesHolder getProperties() {
        return properties;
    }

    public static void setProperties(PropertiesHolder properties) {
        Miranda.properties = properties;
    }


    /**
     * Miranda's main loop
     *
     * <p>
     * <PRE>
     * if this iteration is divisible by 1,000
     * run garbage collection
     * copy all the messages we are responsible for delivering
     * foreach of those messages
     * try and deliver the message
     * if the message was deliver
     * remove it from the collection of messages that we are responsible for
     * if the message has a status URL
     * send a notification that we delivered it
     * </PRE>
     *
     * <p>
     * Note that this is only used if keepRunning is true.
     * </P>
     *
     * @throws IOException If there is a problem copying the messages
     */
    public synchronized void mainLoop() throws IOException, LtsllcException {
        //
        // run garbage collection to avoid running out of memory
        //
        if (iterations % 1000 == 0)
            Runtime.getRuntime().gc();

        if (keepRunning) {
            /*
             * Try and deliver some messages
             */
            if (MessageLog.getInstance().outOfBounds(restartIndex)) {
                restartIndex = 0;
            }

            LoggingCache.CopyMessagesResult temp = MessageLog.getInstance().copyMessages(
                    Miranda.properties.getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT),
                    restartIndex
            );
            if (temp != null) {
                for (Message message : temp.list) {
                    deliver(message);
                }

                restartIndex = temp.restartIndex;

            }
            iterations++;
        }
    }

    /**
     * setup miscellaneous properties
     */
    public void setupMisc() {
        myHost = properties.getProperty(PROPERTY_THIS_HOST);
        myPort = properties.getIntProperty(PROPERTY_THIS_PORT);
        myUuid = UUID.fromString(properties.getProperty(PROPERTY_UUID));
        properties.listen(this, com.ltsllc.miranda.properties.Properties.uuid);
        properties.listen(this, com.ltsllc.miranda.properties.Properties.hostName);
        properties.listen(this, com.ltsllc.miranda.properties.Properties.clusterPort);
        if (myHost == null || myPort == -1 || myUuid == null) {
            System.err.println("UUID, host and port must be set in the properties file");
            System.exit(-1);
        }
    }

    /**
     * determine if we should recover and if we should do so
     */
    public void checkRecovery() throws IOException, LtsllcException {
        try {
            synchronized (this) {
                wait(6000);
            }
        } catch (InterruptedException e) {
            ;
        }
        if (shouldRecover()) {
            event.info("preforming recovery");
            recover();
        } else {
            event.info("No recovery needed backing up files");
            ImprovedFile temp = new ImprovedFile(Miranda.getProperties().getStringProperty(PROPERTY_MESSAGE_LOG));
            temp.backup(".backup");
            temp = new ImprovedFile(Miranda.getProperties().getStringProperty(Miranda.PROPERTY_OWNER_FILE));
            temp.backup(".backup");
        }
    }


    /**
     * start up miranda
     * <p>
     * When Miranda starts up, check for the existence of a sendQueue.  If it exists then copy the file and enter
     * the bidding mode.
     * </P>
     */
    public void startUp(String[] args) throws Exception {
        event.info("Miranda starting up");
        logger.debug("Miranda starting up");
        loadProperties();
        setupMisc();

        logger.debug("About to start cluster port");
        setupClustering();

        setupMessageLog();

        logger.debug("determine if we should recover");
        checkRecovery();

        logger.debug("parsing arguments");
        processArguments(args);

        logger.debug("Parsing nodes");
        parseNodes();

        logger.debug("starting the message port");
        startMessagePort();

        startIdentityNode();
        logger.debug("Leaving startup");
    }

    /**
     * A method that sets up the port that other nodes in
     * the cluster use to communicate.
     */
    public void setupClusterPort() {
        Cluster.getInstance().startServerOn(Miranda.getProperties().getIntProperty(Miranda.PROPERTY_THIS_PORT));
    }


    public void setupClustering() throws LtsllcException, CloneNotSupportedException {
        ClusterThread ct = new ClusterThread();
        ct.start();
    }

    /**
     * listen for new messages
     */
    protected void startMessagePort() throws Exception {
        logger.debug("entering startMessagePort");

        // Create and configure a ThreadPool.
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("server");

        server = new Server(Miranda.getProperties().getIntProperty(Miranda.PROPERTY_MESSAGE_PORT));

        ResourceHandler rh0 = new ResourceHandler();
        rh0.setWelcomeFiles(new String[]{"index.html"});
        rh0.setResourceBase("www");

        ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.addServlet(NumberOfConnections.class, "/api/numberOfConnections");
        servletContextHandler.addServlet(Properties.class, "/api/properties");
        servletContextHandler.addServlet(SaveProperties.class, "/api/saveProperties");
        servletContextHandler.addServlet(NumberOfMessages.class, "/api/numberOfMessages");
        servletContextHandler.addServlet(Status.class, "/api/status");
        servletContextHandler.addServlet(Coalesce.class, "/api/coalesce");
        servletContextHandler.addServlet(ConnectionDetails.class, "/api/connections/details");
        servletContextHandler.addServlet(NewMessage.class, "/api/newMessage");
        servletContextHandler.addServlet(TrackMessage.class, "/api/trackMessage");
        servletContextHandler.addServlet(Queue.class, "/api/queue");
        servletContextHandler.addServlet(DeleteMessage.class, "/api/deleteMessage");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{rh0, servletContextHandler});
        server.setHandler(handlers);


        // Start the Server, so it starts accepting connections from clients.
        server.start();
        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_MESSAGE_PORT);
        logger.debug("started up the port at " + port + " and thread: " + Thread.currentThread());

        logger.debug("leaving startMessagePort");
    }

    /*
     * process the arguments to the program
     *
     * process the arguments to the program --- at this point this consists of the logging level.
     */
    protected java.util.Properties processArguments(String[] arguments) {
        logger.debug("starting processArguments with arguments = " + arguments);

        java.util.Properties temp = new java.util.Properties();

        String prev = null;

        for (int i = 0; i < arguments.length; i++) {
            String arg = arguments[i];
            if (i == 0) {
                prev = null;
            } else {
                if (!arguments[i - 1].startsWith("-")) {
                    prev = arguments[i - 1];
                } else {
                    String previous = arguments[i - 1].substring(1);
                    prev = String.valueOf(previous.toCharArray()[previous.toCharArray().length - 1]);
                }
            }

            processArgument(prev, arg, temp);
        }

        logger.debug("Leaving processArguments with temp = " + temp);
        return temp;
    }

    /**
     * Process a single command line argument
     *
     * @param prev     The previous argument.
     * @param argument The argument.
     * @param map      The map of keys to values.
     */
    protected void processArgument(String prev, String argument, java.util.Properties map) {
        logger.debug("processing argument with argument = " + argument + ", prev = " + prev + " and map = " + map);

        if (argument.startsWith("-")) {
            logger.debug("found an argument " + argument);
            argument = argument.substring(1);

            if (argument.length() > 1) {
                for (char arg : argument.toCharArray()) {
                    String key = String.valueOf(arg);
                    String value = key;
                    map.put(key, value);
                    logger.debug("put key = " + key + " and value = " + value);
                }
            }
        } else {
            // an argument that doesn't start with "-" is a parameter to the previous
            // argument
            logger.debug("found a parameter");
            map.put(prev, argument);
            return;
        }

        logger.debug("leaving processArgument with map = " + map);
    }

    /**
     * reload the system properties
     * <p>
     * This simply tells the system to reload the system properties, but it also defines default values for most
     * properties.
     * </P>
     */
    public void loadProperties() throws LtsllcException {
        FileInputStream in = null;
        if (properties == null) {
            properties = new PropertiesHolder();
        }
        try {
            if (properties.getProperty(PROPERTY_PROPERTIES_FILE) != null) {
                in = new FileInputStream(properties.getProperty(PROPERTY_PROPERTIES_FILE));
            } else {
                in = new FileInputStream(PROPERTY_DEFAULT_PROPERTIES_FILE);
            }
            PropertiesHolder temp = new PropertiesHolder();
            temp.load(in);
            properties = temp;
        } catch (IOException ioException) {
            throw new LtsllcException("exception loading properties file", ioException);
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException ioException) {
                    logger.error("exception closing properties file", ioException);
                    throw new UncheckedLtsllcException(ioException);
                }
            }
        }

        properties.setIfNull(PROPERTY_MESSAGE_PORT, PROPERTY_DEFAULT_MESSAGE_PORT);
        properties.setIfNull(PROPERTY_CACHE_LOAD_LIMIT, PROPERTY_DEFAULT_CACHE_LOAD_LIMIT);
        properties.setIfNull(PROPERTY_BID_TIMEOUT, PROPERTY_DEFAULT_BID_TIMEOUT);
        properties.setIfNull(PROPERTY_OWNER_FILE, PROPERTY_DEFAULT_OWNER_FILE);
        properties.setIfNull(PROPERTY_PROPERTIES_FILE, PROPERTY_DEFAULT_PROPERTIES_FILE);
        properties.setIfNull(PROPERTY_CLUSTER, PROPERTY_DEFAULT_CLUSTER);
        properties.setIfNull(PROPERTY_CLUSTER_RETRY, PROPERTY_DEFAULT_CLUSTER_RETRY);
        properties.setIfNull(PROPERTY_MESSAGE_LOG, PROPERTY_DEFAULT_MESSAGE_LOG);
        properties.setIfNull(PROPERTY_CLUSTER_PORT, PROPERTY_DEFAULT_CLUSTER_PORT);
        properties.setIfNull(PROPERTY_COMPACTION_TIME, PROPERTY_DEFAULT_COMPACTION_TIME);
        properties.setIfNull(PROPERTY_HEART_BEAT_INTERVAL, PROPERTY_DEFAULT_HEART_BEAT_INTERVAL);
        properties.setIfNull(PROPERTY_START_TIMEOUT, PROPERTY_DEFAULT_START_TIMEOUT);
        properties.setIfNull(PROPERTY_HEART_BEAT_TIMEOUT, PROPERTY_DEFAULT_HEART_BEAT_TIMEOUT);
        properties.setIfNull(PROPERTY_DEAD_NODE_TIMEOUT, PROPERTY_DEFAULT_DEAD_NODE_TIMEOUT);
        properties.setIfNull(PROPERTY_COALESCE_PERIOD, PROPERTY_DEFAULT_COALESCE_PERIOD);
        properties.setIfNull(PROPERTY_SCAN_PERIOD, PROPERTY_DEFAULT_SCAN_PERIOD);
        properties.setIfNull(PROPERTY_USE_HEARTBEATS, PROPERTY_DEFAULT_USE_HEART_BEATS);
    }

    /**
     * Store the properties
     *
     * @throws IOException If there is a problem dealing with the properties files.
     */
    public void storeProperties() throws IOException {
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(properties.getProperty(PROPERTY_PROPERTIES_FILE));
            properties.store(fileWriter, null);
        } finally {
            if (null != fileWriter) {
                fileWriter.close();
            }
        }
    }

    public void stop() {
        keepRunning = false;
    }

    /**
     * Release all the ports we are bound to
     *
     * @throws Exception If there is a problem stopping Jetty.
     */
    public void releasePorts() throws Exception {
        releaseMessagePort();
    }

    /**
     * Unbind the message port
     *
     * @throws Exception If there is a problem stopping Jetty
     */
    public synchronized void releaseMessagePort() throws Exception {
        logger.debug("entering releaseMessagePort.");
        logger.debug("server = " + server);
        if (server != null) {
            server.stop();
        }
        logger.debug("leaving releaseMessagePort");

    }

    /**
     * Try to deliver a message
     * <p>
     * This method tries to deliver a message asynchronously.  On success it calls successfulMessage to signal that this
     * is so.
     *
     * @param message The message to
     * @throws IOException If there is a problem with the manipulation of the logfiles
     */
    public void deliver(Message message) throws IOException {
        if (null == message) {
            return;
        }

        //
        // don't send if we haven't gotten a reply from the message we already sent
        //
        if (inflight.contains(message)) {
            return;
        }
        inflight.add(message);

        MessageEventLogger.deliveryAttempted(message);

        AsyncHttpClient httpClient = Dsl.asyncHttpClient();
        BoundRequestBuilder boundRequestBuilder = httpClient.preparePost(message.getDeliveryURL());

        Request request = httpClient.preparePost(message.getDeliveryURL())
                .setBody(message.getContents())
                .build();


        if (null == message.getCompletionHandler()) {
            boundRequestBuilder
                    .execute(new AsyncCompletionHandler<Response>() {
                        @Override
                        public Response onCompleted(Response response) throws IOException {
                            //
                            // we should check for an exception, for example when the destination is not listening, but
                            // response doesn't offer one
                            //
                            if ((response.getStatusCode() > 199) && (response.getStatusCode() < 300)) {
                                //
                                // if we successfully delivered the message then tell everyone and remove the
                                // message from the set of messages we are trying to deliver
                                //
                                successfulMessage(message);
                                event.info("Delivered message (" + message.getMessageID() + ")");
                                MessageEventLogger.delivered(message);
                            } else {
                                //
                                // otherwise note the status and try again
                                //
                                message.setStatus(response.getStatusCode());
                                MessageEventLogger.attemptFailed(message);
                            }

                            httpClient.close();
                            // otherwise, keep trying
                            return response;
                        }
                    });
        }


    }

    /**
     * Called when a message has been successfully delivered
     *
     * <p>
     * This method calls cluster.informOfDelivery to signal to the cluster that the message has been delivered,
     * removes the message from the send queue and notifying the client that we have delivered the message.
     * </P>
     *
     * @param message The message
     */
    public void successfulMessage(Message message) throws IOException {
        logger.debug("entering successfulMessage with: " + message);

        Cluster.getInstance().notifyOfDelivery(message);

        notifyClientOfDelivery(message);
        inflight.remove(message);
        MessageLog.getInstance().remove(message.getMessageID());
        logger.debug("leaving successfulMessage");
    }

    /**
     * Asynchronously tell a client that we delivered their message
     * <p>
     * NOTE: this method ignores the state code of the client's response to this POST.
     *
     * @param message The message that was delivered
     */
    public void notifyClientOfDelivery(Message message) {
        logger.debug("entering notifyClientOfDelivery with " + message);
        event.info("delivered " + message.getMessageID());

        AsyncHttpClient httpClient = Dsl.asyncHttpClient();
        BoundRequestBuilder
                rb = httpClient.preparePost(message.getDeliveryURL());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.MESSAGE_DELIVERED);
        stringBuffer.append(" ");
        stringBuffer.append(message.getMessageID());
        rb.setBody(stringBuffer.toString());
        rb.execute(new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                return response;
            }
        });

        logger.debug("leaving notifyClientOfDelivery");
    }

    /**
     * Parse out the list of other nodes
     *
     * <p>
     * Note that this method assumes that if PROPERTY_CLUSTER + "." &lt;number&gt; + ".host" is defined then the
     * corresponding port is also defined.
     * </P>
     *
     * @see SpecNode
     */
    static public void parseNodes() {
        if (null != properties.getProperty(PROPERTY_CLUSTER_1)) {
            String rootProperty = Miranda.PROPERTY_CLUSTER + "1";
            SpecNode specNode = new SpecNode();
            specNode.setHost(properties.getProperty(rootProperty));
            specNode.setPort(properties.getIntProperty(rootProperty + "Port"));
            List<SpecNode> list = new ArrayList<>();
            int count = 2;
            list.add(specNode);
            while (specNode != null) {
                specNode = null;
                rootProperty = Miranda.PROPERTY_CLUSTER  + count;
                if (null != properties.getProperty(rootProperty)) {
                    specNode = new SpecNode();
                    specNode.setHost(properties.getProperty(rootProperty));
                    specNode.setPort(properties.getIntProperty(rootProperty + "Port"));
                    list.add(specNode);
                    count++;
                }
            }

            Miranda.getInstance().setSpecNodes(list);
        }
    }

    /**
     * Should the message log attempt to recover?
     * <p>
     * This method is essentially a call to MessageLog.shouldRecover
     * </P>
     *
     * @return Whether the system should recover.
     */
    public static boolean shouldRecover() {
        PropertiesHolder p = getProperties();
        ImprovedFile messageFile = new ImprovedFile(p.getProperty(PROPERTY_MESSAGE_LOG));
        boolean messageFileExists = messageFile.exists();

        ImprovedFile ownersFile = new ImprovedFile(p.getProperty(PROPERTY_OWNER_FILE));
        boolean ownerFileExists = ownersFile.exists();

        boolean isAlone = Cluster.getInstance().isAlone(Miranda.getInstance().getMyUuid());

        return (messageFileExists || ownerFileExists) && isAlone;
    }

    /**
     * Attempt to recover from a crash
     *
     * <p>
     * This method attempts to recover from a crash by restoring the message and ownership information that the
     * system had when it crashed.
     * </P>
     *
     * @throws IOException     If there is a problem reading the files
     * @throws LtsllcException
     */
    public void recover() throws IOException, LtsllcException {
        logger.debug("entering recovery");
        event.info("Recovering");

        PropertiesHolder p = properties;
        ImprovedFile messageFile = new ImprovedFile(p.getProperty(PROPERTY_MESSAGE_LOG));
        ImprovedFile ownerFile = new ImprovedFile(p.getProperty(PROPERTY_OWNER_FILE));
        int loadLimit = p.getIntProperty(PROPERTY_CACHE_LOAD_LIMIT);
        UUID owner = UUID.fromString(p.getProperty(PROPERTY_UUID));
        MessageLog.recover(messageFile, loadLimit, ownerFile, owner);

        logger.debug("leaving recovery");
    }

    /**
     * Recover as if we were not clustering
     *
     * @throws LtsllcException If there are problems with the files.
     * @throws IOException     If there are problems recovering.
     */
    public synchronized void recoverLocally(UUID owner) throws LtsllcException, IOException {
        logger.debug("entering recoverLocally");
        event.warn("The other nodes in the cluster are not responding, recovering locally.");

        PropertiesHolder p = properties;
        ImprovedFile messages = new ImprovedFile(p.getProperty(PROPERTY_MESSAGE_LOG));
        ImprovedFile messagesBackup = new ImprovedFile(p.getProperty(PROPERTY_MESSAGE_LOG) + ".backup");
        ImprovedFile owners = new ImprovedFile(p.getProperty(PROPERTY_OWNER_FILE));
        ImprovedFile ownersBackup = new ImprovedFile(p.getProperty(PROPERTY_OWNER_FILE) + ".backup");
        int loadLimit = p.getIntProperty(PROPERTY_CACHE_LOAD_LIMIT);

        if (!messagesBackup.exists()) {
            throw new LtsllcException("backup file, " + messagesBackup + ", does not exist");
        }

        if (!messagesBackup.renameTo(messages)) {
            throw new LtsllcException("failed to rename " + messagesBackup + " to " + messages);
        }

        if (!ownersBackup.exists()) {
            throw new LtsllcException("backup file, " + ownersBackup + ", does not exist");
        }

        if (!ownersBackup.renameTo(owners)) {
            throw new LtsllcException("failed to rename " + ownersBackup + " to " + owners);
        }

        MessageLog.recover(messages, loadLimit, owners, owner);

        logger.debug("leaving recoverLocally");
    }

    public void setUuid() {
        myUuid = UUID.fromString(properties.getProperty(PROPERTY_UUID));
    }

    public void setHost() {
        myHost = properties.getProperty(PROPERTY_THIS_HOST);
    }

    public void setClusterPort() {
        myPort = properties.getIntProperty(PROPERTY_THIS_PORT);
    }

    /**
     * Setup the message log
     */
    public void setupMessageLog() {
        ImprovedFile messageLogfile = new ImprovedFile(properties.getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        ImprovedFile ownersFile = new ImprovedFile(properties.getProperty(Miranda.PROPERTY_OWNER_FILE));
        MessageLog.defineStatics();
    }

    /**
     * A property we were watching changed
     * <H>
     * The properties we watch include the node's uuid, the node's host, and the node's messaging port
     * </H>
     *
     * @param propertyChangedEvent The property change event that kicked off the whole process.
     * @throws Throwable If we don't recognize the property.
     */
    @Override
    public void propertyChanged(PropertyChangedEvent propertyChangedEvent) throws Throwable {
        switch (propertyChangedEvent.getProperty()) {
            case uuid: {
                setUuid();
                break;
            }

            case hostName: {
                setHost();
                break;
            }

            case clusterPort: {
                setClusterPort();
                break;
            }

            default: {
                throw new LtsllcException("propertyChanged called for " + propertyChangedEvent.getProperty());
            }
        }
    }

    /**
     * Start up a node that represents us
     *
     * @throws LtsllcException            This exception is thrown by connectToNode.
     * @throws CloneNotSupportedException This exception is thrown by connectToNode.
     */
    public void startIdentityNode() throws LtsllcException, CloneNotSupportedException {
        Node node = new Node(myUuid, myHost, myPort, null);
        Cluster.getInstance().connectToNode(node, true);
        node.sendStart(true, false);
    }
}
