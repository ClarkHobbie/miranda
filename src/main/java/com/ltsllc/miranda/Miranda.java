package com.ltsllc.miranda;


import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.cluster.ClusterHandler;
import com.ltsllc.miranda.cluster.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.asynchttpclient.*;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class Miranda {
    public static final int STATUS_SUCCESS = 200;
    public static final String PROPERTY_PROPERTIES_FILE = "properties";
    public static final String PROPERTY_DEFAULT_PROPERTIES_FILE = "mirada.properties";
    public static final String PROPERTY_SEND_FILE = "sendFile";
    public static final String PROPERTY_DEFAULT_SEND_FILE = "sendFile.msg";
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
    public static final String PROPERTY_OFFLINE_MESSAGES = "offlineMessages";
    public static final String PROPERTY_DEFAULT_OFFLINE_MESSAGES = "offlineMessages.msg";
    public static final String PROPERTY_OTHER_MESSAGES = "otherMessages";
    public static final String PROPERTY_DEFAULT_OTHER_MESSAGES = "otherMessage.msg";
    public static final String PROPERTY_OWNER_FILE = "ownerFile";
    public static final String PROPERTY_DEFAULT_OWNER_FILE = "owners.msg";
    public static final String PROPERTY_CLUSTER = "cluster";
    public static final String PROPERTY_DEFAULT_CLUSTER = "off";
    public static final String PROPERTY_HOST = "host";
    public static final String PROPERTY_OTHER_MESSAGES_LOAD_LIMIT = "otherMessages.loadLimit";
    public static final String PROPERTY_DEFAULT_OTHER_MESSAGES_LOAD_LIMIT = "104856670"; // 10 MegaBytes


    protected static final Logger logger = LogManager.getLogger();
    protected static boolean keepRunning = true;
    public static Miranda instance = new Miranda();

    protected Cluster cluster;
    protected static ImprovedProperties properties;
    protected List<Message> newMessageQueue = new ArrayList<>();
    protected long clusterAlarm = -1;
    protected Server server;

    public Miranda() {
        cluster = new Cluster();
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


    public synchronized List<Message> getNewMessageQueue () {
        return newMessageQueue;
    }

    public synchronized Message getNewMessage () {
        if (newMessageQueue.size() < 1)
            return null;
        else
            return newMessageQueue.get(0);
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
            Message newMessage = getNewMessage();
            if (newMessage != null) {
                logger.debug("a new message has arrived message = " + newMessage);

                SendQueue.getInstance().add(newMessage);

                try {
                    cluster.informOfNewMessage(newMessage);
                } catch (LtsllcException e) {
                    logger.error("exception telling cluster of new message");
                }
                logger.debug("we told the cluster of it");
            }

            /*
             * Try and deliver all the messages
             */
            List<Message> allMessages = SendQueue.getInstance().copyMessages();
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

        if (clusterAlarm < System.currentTimeMillis()) {
            return;
        }

        //
        // it's time to connect
        //
        Cluster.getInstance().reconnect();
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
        logger.debug("Miranda starting up");
        logger.debug("parsing arguments");
        properties = new ImprovedProperties();
        processArguments(args);

        logger.debug("loading properties");
        loadProperties();

        logger.debug("Starting cluster");
        cluster = Cluster.getInstance();
        cluster.connect();

        logger.debug("starting the message port");
        startMessagePort(properties.getIntProperty(PROPERTY_MESSAGE_PORT));

        if (SendQueue.shouldRecover())
            SendQueue.recover();

        logger.debug("Leaving startup");
    }

    /*
     * listen for new messages
     *
     * @param portNumber The TCP port that we should listen at
     */
    protected void startMessagePort (int portNumber) throws Exception {
        logger.debug("entering startMessagePort with portNumber = " + portNumber);
        /*
        IoAcceptor ioAcceptor = new NioSocketAcceptor();
        ioAcceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
        ioAcceptor.setHandler(new MirandaHandler());

        SocketAddress addr = new InetSocketAddress(Miranda.getProperties().getIntProperty(Miranda.PROPERTY_MESSAGE_PORT));
        ioAcceptor.bind (addr);

        */

        // Create and configure a ThreadPool.
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("server");

        server = new Server(threadPool);

        // Create a ServerConnector to accept connections from clients.
        ServerConnector serverConnector = new ServerConnector(server, 3, 3, new HttpConnectionFactory());
        server.addConnector(serverConnector);
        serverConnector.setPort(properties.getIntProperty(PROPERTY_MESSAGE_PORT));
        serverConnector.setHost("127.0.0.1");

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
            in = new FileInputStream(PROPERTY_DEFAULT_PROPERTIES_FILE);
            Properties temp = new Properties();
            temp.load(in);
            properties.setIfNull(temp);
        } catch (FileNotFoundException e) {
            properties = new ImprovedProperties();
        } catch (IOException ioException) {
            throw new LtsllcException("exception loading properties file", ioException);
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException ioException) {
                    logger.error("exception closing properties file", ioException);
                }
            }
        }

        properties.setIfNull(PROPERTY_MESSAGE_PORT, PROPERTY_DEFAULT_MESSAGE_PORT);
        properties.setIfNull(PROPERTY_SEND_FILE, PROPERTY_DEFAULT_SEND_FILE);
        properties.setIfNull(PROPERTY_CLUSTER_PORT, PROPERTY_DEFAULT_CLUSTER_PORT);
        properties.setIfNull(PROPERTY_CACHE_LOAD_LIMIT, PROPERTY_DEFAULT_CACHE_LOAD_LIMIT);
        properties.setIfNull(PROPERTY_BID_TIMEOUT, PROPERTY_DEFAULT_BID_TIMEOUT);
        properties.setIfNull(PROPERTY_OFFLINE_MESSAGES, PROPERTY_DEFAULT_OFFLINE_MESSAGES);
        properties.setIfNull(PROPERTY_OTHER_MESSAGES, PROPERTY_DEFAULT_OTHER_MESSAGES);
        properties.setIfNull(PROPERTY_OWNER_FILE, PROPERTY_DEFAULT_OWNER_FILE);
        properties.setIfNull(PROPERTY_PROPERTIES_FILE, PROPERTY_DEFAULT_PROPERTIES_FILE);
        properties.setIfNull(PROPERTY_LOGGING_LEVEL, PROPERTY_DEFAULT_LOGGING_LEVEL);
        properties.setIfNull(PROPERTY_CLUSTER, PROPERTY_DEFAULT_CLUSTER);
        properties.setIfNull(PROPERTY_OTHER_MESSAGES_LOAD_LIMIT, PROPERTY_DEFAULT_OTHER_MESSAGES_LOAD_LIMIT);
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
        return SendQueue.shouldRecover() || OtherMessages.shouldRecover() ;
    }

    public static void stop () {
        keepRunning = false;
    }

    /**
     * Release all the ports we are bound to
     */
    public void releasePorts () throws Exception {
        Cluster.getInstance().releasePorts();
        releaseMessagePort();
    }

    /**
     * Unbind the message port
     *
     * Note that this method unbinds ALL ports, not just the message port.
     */
    public synchronized void releaseMessagePort () throws Exception {
        logger.debug("entering releaseMessagePort.");
        logger.debug("server = " + server);
        if (server != null) {
            server.stop();
            //server.destroy();
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
    public void deliver (Message message) {
        AsyncHttpClient httpClient = Dsl.asyncHttpClient();
        BoundRequestBuilder boundRequestBuilder = httpClient.preparePost(message.getDeliveryURL());

        boundRequestBuilder.setBody(message.getContents())
                .setBody("CREATED MESSAGE " + message.getMessageID().toString())
                        .execute(new AsyncCompletionHandler<Response>() {
                            @Override
                            public Response onCompleted(Response response) {
                                if (response.getStatusCode() == 200) {
                                    successfulMessage(message);
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
    public void successfulMessage(Message message) {
        logger.debug("entering successfulMessage with: " + message);
        cluster.informOfDelivery(message);
        try {
            SendQueue.getInstance().remove(message);
        } catch (LtsllcException|IOException e) {
            logger.error ("Exception trying to remove message from the send queue",e);
        }
        notifyClientOfDelivery(message);
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
}
