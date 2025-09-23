package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.logging.LoggingCache;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.logging.MessageLog;
import com.ltsllc.miranda.properties.PropertiesHolder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;

import static org.mockito.Mockito.when;

/**
 * Test the main functions of Miranda
 */
class MirandaTest extends TestSuperclass {

    public static final Logger logger = LogManager.getLogger();

    /**
     * Set the default logging level to DEBUG
     */
    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @BeforeEach
    void setupEach () {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        improvedFile.clear();
        MessageLog.defineStatics();
    }


    @Test
    public void connectToOtherNodes () throws Throwable {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        miranda.setMyUuid(UUID.randomUUID());

        Cluster.defineStatics();

        Miranda.getProperties().setProperty(Miranda.PROPERTY_CLUSTER_RETRY, "500");
        synchronized (this) {
            wait(2 * Miranda.getProperties().getLongProperty(Miranda.PROPERTY_CLUSTER_RETRY));
        }
    }

    @Test
    void loadProperties() throws Exception {
        Miranda miranda = new Miranda();
        try {
            miranda.loadProperties();
            assert (Miranda.getProperties().getProperty(Miranda.PROPERTY_OWNER_FILE) != null);
        } finally {

        }
    }

    @Test
    void startMessagePort () throws Exception {
        Miranda miranda = new Miranda();
        try {
            miranda.loadProperties();
            miranda.startMessagePort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            miranda.getServer().stop();
        }
    }

    @Test
    void processArguments () throws Exception {
        Miranda miranda = new Miranda();
        try {
            miranda.loadProperties();
            String[] args = {"-one", "two"};
            Properties properties = miranda.processArguments(args);
            assert (properties.getProperty("o").equals("o"));
            assert (properties.getProperty("n").equals("n"));
            assert (properties.getProperty("e").equals("two"));
            String[] args2 = {"-l", "two", "-RT"};
            Properties properties2 = miranda.processArguments(args2);
            assert (properties2.getProperty("l").equals("two"));
            assert (properties2.getProperty("R").equals("R"));
            assert (properties2.getProperty("T").equals("T"));
        } finally {
        }
    }

    @Test
    void processArgument () throws Exception {
        Miranda miranda = new Miranda();
        try {
            ImprovedProperties improvedProperties = new ImprovedProperties();
            miranda.processArgument("prev", "-abc", improvedProperties);
            assert (improvedProperties.getProperty("a").equals("a"));
            assert (improvedProperties.getProperty("b").equals("b"));
            assert (improvedProperties.getProperty("c").equals("c"));

            improvedProperties = new ImprovedProperties();
            miranda.processArgument("prev", "abc", improvedProperties);
            assert (improvedProperties.getProperty("prev").equals("abc"));
        } finally {

        }
   }

    /*
    commented out because startup creates it's own cluster
    @Test
    public void startUpNoFile () throws Exception {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        ImprovedFile mirandaProperties = new ImprovedFile("miranda.properties");
        ImprovedFile mirandaBackup = new ImprovedFile("miranda.backup");
        ImprovedFile testProperties = new ImprovedFile("test.properties");
        try {
            Cluster mockCluster = mock(Cluster.class);
            Cluster.setInstance(mockCluster);

            mirandaProperties.copyTo(mirandaBackup);
            testProperties.copyTo(mirandaProperties);

            String[] args = new String[0];
            miranda.startUp(args);

            verify(mockCluster, atLeastOnce()).connect(any());
        } finally {
            mirandaBackup.copyTo(mirandaProperties);
            miranda.getServer().stop();
        }
    }


     //
     // startUp with an existing sendFile
     //
    @Test
    public void startupExistingFile () throws Exception {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        MessageLog mockMessageLog = mock(MessageLog.class);
        MessageLog.setInstance(mockMessageLog);
        ImprovedProperties p = Miranda.getProperties();

        ImprovedFile messages = new ImprovedFile(p.getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        ImprovedFile messagesBackup = new ImprovedFile(p.getProperty(Miranda.PROPERTY_MESSAGE_LOG) + ".backup");
        ImprovedFile ownersFile = new ImprovedFile(p.getProperty(Miranda.PROPERTY_OWNER_FILE));
        ImprovedFile ownersBackup = new ImprovedFile(p.getProperty(Miranda.PROPERTY_OWNER_FILE) + ".backup");
        when(mockMessageLog.performRecover(messages, p.getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT),
                ownersFile)).thenReturn(mockMessageLog);

        Cluster mockCluster = mock(Cluster.class);
        Cluster.setInstance(mockCluster);

        Server mockServer = mock(Server.class);
        miranda.setServer(mockServer);

        try {
            messages.touch();
            String[] args = new String[0];

            miranda.startUp(args);

            assert(miranda.getSynchronizationFlag());
        } finally {
            messages.delete();
            messagesBackup.delete();
        }
    }

     */



    @Test
    public void mainLoopNotRunning () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        Message message = createTestMessage(UUID.randomUUID(), UUID.randomUUID());
        List<Message> list = new ArrayList<>();
        list.add(message);
        Miranda.getInstance().setKeepRunning(false);

        miranda.mainLoop();

