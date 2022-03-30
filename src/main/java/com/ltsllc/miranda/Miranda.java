package com.ltsllc.miranda;


import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.UncheckedLtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.cluster.SpecNode;
import com.ltsllc.miranda.logging.LoggingCache;
import com.ltsllc.miranda.logging.LoggingSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.*;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class Miranda {
    public static final int STATUS_SUCCESS = 200;
    /**
     * The property for specifying what port number to use for cluster connections
     */
    public static final String PROPERTY_CLUSTER_PORT = "ports.cluster";

    /**
     * The port number to use if it's not set.  The default is 2020
     */
    public static final String PROPERTY_DEFAULT_CLUSTER_PORT = "2020";
    public static final String PROPERTY_PROPERTIES_FILE = "properties";

    /**
     * The default properties file.  It's miranda.properties
     */
    public static final String PROPERTY_DEFAULT_PROPERTIES_FILE = "miranda.properties";

    public static final String PROPERTY_LONG_LOGGING_LEVEL = "loggingLevel";
    public static final String PROPERTY_SHORT_LOGGING_LEVEL = "l";

    /**
     * The logging level to use.
     */
    public static final String PROPERTY_LOGGING_LEVEL = "loggingLevel";

    /**
     * The default Logging level to use if the user doesn't specify one.  The default is ERROR.
     */
    public static final String PROPERTY_DEFAULT_LOGGING_LEVEL = "error";

    /**
     * How long the system, in milliseconds, will wait for another node to make a bid
     */
    public static final String PROPERTY_BID_TIMEOUT = "timeouts.bid";

    /**
     * The default bid timeout to use if the user doesn't specify one.  The default is 500msecs.
     */
    public static final String PROPERTY_DEFAULT_BID_TIMEOUT = "500";

    /**
     * The TCP port at which the system will listen for messages from a client
     */
    public static final String PROPERTY_MESSAGE_PORT = "messagePort";

    /**
     * The default TCP port to listen to if the user doesn't specify one.  The default is 3030
     */
    public static final String PROPERTY_DEFAULT_MESSAGE_PORT = "3030";

    /**
     * The value that is used for the message cache.
     *
     * <P>
     *     This is the size, in two character bytes, for the message cache.
     * </P>
     */
    public static final String PROPERTY_CACHE_LOAD_LIMIT = "cache.loadLimit";

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
     * ?!
     */
    public static final String PROPERTY_HOST = "host";

    /**
     * The name of the property to look at to decide if clustering is on or off.  If the user doesn't supply an
     * alternative value then the system assumes that clustering is off.
     */
    public static final String PROPERTY_CLUSTER_1 = "cluster.1.host";

    /**
     * The amount of time, in milliseconds that the system waits after an attempt to contact another node
     */
    public static final String PROPERTY_CLUSTER_RETRY = "cluster.retry";

    /**
     * The default time to wait between calls to another node to see if it's up.  The default is 10,000 milliseconds (10
     * seconds).
     */
    public static final String PROPERTY_DEFAULT_CLUSTER_RETRY = "10000";

    /**
     * The name of the file that the node will log new messages to.
     */
    public static final String PROPERTY_MESSAGE_LOG = "messageLog";

    /**
     * The default logfile name to use.  The default is "messages.log."
     */
    public static final String PROPERTY_DEFAULT_MESSAGE_LOG = "messages.log";

    /**
     * The universally unique identifier (UUID) of this node.
     *
     * <P>
     *     Note that there is no default value for this property the user MUST supply one to avoid system shutdown.
     * </P>
     */
    public static final String PROPERTY_UUID = "UUID";

    /**
     * The logger to use
     */
    protected static final Logger logger = LogManager.getLogger();

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
    protected static ImprovedProperties properties;

    /**
     * The time, after which the system should try to reconnect to the other nodes of the cluster
     */
    protected long clusterAlarm = -1;

    /**
     * The jetty server that receives new messages
     */
    protected Server server;

    /**
     * A list of node specifications that make up the cluster
     */
    protected List<SpecNode> specNodes = new ArrayList<>();

    /**
     * The set of messages that we have sent out, but haven't heard back from
     */
    protected Set<Message> inflight = new HashSet<>();

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

    public List<SpecNode> getSpecNodes() {
        return specNodes;
    }

    public void setSpecNodes(List<SpecNode> specNodes) {
        this.specNodes = specNodes;
    }

    public Miranda() {
        Miranda.instance = this;

        try {
            loadProperties();
        } catch (LtsllcException e) {
            throw new UncheckedLtsllcException(e);
        }
        if (null == properties.getProperty(PROPERTY_UUID)) {
            String msg = "Please specify a UUID for this node by setting the " + PROPERTY_UUID + " property";
            logger.error(msg);
            event.error(msg);
        } else {
            myUuid = UUID.fromString(properties.getProperty(PROPERTY_UUID));
        }
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public long getClusterAlarm() {
        return clusterAlarm;
    }

    public void setClusterAlarm(long clusterAlarm) {
        this.clusterAlarm = clusterAlarm;
    }

    public static Miranda getInstance() {
        return instance;
    }

    public static void setInstance(Miranda instance) {
        Miranda.instance = instance;
    }

    public static ImprovedProperties getProperties() {
        return properties;
    }

    public static void setProperties(ImprovedProperties properties) {
        Miranda.properties = properties;
    }

    /**
     * Miranda's main loop
     *
     * <P><PRE>
     *     copy all the messages we are responsible for delivering
     *     foreach of those messages
     *         try and deliver the message
     *     check if we need to try and connect to other nodes
     * </PRE></P>
     * <P>
     *     Note that this is only used if keepRunning is true.
     * </P>
     * @exception IOException If there is a problem copying the messages
     */
    public void mainLoop () throws IOException  {
        logger.debug("starting mainLoop, with keepRunning = " + keepRunning);
        if (keepRunning) {
            /*
             * Try and deliver all the messages
             */
            List<Message> allMessages = MessageLog.getInstance().copyAllMessages();
            for (Message message : allMessages) {
                deliver(message);
            }

            /*
             * See if it is time to connect to other nodes in the cluster
             */
            connectToOtherNodes();
        }
        logger.debug("leaving mainLoop");
    }

    public void connectToOtherNodes () {
        //
        // first see if it is time to connect
        //
        if (clusterAlarm == -1) {
            return;
        }

        //
        // if it's time to reconnect...
        //
        connectToOtherNodes();
    }

    /**
     * start up miranda
     * <P>
     *     When Miranda starts up, check for the existence of a sendQueue.  If it exists then copy the file and enter
     *     the bidding mode.
     * </P>
     */
    public void startUp (String[] args) throws Exception {
        event.info("Miranda starting up");
        logger.debug("Miranda starting up");
        logger.debug("Checking to see if we should recover");
        ImprovedFile messageLogfile = new ImprovedFile(properties.getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        int messageLoadLimit = properties.getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT);
        ImprovedFile ownersFile = new ImprovedFile(properties.getProperty(Miranda.PROPERTY_OWNER_FILE));
        if (MessageLog.shouldRecover(messageLogfile)) {
            if (messageLogfile.exists()) {
                MessageLog.recover(messageLogfile, messageLoadLimit, ownersFile);
            }
        }

        logger.debug("parsing arguments");
        properties = new ImprovedProperties();
        processArguments(args);

        logger.debug("initializing MessageLog");
        MessageLog.defineStatics(messageLogfile, messageLoadLimit, ownersFile);

        logger.debug("loading properties");
        loadProperties();

        logger.debug("Parsing nodes");
        parseNodes();

        logger.debug("Starting cluster");
        Cluster.defineStatics(myUuid);
        Cluster.getInstance().connect(specNodes);

        logger.debug("starting the message port");
        startMessagePort(properties.getIntProperty(PROPERTY_MESSAGE_PORT));

        logger.debug("Leaving startup");
    }

    /*
     * listen for new messages
     *
     * @param portNumber The TCP port that we should listen at
     */
    protected void startMessagePort (int portNumber) throws Exception {
        logger.debug("entering startMessagePort with portNumber = " + portNumber);

        // Create and configure a ThreadPool.
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("server");

        server = new Server(threadPool);

        // Create a ServerConnector to accept connections from clients.
        ServerConnector serverConnector = new ServerConnector(server, 3, 3, new HttpConnectionFactory());
        server.addConnector(serverConnector);
        serverConnector.setPort(properties.getIntProperty(PROPERTY_MESSAGE_PORT));
        serverConnector.setHost("localhost");

        server.addConnector(serverConnector);

        // Set a simple Handler to handle requests/responses.

        server.setHandler(new MessageHandler(myUuid));

        // Start the Server, so it starts accepting connections from clients.
        server.start();

        logger.debug("leaving startMessagePort");
    }

    /*
     * process the arguments to the program
     *
     * process the arguments to the program --- at this point this consists of the logging level.
     */
    protected Properties processArguments (String[] arguments) {
        logger.debug("starting processArguments with arguments = " + arguments);

        Properties temp = new Properties();

        String prev = null;

        for (int i = 0; i < arguments.length; i++) {
            String arg = arguments[i];
            if (i == 0) {
                prev = null;
            } else {
                if (!arguments[i - 1].startsWith("-")) {
                    prev = arguments[i - 1];
                } else {
                    String previous = arguments[i-1].substring(1);
                    prev = String.valueOf(previous.toCharArray()[previous.toCharArray().length - 1]);
                }
            }

            processArgument(prev, arg, temp);
        }

        logger.debug("Leaving processArguments with temp = " + temp);
        return temp;
    }

    /*
     * process a single argument
     *
     * This method takes care of removing the "-" character and breaking a "block" of arguments
     * (like -abc) into single arguments ("a," "b," "c").
     *
     * @param argument The argument to process
     * @param map      The arguments and parameters of the arguments
     * @param prev     The previous argument in case this is a parameter
     */
    protected void processArgument (String prev, String argument, Properties map) {
        logger.debug("processing argument with argument = " + argument + ", prev = " + prev + " and map = " + map);

        if (argument.startsWith("-")) {
            logger.debug("found an argument " + argument);
            argument = argument.substring(1);

            if (argument.length() > 1) {
                for (char arg : argument.toCharArray()) {
                    String key = String.valueOf(arg);
                    String value = key;
                    map.put(key,value);
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
     * <P>
     *     This simply tells the system to reload the system properties, but it also defines default values for most
     *     properties.
     * </P>
     */
    public void loadProperties() throws LtsllcException {
        FileInputStream in = null;
        if (properties ==  null) {
            properties = new ImprovedProperties();
        }
        try {
            if (properties.getProperty(PROPERTY_PROPERTIES_FILE) != null) {
                in = new FileInputStream(properties.getProperty(PROPERTY_PROPERTIES_FILE));
            } else {
                in = new FileInputStream(PROPERTY_DEFAULT_PROPERTIES_FILE);
            }
            ImprovedProperties temp = new ImprovedProperties();
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
        properties.setIfNull(PROPERTY_LOGGING_LEVEL, PROPERTY_DEFAULT_LOGGING_LEVEL);
        properties.setIfNull(PROPERTY_CLUSTER, PROPERTY_DEFAULT_CLUSTER);
        properties.setIfNull(PROPERTY_CLUSTER_RETRY, PROPERTY_DEFAULT_CLUSTER_RETRY);
        properties.setIfNull(PROPERTY_MESSAGE_LOG, PROPERTY_DEFAULT_MESSAGE_LOG);
        properties.setIfNull(PROPERTY_CLUSTER_PORT, PROPERTY_DEFAULT_CLUSTER_PORT);
    }

    public void storeProperties () throws IOException {
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

    /**
     * Check to see if we should enter recovery mode
     *
     * We should enter recovery mode if the queue is not empty.
     */
    protected boolean shouldEnterRecovery() {
        ImprovedFile messageLog = new ImprovedFile(properties.getProperty(PROPERTY_MESSAGE_LOG));
        ImprovedFile owners = new ImprovedFile(properties.getProperty(PROPERTY_OWNER_FILE));
        return MessageLog.shouldRecover(messageLog) || owners.exists();

    }

    public void stop () {
        keepRunning = false;
    }

    /**
     * Release all the ports we are bound to
     *
     * @exception Exception If there is a problem stopping Jetty.
     */
    public void releasePorts () throws Exception {
        releaseMessagePort();
    }

    /**
     * Unbind the message port
     *
     * @exception Exception If there is a problem stopping Jetty
     */
    public synchronized void releaseMessagePort () throws Exception {
        logger.debug("entering releaseMessagePort.");
        logger.debug("server = " + server);
        if (server != null) {
            server.stop();
        }
        logger.debug("leaving releaseMessagePort");

    }

    /**
     * Try to deliver a message
     *
     * This method tries to deliver a message asynchronously.  On success it calls successfulMessage to signal that this
     * is so.
     *
     * @param message The message to
     * @exception IOException If there is a problem with the manipulation of the logfiles
     */
    public void deliver (Message message) throws IOException {
        //
        // don't send if we haven't gotten a reply from the message we already sent
        //
        if (inflight.contains(message)) {
            return;
        }
        inflight.add(message);

        AsyncHttpClient httpClient = Dsl.asyncHttpClient();
        BoundRequestBuilder boundRequestBuilder = httpClient.preparePost(message.getDeliveryURL());

        boundRequestBuilder.setBody(message.getContents())
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
                        } else {
                            //
                            // otherwise note the status and try again
                            //
                            message.setStatus(response.getStatusCode());
                        }
                        // otherwise, keep trying
                        return response;
                    }
                });


    }

    /**
     * Called when a message has been successfully delivered
     *
     * <P>
     *     This method calls cluster.informOfDelivery to signal to the cluster that the message has been delivered,
     *     removes the message from the send queue and notifying the client that we have delivered the message.
     * </P>
     *
     * @param message The message
     */
    public void successfulMessage(Message message) throws IOException {
        logger.debug("entering successfulMessage with: " + message);
        Cluster.getInstance().informOfDelivery(message);
        try {
            MessageLog.getInstance().remove(message.getMessageID());
        } catch (IOException e) {
            logger.error ("Exception trying to remove message from the message log",e);
            event.error("Exception trying to remove message from the message log",e);
        }
        notifyClientOfDelivery(message);
        inflight.remove(message);
        logger.debug("leaving successfulMessage");
    }

    /**
     * Asynchronously tell a client that we delivered their message
     *
     * NOTE: this method ignores the state code of the client's response to this POST.
     *
     * @param message The message that was delivered
     */
    public void notifyClientOfDelivery(Message message) {
        AsyncHttpClient httpClient = Dsl.asyncHttpClient();
        BoundRequestBuilder
                rb = httpClient.preparePost(message.getDeliveryURL());
        rb.setBody("MESSAGE " + message.getMessageID() + " DELIVERED");
        rb.execute(new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                return response;
            }
        });
    }

    /**
     * Parse out the list of other nodes
     *
     * <P>
     *     Note that this method assumes that if PROPERTY_CLUSTER + "." &lt;number&gt; + ".host" is defined then the
     *     corresponding port is also defined.
     *
     * @return The list of Node specifications (NodeSpecs).
     * @see SpecNode
     */
    public void parseNodes () {
        if (null != properties.getProperty(PROPERTY_CLUSTER_1)) {
            String rootProperty = Miranda.PROPERTY_CLUSTER + ".1";
            SpecNode specNode = new SpecNode();
            specNode.setHost(properties.getProperty(rootProperty + ".host"));
            specNode.setPort(properties.getIntProperty(rootProperty + ".port"));
            List<SpecNode> list = new ArrayList<>();
            int count = 2;
            list.add(specNode);
            while (specNode != null) {
                specNode = null;
                rootProperty = Miranda.PROPERTY_CLUSTER + "." + count;
                if (null != properties.getProperty(rootProperty + ".host")) {
                    specNode = new SpecNode();
                    specNode.setHost(properties.getProperty(rootProperty + ".host"));
                    specNode.setPort(properties.getIntProperty(rootProperty  + ".port"));
                    list.add(specNode);
                    count++;
                }
            }

            specNodes = list;
        }
    }

}
