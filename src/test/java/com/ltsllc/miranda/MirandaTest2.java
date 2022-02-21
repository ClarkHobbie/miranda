package com.ltsllc.miranda;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ltsllc.commons.LtsllcException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.mina.core.session.IoSession;
import org.asynchttpclient.*;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MirandaTest2 {
    public static final Logger logger = LogManager.getLogger();

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

        assert (message.equals(m2));
    }

    @Test
    public void startUp () throws LtsllcException {
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
    public void sartup2 () throws LtsllcException, IOException {
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
    public void newNode () throws LtsllcException, InterruptedException {
        Configurator.setRootLevel(Level.DEBUG);
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        String[] args = new String[0];
        // miranda.getCluster().connect();
        miranda.startUp(args);
        AsyncHttpClient client = Dsl.asyncHttpClient();
        Request postRequest = Dsl.get("http://localhost:" + miranda.getProperties().getProperty(Miranda.PROPERTY_CLUSTER_PORT))
                .build();

        client.executeRequest(postRequest, new AsyncCompletionHandler<Integer>() {
            @Override
            public Integer onCompleted(Response response) {

                int resposeStatusCode = response.getStatusCode();
                assert(200 == resposeStatusCode);
                return resposeStatusCode;
            }
        });

        MirandaThread mirandaThread  = new MirandaThread();
        mirandaThread.setMiranda(miranda);
        mirandaThread.setSleepTime(1000);

        mirandaThread.start();
        synchronized (this) {
            logger.debug("MirandaThread running");
            wait(1000, TimeUnit.MILLISECONDS.ordinal());
        }
        List<IoSession> nodes = Cluster.getNodes();
        assert (nodes.size() > 0);
        mirandaThread.setKeepRunning(false);
    }
}
