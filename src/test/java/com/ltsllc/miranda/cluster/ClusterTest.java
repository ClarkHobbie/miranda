package com.ltsllc.miranda.cluster;


import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.logcache.TestSuperclass;
import jakarta.servlet.FilterChain;
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
import org.eclipse.jetty.io.RuntimeIOException;
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
    public void informOfDelivery () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Node node1 = new Node("192.168.0.12", 2020);
        IoSession mockIoSession1 = mock(IoSession.class);
        node1.setIoSession(mockIoSession1);
        WriteFuture mockWriteFuture1 = mock(WriteFuture.class);
        when(mockIoSession1.write(any())).thenReturn(mockWriteFuture1);

        Node node2 = new Node( "127.0.0.1", 2020);
        IoSession mockIoSession2 = mock(IoSession.class);
        WriteFuture mockWriteFuture2 = mock(WriteFuture.class);
        when(mockIoSession2.write(any())).thenReturn(mockWriteFuture2);

        node2.setIoSession(mockIoSession2);

        List<Node> list = new ArrayList<>();
        list.add(node1);
        list.add(node2);
        Cluster.getInstance().setNodes(list);

        UUID messageUuid = UUID.randomUUID();
        Message message = createTestMessage(messageUuid);

        Cluster.getInstance().informOfDelivery(message);

        StringBuffer IODMessage = new StringBuffer();
        IODMessage.append(ClusterHandler.MESSAGE_DELIVERED);
        IODMessage.append(" ");
        IODMessage.append(messageUuid.toString());

        verify(mockIoSession1,atLeastOnce()).write(IODMessage.toString());
        verify(mockIoSession2,atLeastOnce()).write(IODMessage.toString());
    }

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

        ImprovedFile properties = new ImprovedFile("miranda.properties");
        ImprovedFile backup = new ImprovedFile("miranda.backup");
        ImprovedFile testProperties = new ImprovedFile("test02.properties");

        try {
            miranda.parseNodes();

            Cluster.defineStatics();

            properties.copyTo(backup);
            testProperties.copyTo(properties);
            IoConnector mockIoConnector = mock(IoConnector.class);
            Cluster.getInstance().setIoConnector(mockIoConnector);
            when(mockIoConnector.getFilterChain()).thenReturn(new DefaultIoFilterChainBuilder());
            InetSocketAddress temp = new InetSocketAddress("192.168.0.12", 2020);

            ConnectFuture mockConnectFuture = mock(ConnectFuture.class);
            when(mockIoConnector.connect(temp)).thenReturn(mockConnectFuture);

            IoSession mockIoSession = mock(IoSession.class);
            when(mockConnectFuture.getSession()).thenReturn(mockIoSession);

            WriteFuture mockWriteFuture = mock(WriteFuture.class);
            when(mockIoSession.write(any())).thenReturn(mockWriteFuture);
            when(mockWriteFuture.getSession()).thenReturn(mockIoSession);

            IoSessionConfig mockIoSessionConfig = mock(IoSessionConfig.class);
            when(mockIoSession.getConfig()).thenReturn(mockIoSessionConfig);

            Cluster.getInstance().connect(miranda.getSpecNodes());

            verify(mockIoConnector, atLeastOnce()).connect(temp);
        } finally {
            if (backup.exists()) {
                backup.copyTo(properties);
                backup.delete();
            }
        }
    }

    @Test
    public void listen () throws LtsllcException, IOException {
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

            ImprovedProperties p = Miranda.getProperties();
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
    public void connectToNode () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        Node node = new Node("192.168.0.12", port);
        node.setConnected(false);

        IoConnector mockIoConnector = mock(IoConnector.class);
        InetSocketAddress addr = new InetSocketAddress("192.168.0.12", port);

        Cluster.getInstance().connectToNode(node, mockIoConnector);

        verify(mockIoConnector, atLeastOnce()).connect(addr);
    }

    @Test
    public void reconnectFail () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        List<Node> list = new ArrayList<>();
        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        Node node = new Node("192.168.0.3", port);
        node.setConnected(false);
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

        when(mockWriteFuture.getSession()).thenThrow(new RuntimeIoException());

        Cluster.getInstance().reconnect();

        assert (miranda.getClusterAlarm() != -1);
    }

    @Test
    public void reconnectSuccess () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        List<Node> list = new ArrayList<>();
        int port = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        Node node = new Node("192.168.0.3", port);
        node.setConnected(false);
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

        assert (-1 == miranda.getClusterAlarm());
    }
}