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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;



public class Miranda {
    public static final int STATUS_SUCCESS = 200;
    public static final String PROPERTY_PROPERTIES_FILE = "properties";
    public static final String PROPERTY_DEFAULT_PROPERTIES_FILE = "miranda.properties";
    public static final String PROPERTY_LONG_LOGGING_LEVEL = "loggingLevel";
    public static final String PROPERTY_SHORT_LOGGING_LEVEL = "l";
    public static final String PROPERTY_LOGGING_LEVEL = "loggingLevel";
    public static final String PROPERTY_DEFAULT_LOGGING_LEVEL = "error";
    public static final String PROPERTY_BID_TIMEOUT = "timeouts.bid";
    public static final String PROPERTY_DEFAULT_BID_TIMEOUT = "500";
    public static final String PROPERTY_MESSAGE_PORT = "messagePort";
    public static final String PROPERTY_DEFAULT_MESSAGE_PORT = "3030";
    public static final String PROPERTY_CLUSTER_PORT = "clusterPort";
    public static final String PROPERTY_DEFAULT_CLUSTER_PORT = "2020";
    public static final String PROPERTY_CACHE_LOAD_LIMIT = "cache.loadLimit";
    public static final String PROPERTY_DEFAULT_CACHE_LOAD_LIMIT = "104856700"; // 100 megabytes
    public static final String PROPERTY_OWNER_FILE = "ownerFile";
    public static final String PROPERTY_DEFAULT_OWNER_FILE = "owners.log";
    public static final String PROPERTY_CLUSTER = "cluster";
    public static final String PROPERTY_DEFAULT_CLUSTER = "off";
    public static final String PROPERTY_HOST = "host";
    public static final String PROPERTY_CLUSTER_1 = "cluster.1.host";
    public static final String PROPERTY_CLUSTER_RETRY = "cluster.retry";
    public static final String PROPERTY_DEFAULT_CLUSTER_RETRY = "10000";
    public static final String PROPERTY_MESSAGE_LOG = "messageLog";
    public static final String PROPERTY_DEFAULT_MESSAGE_LOG = "messages.log";
    public static final String PROPERTY_INFLIGHT = "inflight";
    public static final String PROPERTY_DEFAULT_INFLIGHT = "inflight";

    protected static final Logger logger = LogManager.getLogger();
    public static final Logger event = LogManager.getLogger("events");
    protected static boolean keepRunning = true;
    public static Miranda instance = new Miranda();

    protected Cluster cluster;
    protected static ImprovedProperties properties;
    protected long clusterAlarm = -1;
    protected Server server;
    protected List<SpecNode> specNodes = new ArrayList<>();
    protected LoggingSet inflight = null;
    protected LoggingCache loggingCache = null;

    public LoggingCache getLoggingCache() {
        return loggingCache;
    }

    public void setLoggingCache(LoggingCache loggingCache) {
        this.loggingCache = loggingCache;
    }

    public LoggingSet getInflight() {
        return inflight;
    }

