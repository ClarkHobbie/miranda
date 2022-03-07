package com.ltsllc.miranda;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.commons.util.Utils;
import com.ltsllc.miranda.cluster.Cluster;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.mina.core.session.IoSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Test the main functions of Miranda
 */
class MirandaTest {

    public static final Logger logger = LogManager.getLogger();

    /**
     * Set the default logging level to DEBUG
     */
    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    void loadProperties() throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
    }

    @Test
    void startMessagePort () throws Exception {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        miranda.startMessagePort(1234);
    }

    @Test
    void processArguments () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        String[] args = {"-one", "two"};
        Properties properties = miranda.processArguments(args);
        assert (properties.getProperty("o").equals("o"));
        assert (properties.getProperty("n").equals("n"));
        assert (properties.getProperty("e").equals("two"));
        String[] args2 = { "-l", "two","-RT"};
        Properties properties2 = miranda.processArguments(args2);
        assert (properties2.getProperty("l").equals("two"));
        assert (properties2.getProperty("R").equals("R"));
        assert (properties2.getProperty("T").equals("T"));
    }

    @Test
    void processArgument () throws LtsllcException {
        Miranda miranda = new Miranda();
        ImprovedProperties improvedProperties = new ImprovedProperties();
        miranda.processArgument("prev", "-abc", improvedProperties);
        assert (improvedProperties.getProperty("a").equals("a"));
        assert (improvedProperties.getProperty("b").equals("b"));
        assert (improvedProperties.getProperty("c").equals("c"));

        improvedProperties = new ImprovedProperties();
        miranda.processArgument("prev", "abc", improvedProperties);
        assert (improvedProperties.getProperty("prev").equals("abc"));
   }

    @Test
    public void loadSendFile () throws LtsllcException, IOException {
        Configurator.setRootLevel(Level.DEBUG);
        Miranda miranda = new Miranda();
        miranda.loadProperties();


        miranda = new Miranda();
        miranda.loadProperties();
        File sendFile = new File(Miranda.PROPERTY_DEFAULT_SEND_FILE);
        FileOutputStream fileOutputStream = new FileOutputStream(sendFile);
        Writer writer = new OutputStreamWriter(fileOutputStream);

        Date d1 = new Date();
        Date d2 = new Date();
        UUID uuid = new UUID(d1.getTime(), d2.getTime());

        Message message = new Message();
        message.setMessageID(uuid);
        message.setStatusURL("http://google.com");
        message.setContents("Hi there!".getBytes());
        message.setDeliveryURL("http://google.com");

        List<Message> list = new ArrayList<>();
        list.add(message);

        miranda.writeJson(list,writer);
        fileOutputStream.close();

        Miranda.setSendQueue(new ArrayList<>());

        miranda.loadSendFile();

        list = Miranda.getSendQueue();
        Message m2 = list.get(0);

        assert (Utils.bothEqualCheckForNull(message,m2));
    }

    @Test
    public void startUpNoFile () throws Exception {
        Configurator.setRootLevel(Level.DEBUG);
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        String[] args = new String[0];
        miranda.startUp(args);
    }

    /*
     * startUp with an existing sendFile
     */
    @Test
    public void starupExistigFile () throws Exception {
        Message m1 = new Message();
        Date d1 = new Date();
        Date d2 = new Date();
        UUID uuid1 = new UUID(d1.getTime(), d2.getTime());
        m1.setMessageID(uuid1);
        m1.setContents("hi there".getBytes());
        m1.setStatusURL("http://google.com");
        m1.setDeliveryURL("http://google.com");

        List<Message> list = new ArrayList<>();
        list.add(m1);

        Miranda miranda = new Miranda();
        miranda.loadProperties();
        miranda.releasePorts();

        String sendFileName = miranda.getProperties().getProperty(Miranda.PROPERTY_SEND_FILE);
        FileOutputStream fileOutputStream = new FileOutputStream(sendFileName);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();

        Gson gson = builder.create();
        gson.toJson(list, outputStreamWriter);

        outputStreamWriter.close();
        fileOutputStream.close();

        String[] args = new String[0];

        miranda.startUp(args);

        list = miranda.getSendQueue();
        assert (list != null && list.size() > 0);
        Message m2 = list.get(0);
        assert (m1.equals(m2));
    }

    @Test
    public void newNode () throws Exception {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        String[] args = new String[0];
        miranda.startUp(args);

        MirandaThread mirandaThread  = new MirandaThread();
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
            List<IoSession> nodes = Cluster.getNodes();
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

    }

    @Test
    public void biddingMode() {
        // TODO: define
    }

    @Test
    public void mainLoop () {
        // TODO: define
    }



}