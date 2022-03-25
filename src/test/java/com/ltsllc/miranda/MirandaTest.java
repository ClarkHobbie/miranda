package com.ltsllc.miranda;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.cluster.Node;
import com.ltsllc.miranda.logcache.TestSuperclass;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

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

        miranda.setClusterAlarm(-1);
        miranda.connectToOtherNodes();

        List<Node> list = Cluster.getInstance().getNodes();
        assert (list.size() < 1);

        long now = System.currentTimeMillis();
        miranda.setClusterAlarm(now);

        miranda.connectToOtherNodes();

        list = Cluster.getInstance().getNodes();
        assert (list.size() > 0);
    }

    @Test
    void loadProperties() throws Exception {
        Miranda miranda = new Miranda();
        try {
            miranda.loadProperties();
        } finally {
            miranda.releasePorts();
        }
    }

    @Test
    void startMessagePort () throws Exception {
        Miranda miranda = new Miranda();
        try {
            miranda.loadProperties();
            miranda.startMessagePort(1234);
        } finally {
            miranda.releasePorts();
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
            miranda.releasePorts();
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
            miranda.releasePorts();
        }
   }


    @Test
    public void startUpNoFile () throws Exception {
        Miranda miranda = new Miranda();
        try {
            miranda.loadProperties();
            Cluster.defineStatics();
            String[] args = new String[0];
            miranda.startUp(args);
        } finally {
            miranda.releasePorts();
        }
    }

    /*
     * startUp with an existing sendFile
     */
    @Test
    public void starupExistigFile () throws Exception {
        Message m1 = new Message();
        UUID uuid1 = UUID.randomUUID();
        m1.setMessageID(uuid1);
        m1.setContents("hi there".getBytes());
        m1.setStatusURL("http://google.com");
        m1.setDeliveryURL("http://google.com");

        List<Message> list = new ArrayList<>();
        list.add(m1);

        Miranda miranda = new Miranda();
        try {


            miranda.loadProperties();
            miranda.releasePorts();

            String sendFileName = miranda.getProperties().getProperty(Miranda.PROPERTY_SEND_FILE);
            ImprovedFile file = new ImprovedFile(sendFileName);
            file.touch();
            String[] args = new String[0];

            miranda.startUp(args);

            list = SendQueue.getInstance().copyMessages();
            assert (list != null && list.size() > 0);
            Message m2 = list.get(0);
            assert (m1.equals(m2));
        } finally {
            miranda.releasePorts();
        }
    }

    @Test
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
            miranda.releasePorts();
        }

    }

    @Test
    public void mainLoopNotRunning () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        Message message = createTestMessage(UUID.randomUUID());
        List<Message> list = new ArrayList<>();
        list.add(message);
        Miranda.setKeepRunning(false);

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
        Miranda.setKeepRunning(true);

        miranda.mainLoop();

        assert (list.size() == 0);
    }

    @Test
    public void setClusterAlarm () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        miranda.setClusterAlarm(System.currentTimeMillis() - 10);

        miranda.mainLoop();

        assert (Cluster.getInstance().getNodes().size() > 0);
    }

    @Test
    public void deliver () {

    }
}