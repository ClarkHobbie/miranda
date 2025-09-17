package com.ltsllc.miranda.cluster;


import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.TestSuperclass;
import com.ltsllc.miranda.alarm.AlarmClock;
import com.ltsllc.miranda.alarm.Alarms;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.logging.MessageLog;
import com.ltsllc.miranda.netty.HeartBeatHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.*;

class ClusterTest extends TestSuperclass {
    public static Logger logger = LogManager.getLogger(ClusterTest.class);

    @BeforeAll
    public static void setup() {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @AfterEach
    public void tearDown () {
        closePorts();
    }


    public void connect() throws LtsllcException, CloneNotSupportedException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.getInstance().setNodes(new ArrayList<>());
        ImprovedRandom improvedRandom = new ImprovedRandom();
        Cluster.setRandom(improvedRandom);

        InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.0.12", 2020);

        closePorts();

        miranda.parseNodes();
        try {
            Cluster.getInstance().start(miranda.getSpecNodes());
        } catch (BindException e) {
            throw new RuntimeException(e);
        }
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

        Node node1 = new Node(UUID.randomUUID(),"71.237.68.250", 2021, null);
        Node node2 = new Node(UUID.randomUUID(), "127.0.0.1", 2021, null);
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
    public void listen() throws LtsllcException {
        Cluster.defineStatics();

        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        boolean listenFailed = true;
        Cluster cluster = Cluster.getInstance();
        for (int i = 0; listenFailed && i < 5; i++) {
            try {
                cluster.listen(port + i);
                listenFailed = false;
            } catch (BindException e) {
                // swallow the exception
            }

        }

        assert(Cluster.getInstance().isBound());
    }


    @Test
    public void reconnectFail() throws LtsllcException, CloneNotSupportedException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        List<Node> list = new ArrayList<>();
        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(), "71.237.68.250", port, channel);
        list.add(node);
        Cluster.getInstance().setNodes(list);

        Cluster.getInstance().reconnect();

