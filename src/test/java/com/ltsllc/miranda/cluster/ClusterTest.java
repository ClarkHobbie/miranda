package com.ltsllc.miranda.cluster;


import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.asynchttpclient.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

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

        IoConnector mockIoConnector = mock(IoConnector.class);
        Cluster.getInstance().setCreatedIoConnector(mockIoConnector);

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
            Cluster.getInstance().setCreatedIoConnector(mockIoConnector);
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
}