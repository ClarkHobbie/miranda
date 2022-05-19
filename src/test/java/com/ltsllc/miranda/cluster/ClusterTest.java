package com.ltsllc.miranda.cluster;


import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.message.MessageLog;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.TestSuperclass;
import com.ltsllc.miranda.properties.PropertiesHolder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class ClusterTest extends TestSuperclass {

    @BeforeAll
    public static void setup() {
        Configurator.setRootLevel(Level.DEBUG);
    }


    @Test
    public void connect() throws LtsllcException, InterruptedException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.getInstance().setNodes(new ArrayList<>());
        ImprovedRandom improvedRandom = new ImprovedRandom();
        Cluster.setRandomNumberGenerator(improvedRandom);

        IoConnector mockIoConnector = mock(IoConnector.class);
        Cluster.getInstance().setIoConnector(mockIoConnector);

        DefaultIoFilterChainBuilder mockIoFilterChainBuilder = mock(DefaultIoFilterChainBuilder.class);
        when(mockIoConnector.getFilterChain()).thenReturn(mockIoFilterChainBuilder);

        ConnectFuture mockConnectFuture = mock(ConnectFuture.class);

        InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.0.12", 2020);
        when(mockIoConnector.connect(inetSocketAddress)).thenReturn(mockConnectFuture);

        IoSession mockIoSession = mock(IoSession.class);
        when(mockConnectFuture.getSession()).thenReturn(mockIoSession);

        WriteFuture mockWriteFuture = mock(WriteFuture.class);
        when(mockIoSession.write(any())).thenReturn(mockWriteFuture);
        when(mockWriteFuture.getSession()).thenReturn(mockIoSession);

        IoSessionConfig mockIoSessionConfig = mock(IoSessionConfig.class);
        when(mockIoSession.getConfig()).thenReturn(mockIoSessionConfig);

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
        IoSession mockIoSession1 = mock(IoSession.class);
        node1.setIoSession(mockIoSession1);
        WriteFuture mockWriteFuture1 = mock(WriteFuture.class);
        when(mockIoSession1.write(any())).thenReturn(mockWriteFuture1);

        Node node2 = new Node(null, "127.0.0.1", 2020, null);
        IoSession mockIoSession2 = mock(IoSession.class);
        node2.setIoSession(mockIoSession2);
        WriteFuture mockWriteFuture2 = mock(WriteFuture.class);
        when(mockIoSession2.write(any())).thenReturn(mockWriteFuture2);


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
        IODMessage.append(messageUuid.toString());

        verify(mockIoSession1, atLeastOnce()).write(IODMessage.toString());
        verify(mockIoSession2, atLeastOnce()).write(IODMessage.toString());
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

            IoAcceptor mockIoAcceptor = mock(IoAcceptor.class);

            Cluster.defineStatics();
            Cluster.getInstance().setIoAcceptor(mockIoAcceptor);

            DefaultIoFilterChainBuilder mockDefaultFilterChainBuilder = mock(DefaultIoFilterChainBuilder.class);
            when(mockIoAcceptor.getFilterChain()).thenReturn(mockDefaultFilterChainBuilder);

            Cluster.getInstance().listen();

            PropertiesHolder p = Miranda.getProperties();
            int port = p.getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);

            InetSocketAddress inetSocketAddress = new InetSocketAddress("0.0.0.0", port);

            verify(mockIoAcceptor, atLeastOnce()).bind(inetSocketAddress);
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
    public void connectToNode() throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        Node node = new Node(null,"192.168.0.12", port, null);

        InetSocketAddress addr = new InetSocketAddress("192.168.0.12", port);

        IoConnector mockIoConnector = mock(IoConnector.class);
        Cluster.getInstance().setIoConnector(mockIoConnector);


        ConnectFuture mockConnectionFuture = mock(ConnectFuture.class);
        IoSession mockIoSession = mock(IoSession.class);
        when(mockConnectionFuture.getSession()).thenReturn(mockIoSession);

        WriteFuture mockWriteFuture = mock(WriteFuture.class);
        when(mockIoSession.write(any())).thenReturn(mockWriteFuture);

        IoSessionConfig mockIoSessionConfig = mock(IoSessionConfig.class);
        when(mockIoSession.getConfig()).thenReturn(mockIoSessionConfig);
        when(mockIoConnector.connect(addr)).thenReturn(mockConnectionFuture);

        when(mockWriteFuture.getSession()).thenReturn(mockIoSession);

        Cluster.getInstance().connectToNode(node, mockIoConnector);

        verify(mockIoConnector, atLeastOnce()).connect(addr);
    }

    @Test
    public void reconnectFail() throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        List<Node> list = new ArrayList<>();
        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        Node node = new Node(null, "192.168.0.12", port, null);
        list.add(node);
        Cluster.getInstance().setNodes(list);

        IoConnector mockIoConnector = mock(IoConnector.class);
        Cluster.getInstance().setIoConnector(mockIoConnector);

        InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.0.12", port);
        ConnectFuture mockConnectFuture = mock(ConnectFuture.class);
        when(mockIoConnector.connect(inetSocketAddress)).thenReturn(mockConnectFuture);
        when(mockConnectFuture.getException()).thenReturn(new Exception());

        IoSession mockIoSession = mock(IoSession.class);
        when(mockConnectFuture.getSession()).thenReturn(mockIoSession);

        WriteFuture mockWriteFuture = mock(WriteFuture.class);
        when(mockIoSession.write(any())).thenReturn(mockWriteFuture);

        when(mockWriteFuture.getSession()).thenReturn(mockIoSession);

        IoSessionConfig mockIoSessionConfig = mock(IoSessionConfig.class);
        when(mockIoSession.getConfig()).thenReturn(mockIoSessionConfig);

        when(mockWriteFuture.getSession()).thenThrow(new RuntimeIoException());

        Cluster.getInstance().reconnect();

        assert (!Cluster.getInstance().allConnected());
    }

    @Test
    public void reconnectSuccess() throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        List<Node> list = new ArrayList<>();
        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        Node node = new Node(null,"192.168.0.3", port, null);
        list.add(node);
        Cluster.getInstance().setNodes(list);

        IoConnector mockIoConnector = mock(IoConnector.class);
        Cluster.getInstance().setIoConnector(mockIoConnector);

        InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.0.3", port);
        ConnectFuture mockConnectFuture = mock(ConnectFuture.class);
        when(mockIoConnector.connect(inetSocketAddress)).thenReturn(mockConnectFuture);

        IoSession mockIoSession = mock(IoSession.class);
        when(mockConnectFuture.getSession()).thenReturn(mockIoSession);

        WriteFuture mockWriteFuture = mock(WriteFuture.class);
        when(mockIoSession.write(any())).thenReturn(mockWriteFuture);

        when(mockWriteFuture.getSession()).thenReturn(mockIoSession);

        IoSessionConfig mockIoSessionConfig = mock(IoSessionConfig.class);
        when(mockIoSession.getConfig()).thenReturn(mockIoSessionConfig);

        when(mockWriteFuture.getSession()).thenReturn(mockIoSession);

        Cluster.getInstance().reconnect();
        assert (Cluster.getInstance().allConnected());
    }

    @Test
    public void deadNode() throws LtsllcException, IOException {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();


        MessageLog.defineStatics();

        IoSession mockIoSession = mock(IoSession.class);
        UUID node1Uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

        Node node1 = new Node(node1Uuid, "192.168.0.20", 2020, mockIoSession);
        Cluster.getInstance().addNode(node1, mockIoSession);

        mockIoSession = mock(IoSession.class);
        UUID node2Uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Node node2 = new Node(node2Uuid, "192.168.0.12", 2020, mockIoSession);
        Cluster.getInstance().addNode(node2, mockIoSession);

        mockIoSession = mock(IoSession.class);
        UUID node3Uuid = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Node node3 = new Node(node3Uuid, "192.168.0.30", 2020, mockIoSession);
        Cluster.getInstance().addNode(node3, mockIoSession);

        UUID message1Uuid = UUID.fromString("12345678-9abc-def0-1234-567890123456");
        UUID message2Uuid = UUID.fromString("cdef0123-4567-89ab-cdef-0123456789ab");
        UUID message3Uuid = UUID.fromString("cdef0123-5467-89ab-cdef-0123456789ab");

        Message message1 = createTestMessage(message1Uuid);
        MessageLog.getInstance().add(message1, node2Uuid);

        Message message2 = createTestMessage(message2Uuid);
        MessageLog.getInstance().add(message2, node2Uuid);

        Message message3 = createTestMessage(message3Uuid);
        MessageLog.getInstance().add(message3, node2Uuid);

        Cluster.getInstance().deadNode(node2Uuid);

        assert (!MessageLog.getInstance().getOwnerOf(message1Uuid).equals(node2Uuid));
        assert (!MessageLog.getInstance().getOwnerOf(message2Uuid).equals(node2Uuid));
        assert (!MessageLog.getInstance().getOwnerOf(message3Uuid).equals(node2Uuid));
    }

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

    @Test
    public void takeOwnershipOf () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        IoSession mockIoSession = mock(IoSession.class);

        for (int i = 0; i < 3; i++) {
            Node node = new Node(UUID.randomUUID(), "192.168.0.20", 2020, mockIoSession);
            Cluster.getInstance().addNode(node, mockIoSession);
        }

        UUID nodeUuid = UUID.randomUUID();
        UUID messageUuid = UUID.randomUUID();
        Cluster.getInstance().takeOwnershipOf(nodeUuid, messageUuid);

        verify(mockIoSession, times(3)).write(any());
    }
}