    public void setInflight(LoggingSet inflight) {
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
        cluster = Cluster.getInstance();
        ImprovedFile logfile = new ImprovedFile("inflight");
        inflight = new LoggingSet(logfile);
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

    public static boolean getKeepRunning() {
        return keepRunning;
    }

    public static void setKeepRunning(boolean keepRunning) {
        Miranda.keepRunning = keepRunning;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }



    public static ImprovedProperties getProperties() {
        return properties;
    }

    public static void setProperties(ImprovedProperties properties) {
        Miranda.properties = properties;
    }

    /*
     * Miranda's main loop
     *
     * Each call to this method is one iteration of Miranda's main loop. Each iteration consists
     * of:
     *     checking for new messages
     *     if there is a new message
     *         create a message for it
     *         tell the cluster about it
     *         tell the sender of the message that we created it
     *     taking the first message off the send queue
     *     attempting to deliver that message
     *     if the delivery was successful
     *         tell the cluster that we delivered it
     */
    public void mainLoop () throws LtsllcException, IOException {
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
    /*
     *
     * start up miranda
     *
     * When Miranda starts up, check for the existence of a sendQueue.  If it exists then copy the
     * file and enter the bidding mode.
     *
     */
    public void startUp (String[] args) throws Exception {
        event.info("Miranda starting up");
        logger.debug("Miranda starting up");
        logger.debug("parsing arguments");
        properties = new ImprovedProperties();
        processArguments(args);

        logger.debug("loading properties");
        loadProperties();

        logger.debug("Parsing nodes");
        parseNodes();

        logger.debug("Starting cluster");
        cluster = Cluster.getInstance();
        cluster.connect(specNodes);

        logger.debug("starting the message port");
        startMessagePort(properties.getIntProperty(PROPERTY_MESSAGE_PORT));

        ImprovedFile logfile = new ImprovedFile(Miranda.getProperties().getProperty(PROPERTY_MESSAGE_LOG));
        int loadLimit = Miranda.getProperties().getIntProperty(PROPERTY_CACHE_LOAD_LIMIT);
        ImprovedFile ownersFile = new ImprovedFile(properties.getProperty(PROPERTY_OWNER_FILE));
        if (MessageLog.shouldRecover(logfile)) {
            MessageLog.recover(logfile, loadLimit, ownersFile);
        }

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
        server.setHandler(new MessageHandler());

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

     /*
     * reload the system properties
     *
     * This simply tells the system to reload the system properties
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
        properties.setIfNull(PROPERTY_CLUSTER_PORT, PROPERTY_DEFAULT_CLUSTER_PORT);
        properties.setIfNull(PROPERTY_CACHE_LOAD_LIMIT, PROPERTY_DEFAULT_CACHE_LOAD_LIMIT);
        properties.setIfNull(PROPERTY_BID_TIMEOUT, PROPERTY_DEFAULT_BID_TIMEOUT);
        properties.setIfNull(PROPERTY_OWNER_FILE, PROPERTY_DEFAULT_OWNER_FILE);
        properties.setIfNull(PROPERTY_PROPERTIES_FILE, PROPERTY_DEFAULT_PROPERTIES_FILE);
        properties.setIfNull(PROPERTY_LOGGING_LEVEL, PROPERTY_DEFAULT_LOGGING_LEVEL);
        properties.setIfNull(PROPERTY_CLUSTER, PROPERTY_DEFAULT_CLUSTER);
        properties.setIfNull(PROPERTY_CLUSTER_RETRY, PROPERTY_DEFAULT_CLUSTER_RETRY);
        properties.setIfNull(PROPERTY_MESSAGE_LOG, PROPERTY_DEFAULT_MESSAGE_LOG);
        properties.setIfNull(PROPERTY_INFLIGHT, PROPERTY_DEFAULT_INFLIGHT);
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

    //
    // ********************************************************************************************
    //
    // Check to see if we should enter recovery mode
    //
    // We should enter recovery mode if the queue is not empty.
    //
    // ********************************************************************************************
    //
    protected boolean shouldEnterRecovery() {
        ImprovedFile messageLog = new ImprovedFile(properties.getProperty(PROPERTY_MESSAGE_LOG));
        ImprovedFile owners = new ImprovedFile(properties.getProperty(PROPERTY_OWNER_FILE));
        ImprovedFile inflight = new ImprovedFile(properties.getProperty(PROPERTY_INFLIGHT));
        return MessageLog.shouldRecover(messageLog) || owners.exists() || inflight.exists();

    }

    public static void stop () {
        keepRunning = false;
    }

    /**
     * Release all the ports we are bound to
     */
    public void releasePorts () throws Exception {
        releaseMessagePort();
    }

    /**
     * Unbind the message port
     * <P>
     *      Note that this method unbinds ALL ports bound with Mina, not just the message port.
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
     * @param message The message to deliver
     */
    public void deliver (Message message) throws IOException {
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
                                if ((response.getStatusCode() > 199) && (response.getStatusCode() < 300)){
                                    successfulMessage(message);
                                    event.info ("Delivered message (" + message.getMessageID() + ")");
                                }
                                // otherwise, keep trying
                                return response;
                            }
                        });


    }

    /**
     * Called when a message has been successfully delivered
     *
     * This method calls cluster.informOfDelivery to signal to the cluster that the message has been delivered,
     * removes the message from the send queue and takes care of notifying the client that we have delivered the
     * message.
     *
     * @param message The message
     */
    public void successfulMessage(Message message) throws IOException {
        logger.debug("entering successfulMessage with: " + message);
        cluster.informOfDelivery(message);
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
