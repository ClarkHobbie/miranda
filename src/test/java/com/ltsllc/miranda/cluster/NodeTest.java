package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.commons.util.Utils;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.message.MessageLog;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.logging.LoggingCache;
import com.ltsllc.miranda.properties.PropertiesHolder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
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
    /*
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
            reply.append(Node.BID);
            reply.append(" ");
            reply.append(message.getMessageID());
            reply.append(" 123");

            Mockito.when(mockReadFuture.getMessage()).thenReturn(reply.toString());

            Miranda.getInstance().setMyUuid(UUID.randomUUID());
            node.setUuid(UUID.randomUUID());

            ImprovedRandom mockImprovedRandom = Mockito.mock(ImprovedRandom.class);
            Mockito.when(mockImprovedRandom.nextInt()).thenReturn(456);

            Node.setOurRandom(mockImprovedRandom);

            node.auctionMessage(message);

            assert (MessageLog.getInstance().getOwnerOf(message.getMessageID()) == Miranda.getInstance().getMyUuid());
        } finally {
            messages.delete();
            owners.delete();
        }
    }
     */

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
        stringBuffer.append(Node.MESSAGE_DELIVERED);
        stringBuffer.append(" ");
        stringBuffer.append(newMessage.getMessageID());

        Node node = new Node(UUID.randomUUID(),"192.168.0.12",2020, null);
        node.setIoSession(mockIoSession);
        node.informOfMessageDelivery(newMessage);

        assert (MessageLog.getInstance().getOwnerOf(newMessage.getMessageID()) == null);
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(stringBuffer.toString());
    }

    @Test
    public void informOfMessageCreation () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        MessageLog.defineStatics();

        Message newMessage = createTestMessage(UUID.randomUUID());

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.NEW_MESSAGE);
        stringBuffer.append(" ");
        stringBuffer.append(newMessage.internalsToString());

        IoSession mockIoSession = Mockito.mock(IoSession.class);

        Node node = new Node(UUID.randomUUID(), "192.168.0.12", 2020, null);
        node.setIoSession(mockIoSession);

        node.informOfMessageCreation(newMessage);

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
    public void testStartMessage () throws LtsllcException, IOException, CloneNotSupportedException {
        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile owners = new ImprovedFile("owners.log");

        try {
            StringBuffer stringBuffer = new StringBuffer();
            Message message = createTestMessage(UUID.randomUUID());
            stringBuffer.append(message.longToString());

            MessageLog.defineStatics();

            Node node = new Node(null, "localhost", 2020, null);


            IoSession mockIoSession = mock(IoSession.class);

            node.messageReceived(mockIoSession, stringBuffer.toString());

            assert (node.getState() == ClusterConnectionStates.START);
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
    public void testStartStart () throws LtsllcException, IOException, CloneNotSupportedException {
        Miranda miranda = new Miranda();
        Node node = new Node(null, "localhost", 2020, null);
        node.setUuid(UUID.randomUUID());

        StringBuffer strMessage = new StringBuffer();
        strMessage.append(Node.START);
        strMessage.append(" ");
        strMessage.append(UUID.randomUUID());
        strMessage.append(" 192.168.0.12 2020 ");
        strMessage.append(Miranda.getInstance().getMyStart());

        IoSessionConfig mockIoSessionConfig = mock(IoSessionConfig.class);
        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(new Any())).thenReturn(null);
        when(mockIoSession.getConfig()).thenReturn(mockIoSessionConfig);

        Cluster mockCluster = mock(Cluster.class);
        Cluster.setInstance(mockCluster);
        when(mockCluster.containsNode(any())).thenReturn(true);

        node.messageReceived(mockIoSession, strMessage.toString());

        assert (node.getState() == ClusterConnectionStates.START);
    }

    @Test
    public void testGeneralDeadNode () throws LtsllcException, IOException, CloneNotSupportedException {
        Node node = new Node(null,"192.168.0.12",2020, null);

        node.setState(ClusterConnectionStates.GENERAL);

        LoggingCache mockCache = mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123E4567-E89B-42D3-A456-556642440000");

        node.setCache(mockCache);

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(Node.AUCTION)).thenReturn(null);

        String strMessage = Node.DEAD_NODE_START;
        strMessage += " ";
        strMessage += uuid;

        node.messageReceived(mockIoSession, strMessage);

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.DEAD_NODE);
        stringBuffer.append(" ");
        stringBuffer.append(uuid);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        verify(mockIoSession, atLeastOnce()).write(stringBuffer.toString().toUpperCase());
    }

    @Test
    public void testGeneralGetMessageCacheMiss () throws LtsllcException, IOException, CloneNotSupportedException {
        Node node = new Node(null,"192.168.0.12",2020, null);

        node.setState(ClusterConnectionStates.GENERAL);

        LoggingCache mockCache = mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");

        node.setCache(mockCache);

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(Node.MESSAGE_NOT_FOUND)).thenReturn(null);

        String strMessage = Node.GET_MESSAGE;
        strMessage += " ";
        strMessage += uuid;

        node.messageReceived(mockIoSession, strMessage);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession).write(Node.MESSAGE_NOT_FOUND);
    }

    @Test
    public void testGeneralGetMessageCacheHit () throws LtsllcException, IOException, CloneNotSupportedException {
        Node node = new Node(null,"192.168.0.12",2020, null);

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
        when(mockIoSession.write(Node.AUCTION)).thenReturn(null);

        String strMessage = Node.GET_MESSAGE;
        strMessage += " ";
        strMessage += uuid;

        node.messageReceived(mockIoSession, strMessage);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession).write(message.longToString());
    }

    @Test
    public void testGeneralHeartBeat () throws LtsllcException, IOException, CloneNotSupportedException {
        Node node = new Node(null,"192.168.0.12",2020, null);
        Cluster.defineStatics();

        node.setState(ClusterConnectionStates.GENERAL);

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(Node.HEART_BEAT)).thenReturn(null);

        node.messageReceived(mockIoSession, Node.HEART_BEAT);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession,atMost(0)).write(Node.HEART_BEAT);
    }

    @Test
    public void testGeneralMessageDelivered () throws LtsllcException, IOException, CloneNotSupportedException {
        Node node = new Node(null,"192.168.0.12",2020, null);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.setState(ClusterConnectionStates.GENERAL);
        node.setCache(cache);

        MessageCache mockCache = mock(MessageCache.class);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");

        when(mockCache.contains(uuid)).thenReturn(true);
        when(mockCache.isOnline(uuid)).thenReturn(true);

        String strMessage = Node.MESSAGE_DELIVERED;
        strMessage += " ";
        strMessage += uuid;

        IoSession mockIoSession = mock(IoSession.class);
        when(mockIoSession.write(strMessage)).thenReturn(null);

        node.messageReceived(mockIoSession, strMessage);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession, Mockito.atMost(0)).write(strMessage);
    }

    @Test
    public void testGeneralNewMessage () throws LtsllcException, IOException, CloneNotSupportedException {
        MessageLog mockMessageLog = mock(MessageLog.class);
        MessageLog.setInstance(mockMessageLog);

        StringBuffer strMessage = new StringBuffer();
        strMessage.append(Node.NEW_MESSAGE);
        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        strMessage.append(" ");
        Message message = createTestMessage(uuid);
        strMessage.append(message.longToString());

        Node node = new Node(null,"192.168.0.12",2020, null);

        node.setState(ClusterConnectionStates.GENERAL);

        UUID nodeUuid = UUID.randomUUID();
        node.setUuid(nodeUuid);

        IoSession mockIoSession = mock(IoSession.class);

        LoggingCache mockMessageCache = mock(LoggingCache.class);

        Cluster mockCluster = mock(Cluster.class);
        Cluster.setInstance(mockCluster);
        when(mockCluster.containsNode(any())).thenReturn(true);

        node.setCache(mockMessageCache);
        node.setIoSession(mockIoSession);

        node.messageReceived(mockIoSession, strMessage.toString());

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockMessageLog, atLeastOnce()).add(message, nodeUuid);
    }

    @Captor
    ArgumentCaptor<String> valueCaptor;


    @Test
    public void testGeneralBid () throws LtsllcException, IOException, CloneNotSupportedException {
        String strMessage = Node.BID;
        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        UUID partnerID = UUID.randomUUID();
        strMessage += " ";
        strMessage += uuid;
        strMessage += " 1234 ";

        Node node = new Node(null,"192.168.0.12",2020, null);

        node.setState(ClusterConnectionStates.GENERAL);

        IoSession mockIoSession = mock(IoSession.class);

        LoggingCache mockMessageCache = mock(LoggingCache.class);

        node.setCache(mockMessageCache);

        node.messageReceived(mockIoSession, strMessage);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockIoSession, times(2)).write(valueCaptor.capture());

        assert(node.getState() == ClusterConnectionStates.START);
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(Node.ERROR_START);
    }

    @Test
    public void sessionCreated () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        Cluster.getInstance().setNodes(new ArrayList<>());

        Node node = new Node(null,"192.168.0.12",2020, null);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.sessionCreated(null);

        List<Node> list = Cluster.getInstance().getNodes();
        assert (list.size() == 1);
    }

    @Test
    public void sessionClosed () throws Exception {
        Configurator.setRootLevel(Level.DEBUG);

        Cluster.defineStatics();
        Cluster.getInstance().setIoConnector(new NioSocketConnector());
        Cluster.getInstance().setIoAcceptor(new NioSocketAcceptor());
        Cluster.getInstance().setClusterHandler(new ClusterHandler());
        IoSession mockIoSession = mock(IoSession.class);
        List<Node> nodeList = new ArrayList<>();
        Node node = new Node(UUID.randomUUID(), "localhost", 2020, null);
        nodeList.add(node);

        Cluster cluster = Cluster.getInstance();
        cluster.setNodes(nodeList);

        node.sessionClosed(mockIoSession);

        nodeList = Cluster.getInstance().getNodes();
        assert (nodeList.size() < 1);
    }

    @Test
    public void exceptionCaught () throws LtsllcException {
        Node node = new Node(null, "192.168.0.12",2020, null);

        ImprovedFile messagesLog = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messagesLog,104857600);

        IoSession mockIoSession = mock(IoSession.class);

        node.exceptionCaught(mockIoSession, new Exception("Exception"));

        assert (node.getState() == ClusterConnectionStates.START);
        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(Node.ERROR_START);
    }

    @Test
    public void handleGetMessage () throws IOException, LtsllcException {
        UUID messageUuid = UUID.randomUUID();
        String strMessage = Node.GET_MESSAGE;
        strMessage += " ";
        strMessage += messageUuid;

        Message testMessage = createTestMessage(messageUuid);

        IoSession mockIoSession = mock(IoSession.class);

        LoggingCache mockMessageCache = mock(LoggingCache.class);
        when(mockMessageCache.contains(messageUuid)).thenReturn(true);
        when(mockMessageCache.get(messageUuid)).thenReturn(testMessage);

        Node node = new Node(null,"192.168.0.12",2020, null);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);

        node.setCache(mockMessageCache);

        node.handleGetMessage(strMessage, mockIoSession);

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(testMessage.longToString());

        when(mockMessageCache.contains(messageUuid)).thenReturn(false);

        node.setCache(mockMessageCache);

        strMessage = Node.GET_MESSAGE;
        strMessage += " ";
        strMessage += messageUuid;

        String replyMessage = Node.MESSAGE_NOT_FOUND;

        node.handleGetMessage(strMessage, mockIoSession);

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(replyMessage);
    }

    @Test
    public void handleGetMessageAuction () throws IOException, LtsllcException {
        Node node = new Node(null, "192.168.0.12",2020, null);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        UUID messageId = UUID.randomUUID();
        Message testMessage = createTestMessage(messageId);

        String strMessage = Node.GET_MESSAGE;
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

        replyMessage = Node.MESSAGE_NOT_FOUND;

        node.handleGetMessageAuction(strMessage, mockIoSession);

        Mockito.verify(mockIoSession, Mockito.atLeastOnce()).write(replyMessage);
    }

    @Test
    public void handleMessage () throws LtsllcException, IOException {
        Node node = new Node(null,"192.168.0.12",2020, null);

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
        Miranda miranda = new Miranda();
        Node node = new Node(null,"192.168.0.12",2020, null);
        node.setUuid(UUID.randomUUID());

        MessageLog.defineStatics();

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);

        MessageLog.defineStatics();
        MessageLog mockMessageLog = mock(MessageLog.class);
        MessageLog.setInstance(mockMessageLog);

        Message message = createTestMessage(UUID.randomUUID());

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.MESSAGE_DELIVERED);
        stringBuffer.append(" ");
        stringBuffer.append(message.getMessageID());

        node.handleMessageDelivered(stringBuffer.toString());

        Mockito.verify(mockMessageLog, Mockito.atLeastOnce()).remove(message.getMessageID());
    }

    @Test
    public void handleNewMessage () throws LtsllcException, IOException {
        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile ownersLog = new ImprovedFile("owners.log");
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Node node = new Node(null,"localhost", 2020, null);
        node.setUuid(UUID.randomUUID());
        StringBuffer stringBuffer = new StringBuffer();

        MessageLog.defineStatics();

        Message message = createTestMessage(UUID.randomUUID());

        LoggingCache mockMessageCache = mock(LoggingCache.class);
        node.setCache(mockMessageCache);

        stringBuffer.append(Node.NEW_MESSAGE);
        stringBuffer.append(" ");
        stringBuffer.append(message.longToString());

        MessageLog mockMessageLog = mock(MessageLog.class);
        MessageLog.setInstance(mockMessageLog);

        node.handleNewMessage(stringBuffer.toString(),node.getUuid());

        Mockito.verify(mockMessageLog, Mockito.atLeastOnce()).add(message, node.getUuid());
    }

    @Test
    public void handleStart () throws LtsllcException, CloneNotSupportedException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        Cluster.defineStatics();
        Node node = new Node(null,"192.168.0.12",2020, null);

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);
        node.setUuid(UUID.randomUUID());

        UUID partnerID = UUID.randomUUID();

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.START);
        stringBuffer.append(" ");
        stringBuffer.append(partnerID);
        stringBuffer.append(" ");
        stringBuffer.append("192.168.0.20 2020 ");
        stringBuffer.append(Miranda.getInstance().getMyStart());

        IoSession mockIoSession = mock(IoSession.class);
        IoSessionConfig mockIoSessionConfig = mock(IoSessionConfig.class);
        when(mockIoSession.getConfig()).thenReturn(mockIoSessionConfig);

        StringBuffer reply = new StringBuffer();
        reply.append(Node.START);
        reply.append(" ");
        reply.append(miranda.getMyUuid());
        reply.append(" ");
        reply.append(miranda.getMyHost());
        reply.append(" ");
        reply.append(miranda.getMyPort());
        reply.append(" ");
        reply.append(Miranda.getInstance().getMyStart());

        node.handleStart(stringBuffer.toString().toUpperCase(), mockIoSession);

        assert(node.getState() == ClusterConnectionStates.START);
    }


    @Test
    public void popState () throws LtsllcException {
        Cluster.defineStatics();
        Node node = new Node(null,"192.168.0.12",2020, null);

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
        Node node = new Node(null,"192.168.0.12",2020, null);

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

        Node node = new Node(null, "192.168.0.12", 2020, null);
        node.setUuid(UUID.randomUUID());

        PropertiesHolder p = Miranda.getProperties();
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
        message.append(Node.ERROR);

        IoSession mockIoSession = mock(IoSession.class);

        Node node = new Node(null, "192.168.0.12", 2020, null);
        UUID partnerID = UUID.randomUUID();
        node.setUuid(partnerID);

        PropertiesHolder p = Miranda.getProperties();
        ImprovedFile messages = new ImprovedFile(p.getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        int loadLimit = p.getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT);

        LoggingCache loggingCache = new LoggingCache(messages, loadLimit);

        node.handleError(mockIoSession);

        verify(mockIoSession, atLeastOnce()).write(Node.ERROR_START);
        assert(node.getState() == ClusterConnectionStates.START);
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

            MessageLog.defineStatics();

            String strUuid1 = "00000000-0000-0000-0000-000000000001";
            String strUuid2 = "00000000-0000-0000-0000-000000000002";
            String strUuid3 = "00000000-0000-0000-0000-000000000001";
            UUID message1 = UUID.fromString(strUuid1);
            UUID message2 = UUID.fromString(strUuid2);
            UUID owner1 = UUID.fromString(strUuid3);
            MessageLog.getInstance().setOwner(message1, owner1);
            MessageLog.getInstance().setOwner(message2, owner1);

            IoSession mockIoSession = mock(IoSession.class);

            Node node = new Node(null,"localhost", 2020, null);
            node.setIoSession(mockIoSession);

            node.handleSendOwners(mockIoSession);

            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

            verify(mockIoSession, times(3)).write(valueCaptor.capture());

            List<String> list = valueCaptor.getAllValues();
            assert ("OWNER 00000000-0000-0000-0000-000000000001 00000000-0000-0000-0000-000000000001 ".equals(list.get(0)));
            assert ("OWNER 00000000-0000-0000-0000-000000000002 00000000-0000-0000-0000-000000000001 ".equals(list.get(1)));
            assert (Node.OWNERS_END.equals(list.get(2)));
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
            String string3 = Node.MESSAGES_END;

            Miranda miranda = new Miranda();
            miranda.loadProperties();

            MessageLog.defineStatics();

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

            Node node = new Node(null,"localhost", 2020, null);

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

            MessageLog.defineStatics();

            Node node = new Node(null, "localhost", 2020, null);

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

    @Test
    public void startTimeout () throws LtsllcException, InterruptedException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        miranda.setMyUuid(UUID.randomUUID());
        miranda.setMyHost("192.168.0.20");
        miranda.setMyPort(2020);
        Miranda.getProperties().setProperty(Miranda.PROPERTY_START_TIMEOUT, Miranda.PROPERTY_DEFAULT_START_TIMEOUT);

        Node node = new Node(null,"192.168.0.12", 2020, null);
        IoSession mockIoSession = mock(IoSession.class);
        node.setIoSession(mockIoSession);

        node.sendStart(mockIoSession);

        synchronized (this) {
System.out.println(Miranda.getProperties().getLongProperty(Miranda.PROPERTY_START_TIMEOUT));
            wait(2 * Miranda.getProperties().getLongProperty(Miranda.PROPERTY_START_TIMEOUT));
        }

        assert (node.getState() == ClusterConnectionStates.START);
    }

    @Test
    public void startTimeoutNot () throws LtsllcException, InterruptedException, IOException, CloneNotSupportedException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        UUID uuid = UUID.randomUUID();
        Miranda.getProperties().setProperty(Miranda.PROPERTY_START_TIMEOUT, Miranda.PROPERTY_DEFAULT_START_TIMEOUT);
        miranda.setMyUuid(uuid);
        miranda.setMyHost("192.168.0.20");
        miranda.setMyPort(2020);

        Node node = new Node(null,"192.168.0.12", 2020, null);
        IoSession mockIoSession = mock(IoSession.class);
        IoSessionConfig mockIoSessionConfig = mock(IoSessionConfig.class);
        when(mockIoSession.getConfig()).thenReturn(mockIoSessionConfig);

        Cluster mockCluster = mock(Cluster.class);
        Cluster.setInstance(mockCluster);

        node.setIoSession(mockIoSession);

        node.sendStart(mockIoSession);

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.START_ACKNOWLEDGED);

        mockIoSession = mock(IoSession.class);
        node.setIoSession(mockIoSession);

        node.messageReceived(mockIoSession, stringBuffer.toString());

        StringBuffer stringBuffer2 = new StringBuffer();
        stringBuffer2.append(Node.START);
        stringBuffer2.append(" ");
        stringBuffer2.append(uuid);
        stringBuffer2.append(" 192.168.0.20 2020 ");
        stringBuffer2.append(Miranda.getInstance().getMyStart());

        synchronized (this) {
            wait(2 * Miranda.getProperties().getLongProperty(Miranda.PROPERTY_START_TIMEOUT));
        }

        verify (mockIoSession, atLeastOnce()).write(stringBuffer2.toString());
    }

    @Test
    public void heartBeatTimeout () throws LtsllcException, InterruptedException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        IoSession mockIoSession = mock(IoSession.class);
        UUID nodeUuid = UUID.randomUUID();

        Cluster.defineStatics();
        Cluster mockCluster = mock(Cluster.class);
        Cluster.setInstance(mockCluster);

        Node node = new Node(nodeUuid, "192.168.0.1", 2020, mockIoSession);
        node.sendHeartBeat();

        synchronized (this) {
            wait(2 * Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_TIMEOUT));
        }

        verify(mockCluster, atLeastOnce()).deadNode(any());
    }

    @Test
    public void heartBeatTimeoutNot () throws LtsllcException, IOException, CloneNotSupportedException, InterruptedException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        IoSession mockIoSession = mock(IoSession.class);

        Cluster mockCluster = mock(Cluster.class);
        Cluster.setInstance(mockCluster);
        when(mockCluster.containsNode(any())).thenReturn(true);

        UUID nodeUuid = UUID.randomUUID();

        Node node = new Node (nodeUuid, "192.168.0.12", 2020, mockIoSession);
        node.sendHeartBeat();

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.HEART_BEAT);
        stringBuffer.append(" ");
        stringBuffer.append(nodeUuid);

        node.messageReceived(mockIoSession, stringBuffer.toString());

        synchronized (this) {
            wait(2 * Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_TIMEOUT));
        }

        verify(mockCluster, times(0)).deadNode(any());
    }

    @Test
    public void sendDeadNode () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        IoSession mockIoSession = mock(IoSession.class);

        UUID nodeUuid = UUID.randomUUID();

        Node node = new Node(nodeUuid, "192.168.0.20", 2020, mockIoSession);
        node.sendDeadNode (UUID.randomUUID());

        verify(mockIoSession,  atLeastOnce()).write(any());
    }

    @Test
    public void deadNodeTimeout () throws LtsllcException, InterruptedException, IOException, CloneNotSupportedException {
        UUID nodeUuid = UUID.randomUUID();

        Miranda miranda = new Miranda();
        miranda.loadProperties();
        Miranda.getProperties().setProperty(Miranda.PROPERTY_DEAD_NODE_TIMEOUT, "500");
        miranda.setMyUuid(nodeUuid);
        miranda.setMyHost("192.168.0.20");
        miranda.setMyPort(2020);

        IoSession mockIoSession = mock(IoSession.class);

        Node node = new Node(nodeUuid, "192.168.0.20", 2020, mockIoSession);
        node.sendDeadNodeStart(UUID.randomUUID());


        synchronized (this) {
            wait(2 * Miranda.getProperties().getLongProperty(Miranda.PROPERTY_DEAD_NODE_TIMEOUT));
        }

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.START);
        stringBuffer.append(" ");
        stringBuffer.append(nodeUuid);
        stringBuffer.append(" 192.168.0.20 2020 ");
        stringBuffer.append(Miranda.getInstance().getMyStart());

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockIoSession, atLeastOnce()).write(valueCaptor.capture());
        List<String> list = valueCaptor.getAllValues();

        assert(list.get(1).equalsIgnoreCase(stringBuffer.toString()));
    }

    @Test
    public void deadNodeTimeoutNot () throws InterruptedException, LtsllcException, IOException, CloneNotSupportedException {
        UUID nodeUuid = UUID.randomUUID();

        Miranda miranda = new Miranda();
        miranda.loadProperties();
        Miranda.getProperties().setProperty(Miranda.PROPERTY_DEAD_NODE_TIMEOUT, "500");
        miranda.setMyUuid(nodeUuid);
        miranda.setMyHost("192.168.0.20");
        miranda.setMyPort(2020);

        IoSession mockIoSession = mock(IoSession.class);

        Cluster mockCluster = mock(Cluster.class);
        Cluster.setInstance(mockCluster);
        when (mockCluster.containsNode(any())).thenReturn(true);

        Node node = new Node(nodeUuid, "192.168.0.20", 2020, mockIoSession);
        node.setState(ClusterConnectionStates.GENERAL);

        node.sendDeadNodeStart(UUID.randomUUID());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.DEAD_NODE);
        stringBuffer.append(" ");
        stringBuffer.append(nodeUuid);

        node.messageReceived(mockIoSession, stringBuffer.toString());

        synchronized (this) {
            wait(2 * Miranda.getProperties().getLongProperty(Miranda.PROPERTY_DEAD_NODE_TIMEOUT));
        }

        assert (node.state != ClusterConnectionStates.START);
    }

}
