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
    public void connectToOtherNodes () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        miranda.setClusterAlarm(System.currentTimeMillis());
        miranda.connectToOtherNodes();

        Cluster mockCluster = Mockito.mock(Cluster.class);
        Cluster.setInstance(mockCluster);

        miranda.connectToOtherNodes();

        Mockito.verify(mockCluster, atLeastOnce()).reconnect();
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
            miranda.startMessagePort(1234);
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


    @Test
    public void startUpNoFile () throws Exception {
        Miranda miranda = new Miranda();
        ImprovedFile mirandaProperties = new ImprovedFile("mirada.properties");
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

    /*
     * startUp with an existing sendFile
     */
    @Test
    public void startupExistingFile () throws Exception {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        MessageLog mockMessageLog = mock(MessageLog.class);
        MessageLog.setInstance(mockMessageLog);
        ImprovedProperties p = Miranda.getProperties();

        ImprovedFile messages = new ImprovedFile(p.getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        ImprovedFile ownersFile = new ImprovedFile(p.getProperty(Miranda.PROPERTY_OWNER_FILE));
        when(mockMessageLog.performRecover(messages, p.getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT),
                ownersFile)).thenReturn(mockMessageLog);

        Cluster mockCluster = mock(Cluster.class);
        Cluster.setInstance(mockCluster);

        ImprovedFile events = new ImprovedFile("events.log");
        try {
            messages.touch();
            String[] args = new String[0];

            miranda.startUp(args);

            verify(mockMessageLog, atLeastOnce()).performRecover(messages,
                    p.getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT) , ownersFile);
        } finally {
            messages.delete();
            events.delete();
            miranda.getServer().stop();
        }
    }

    public void newNode () throws Exception {
        Miranda miranda = new Miranda();

        try {
            miranda.loadProperties();
            String[] args = new String[0];
            miranda.startUp(args);

            MirandaThread mirandaThread = new MirandaThread();
            mirandaThread.setMiranda(miranda);
            mirandaThread.setSleepTime(500);

            mirandaThread.start();

            Socket socket = null;
            InputStream inputStream = null;

            try {
                socket = new Socket("localhost", miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT));
                inputStream = socket.getInputStream();

                logger.debug("MirandaThread running");
                synchronized (this) {
                    wait(1000);
                }
                List<Node> nodes = Cluster.getInstance().getNodes();
                System.out.println(nodes.size());
                assert (nodes.size() > 0);
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (socket != null) {
                    socket.close();
                }
                if (mirandaThread != null) {
                    mirandaThread.setKeepRunning(false);
                }
            }
        } finally {

        }

    }

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

        miranda.setClusterAlarm(System.currentTimeMillis() - 10000);

        miranda.mainLoop();

        verify(mockCluster, atLeastOnce()).reconnect();
    }

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
            miranda.startMessagePort(3030);
            miranda.releasePorts();

            miranda.getServer().start();
        } finally {
            miranda.getServer().stop();
        }
    }

    @Test
    public void shouldEnterRecovery () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedProperties p = Miranda.getProperties();
        ImprovedFile messages = new ImprovedFile(p.getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        messages.touch();
        ImprovedFile owners = new ImprovedFile(p.getProperty(Miranda.PROPERTY_OWNER_FILE));

        assert(miranda.shouldEnterRecovery());

        messages.delete();

        assert (miranda.shouldEnterRecovery());

        owners.touch();

        assert (miranda.shouldEnterRecovery());
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

        assert (MessageLog.getInstance().getLocationFor(message.getMessageID()) == -1);

    }
}