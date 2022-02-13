package com.ltsllc.miranda;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.commons.util.ImprovedRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.session.IoSession;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Miranda {
    public static final int STATUS_SUCCESS = 200;
    public static final String PROPERTY_PROPERTIES_FILE = "properties";
    public static final String PROPERTY_DEFAULT_PROPERTIES_FILE = "mirada.properties";
    public static final String PROPERTY_SEND_FILE = "sendFile";
    public static final String PROPERTY_DEFAULT_SEND_FILE = "sendFile.jsn";
    public static final String PROPERTY_LONG_LOGGING_LEVEL = "loggingLevel";
    public static final String PROPERTY_SHORT_LOGGING_LEVEL = "l";
    public static final String PROPERTY_LOGGING_LEVEL = "loggingLevel";
    public static final String PROPERTY_MESSAGE_PORT = "messagePort";
    public static final String PROPERTY_DEFAULT_MESSAGE_PORT = "80";
    public static final String PROPERTY_CLUSTER_PORT = "clusterPort";
    public static final String PROPERTY_DEFAULT_CLUSTER_PORT = "2020";
    protected static final Logger logger = LogManager.getLogger();

    protected static List<Message> sendQueue;
    protected static volatile List<Message> newMessageQueue;

    protected Cluster cluster;
    protected ImprovedFile sendQueueFile;
    protected ImprovedRandom randomNumberGenerator = new ImprovedRandom();
    protected static ImprovedProperties properties;
    protected IoSession mainPort;

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public static List<Message> getSendQueue() {
        return sendQueue;
    }

    public static void setSendQueue(List<Message> sendQueue) {
        Miranda.sendQueue = sendQueue;
    }

    public static synchronized void addNewMessage (Message message) {
        newMessageQueue.add(message);
    }

    public static synchronized Message getNewMessage () {
        return newMessageQueue.get(0);
    }

    public static synchronized void addMessage(Message message) {
        sendQueue.add(message);
    }

    public static synchronized Message getNextMessage () {
        return sendQueue.get(0);
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
    protected void mainLoop () {
        logger.debug("starting mainLoop");
        Message newMessage = getNewMessage();
        if (newMessage != null) {
            logger.debug("a new message has arrived message = " + newMessage);

            addMessage(newMessage);

            try {
                cluster.informOfNewMessage(newMessage);
            } catch (LtsllcException e) {
                logger.error("exception telling cluster of new message");
            }
            logger.debug("we told the cluster of it");

            newMessage.informOfCreated();
            logger.debug("we told the sender we created it");
        }

        Message message = getNextMessage();
        if (message != null) {
            Result result = message.deliver();
            if (result.getStatus() == STATUS_SUCCESS) {
                cluster.informOfDelivery(message);
                message.informOfDelivery();
            }
            else {
                sendQueue.add(sendQueue.size(), message);
            }

        }
    }

    /*
     *
     * start up miranda
     *
     * When Miranda starts up, check for the existence of a sendQueue.  If it exists then copy the
     * file and enter the bidding mode.
     *
     */
    public void startUp (String[] args) throws LtsllcException {
        logger.debug("Miranda starting up");
        logger.debug("parsing arguments");
        properties = new ImprovedProperties();
        processArguments(args);

        logger.debug("loading properties");
        loadProperties();

        logger.debug("Starting cluster");
        cluster = new Cluster();
        cluster.connect();

        logger.debug("starting the message port");
        startMessagePort(properties.getIntProperty(PROPERTY_MESSAGE_PORT, PROPERTY_DEFAULT_MESSAGE_PORT));

        logger.debug("Checking for recovery");
        if (sendQueueFile.exists()) {
            logger.debug("send queue file exists, recovering");
            sendQueueFile = sendQueueFile.copy();
            loadSendFile();
            biddingMode();
        }


        logger.debug("Startup complete going into main loop");
        for (;;) {
            mainLoop();
        }
    }

    /*
     * listen for new messages
     *
     * @param portNumber The TCP port that we should listen at
     */
    protected void startMessagePort (int portNumber) throws LtsllcException {
        logger.debug("entering startMessagePort with portNumber = " + portNumber);
        // Create and configure the thread pool.
        newMessageQueue = new ArrayList<>();
        sendQueue = new ArrayList<>();
        logger.debug("initialized sendQueue and newMessageQueue");

        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("client");
        logger.debug("initialized threadPool");

        // Create and configure the scheduler.
        Scheduler scheduler = new ScheduledExecutorScheduler("scheduler-client", false);
        logger.debug("initialized scheduler");

        // Create and configure the custom ClientConnector.
        ClientConnector clientConnector = new ClientConnector();
        clientConnector.setExecutor(threadPool);
        clientConnector.setScheduler(scheduler);
        logger.debug("initialized clientConnector");

        // create and configure a ContextHandler
        ContextHandler contextHandler = new ContextHandler();
        contextHandler.setContextPath("/");
        contextHandler.setHandler(new MessageHandler());
        logger.debug("initialized contextHandler");

        try {
            clientConnector.start();
            logger.debug("listening for messages at port " + portNumber);
        } catch (Exception e) {
            throw new LtsllcException("exception starting the server", e);
        }
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
            map.put(prev, argument);
            return;
        }

        logger.debug("leaving processArgument with map = " + map);
    }

    /*
     * "bid" for each message
     *
     * for each message...
     *    bid for it
     *    if we didn't win the bid then remove the message from the sendqueue
     */
    public synchronized void biddingMode() throws LtsllcException {
        for (int i = 0;i < sendQueue.size();i++) {
            Message message = sendQueue.get(i);
            logger.debug("bidding on " + message);

            Message ourMessage = cluster.bid(message);
            if (ourMessage == null) {
                logger.debug("we lost bid so removing the message");
                sendQueue.remove(i);
            } else {
                logger.debug("we won the bid, keep the message");
            }
        }
    }

    /*
     * load the send file
     *
     * This simply tells the system to reload the send file
     */
    protected void loadSendFile () throws LtsllcException {
        logger.debug("entering loadSendFile");
        String tempFileName = properties.getProperty(PROPERTY_SEND_FILE);
        sendQueueFile = new ImprovedFile(tempFileName);
        ImprovedFile temp = new ImprovedFile(tempFileName);
        FileInputStream fileInputStream;
        InputStreamReader reader;
        try {
            fileInputStream = new FileInputStream(temp);
            reader = new InputStreamReader(fileInputStream);
            logger.debug("Found send file");

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setPrettyPrinting();
            Gson gson = gsonBuilder.create();
            sendQueue = gson.fromJson(reader, List.class);
            logger.debug("loaded send file");
        } catch (FileNotFoundException e) {
            ;
        } catch (IOException ioException) {
            throw new LtsllcException("exception loading send queue", ioException);
        }

        logger.debug("leaving loadSendFile");
    }

    /*
     * reload the system properties
     *
     * This simply tells the system to reload the system properties
     */
    protected void loadProperties () throws LtsllcException {
        FileInputStream in = null;
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
        properties.setIfNull(Miranda.PROPERTY_CLUSTER_PORT, Miranda.PROPERTY_DEFAULT_MESSAGE_PORT);
        properties.setIfNull(Miranda.PROPERTY_SEND_FILE,PROPERTY_DEFAULT_SEND_FILE);
        properties.setIfNull(PROPERTY_CLUSTER_PORT, PROPERTY_DEFAULT_CLUSTER_PORT);

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
        return sendQueueFile.exists();
    }

    /*
     * write an object to a Writer
     *
     * This method simply writes an object, converted to JSON, to a writer.  Note that this method
     * does a flush to the writer after it writes the JSON.
     */
    protected void writeJson (Object object,Writer writer) throws IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();

        String json = gson.toJson(object);
        writer.write(json);
        writer.flush();
    }
}
