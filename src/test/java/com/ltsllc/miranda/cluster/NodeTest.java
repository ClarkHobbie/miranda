package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.commons.util.Utils;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.MessageLog;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.logging.LoggingCache;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Any;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class NodeTest {
    @Test
    public void auctionMessage () throws InterruptedException, LtsllcException, IOException {
        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile owners = new ImprovedFile("owners.log");

        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();

            MessageLog.defineStatics(messages, 1000000, owners);
            Node node = new Node("localhost", 2020);

            Message message = createTestMessage(UUID.randomUUID());

            IoSession mockIoSession = Mockito.mock(IoSession.class);

            ReadFuture mockReadFuture = Mockito.mock(ReadFuture.class);

            Mockito.when(mockIoSession.read()).thenReturn(mockReadFuture);

            node.setIoSession(mockIoSession);

            StringBuffer reply = new StringBuffer();
            reply.append(ClusterHandler.BID);
            reply.append(" ");
            reply.append(message.getMessageID());
            reply.append(" 123");

            Mockito.when(mockReadFuture.getMessage()).thenReturn(reply.toString());

            UUID nodeUuid = UUID.randomUUID();
            UUID partnerUuid = UUID.randomUUID();
            node.setUuid(nodeUuid);
            node.setPartnerID(partnerUuid);

            ImprovedRandom mockImprovedRandom = Mockito.mock(ImprovedRandom.class);
            Mockito.when(mockImprovedRandom.nextInt()).thenReturn(456);

            Node.setOurRandom(mockImprovedRandom);

            node.auctionMessage(message);

            assert (MessageLog.getInstance().getOwnerOf(message.getMessageID()) == nodeUuid);
        } finally {
            messages.delete();
            owners.delete();
        }
    }

    public Message createTestMessage (UUID uuid) {
        Message message = new Message();
        message.setMessageID(uuid);
        message.setStatusURL("HTTP://GOOGLE.COM");
        message.setDeliveryURL("HTTP://GOOGLE.COM");
        byte[] contents = {1, 2, 3};
        message.setContents(contents);

        return message;
    }

    @Test
    public void informOfMessageDelivery () {
        Message newMessage = createTestMessage(UUID.randomUUID());
        IoSession mockIoSession = Mockito.mock(IoSession.class);

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ClusterHandler.MESSAGE_DELIVERED);
        stringBuffer.append(" ");
        stringBuffer.append(newMessage.getMessageID());

        Node node = new Node(UUID.randomUUID(),UUID.randomUUID(),mockIoSession);
        node.informOfMessageDelivery(newMessage);

        assert (MessageLog.getInstance().getOwnerOf(newMessage.getMessageID()) == null);
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(stringBuffer.toString());
    }

    @Test
    public void informOfMessageCreation () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics(UUID.randomUUID());

        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile owners = new ImprovedFile("owners.log");
        MessageLog.defineStatics(messages, 1000000, owners);

        Message newMessage = createTestMessage(UUID.randomUUID());

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ClusterHandler.NEW_MESSAGE);
        stringBuffer.append(" ");
        stringBuffer.append(newMessage.internalsToString());

        IoSession mockIoSession = Mockito.mock(IoSession.class);

        Node node = new Node(UUID.randomUUID(), UUID.randomUUID(), mockIoSession);

        node.informOfMessageCreation(newMessage);

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(stringBuffer.toString());
    }

    @Test
    public void informOfStartOfAuction () {
        IoSession mockIoSession = Mockito.mock(IoSession.class);
        Node node = new Node(UUID.randomUUID(), UUID.randomUUID(), mockIoSession);

        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile owners = new ImprovedFile("owners.log");
        MessageLog.defineStatics(messages, 1000000, owners);
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ClusterHandler.AUCTION);
        stringBuffer.append(" ");
        stringBuffer.append(node.getUuid());

        node.informOfStartOfAuction(node.getUuid());

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write (stringBuffer.toString());
    }

    @Test
    public void informOfEndOfAuction () {
        IoSession mockIoSession = Mockito.mock(IoSession.class);
        Node node = new Node(UUID.randomUUID(), UUID.randomUUID(), mockIoSession);

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ClusterHandler.AUCTION_OVER);

        node.informOfAuctionEnd();

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(stringBuffer.toString());
    }

    public static final Logger logger = LogManager.getLogger();

    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);

        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();
        } catch (LtsllcException e) {
            logger.error("exception trying to load properties",e);
        }
    }

    @Test
    public void testStartMessage () throws LtsllcException, IOException {
        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile owners = new ImprovedFile("owners.log");

        try {
            StringBuffer stringBuffer = new StringBuffer();
            Message message = createTestMessage(UUID.randomUUID());
            stringBuffer.append(message.longToString());

            MessageLog.defineStatics(messages, 1000000, owners);

            Node node = new Node("localhost", 2020);


            IoSession mockIoSession = mock(IoSession.class);

            node.messageReceived(mockIoSession, stringBuffer.toString());

            assert (node.getState() == ClusterConnectionStates.GENERAL);
        } finally {
            if (messages.exists()) {
                messages.delete();
            }

            if (owners.exists()) {
                owners.delete();
            }
        }
    }

    @Test
    public void testStartStart () throws LtsllcException, IOException {
        Node node = new Node("localhost", 2020);
        node.setUuid(UUID.randomUUID());

        ImprovedFile messageLog = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messageLog, 104857600); // 100Meg

        StringBuffer strMessage = new StringBuffer();
        strMessage.append(ClusterHandler.START);
        strMessage.append(" ");
        strMessage.append(UUID.randomUUID());
        strMessage.append(" 192.168.0.12 2020");

        IoSessionConfig mockIoSessionConfig = mock(IoSessionConfig.class);
        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(new Any())).thenReturn(null);
        when(mockIoSession.getConfig()).thenReturn(mockIoSessionConfig);

        node.messageReceived(mockIoSession, strMessage.toString());

        assert (node.getState() == ClusterConnectionStates.GENERAL);
    }

    @Test
    public void testStartBid () throws LtsllcException, IOException {
        String strMessage = "BID 123e4567-e89b-42d3-a456-556642440000 123456";
        Node node = new Node("192.168.0.12", 2020);
        node.setState(ClusterConnectionStates.START);

        ImprovedFile messageLog = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messageLog, 104857600);
        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write("ERROR")).thenReturn(null);

        node.messageReceived(mockIoSession, strMessage);

        assert (node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession).write(ClusterHandler.ERROR_START);
    }

    @Test
    public void testAuctionHeartBeat () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);
        node.setState(ClusterConnectionStates.AUCTION);

        Miranda.getInstance().setMyHost("192.168.0.18");
        Miranda.getInstance().setMyPort(2020);

        String strMessage = ClusterHandler.HEART_BEAT_START;

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(new Any())).thenReturn(null);

        node.messageReceived(mockIoSession, strMessage);

        assert (node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(ClusterHandler.ERROR_START);
    }

    @Test
    public void testAuctionBidWonNotInCache () throws LtsllcException, IOException {
        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile owners = new ImprovedFile("owners.log");
        MessageLog.defineStatics(messages, 1000000, owners);
        try {
            String strMessage = ClusterHandler.BID;
            strMessage += " ";
            UUID uuid = UUID.randomUUID();
            strMessage += uuid;
            strMessage += " ";
            strMessage += "1234";

            Node node = new Node("192.168.0.12", 2020);
            node.setCache(MessageLog.getInstance().getCache());

            node.setState(ClusterConnectionStates.AUCTION);

            IoSession mockIoSession = mock(IoSession.class);
            when(mockIoSession.write(new Any())).thenReturn(null);

            ImprovedRandom mockRandom = mock(ImprovedRandom.class);
            when(mockRandom.nextInt()).thenReturn(4567);

            node.setUuid(UUID.randomUUID());
            node.setOurRandom(mockRandom);

            node.messageReceived(mockIoSession, strMessage);

            strMessage = ClusterHandler.GET_MESSAGE;
            strMessage += " ";
            strMessage += uuid;

            Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(strMessage);
        } finally {
            messages.delete();
            owners.delete();
        }
    }

    @Test
    public void testAuctionBidLost () throws IOException, LtsllcException {
        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile owners = new ImprovedFile("owners.log");
        try {
            MessageLog.defineStatics(messages, 1000000, owners);

            UUID ourUuid = UUID.randomUUID();
            UUID theirUuid = UUID.randomUUID();
            Node node = new Node(ourUuid, "localhost", 2020);

            LoggingCache cache = new LoggingCache(messages, 104857600);
            node.setUuid(ourUuid);
            node.setPartnerID(theirUuid);
            node.setState(ClusterConnectionStates.AUCTION);

            MessageCache mockCache = mock(MessageCache.class);

            UUID messageUuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");

            IoSession mockIoSession = mock(IoSession.class);
            when(mockIoSession.write(new Any())).thenReturn(null);

            String strMessage = ClusterHandler.BID;
            strMessage += " ";
            strMessage += messageUuid;
            strMessage += " 1234";

            ImprovedRandom mockRandom = mock(ImprovedRandom.class);
            when(mockRandom.nextInt()).thenReturn(123);
            node.setOurRandom(mockRandom);

            node.messageReceived(mockIoSession, strMessage);

            assert (node.getState() == ClusterConnectionStates.AUCTION);
            Mockito.verify(mockIoSession, Mockito.atMost(0)).write(new Any());
        } finally {
            messages.delete();
            owners.delete();
        }
    }

    @Test
    public void testAuctionGetMessageInCache () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.setState(ClusterConnectionStates.AUCTION);

        LoggingCache mockCache = mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        when(mockCache.contains(uuid)).thenReturn(true);

        node.setCache(mockCache);

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(new Any())).thenReturn(null);

        Message message = new Message();
        message.setMessageID(uuid);
        message.setStatusURL("HTTP://GOOGLE.COM");
        message.setDeliveryURL("HTTP://GOOGLE.COM");
        byte[] contents = {1, 2, 3};
        message.setContents(contents);
        when(mockCache.contains(uuid)).thenReturn(true);
        when(mockCache.isInMemory(uuid)).thenReturn(true);
        when(mockCache.get(uuid)).thenReturn(message);

        String strMessage = ClusterHandler.GET_MESSAGE;
        strMessage +=  " ";
        strMessage += uuid;
        node.messageReceived(mockIoSession, strMessage);

        assert(node.getState() == ClusterConnectionStates.AUCTION);

        strMessage = ClusterHandler.MESSAGE;
        strMessage += " ID: ";
        strMessage += uuid;
        strMessage += " STATUS: HTTP://GOOGLE.COM";
        strMessage += " DELIVERY: HTTP://GOOGLE.COM";
        strMessage += " CONTENTS: ";
        strMessage += Utils.hexEncode(contents); // have to trust that contents is {1, 2, 3}
        strMessage.toUpperCase();

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(strMessage);
    }

    @Test
    public void testAuctionGetMessageCacheMiss () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);

        LoggingCache mockCache = mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        when(mockCache.contains(uuid)).thenReturn(false);
        node.setState(ClusterConnectionStates.AUCTION);
        node.setCache(mockCache);

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(new Any())).thenReturn(null);

        String strMessage = ClusterHandler.GET_MESSAGE;
        strMessage +=  " ";
        strMessage += uuid;

        node.messageReceived(mockIoSession, strMessage);

        assert(node.getState() == ClusterConnectionStates.AUCTION);
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(ClusterHandler.MESSAGE_NOT_FOUND);
    }

    @Test
    public void testAuctionAuctionOver () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.setState(ClusterConnectionStates.AUCTION);

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(new Any())).thenReturn(null);

        String strMessage = ClusterHandler.AUCTION_OVER;

        node.messageReceived(mockIoSession, strMessage);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession, Mockito.atMost(0)).write(new Any());

    }

    @Test
    public void testGeneralAuction () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.setState(ClusterConnectionStates.GENERAL);

        LoggingCache mockCache = mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");

        node.setCache(mockCache);

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(ClusterHandler.AUCTION)).thenReturn(null);

        String strMessage = ClusterHandler.AUCTION;
        node.messageReceived(mockIoSession, strMessage);

        assert(node.getState() == ClusterConnectionStates.AUCTION);
        Mockito.verify(mockIoSession).write(ClusterHandler.AUCTION);
    }

    @Test
    public void testGeneralDeadNode () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.setState(ClusterConnectionStates.GENERAL);

        LoggingCache mockCache = mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123E4567-E89B-42D3-A456-556642440000");

        node.setCache(mockCache);

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(ClusterHandler.AUCTION)).thenReturn(null);

        String strMessage = ClusterHandler.DEAD_NODE;
        strMessage += " ";
        strMessage += uuid;

        node.messageReceived(mockIoSession, strMessage);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession).write(strMessage.toUpperCase());
    }

    @Test
    public void testGeneralGetMessageCacheMiss () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.setState(ClusterConnectionStates.GENERAL);

        LoggingCache mockCache = mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");

        node.setCache(mockCache);

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(ClusterHandler.MESSAGE_NOT_FOUND)).thenReturn(null);

        String strMessage = ClusterHandler.GET_MESSAGE;
        strMessage += " ";
        strMessage += uuid;

        node.messageReceived(mockIoSession, strMessage);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession).write(ClusterHandler.MESSAGE_NOT_FOUND);
    }

    @Test
    public void testGeneralGetMessageCacheHit () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.setState(ClusterConnectionStates.GENERAL);

        LoggingCache mockCache = mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        Message message = createTestMessage(uuid);

        when(mockCache.contains(uuid)).thenReturn(true);
        when(mockCache.isInMemory(uuid)).thenReturn(true);
        when(mockCache.get(uuid)).thenReturn(message);

        node.setCache(mockCache);

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(ClusterHandler.AUCTION)).thenReturn(null);

        String strMessage = ClusterHandler.GET_MESSAGE;
        strMessage += " ";
        strMessage += uuid;

        node.messageReceived(mockIoSession, strMessage);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession).write(message.longToString());
    }

    @Test
    public void testGeneralHeartBeat () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.setState(ClusterConnectionStates.GENERAL);

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(ClusterHandler.HEART_BEAT)).thenReturn(null);

        node.messageReceived(mockIoSession, ClusterHandler.HEART_BEAT);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession,atMost(0)).write(ClusterHandler.HEART_BEAT);
    }

    @Test
    public void testGeneralMessageDelivered () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.setState(ClusterConnectionStates.GENERAL);
        node.setCache(cache);

        MessageCache mockCache = mock(MessageCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");

        when(mockCache.contains(uuid)).thenReturn(true);
        when(mockCache.isOnline(uuid)).thenReturn(true);

        String strMessage = ClusterHandler.MESSAGE_DELIVERED;
        strMessage += " ";
        strMessage += uuid;

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(strMessage)).thenReturn(null);

        node.messageReceived(mockIoSession, strMessage);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession, Mockito.atMost(0)).write(strMessage);
    }

    @Test
    public void testGeneralNewMessage () throws LtsllcException, IOException {
        MessageLog mockMessageLog = mock(MessageLog.class);
        MessageLog.setInstance(mockMessageLog);

        StringBuffer strMessage = new StringBuffer();
        strMessage.append(ClusterHandler.NEW_MESSAGE);
        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        strMessage.append(" ");
        Message message = createTestMessage(uuid);
        strMessage.append(message.longToString());

        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.setState(ClusterConnectionStates.GENERAL);

        UUID partnerID = UUID.randomUUID();
        UUID nodeUuid = UUID.randomUUID();
        node.setUuid(nodeUuid);

        UUID otherUuid = UUID.randomUUID();
        node.setPartnerID(partnerID);

        IoSession mockIoSession = mock(IoSession.class);

        LoggingCache mockMessageCache = mock(LoggingCache.class);

        node.setCache(mockMessageCache);

        node.messageReceived(mockIoSession, strMessage.toString());

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockMessageLog, Mockito.atLeastOnce()).setOwner(message.getMessageID(), partnerID);
    }

    @Captor
    ArgumentCaptor<String> valueCaptor;


    @Test
    public void testGeneralBid () throws LtsllcException, IOException {
        String strMessage = ClusterHandler.BID;
        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        UUID partnerID = UUID.randomUUID();
        strMessage += " ";
        strMessage += uuid;
        strMessage += " 1234 ";

        Node node = new Node("192.168.0.12",2020);

        node.setState(ClusterConnectionStates.GENERAL);

        IoSession mockIoSession = mock(IoSession.class);

        LoggingCache mockMessageCache = mock(LoggingCache.class);

        node.setCache(mockMessageCache);

        node.messageReceived(mockIoSession, strMessage);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockIoSession, times(2)).write(valueCaptor.capture());

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(ClusterHandler.ERROR_START);
    }

    @Test
    public void sessionCreated () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics(UUID.randomUUID());

        Cluster.getInstance().setNodes(new ArrayList<>());

        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.sessionCreated(null);

        List<Node> list = Cluster.getInstance().getNodes();
        assert (list.size() == 0);
    }

    @Test
    public void sessionClosed () throws Exception {
        Configurator.setRootLevel(Level.DEBUG);

        Cluster.defineStatics(UUID.randomUUID());
        Cluster.getInstance().setIoConnector(new NioSocketConnector());
        Cluster.getInstance().setIoAcceptor(new NioSocketAcceptor());
        Cluster.getInstance().setConnectorClusterHandler(new ClusterHandler());
        IoSession mockIoSession = mock(IoSession.class);
        List<Node> nodeList = new ArrayList<>();
        Node node = new Node(UUID.randomUUID(), "localhost", 2020);
        nodeList.add(node);

        Cluster cluster = Cluster.getInstance();
        cluster.setNodes(nodeList);

        node.sessionClosed(mockIoSession);

        nodeList = Cluster.getInstance().getNodes();
        assert (nodeList.size() < 1);
    }

    @Test
    public void exceptionCaught () throws LtsllcException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messagesLog = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messagesLog,104857600);

        IoSession mockIoSession = mock(IoSession.class);

        node.exceptionCaught(mockIoSession, new Exception("Exception"));

        assert (node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(ClusterHandler.ERROR_START);
    }

    @Test
    public void handleBid () throws LtsllcException, IOException {
        ImprovedFile messagesLog = new ImprovedFile("messages.log");
        ImprovedFile owners = new ImprovedFile("owner.log");
        try {
            MessageLog.defineStatics(messagesLog, 1000000, owners);
            UUID messageUuid = UUID.randomUUID();
            UUID ourUuid = UUID.randomUUID();

            String strMessage = ClusterHandler.BID;
            strMessage += " ";
            strMessage += messageUuid;
            strMessage += " ";
            strMessage += "123";

            ImprovedRandom mockImprovedRandom = mock(ImprovedRandom.class);
            when(mockImprovedRandom.nextInt()).thenReturn(456);

            IoSession mockIoSession = mock(IoSession.class);
            when(mockIoSession.write(new Any())).thenReturn(null);

            LoggingCache mockMessageCache = mock(LoggingCache.class);
            when(mockMessageCache.contains(messageUuid)).thenReturn(true);
            Node node = new Node("192.168.0.12", 2020);

            ImprovedFile messages = new ImprovedFile("messages.log");
            LoggingCache cache = new LoggingCache(messages, 104857600);

            node.setCache(mockMessageCache);
            node.setOurRandom(mockImprovedRandom);
            node.setState(ClusterConnectionStates.AUCTION);
            node.setUuid(ourUuid);

            node.handleBid(strMessage, mockIoSession, UUID.randomUUID());

            assert (MessageLog.getInstance().getOwnerOf(messageUuid) == ourUuid);
            assert (node.getState() == ClusterConnectionStates.AUCTION);
            Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write("BID " + messageUuid + " 456");
        } finally {
            messagesLog.delete();
            owners.delete();
        }
    }

    @Test
    public void handleGetMessage () throws IOException, LtsllcException {
        UUID messageUuid = UUID.randomUUID();
        String strMessage = ClusterHandler.GET_MESSAGE;
        strMessage += " ";
        strMessage += messageUuid;

        Message testMessage = createTestMessage(messageUuid);

        IoSession mockIoSession = mock(IoSession.class);

        LoggingCache mockMessageCache = mock(LoggingCache.class);
        when(mockMessageCache.contains(messageUuid)).thenReturn(true);
        when(mockMessageCache.get(messageUuid)).thenReturn(testMessage);

        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);

        node.setCache(mockMessageCache);

        node.handleGetMessage(strMessage, mockIoSession);

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(testMessage.longToString());

        when(mockMessageCache.contains(messageUuid)).thenReturn(false);

        node.setCache(mockMessageCache);

        strMessage = ClusterHandler.GET_MESSAGE;
        strMessage += " ";
        strMessage += messageUuid;

        String replyMessage = ClusterHandler.MESSAGE_NOT_FOUND;

        node.handleGetMessage(strMessage, mockIoSession);

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(replyMessage);
    }

    @Test
    public void handleGetMessageAuction () throws IOException, LtsllcException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        UUID messageId = UUID.randomUUID();
        Message testMessage = createTestMessage(messageId);

        String strMessage = ClusterHandler.GET_MESSAGE;
        strMessage += " ";
        strMessage += messageId;

        LoggingCache mockMessageCache = mock(LoggingCache.class);
        when(mockMessageCache.contains(messageId)).thenReturn(true);
        when(mockMessageCache.get(messageId)).thenReturn(testMessage);

        node.setCache(mockMessageCache);

        IoSession mockIoSession = mock(IoSession.class);

        node.handleGetMessageAuction(strMessage, mockIoSession);

        String replyMessage = testMessage.longToString();
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(testMessage.longToString());

        mockMessageCache = mock(LoggingCache.class);
        when(mockMessageCache.contains(messageId)).thenReturn(false);
        node.setCache(mockMessageCache);

        replyMessage = ClusterHandler.MESSAGE_NOT_FOUND;

        node.handleGetMessageAuction(strMessage, mockIoSession);

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(replyMessage);
    }

    @Test
    public void handleMessage () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.pushState(ClusterConnectionStates.GENERAL);

        Message message = createTestMessage(UUID.randomUUID());
        String strMessage = message.longToString();

        IoSession mockIoSession = mock(IoSession.class);

        LoggingCache mockMessageCache = mock(LoggingCache.class);

        node.setCache(mockMessageCache);

        node.handleMessage(strMessage, mockIoSession);

        assert (node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockMessageCache, Mockito.atLeastOnce()).add(message);
    }

    @Test
    public void handleMessageDelivered () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);
        node.setUuid(UUID.randomUUID());

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);

        LoggingCache mockMessageCache = mock(LoggingCache.class);
        node.setCache(mockMessageCache);

        Message message = createTestMessage(UUID.randomUUID());

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ClusterHandler.MESSAGE_DELIVERED);
        stringBuffer.append(" ");
        stringBuffer.append(message.getMessageID());

        node.handleMessageDelivered(stringBuffer.toString());

        Mockito.verify(mockMessageCache, Mockito.atLeastOnce()).remove(message.getMessageID());
    }

    @Test
    public void handleNewMessage () throws LtsllcException, IOException {
        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile ownersLog = new ImprovedFile("owners.log");
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Node node = new Node("localhost", 2020);
        node.setPartnerID(UUID.randomUUID());
        StringBuffer stringBuffer = new StringBuffer();

        LoggingCache cache = new LoggingCache(messages,104857600);

        MessageLog.defineStatics(messages, 104857600, ownersLog);

        UUID partnerId = UUID.randomUUID();

        Message message = createTestMessage(UUID.randomUUID());

        LoggingCache mockMessageCache = mock(LoggingCache.class);
        node.setCache(mockMessageCache);

        stringBuffer.append(ClusterHandler.NEW_MESSAGE);
        stringBuffer.append(" ");
        stringBuffer.append(message.longToString());

        MessageLog mockMessageLog = mock(MessageLog.class);
        MessageLog.setInstance(mockMessageLog);

        node.handleNewMessage(stringBuffer.toString(),partnerId);

        Mockito.verify(mockMessageLog, Mockito.atLeastOnce()).setOwner(message.getMessageID(), partnerId);
    }

    @Test
    public void handleStart () throws LtsllcException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.setUuid(UUID.randomUUID());

        UUID partnerID = UUID.randomUUID();

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ClusterHandler.START);
        stringBuffer.append(" ");
        stringBuffer.append(partnerID);
        stringBuffer.append(" ");
        stringBuffer.append("192.168.0.12 2020");
        IoSession mockIoSession = mock(IoSession.class);
        IoSessionConfig mockIoSessionConfig = mock(IoSessionConfig.class);
        when(mockIoSession.getConfig()).thenReturn(mockIoSessionConfig);

        StringBuffer reply = new StringBuffer();
        reply.append(ClusterHandler.START);
        reply.append(" ");
        reply.append(node.getUuid());

        node.handleStart(stringBuffer.toString().toUpperCase(), mockIoSession);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
    }


    @Test
    public void popState () throws LtsllcException {
        Cluster.defineStatics(UUID.randomUUID());
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        LtsllcException ltsllcException = null;

        try {
            node.popState();
        } catch (LtsllcException e) {
            ltsllcException = e;
        }

        assert (ltsllcException != null);
    }

    @Test
    public void sendMessage () throws LtsllcException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);

        IoSession mockIoSession = mock(IoSession.class);

        Message message = createTestMessage(UUID.randomUUID());

        node.sendMessage(message, mockIoSession);

        String strMessage = message.longToString();

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(strMessage);
    }

    @Test
    public void constructor () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Node node = new Node("192.168.0.12", 2020);
        node.setUuid(UUID.randomUUID());
        node.setPartnerID(UUID.randomUUID());

        ImprovedProperties p = Miranda.getProperties();
        ImprovedFile messages = new ImprovedFile(p.getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        int loadLimit = p.getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT);
        LoggingCache cache = new LoggingCache(messages, loadLimit);
        node.setCache(cache);

        assert (node.getHost().equals("192.168.0.12"));
        assert (node.getPort() == 2020);
        assert (node.getCache().getFile().equals(messages));
        assert (node.getCache().getLoadLimit() == loadLimit);
    }

    @Test
    public void handleError () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        miranda.setMyHost("192.168.0.18");
        miranda.setMyPort(2020);

        StringBuffer message = new StringBuffer();
        message.append(ClusterHandler.ERROR);

        IoSession mockIoSession = mock(IoSession.class);

        Node node = new Node("192.168.0.12", 2020);
        UUID partnerID = UUID.randomUUID();
        node.setPartnerID(partnerID);

        ImprovedProperties p = Miranda.getProperties();
        ImprovedFile messages = new ImprovedFile(p.getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        int loadLimit = p.getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT);

        LoggingCache loggingCache = new LoggingCache(messages, loadLimit);

        node.handleError(mockIoSession);

        verify(mockIoSession, atLeastOnce()).write(ClusterHandler.ERROR_START);
        assert(node.getState() == ClusterConnectionStates.GENERAL);
    }

    @Test
    public void handleSendOwners () throws LtsllcException, IOException {
        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile owners = new ImprovedFile("owners.log");

        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();
            miranda.setMyHost("198.162.0.18");
            miranda.setMyPort(2020);

            MessageLog.defineStatics(messages, 1000000, owners);

            String strUuid1 = "00000000-0000-0000-0000-000000000001";
            String strUuid2 = "00000000-0000-0000-0000-000000000002";
            String strUuid3 = "00000000-0000-0000-0000-000000000001";
            UUID message1 = UUID.fromString(strUuid1);
            UUID message2 = UUID.fromString(strUuid2);
            UUID owner1 = UUID.fromString(strUuid3);
            MessageLog.getInstance().setOwner(message1, owner1);
            MessageLog.getInstance().setOwner(message2, owner1);

            IoSession mockIoSession = mock(IoSession.class);

            Node node = new Node("localhost", 2020);
            node.setConnected(true);
            node.setIoSession(mockIoSession);

            node.handleSendOwners(mockIoSession);

            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

            verify(mockIoSession, times(3)).write(valueCaptor.capture());

            List<String> list = valueCaptor.getAllValues();
            assert ("OWNER 00000000-0000-0000-0000-000000000001 00000000-0000-0000-0000-000000000001 ".equals(list.get(0)));
            assert ("OWNER 00000000-0000-0000-0000-000000000002 00000000-0000-0000-0000-000000000001 ".equals(list.get(1)));
            assert (ClusterHandler.OWNERS_END.equals(list.get(2)));
        } finally {
            if (messages.exists()) {
                messages.delete();
            }

            if (owners.exists()) {
                owners.delete();
            }
        }
    }

    @Test
    public void handleSendMessages () throws LtsllcException, IOException {
        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile owners = new ImprovedFile("owners.log");

        try {
            String string1 = "MESSAGE ID: 00000000-0000-0000-0000-000000000001 STATUS: http://localhost:8080 DELIVERY: " +
                    "http://localhost:8080 CONTENTS: 010203 ";
            String string2 = "MESSAGE ID: 00000000-0000-0000-0000-000000000002 STATUS: http://localhost:8080 DELIVERY: " +
                    "http://localhost:8080 CONTENTS: 010203 ";
            String string3 = ClusterHandler.MESSAGES_END;

            Miranda miranda = new Miranda();
            miranda.loadProperties();

            MessageLog.defineStatics(messages, 1000000, owners);

            UUID uuid1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID uuid2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

            byte[] contents1 = {1, 2, 3};
            byte[] contents2 = {1, 2, 3};

            Message message1 = new Message();
            message1.setMessageID(uuid1);
            message1.setStatusURL("http://localhost:8080");
            message1.setDeliveryURL("http://localhost:8080");
            message1.setContents(contents1);
            MessageLog.getInstance().add(message1, null);

            Message message2 = new Message();
            message2.setMessageID(uuid2);
            message2.setStatusURL("http://localhost:8080");
            message2.setDeliveryURL("http://localhost:8080");
            message2.setContents(contents2);
            MessageLog.getInstance().add(message2, null);

            Node node = new Node("localhost", 2020);

            IoSession mockIoSession = mock(IoSession.class);

            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

            node.handleSendMessages(mockIoSession);

            verify(mockIoSession, times(3)).write(valueCaptor.capture());

            List<String> list = valueCaptor.getAllValues();
            assert (list.get(0).equals(string1));
            assert (list.get(1).equals(string2));
            assert (list.get(2).equals(string3));
            assert (MessageLog.getInstance().get(uuid1).equals(message1));
            assert (MessageLog.getInstance().get(uuid2).equals(message2));
        } finally {
            if (messages.exists()) {
                messages.delete();
            }

            if (owners.exists()) {
                owners.delete();
            }
        }
    }

    @Test
    public void handleReceiveMessage () throws LtsllcException, IOException {
        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile owners = new ImprovedFile("owners.log");

        try {
            UUID uuid = UUID.randomUUID();
            String string1 = createTestMessage(uuid).longToString();
            Message message = Message.readLongFormat(string1);

            MessageLog.defineStatics(messages, 1000000, owners);

            Node node = new Node("localhost", 2020);

            node.handleReceiveMessage(string1);

            assert (MessageLog.getInstance().get(uuid).equals(message));
        } finally {
            if (messages.exists()) {
                messages.delete();
            }

            if (owners.exists()) {
                owners.delete();
            }
        }
    }

}
