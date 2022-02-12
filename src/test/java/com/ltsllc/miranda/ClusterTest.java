package com.ltsllc.miranda;


import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.commons.util.ImprovedRandom;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.mina.core.session.IoSession;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClusterTest {

    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void bid() throws LtsllcException {
        Cluster cluster = new Cluster();
        cluster.setNodes(new ArrayList<>());
        ImprovedRandom improvedRandom = new ImprovedRandom();
        Cluster.setRandomNumberGenerator(improvedRandom);

        Message message = new Message();
        message.setStatusURL("http://goggle.com");
        Date date1 = new Date();
        Date date2 = new Date();
        UUID uuid = new UUID(date1.getTime(), date2.getTime());
        message.setMessageID(uuid);

        cluster.bid(message);
    }

    @Test
    public void connect () throws LtsllcException {
        Cluster cluster = new Cluster();
        cluster.setNodes(new ArrayList<>());
        ImprovedRandom improvedRandom = new ImprovedRandom();
        Cluster.setRandomNumberGenerator(improvedRandom);

        Miranda.setProperties(new ImprovedProperties());
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        cluster.connect();
    }

    @Test
    void informOfNewMessage () throws LtsllcException {
        Cluster cluster = new Cluster();
        cluster.setNodes(new ArrayList<>());
        ImprovedRandom improvedRandom = new ImprovedRandom();
        Cluster.setRandomNumberGenerator(improvedRandom);

        Message message = new Message();
        message.setStatusURL("http://goggle.com");
        Date date1 = new Date();
        Date date2 = new Date();
        UUID uuid = new UUID(date1.getTime(), date2.getTime());
        message.setMessageID(uuid);

        cluster.informOfNewMessage(message);
    }


}