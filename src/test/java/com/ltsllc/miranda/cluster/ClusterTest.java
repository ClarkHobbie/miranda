package com.ltsllc.miranda.cluster;


import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.TestSuperclass;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.message.MessageLog;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class ClusterTest extends TestSuperclass {

    @BeforeAll
    public static void setup() {
        Configurator.setRootLevel(Level.DEBUG);
    }



    public void connect() throws LtsllcException, CloneNotSupportedException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.getInstance().setNodes(new ArrayList<>());
        ImprovedRandom improvedRandom = new ImprovedRandom();
        Cluster.setRandomNumberGenerator(improvedRandom);


         InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.0.12", 2020);
        // when(mockIoConnector.connect(inetSocketAddress)).thenReturn(mockConnectFuture);




        miranda.parseNodes();
        Cluster.getInstance().start(miranda.getSpecNodes());
    }

    /*
     * TODO the receiving side of informOfNewMessage
     * o    record the new message
     * o    send back a 200
     * o    send back a non-200
     * TODO how does miranda respond to
     *  o   2 other nodes
     * TODO how does miranda respond to
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
    public void informOfDelivery() throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        miranda.setMyUuid(UUID.randomUUID());

        Cluster.defineStatics();

        Node node1 = new Node(null,"192.168.0.12", 2020, null);
        Node node2 = new Node(null, "127.0.0.1", 2020, null);
        // node2.setIoSession(mockIoSession2);


        List<Node> list = new ArrayList<>();
        list.add(node1);
        list.add(node2);
        Cluster.getInstance().setNodes(list);

        UUID messageUuid = UUID.randomUUID();
        Message message = createTestMessage(messageUuid);

        Cluster.getInstance().informOfDelivery(message);

        StringBuffer IODMessage = new StringBuffer();
        IODMessage.append(Node.MESSAGE_DELIVERED);
        IODMessage.append(" ");
        IODMessage.append(messageUuid);

    }

    @Test
    void informOfNewMessage() throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.getInstance().setNodes(new ArrayList<>());

        Message message = new Message();
        message.setStatusURL("http://goggle.com");
        UUID uuid = UUID.randomUUID();
        message.setMessageID(uuid);
        byte[] contents = {1, 2, 3};
        message.setContents(contents);

        Cluster.getInstance().informOfNewMessage(message);
    }

    @Test
    public void listen() throws LtsllcException, IOException {
        ImprovedFile events = new ImprovedFile("events.log");

        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();

            Cluster.defineStatics();

            Cluster.getInstance().listen();

            assert(Cluster.getInstance().isBound());

        } finally {
            if (events.exists()) {
                boolean deleted = events.delete();
                if (deleted) {
                    System.out.println("deleted");
                }
            }
        }
    }

    @Test
    public void connectToNode() throws LtsllcException, CloneNotSupportedException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(null,"192.168.0.12", port, channel);

        Cluster.getInstance().connectToNode(node, false);
    }

    @Test
    public void reconnectFail() throws LtsllcException, CloneNotSupportedException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        List<Node> list = new ArrayList<>();
        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(null, "192.168.0.12", port, channel);
        list.add(node);
        Cluster.getInstance().setNodes(list);

        Cluster.getInstance().reconnect();

        assert (Cluster.getInstance().allConnected());
    }


    public void reconnectSuccess() throws LtsllcException, CloneNotSupportedException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        List<Node> list = new ArrayList<>();
        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        Node node = new Node(null,"192.168.0.3", port, null);
        list.add(node);
        Cluster.getInstance().setNodes(list);


        InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.0.3", port);


        Cluster.getInstance().reconnect();
        assert (Cluster.getInstance().allConnected());
    }


    public void deadNode() throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();


        MessageLog.defineStatics();

        UUID node1Uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

        // Node node1 = new Node(node1Uuid, "192.168.0.20", 2020, mockIoSession);
        // Cluster.getInstance().addNode(node1, mockIoSession);

        UUID node2Uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        // Node node2 = new Node(node2Uuid, "192.168.0.12", 2020, mockIoSession);
        // Cluster.getInstance().addNode(node2, mockIoSession);

        UUID node3Uuid = UUID.fromString("00000000-0000-0000-0000-000000000002");
        // Node node3 = new Node(node3Uuid, "192.168.0.30", 2020, mockIoSession);
        // Cluster.getInstance().addNode(node3, mockIoSession);

        UUID message1Uuid = UUID.fromString("12345678-9abc-def0-1234-567890123456");
        UUID message2Uuid = UUID.fromString("cdef0123-4567-89ab-cdef-0123456789ab");
        UUID message3Uuid = UUID.fromString("cdef0123-5467-89ab-cdef-0123456789ab");

        Message message1 = createTestMessage(message1Uuid);
        MessageLog.getInstance().add(message1, node2Uuid);

        Message message2 = createTestMessage(message2Uuid);
        MessageLog.getInstance().add(message2, node2Uuid);

        Message message3 = createTestMessage(message3Uuid);
        MessageLog.getInstance().add(message3, node2Uuid);
        

        assert (!MessageLog.getInstance().getOwnerOf(message1Uuid).equals(node2Uuid));
        assert (!MessageLog.getInstance().getOwnerOf(message2Uuid).equals(node2Uuid));
        assert (!MessageLog.getInstance().getOwnerOf(message3Uuid).equals(node2Uuid));
    }

    /*
    @Test
    public void divideUpMessages () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        List<UUID> list = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            UUID uuid = UUID.randomUUID();
            list.add(uuid);
        }

        for (int i = 0; i < 3; i++) {
            List<UUID> portion = Cluster.getInstance().divideUpMessages(4, i, list);
            if (i != 3) {
                assert (portion.size() == 13 / 4);
            } else {
                assert (portion.size() == 4);
            }
        }
    }
*/

    @Test
    public void takeOwnershipOf () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();



        EmbeddedChannel channel = new EmbeddedChannel();
        for (int i = 0; i < 3; i++) {
            Node node = new Node(UUID.randomUUID(), "192.168.0.20", 2020, channel);
            Cluster.getInstance().addNode(node);
        }

        UUID nodeUuid = UUID.randomUUID();
        UUID messageUuid = UUID.randomUUID();
        Cluster.getInstance().takeOwnershipOf(nodeUuid, messageUuid);
        String s = channel.readOutbound();
        assert (s.startsWith(Node.TAKE));
    }

    @Test
    public void divideUpMessages () {
        Miranda miranda = new Miranda();
        Cluster.defineStatics();
        List<Message> list = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            Message message = new Message();
            message.setMessageID(UUID.randomUUID());
            message.setDeliveryURL("http://localhost:3030");
            message.setContents("hi there".getBytes());
            message.setStatusURL("http://localhost:3030");
            list.add(message);
        }

        List<Node> voterList = new ArrayList<>();
        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(), "192.168.0.12", 2020, channel);
        voterList.add(node);
        node = new Node(UUID.randomUUID(), "192.168.0.6", 2020, channel);
        voterList.add(node);

        // Cluster.getInstance().divideUpMessages(voterList, list);

    }

}