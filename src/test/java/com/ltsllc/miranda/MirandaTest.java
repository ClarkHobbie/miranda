package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.cluster.Node;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.mockito.Mockito.*;

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

    @Test
    public void connectToOtherNodes () throws LtsllcException, InterruptedException {
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
        Message message = createTestMessage(UUID.randomUUID());
        List<Message> list = new ArrayList<>();
        list.add(message);
        Miranda.getInstance().setKeepRunning(false);

        miranda.mainLoop();

        assert (list.size() > 0);
    }

    @Test
    public void mainLoopRunning () throws LtsllcException, IOException, InterruptedException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        miranda.setMyUuid(UUID.randomUUID());

        Cluster.defineStatics();

        Message message = createTestMessage(UUID.randomUUID());
        List<Message> list = new ArrayList<>();
        list.add(message);
        MessageLog mockMessageLog = Mockito.mock(MessageLog.class);
        MessageLog.setInstance(mockMessageLog);

        when(mockMessageLog.copyAllMessages()).thenReturn(list);
        Miranda.getInstance().setKeepRunning(true);

        miranda.mainLoop();

        assert (miranda.getInflight().contains(message));
    }

    /*
    this doesn't correspond to any method

    @Test
    public void setClusterAlarm () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster mockCluster = mock(Cluster.class);
        Cluster.setInstance(mockCluster);

        List<Message> list = new ArrayList<>();

        MessageLog mockMessageLog = mock(MessageLog.class);
        MessageLog.setInstance(mockMessageLog);
        when (mockMessageLog.copyAllMessages()).thenReturn(list);

        miranda.mainLoop();

        verify(mockCluster, atLeastOnce()).reconnect();
    }
    */

    @Test
    public void deliver () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Message message = createTestMessage(UUID.randomUUID());

        miranda.deliver(message);

        assert (miranda.getInflight().contains(message));
    }

    @Test
    public void parseNodes () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile properties = new ImprovedFile("miranda.properties");
        ImprovedFile backup = new ImprovedFile("miranda.properties.backup");
        ImprovedFile test = new ImprovedFile("test03.properties");
        try {
            properties.copyTo(backup);
            properties.delete();
            test.copyTo(properties);
            miranda.loadProperties();

            assert (miranda.specNodes.size() == 0);
            miranda.parseNodes();
            assert (miranda.specNodes.size() == 1);
        } finally {
            if (backup.exists()) {
                properties.delete();
                backup.renameTo(properties);
            }
        }
    }

    @Test
    public void releasePorts () throws Exception {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        try {
            miranda.startMessagePort();
            miranda.releasePorts();

            miranda.getServer().start();
        } finally {
            miranda.getServer().stop();
        }
    }

    @Test
    public void shouldRecover () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        ImprovedProperties p = Miranda.getProperties();

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
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedProperties p = Miranda.getProperties();
        ImprovedFile messages = new ImprovedFile(p.getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        ImprovedFile owners = new ImprovedFile(p.getProperty(Miranda.PROPERTY_OWNER_FILE));
        MessageLog.defineStatics(messages, p.getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT), owners);

        Message message = createTestMessage(UUID.randomUUID());
        MessageLog.getInstance().add(message, UUID.randomUUID());

        miranda.successfulMessage(message);

        assert (MessageLog.getInstance().getOwnerOf(message.getMessageID()) == null);
    }

    @Test
    public void recover () {

    }
 }