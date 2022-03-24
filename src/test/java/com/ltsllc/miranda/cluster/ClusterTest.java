package com.ltsllc.miranda.cluster;


import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.mina.core.session.IoSession;
import org.asynchttpclient.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

class ClusterTest {

    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }


    @Test
    public void connect () throws LtsllcException, InterruptedException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.getInstance().setNodes(new ArrayList<>());
        ImprovedRandom improvedRandom = new ImprovedRandom();
        Cluster.setRandomNumberGenerator(improvedRandom);


        miranda.parseNodes();
        Cluster.getInstance().connect(miranda.getSpecNodes());
    }

    /*
     * TODO the receiving side of informOfNewMessage
     * o    record the new message
     * o    send back a 200
     * o    send back a non-200
     * TODO how does miranda respond to
     *  o   No other nodes
     *  o   1 other nodes
     *  o   2 other nodes
     * TODO how does miranda respond to
     *  o a timeout when sending the notification of a new message
     *  o an interrupt during the write
     *  o a timeout reading the response
     *  o an interrupt during the response
     *  o a success code
     *  o a non-success (non-200) response, 1st time then success
     *  o a non-success response two times, the a success
     *  o a non-success all times
     *
     * TODO how does miranda recover?
     *  o   a node going down permanently
     *  o   a node going down but coming back up during recovery
     *  o   a node going down and there are 3 nodes in the cluster
     *  o   a node going down and there are 2 nodes in the cluster
     *
     * TODO how many messages?
     *  o   a node going down and it has no messages
     *  o   a node going down and it has 1 message
     *  o   a node going down and it has more than 1 message
     *
     * TODO message contents
     *  o   Zero contents
     *  o   1 byte
     *  o   more than 1 byte
     *
     * TODO how does miranda deal with heart beets?
     *  o   what happens when a node misses 1 heart beet?
     *  o   when does a node decide that another node is down?
     *  o   how does miranda react to a node coming online?
     */

    @Test
    void informOfNewMessage () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.getInstance().setNodes(new ArrayList<>());

        Message message = new Message();
        message.setStatusURL("http://goggle.com");
        UUID uuid = UUID.randomUUID();
        message.setMessageID(uuid);
        byte[] contents = {1,2,3};
        message.setContents(contents);

        Cluster.getInstance().informOfNewMessage(message);
    }

    @Test
    public void testClustered () throws Exception {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        miranda.parseNodes();

        Cluster.defineStatics();
        Cluster.getInstance().connect(miranda.getSpecNodes());

        List<Node> list = Cluster.getInstance().getNodes();
        assert (list.size() > 0);
    }
}