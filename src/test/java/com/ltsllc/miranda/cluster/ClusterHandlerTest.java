package com.ltsllc.miranda.cluster;


import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.commons.util.Utils;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.MessageLog;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.logcache.LoggingCache;
import com.ltsllc.miranda.logcache.TestSuperclass;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Any;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

class ClusterHandlerTest extends TestSuperclass {
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
        StringBuffer stringBuffer = new StringBuffer();
        Message message = createTestMessage(UUID.randomUUID());
        stringBuffer.append(message.longToString());
        Node node = new Node("localhost", 2020);
        ImprovedFile cacheFile = new ImprovedFile("messages");
        LoggingCache cache = new LoggingCache(cacheFile, 104857600); // 100 Meg
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.START);

        IoSession mockIoSession = Mockito.mock(IoSession.class);

        clusterHandler.messageReceived(mockIoSession, stringBuffer.toString());

        assert (clusterHandler.getState() == ClusterConnectionStates.START);
        Mockito.verify(mockIoSession).write("ERROR");
    }

    @Test
    public void testStartStart () throws LtsllcException, IOException {
        Node node = new Node("localhost", 2020);
        node.setUuid(UUID.randomUUID());

        ImprovedFile messageLog = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messageLog, 104857600); // 100Meg
        ClusterHandler clusterHandler = new ClusterHandler(node,cache);
        clusterHandler.setState(ClusterConnectionStates.START);

        StringBuffer strMessage = new StringBuffer();
        strMessage.append(ClusterHandler.START);
        strMessage.append(" ");
        strMessage.append(UUID.randomUUID());

        IoSessionConfig mockIoSessionConfig = Mockito.mock(IoSessionConfig.class);
        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write(new Any())).thenReturn(null);
        when(mockIoSession.getConfig()).thenReturn(mockIoSessionConfig);

        clusterHandler.messageReceived(mockIoSession, strMessage.toString());

        assert (clusterHandler.getState() == ClusterConnectionStates.GENERAL);
    }

    @Test
    public void testStartNewNodeConfirmed () throws LtsllcException, IOException {
        String strMessage = ClusterHandler.NEW_NODE;
        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        strMessage += " ";
        strMessage += uuid;

        ImprovedFile messageFile = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messageFile, 104857600); // 100Meg

        Node node = new Node("192.168.0.12",2020);
        node.setUuid(UUID.randomUUID());
        node.setPartnerID(UUID.randomUUID());

        ClusterHandler clusterHandler = new ClusterHandler(node,cache);
        clusterHandler.setState(ClusterConnectionStates.START);
        clusterHandler.setUuid(uuid);

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write("NEW NODE CONFIRMED 123e4567-e89b-42d3-a456-556642440000")).thenReturn(null);

        clusterHandler.messageReceived(mockIoSession, strMessage);

        assert (clusterHandler.getState() == ClusterConnectionStates.NEW_NODE);
        Mockito.verify(mockIoSession).write("NEW NODE CONFIRMED 123e4567-e89b-42d3-a456-556642440000");
    }

    @Test
    public void testStartBid () throws LtsllcException, IOException {
        String strMessage = "BID 123e4567-e89b-42d3-a456-556642440000 123456";
        Node node = new Node("192.168.0.12", 2020);

        ImprovedFile messageLog = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messageLog, 104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.MESSAGE);
        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write("ERROR")).thenReturn(null);

        clusterHandler.messageReceived(mockIoSession, strMessage);

        assert (clusterHandler.getState() == ClusterConnectionStates.START);
        Mockito.verify(mockIoSession).write("ERROR");
    }

    @Test
    public void testAuctionHeartBeat () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.AUCTION);

        String strMessage = ClusterHandler.HEART_BEAT;

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write(new Any())).thenReturn(null);

        clusterHandler.messageReceived(mockIoSession, strMessage);

        assert (clusterHandler.getState() == ClusterConnectionStates.START);
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(ClusterHandler.ERROR);
    }

    @Test
    public void testAuctionBidWonNotInCache () throws LtsllcException, IOException {
        String strMessage = ClusterHandler.BID;
        strMessage += " ";
        UUID uuid = UUID.randomUUID();
        strMessage += uuid;
        strMessage += " ";
        strMessage += "1234";
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);

        clusterHandler.setState(ClusterConnectionStates.AUCTION);

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write(new Any())).thenReturn(null);

        ImprovedRandom mockRandom = Mockito.mock(ImprovedRandom.class);
        when(mockRandom.nextInt()).thenReturn(4567);

        clusterHandler.setOurRandom(mockRandom);

        clusterHandler.messageReceived(mockIoSession, strMessage);

        strMessage = ClusterHandler.GET_MESSAGE;
        strMessage += " ";
        strMessage += uuid;

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(strMessage);
    }

    @Test
    public void testAuctionBidLost () throws IOException, LtsllcException {
        UUID ourUuid = UUID.randomUUID();
        UUID theirUuid = UUID.randomUUID();
        Node node = new Node(ourUuid, "localhost",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.AUCTION);
        clusterHandler.setUuid(ourUuid);
        clusterHandler.setPartnerID(theirUuid);

        MessageCache mockCache = Mockito.mock(MessageCache.class);

        UUID messageUuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write(new Any())).thenReturn(null);

        String strMessage = ClusterHandler.BID;
        strMessage += " ";
        strMessage += messageUuid;
        strMessage += " 1234";

        ImprovedRandom mockRandom = Mockito.mock(ImprovedRandom.class);
        when(mockRandom.nextInt()).thenReturn(123);
        clusterHandler.setOurRandom(mockRandom);

        clusterHandler.messageReceived(mockIoSession, strMessage);

        assert (clusterHandler.getState() == ClusterConnectionStates.AUCTION);
        Mockito.verify(mockIoSession, Mockito.atMost(0)).write(new Any());
    }

    @Test
    public void testAuctionGetMessageInCache () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.AUCTION);

        LoggingCache mockCache = Mockito.mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        when(mockCache.contains(uuid)).thenReturn(true);

        clusterHandler.setCache(mockCache);

        IoSession mockIoSession = Mockito.mock(IoSession.class);
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
        clusterHandler.messageReceived(mockIoSession, strMessage);

        assert(clusterHandler.getState() == ClusterConnectionStates.AUCTION);

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
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.AUCTION);

        LoggingCache mockCache = Mockito.mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        when(mockCache.contains(uuid)).thenReturn(false);

        clusterHandler.setCache(mockCache);

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write(new Any())).thenReturn(null);

        String strMessage = ClusterHandler.GET_MESSAGE;
        strMessage +=  " ";
        strMessage += uuid;

        clusterHandler.messageReceived(mockIoSession, strMessage);

        assert(clusterHandler.getState() == ClusterConnectionStates.AUCTION);
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(ClusterHandler.MESSAGE_NOT_FOUND);
    }

    @Test
    public void testAuctionAuctionOver () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.AUCTION);

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write(new Any())).thenReturn(null);

        String strMessage = ClusterHandler.AUCTION_OVER;

        clusterHandler.messageReceived(mockIoSession, strMessage);

        assert(clusterHandler.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession, Mockito.atMost(0)).write(new Any());

    }

    @Test
    public void testGeneralAuction () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.GENERAL);

        LoggingCache mockCache = Mockito.mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");

        clusterHandler.setCache(mockCache);

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write(ClusterHandler.AUCTION)).thenReturn(null);

        String strMessage = ClusterHandler.AUCTION;
        clusterHandler.messageReceived(mockIoSession, strMessage);

        assert(clusterHandler.getState() == ClusterConnectionStates.AUCTION);
        Mockito.verify(mockIoSession).write(ClusterHandler.AUCTION);
    }

    @Test
    public void testGeneralDeadNode () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.GENERAL);

        LoggingCache mockCache = Mockito.mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123E4567-E89B-42D3-A456-556642440000");

        clusterHandler.setCache(mockCache);

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write(ClusterHandler.AUCTION)).thenReturn(null);

        String strMessage = ClusterHandler.DEAD_NODE;
        strMessage += " ";
        strMessage += uuid;

        clusterHandler.messageReceived(mockIoSession, strMessage);

        assert(clusterHandler.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession).write(strMessage.toUpperCase());
    }

    @Test
    public void testGeneralGetMessageCacheMiss () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.GENERAL);

        LoggingCache mockCache = Mockito.mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");

        clusterHandler.setCache(mockCache);

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write(ClusterHandler.MESSAGE_NOT_FOUND)).thenReturn(null);

        String strMessage = ClusterHandler.GET_MESSAGE;
        strMessage += " ";
        strMessage += uuid;

        clusterHandler.messageReceived(mockIoSession, strMessage);

        assert(clusterHandler.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession).write(ClusterHandler.MESSAGE_NOT_FOUND);
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
    public void testGeneralGetMessageCacheHit () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.GENERAL);

        LoggingCache mockCache = Mockito.mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        Message message = createTestMessage(uuid);

        when(mockCache.contains(uuid)).thenReturn(true);
        when(mockCache.isInMemory(uuid)).thenReturn(true);
        when(mockCache.get(uuid)).thenReturn(message);

        clusterHandler.setCache(mockCache);

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write(ClusterHandler.AUCTION)).thenReturn(null);

        String strMessage = ClusterHandler.GET_MESSAGE;
        strMessage += " ";
        strMessage += uuid;

        clusterHandler.messageReceived(mockIoSession, strMessage);

        assert(clusterHandler.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession).write(message.longToString());
    }

    @Test
    public void testGeneralHeartBeat () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.GENERAL);

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write(ClusterHandler.HEART_BEAT)).thenReturn(null);

        clusterHandler.messageReceived(mockIoSession, ClusterHandler.HEART_BEAT);

        assert(clusterHandler.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession).write(ClusterHandler.HEART_BEAT);
    }

    @Test
    public void testGeneralMessageDelivered () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.GENERAL);

        MessageCache mockCache = Mockito.mock(MessageCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");

        when(mockCache.contains(uuid)).thenReturn(true);
        when(mockCache.isOnline(uuid)).thenReturn(true);

        String strMessage = ClusterHandler.MESSAGE_DELIVERED;
        strMessage += " ";
        strMessage += uuid;

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write(strMessage)).thenReturn(null);

        clusterHandler.messageReceived(mockIoSession, strMessage);

        assert(clusterHandler.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession, Mockito.atMost(0)).write(strMessage);
    }

    @Test
    public void testGeneralNewMessage () throws LtsllcException, IOException {
        MessageLog mockMessageLog = Mockito.mock(MessageLog.class);
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
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.GENERAL);

        UUID partnerID = UUID.randomUUID();
        UUID nodeUuid = UUID.randomUUID();
        clusterHandler.setUuid(nodeUuid);
        node.setUuid(nodeUuid);

        UUID otherUuid = UUID.randomUUID();
        clusterHandler.setPartnerID(otherUuid);

        IoSession mockIoSession = Mockito.mock(IoSession.class);

        LoggingCache mockMessageCache = Mockito.mock(LoggingCache.class);

        clusterHandler.setCache(mockMessageCache);

        clusterHandler.messageReceived(mockIoSession, strMessage.toString());

        assert(clusterHandler.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockMessageLog, Mockito.atLeastOnce()).add(message, partnerID);
    }

    @Test
    public void testGeneralBid () throws LtsllcException, IOException {
        String strMessage = ClusterHandler.BID;
        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        strMessage += " ";
        strMessage += uuid;
        strMessage += " 1234 ";

        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setState(ClusterConnectionStates.GENERAL);

        IoSession mockIoSession = Mockito.mock(IoSession.class);

        LoggingCache mockMessageCache = Mockito.mock(LoggingCache.class);

        clusterHandler.setCache(mockMessageCache);

        clusterHandler.messageReceived(mockIoSession, strMessage);

        assert(clusterHandler.getState() == ClusterConnectionStates.START);
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(ClusterHandler.ERROR);
    }

    @Test
    public void sessionCreated () throws LtsllcException {
        // Configurator.setRootLevel(Level.DEBUG);


        Cluster.getInstance().setNodes(new ArrayList<>());

        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.sessionCreated(null);

        List<Node> list = Cluster.getInstance().getNodes();
        assert (list.size() == 0);
    }

    @Test
    public void sessionClosed () throws Exception {
        Configurator.setRootLevel(Level.DEBUG);

        Cluster.defineStatics();
        IoSession mockIoSession = Mockito.mock(IoSession.class);
        List<Node> nodeList = new ArrayList<>();
        Node node = new Node(UUID.randomUUID(), "localhost", 2020);
        nodeList.add(node);

        Cluster cluster = Cluster.getInstance();
        cluster.setNodes(nodeList);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.sessionClosed(mockIoSession);

        nodeList = Cluster.getInstance().getNodes();
        assert (nodeList.size() < 1);
    }

    @Test
    public void exceptionCaught () throws LtsllcException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messagesLog = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messagesLog,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);

        IoSession mockIoSession = Mockito.mock(IoSession.class);

        clusterHandler.exceptionCaught(mockIoSession, new Exception("Exception"));

        assert (clusterHandler.getState() == ClusterConnectionStates.START);
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(ClusterHandler.ERROR);
    }

    @Test
    public void handleBid () throws LtsllcException, IOException {
        ImprovedFile messagesLog = new ImprovedFile("messages.log");
        ImprovedFile owners = new ImprovedFile("owner.log");
        MessageLog.defineStatics(messagesLog, 1000000, owners);
        UUID messageUuid = UUID.randomUUID();
        UUID ourUuid = UUID.randomUUID();

        String strMessage = ClusterHandler.BID;
        strMessage += " ";
        strMessage += messageUuid;
        strMessage += " ";
        strMessage += "123";

        ImprovedRandom mockImprovedRandom = Mockito.mock(ImprovedRandom.class);
        when(mockImprovedRandom.nextInt()).thenReturn(456);

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        when(mockIoSession.write(new Any())).thenReturn(null);

        LoggingCache mockMessageCache = Mockito.mock(LoggingCache.class);
        when(mockMessageCache.contains(messageUuid)).thenReturn(true);
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);

        clusterHandler.setCache(mockMessageCache);
        clusterHandler.setOurRandom(mockImprovedRandom);
        clusterHandler.setState(ClusterConnectionStates.AUCTION);
        clusterHandler.setUuid(ourUuid);

        clusterHandler.handleBid(strMessage, mockIoSession, UUID.randomUUID());

        assert (MessageLog.getInstance().getOwnerOf(messageUuid) == ourUuid);
        assert (clusterHandler.getState() == ClusterConnectionStates.AUCTION);
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write("BID " + messageUuid + " 456");
    }

    @Test
    public void handleGetMessage () throws IOException, LtsllcException {
        UUID messageUuid = UUID.randomUUID();
        String strMessage = ClusterHandler.GET_MESSAGE;
        strMessage += " ";
        strMessage += messageUuid;

        Message testMessage = createTestMessage(messageUuid);

        IoSession mockIoSession = Mockito.mock(IoSession.class);

        LoggingCache mockMessageCache = Mockito.mock(LoggingCache.class);
        when(mockMessageCache.contains(messageUuid)).thenReturn(true);
        when(mockMessageCache.get(messageUuid)).thenReturn(testMessage);

        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);

        clusterHandler.setCache(mockMessageCache);

        clusterHandler.handleGetMessage(strMessage, mockIoSession);

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(testMessage.longToString());

        when(mockMessageCache.contains(messageUuid)).thenReturn(false);

        clusterHandler.setCache(mockMessageCache);

        strMessage = ClusterHandler.GET_MESSAGE;
        strMessage += " ";
        strMessage += messageUuid;

        String replyMessage = ClusterHandler.MESSAGE_NOT_FOUND;

        clusterHandler.handleGetMessage(strMessage, mockIoSession);

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(replyMessage);
    }

    @Test
    public void handleGetMessageAuction () throws IOException, LtsllcException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        UUID messageId = UUID.randomUUID();
        Message testMessage = createTestMessage(messageId);

        String strMessage = ClusterHandler.GET_MESSAGE;
        strMessage += " ";
        strMessage += messageId;

        LoggingCache mockMessageCache = Mockito.mock(LoggingCache.class);
        when(mockMessageCache.contains(messageId)).thenReturn(true);
        when(mockMessageCache.get(messageId)).thenReturn(testMessage);

        clusterHandler.setCache(mockMessageCache);

        IoSession mockIoSession = Mockito.mock(IoSession.class);

        clusterHandler.handleGetMessageAuction(strMessage, mockIoSession);

        String replyMessage = testMessage.longToString();
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(testMessage.longToString());

        mockMessageCache = Mockito.mock(LoggingCache.class);
        when(mockMessageCache.contains(messageId)).thenReturn(false);
        clusterHandler.setCache(mockMessageCache);

        replyMessage = ClusterHandler.MESSAGE_NOT_FOUND;

        clusterHandler.handleGetMessageAuction(strMessage, mockIoSession);

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(replyMessage);
    }

    @Test
    public void handleMessage () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.pushState(ClusterConnectionStates.GENERAL);

        Message message = createTestMessage(UUID.randomUUID());
        String strMessage = message.longToString();

        IoSession mockIoSession = Mockito.mock(IoSession.class);

        LoggingCache mockMessageCache = Mockito.mock(LoggingCache.class);

        clusterHandler.setCache(mockMessageCache);

        clusterHandler.handleMessage(strMessage, mockIoSession);

        assert (clusterHandler.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockMessageCache, Mockito.atLeastOnce()).add(message);
    }

    @Test
    public void handleMessageDelivered () throws LtsllcException, IOException {
        Node node = new Node("192.168.0.12",2020);
        node.setUuid(UUID.randomUUID());

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);

        LoggingCache mockMessageCache = Mockito.mock(LoggingCache.class);
        clusterHandler.setCache(mockMessageCache);

        Message message = createTestMessage(UUID.randomUUID());

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ClusterHandler.MESSAGE_DELIVERED);
        stringBuffer.append(" ");
        stringBuffer.append(message.getMessageID());

        clusterHandler.handleMessageDelivered(stringBuffer.toString());

        Mockito.verify(mockMessageCache, Mockito.atLeastOnce()).remove(message.getMessageID());
    }

    @Test
    public void handleNewMessage () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Node node = new Node("localhost", 2020);
        node.setPartnerID(UUID.randomUUID());
        StringBuffer stringBuffer = new StringBuffer();

        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile ownersLog = new ImprovedFile("owners.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);

        MessageLog.defineStatics(messages, 104857600, ownersLog);

        UUID partnerId = UUID.randomUUID();

        Message message = createTestMessage(UUID.randomUUID());

        LoggingCache mockMessageCache = Mockito.mock(LoggingCache.class);
        clusterHandler.setCache(mockMessageCache);

        stringBuffer.append(ClusterHandler.NEW_MESSAGE);
        stringBuffer.append(" ");
        stringBuffer.append(message.longToString());

        MessageLog mockMessageLog = Mockito.mock(MessageLog.class);
        MessageLog.setInstance(mockMessageLog);

        clusterHandler.handleNewMessage(stringBuffer.toString(),partnerId);

        Mockito.verify(mockMessageLog, Mockito.atLeastOnce()).add(message, partnerId);
    }

    @Test
    public void handleNewNode () throws IOException, LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile messageLog = new ImprovedFile("messages.log");
        ImprovedFile ownersLog = new ImprovedFile("owners.log");
        MessageLog.defineStatics(messageLog, 1048577600, ownersLog);
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("NEW NODE ");
        UUID uuid = UUID.randomUUID();
        stringBuffer.append(uuid);

        IoSession mockIoSession = Mockito.mock(IoSession.class);

        clusterHandler.setUuid(UUID.randomUUID());

        clusterHandler.handleNewNode(stringBuffer.toString(), mockIoSession);

        StringBuffer reply = new StringBuffer();
        reply.append(ClusterHandler.NEW_NODE_CONFIRMED);
        reply.append(" ");
        reply.append(clusterHandler.getUuid());

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(reply.toString());
    }

    @Test
    public void handleStart () throws LtsllcException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        clusterHandler.setUuid(UUID.randomUUID());

        UUID partnerID = UUID.randomUUID();

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ClusterHandler.START);
        stringBuffer.append(" ");
        stringBuffer.append(partnerID);

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        IoSessionConfig mockIoSessionConfig = Mockito.mock(IoSessionConfig.class);
        when(mockIoSession.getConfig()).thenReturn(mockIoSessionConfig);

        StringBuffer reply = new StringBuffer();
        reply.append(ClusterHandler.START);
        reply.append(" ");
        reply.append(clusterHandler.getUuid());

        clusterHandler.handleStart(stringBuffer.toString().toUpperCase(), mockIoSession);

        assert(clusterHandler.getState() == ClusterConnectionStates.GENERAL);
    }

    @Test
    public void inputClosed () throws LtsllcException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);

        IoSession mockIoSession = Mockito.mock(IoSession.class);

        node.setIoSession(mockIoSession);
        Cluster cluster = Cluster.getInstance();

        clusterHandler.inputClosed(mockIoSession);

        assert(!Cluster.getInstance().getNodes().contains(mockIoSession));
    }

    @Test
    public void popState () throws LtsllcException {
        Node node = new Node("192.168.0.12",2020);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);
        LtsllcException ltsllcException = null;

        try {
            clusterHandler.popState();
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
        ClusterHandler clusterHandler = new ClusterHandler(node, cache);

        IoSession mockIoSession = Mockito.mock(IoSession.class);

        Message message = createTestMessage(UUID.randomUUID());

        clusterHandler.sendMessage(message, mockIoSession);

        String strMessage = message.longToString();

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(strMessage);
    }

}