        assert (list.size() > 0);
    }

    @Test
    public void mainLoopRunning () throws LtsllcException, IOException, InterruptedException {
        Miranda miranda = new Miranda();
        MessageLog.defineStatics();
        miranda.loadProperties();
        miranda.setMyUuid(UUID.randomUUID());

        Cluster.defineStatics();

        Message message = createTestMessage(UUID.randomUUID(), UUID.randomUUID());
        message.setNextSend(System.currentTimeMillis());
        MessageLog.getInstance().add(message, message.getOwner());
        Miranda.getInstance().setKeepRunning(true);

        miranda.mainLoop();

        assert (miranda.getInflight().contains(message));
    }

    @Test
    public void deliver () throws LtsllcException, IOException {
        try {
            ImprovedFile temp = new ImprovedFile("messages.log.backup");
            if (temp.exists()) {
                temp.delete();
            }

            temp = new ImprovedFile("owners.log.backup");
            if (temp.exists()) {
                temp.delete();
            }

            Thread thread = startMiranda();

            setLoggingLevel("io.netty", Level.WARN);

            Miranda miranda = Miranda.getInstance();

            long now = System.currentTimeMillis();
            String strUuid = "c4fa2935-034a-4602-ae10-0f829a3bf16f";
            Message message = createTestMessage(UUID.randomUUID());
            message.setNextSend(now);
            message.setMessageID(UUID.fromString(strUuid));

            strUuid = "7a8afadf-2954-48f1-9477-c4eb627f1a9b";
            Message message2 = createTestMessage(UUID.randomUUID());
            long sendTime = now + 1000000;
            message2.setNextSend(sendTime);
            message2.setMessageID(UUID.fromString(strUuid));

            MessageLog.getInstance().add(message, miranda.getMyUuid());
            MessageLog.getInstance().add(message2, miranda.getMyUuid());

            assert (message.getNumberOfSends() == 0);
            assert (message.getLastSend() == 0);

            logger.debug("thread state: " + thread.getState());

            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            logger.debug("thread state: " + thread.getState());

            assert (isMirandaRunning() == true);
            assert (!miranda.getInflight().contains(message));
            assert (message.getLastSend() != 0);
            assert (message.getNumberOfSends() != 0);
            assert (message.getNextSend() != 0);

            Set<Message> messageSet = miranda.getInflight();
            boolean inSet = messageSet.contains(message2);

            assert (!inSet);
            long lastSend = message2.getLastSend();
            assert (lastSend == 0);

            long numberOfSends = message2.getNumberOfSends();
            assert (numberOfSends == 0);

            long nextSend = message2.getNextSend();
            assert (nextSend == now + 1000000);
        } finally {
            MessageLog.getInstance().clear();
        }
    }

    @Test
    public void deliver2 () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();

        try {
            Thread thread = startMiranda();
            MessageLog.defineStatics();

            logger.debug("thread state: " + thread.getState());

            Message message = createTestMessage(UUID.randomUUID());

            UUID uuid = UUID.fromString("7f246518-c9bb-417d-b61d-08c2e9d2aed8");
            message.setMessageID(uuid);

            long currentTime = System.currentTimeMillis();
            message.setNextSend(currentTime);

            assert (message.getNumberOfSends() == 0);
            assert (message.getLastSend() == 0);

            logger.debug("thread state: " + thread.getState());

            MessageLog.getInstance().add(message, miranda.getMyUuid());

            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            assert (message.getLastSend() != 0);
            assert (message.getNumberOfSends() != 0);
            assert (message.getNextSend() != 0);
        } finally {
            closePorts();
            MessageLog.getInstance().clear();
        }
    }

    @Test
    public void parseNodes () throws LtsllcException {
        try {
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            startMiranda();

            Miranda miranda = Miranda.getInstance();

            ImprovedFile properties = new ImprovedFile("miranda.properties");
            ImprovedFile backup = new ImprovedFile("miranda.properties.backup");
            ImprovedFile test = new ImprovedFile("test03.properties");
            try {
                properties.copyTo(backup);
                //            properties.delete();
                //            test.copyTo(properties);
                miranda.loadProperties();


                miranda.parseNodes();
                assert (miranda.specNodes.size() == 1);
            } finally {
                if (backup.exists()) {
                    properties.delete();
                    backup.renameTo(properties);
                }
            }
        } finally {
            closePorts();
        }
    }

    @Test
    public void releasePorts () throws Exception {
        startMiranda();

        synchronized (this) {
            wait(1000);
        }
        Miranda miranda = Miranda.getInstance();

        miranda.getServer().stop();
    }

    @Test
    public void shouldRecover () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        PropertiesHolder p = Miranda.getProperties();

        ImprovedFile messages = new ImprovedFile(p.getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        ImprovedFile owners = new ImprovedFile(p.getProperty(Miranda.PROPERTY_OWNER_FILE));
        try {

            owners.delete();
            messages.touch();

            assert (Miranda.shouldRecover());

            messages.delete();

            assert (!Miranda.shouldRecover());

            owners.touch();

            assert (Miranda.shouldRecover());
        } finally {
            messages.delete();
            owners.delete();
        }
    }

    @Test
    public void successfulMessage () throws LtsllcException, IOException {
        try {
            Miranda miranda = new Miranda();

            miranda.loadProperties();

            Message message = createTestMessage(UUID.randomUUID());
            MessageLog.getInstance().add(message, UUID.randomUUID());

            miranda.successfulMessage(message);

            assert (MessageLog.getInstance().getOwnerOf(message.getMessageID()) == null);
        } finally {
            MessageLog.getInstance().clear();
        }
    }

    @Test
    public void recover () {

    }
 }