        assert (Cluster.getInstance().allConnected());
    }


    public void reconnectSuccess() throws LtsllcException, CloneNotSupportedException {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        improvedFile.clear();

        Miranda miranda = new Miranda();
        miranda.loadProperties();
        Cluster.defineStatics();

        List<Node> list = new ArrayList<>();
        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        Node node = new Node(UUID.randomUUID(),"192.168.0.3", port, null);
        list.add(node);
        Cluster.getInstance().setNodes(list);
        Cluster.getInstance().reconnect();
        assert (Cluster.getInstance().allConnected());
    }

    @Test
    public void divideUpMessages () {
        Miranda miranda = new Miranda();
        Cluster.defineStatics();

        List<Node> voterList = new ArrayList<>();
        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(), "192.168.0.12", 2020, channel);
        voterList.add(node);
        node = new Node(UUID.randomUUID(), "192.168.0.6", 2020, channel);
        voterList.add(node);

        // Cluster.getInstance().divideUpMessages(voterList, list);

    }

    public Cluster buildCluster () {
        Cluster cluster = new Cluster();

        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline().addLast(Cluster.HEART_BEAT, new HeartBeatHandler(embeddedChannel));
        Node node = new Node(UUID.fromString("00000000-0000-0000-0000-000000000000"), "10.0.0.236", 2020, embeddedChannel);
        cluster.addNode(node);

        embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline().addLast(Cluster.HEART_BEAT, new HeartBeatHandler(embeddedChannel));
        node = new Node(UUID.fromString("00000000-0000-0000-0000-000000000001"), "10.0.0.237", 2020, embeddedChannel);
        cluster.addNode(node);

        embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline().addLast(Cluster.HEART_BEAT, new HeartBeatHandler(embeddedChannel));
        node = new Node(UUID.fromString("00000000-0000-0000-0000-000000000002"), "10.0.0..238", 2020, embeddedChannel);
        cluster.addNode(node);

        return cluster;
    }

    @Test
    public void addNodeAlreadyPresent () {
        Cluster cluster = buildCluster();

        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(), "71.237.68.250", 2021, embeddedChannel);
        cluster.addNode(node);
        cluster.addNode(node);

        Map<UUID, Boolean> map = new HashMap<>();
        for (Node node2 : cluster.getNodes())
        {
            if (map.containsKey(node2.getUuid())) {
                throw new RuntimeException("duplicate");
            }

            map.put(node2.getUuid(), true);
        }

        assert (true);
    }

    @Test
    public void addNodeLoopback () {
        Miranda.setInstance(new Miranda());
        Miranda.getInstance().setMyUuid(UUID.randomUUID());

        Cluster cluster = buildCluster();
        EmbeddedChannel embeddedChannel = new EmbeddedChannel();

        HeartBeatHandler heartBeatHandler = new HeartBeatHandler(embeddedChannel);
        embeddedChannel.pipeline().addLast(Cluster.HEART_BEAT, heartBeatHandler);

        Node node = new Node(Miranda.getInstance().getMyUuid(), "71.237.68.250", 2020, embeddedChannel);
        cluster.addNode(node);

        assert (node.getIsLoopback());
    }

    @Test
    public void addNode () {
        Cluster cluster = null;
        try {
            cluster = buildCluster();
            EmbeddedChannel embeddedChannel = new EmbeddedChannel();
            Node node = new Node(UUID.randomUUID(), "71.237.68.250", 2021, embeddedChannel);

            cluster.addNode(node);

            assert (cluster.containsNode(node));
        } finally {
            if (cluster != null) {
                cluster.close();
            }
        }
    }

    @Test
    public void coalesce () throws LtsllcException, CloneNotSupportedException {
        Cluster.defineStatics();
        Cluster cluster = Cluster.getInstance();

        EmbeddedChannel embeddedChannel = new EmbeddedChannel();

        Node node = new Node(UUID.randomUUID(), "71.237.68.250", 2020, embeddedChannel);
        cluster.addNode(node);

        Node node2 = new Node(UUID.randomUUID(), "71.237.68.250", 2020, embeddedChannel);
        node2.setUuid(node.getUuid());
        cluster.addNode(node2);

        cluster.coalesce();

        assert (cluster.getNodes().size() == 1);
    }

    @Test
    public void alarm () {
        Cluster cluster = buildCluster();

    }

    @Test
    public void deadNodeTimeout () throws InterruptedException {
        Cluster cluster = buildCluster();
        Cluster.setInstance(cluster);
        UUID deadNodeUuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Node deadNode = null;

        for (Node node : cluster.getNodes()) {
            if (node.getUuid().equals(deadNodeUuid)) {
                deadNode = node;
            }
        }

        assert (cluster.getNodes().contains(deadNode));

        cluster.startDeadNode(deadNodeUuid);
        pause(Miranda.getProperties().getLongProperty(Miranda.PROPERTY_DEAD_NODE_TIMEOUT) * 2);

        assert (!cluster.getNodes().contains(deadNode));
    }

    @Test
    public void leaderTimeout () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        Cluster cluster = buildCluster();
        Node deadNode = cluster.getNodes().getLast();
        cluster.setDeadNode(deadNode.getUuid());
        cluster.removeNode(deadNode);
        Node node = cluster.getNodes().getLast();
        cluster.removeNode(node);

        Cluster.setInstance(cluster);

        MessageLog.defineStatics();

        Message message = createTestMessage(UUID.randomUUID());
        MessageLog.getInstance().add(message, deadNode.getUuid());

        ImprovedRandom random = new ImprovedRandom();
        cluster.startElection(deadNode.getUuid());

        cluster.getElection().vote(cluster.getNodes().getFirst(), random.nextInt());

        node = cluster.getNodes().get(0);

        assert (cluster.getNodes().size() == 1);
        assert (node.getUuid().equals(Miranda.getInstance().getMyUuid()));
        assert (cluster.getElection().getLeader() == null);

        cluster.leaderTimeout();

        Node leader = cluster.getLeader();
        leader.sendDeadNode(deadNode.getUuid());

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Node.OWNER);
        stringBuilder.append(' ');
        stringBuilder.append(message.getMessageID().toString());
        stringBuilder.append(' ');
        stringBuilder.append(cluster.getNodes().getLast().getUuid().toString());

        List<UUID> list = MessageLog.getInstance().getAllMessagesOwnedBy(deadNode.getUuid());

        assert (cluster.getLeader()  != null);
        assert (list.size() == 0);
    }

    public Election buildElection() {
        Cluster cluster = Cluster.getInstance();
        UUID deadNode = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Election election = new Election(deadNode);

        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        Node node = null;
        for (Node node2 : cluster.getNodes()) {
            if (node2.getUuid().equals(uuid)) {
                node = node2;
            }
        }
        Election.Voter voter = new Election.Voter();
        voter.setNode(node);
        voter.vote = Integer.MAX_VALUE;

        election.addVoter(voter);

        uuid = UUID.fromString("00000000-0000-0000-0000-000000000002");
        node = null;
        for (Node node2 : cluster.getNodes()) {
            if (node2.getUuid().equals(uuid)) {
                node = node2;
            }
        }
        voter = new Election.Voter();
        voter.setNode(node);
        voter.vote = 0;

        election.addVoter(voter);

        return election;
    }

    @Test
    public void chooseNode () {
        Cluster.defineStatics();
        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(), "10.0.0.236", 2020, channel);
        Cluster.getInstance().addNode(node);

        Node node2 = Cluster.getInstance().chooseNode();

        assert (node2.equals(node));
    }

    @Test
    public void isAlone () {
        Cluster.defineStatics();
        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(), "10.0.0.236", 2020, channel);
        Cluster.getInstance().addNode(node);

        assert (Cluster.getInstance().isAlone(node.getUuid()));

        channel = new EmbeddedChannel();
        Node node2 = new Node(UUID.randomUUID(), "10.0.0.1", 2029, channel);
        Cluster.getInstance().addNode(node2);

        assert (!Cluster.getInstance().isAlone(node.getUuid()));
    }
}