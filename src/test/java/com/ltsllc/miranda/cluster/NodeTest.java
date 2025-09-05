package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.TestSuperclass;
import com.ltsllc.miranda.logging.LoggingCache;
import com.ltsllc.miranda.logging.MessageEventLogger;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.logging.MessageLog;
import com.ltsllc.miranda.message.MessageType;
import com.ltsllc.miranda.netty.HeartBeatHandler;
import com.ltsllc.miranda.properties.PropertiesHolder;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
public class NodeTest extends TestSuperclass
{
    @BeforeEach
    public void setupEach () {
        clearMessageLog();
    }

    @Test
    public void informOfMessageDelivery () {
        Message newMessage = createTestMessage(UUID.randomUUID());
        MessageLog.defineStatics();


        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.MESSAGE_DELIVERED);
        stringBuffer.append(" ");
        stringBuffer.append(newMessage.getMessageID());

        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(),"192.168.0.12",2020, channel);
        node.informOfMessageDelivery(newMessage);

        assert (MessageLog.getInstance().getOwnerOf(newMessage.getMessageID()) == null);
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
        stringBuffer.append(newMessage.longFormatToString());

        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(), "192.168.0.12", 2020, channel);


        node.informOfMessageCreation(newMessage);
        Object temp = channel.readOutbound();
        temp = temp;
    }

    public static final Logger logger = LogManager.getLogger(NodeTest.class);

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

            EmbeddedChannel channel = new EmbeddedChannel();
            Node node = new Node(UUID.randomUUID(), "localhost", 2020, channel);


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
    @ExtendWith(MockitoExtension.class)
    public void testStartStart () throws LtsllcException, IOException, CloneNotSupportedException {
        Miranda miranda = new Miranda();
        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(), "localhost", 2020, channel);
        node.setUuid(UUID.randomUUID());

        StringBuffer strMessage = new StringBuffer();
        strMessage.append(Node.START);
        strMessage.append(" ");
        strMessage.append(UUID.randomUUID());
        strMessage.append(" 192.168.0.12 2020 ");
        strMessage.append(Miranda.getInstance().getMyStart());

        Cluster mockCluster = mock(Cluster.class);
        Cluster.setInstance(mockCluster);
        when(mockCluster.containsNode(any())).thenReturn(true);

        node.messageReceived(strMessage.toString());

        assert (node.getState() == ClusterConnectionStates.START);
    }

    @Test
    public void testGeneralDeadNode () throws LtsllcException, IOException, CloneNotSupportedException {
        SecureRandom random = new SecureRandom();
        Cluster.defineStatics();
        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(),"71.237.68.250",2020, channel);

        node.setState(ClusterConnectionStates.GENERAL);

        LoggingCache mockCache = mock(LoggingCache.class);

        UUID uuid = UUID.fromString("123E4567-E89B-42D3-A456-556642440000");

        String strMessage = Node.DEAD_NODE;
        strMessage += " ";
        strMessage += uuid;
        strMessage += " ";
        strMessage += UUID.randomUUID();

        List<Node> list = new ArrayList<>();
        list.add(node);
        Election election = new Election(list,uuid);
        Cluster.getInstance().setElection(election);

        node.messageReceived(strMessage);

        assert(node.getState() == ClusterConnectionStates.AWAITING_ASSIGNMENTS);
    }



    @Test
    public void testGeneralMessageDelivered () throws LtsllcException, IOException, CloneNotSupportedException {
        Cluster.defineStatics();
        MessageLog.defineStatics();
        EmbeddedChannel channel = new EmbeddedChannel();
        HeartBeatHandler heartBeatHandler = new HeartBeatHandler(channel);
        channel.pipeline().addLast("HEARTBEAT",heartBeatHandler);
        Node node = new Node(UUID.randomUUID(),"71.237.68.250",2020, channel);

        ImprovedFile messages = new ImprovedFile("messages.log");
        node.setState(ClusterConnectionStates.GENERAL);

        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        UUID uuid2 = UUID.randomUUID();

        Message newMessage = new Message();
        newMessage.setMessageID(uuid);
        newMessage.setContents("hi".getBytes());
        newMessage.setStatusURL("http://localhost:3030/api/receiveStatus");
        newMessage.setStatusURL("http://localhost:3039/api/deliver");

        MessageLog.getInstance().add(newMessage, uuid2);

        String strMessage = Node.MESSAGE_DELIVERED;
        strMessage += " ";
        strMessage += uuid;
        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline().addLast(new HeartBeatHandler(embeddedChannel));

        node.messageReceived(strMessage);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
    }

    @Test
    public void testGeneralNewMessage () throws LtsllcException, IOException, CloneNotSupportedException {
        MessageLog mockMessageLog = mock(MessageLog.class);
        MessageLog.setInstance(mockMessageLog);

        StringBuilder strMessage = new StringBuilder();
        strMessage.append(Node.NEW_MESSAGE);
        UUID uuid = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        strMessage.append(" ");
        Message message = createTestMessage(uuid);
        message.convertToUpperCase();
        strMessage.append(message.longToString());

        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(),"71.237.68.250",2020, channel);

        node.setState(ClusterConnectionStates.GENERAL);

        UUID nodeUuid = UUID.randomUUID();
        node.setUuid(nodeUuid);

        Cluster mockCluster = mock(Cluster.class);
        Cluster.setInstance(mockCluster);
        when(mockCluster.containsNode(any())).thenReturn(true);

        String temp = strMessage.toString().toUpperCase();
        node.messageReceived(temp);

        assert(node.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockMessageLog, atLeastOnce()).add(message, nodeUuid);
    }

    @Captor
    ArgumentCaptor<String> valueCaptor;



    @Test
    public void sessionCreated () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Cluster.defineStatics();

        Cluster.getInstance().setNodes(new ArrayList<>());

        EmbeddedChannel channel = new EmbeddedChannel();

        Node node = new Node(UUID.randomUUID(),"71.237.68.250",2021, channel);

        ImprovedFile messages = new ImprovedFile("messages.log");
        node.sessionCreated();

        List<Node> list = Cluster.getInstance().getNodes();
        assert (list.size() == 1);
    }

    @Test
    public void sessionClosed () throws Exception {
        Configurator.setRootLevel(Level.DEBUG);

        Cluster.defineStatics();
        // Cluster.getInstance().setIoConnector(new NioSocketConnector());
        // Cluster.getInstance().setIoAcceptor(new NioSocketAcceptor());
        // ClusterHandler.defineStatics();
        // IoSession mockIoSession = mock(IoSession.class);
        List<Node> nodeList = new ArrayList<>();
        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(), "localhost", 2020, channel);
        nodeList.add(node);

        Cluster cluster = Cluster.getInstance();
        cluster.setNodes(nodeList);

        node.sessionClosed();

        nodeList = Cluster.getInstance().getNodes();
        assert (nodeList.size() < 1);
    }

    /*
    @Test
    public void exceptionCaught () throws LtsllcException {
        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(), "71.237.68.250",2020, channel);

        ImprovedFile messagesLog = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messagesLog,104857600);

        node.exceptionCaught(new Exception("Exception"));

        assert (node.getState() == ClusterConnectionStates.START);
    }


     */
    /*
    @Test
    public void handleGetMessage () throws IOException, LtsllcException {
        MessageLog.defineStatics();
        UUID messageUuid = UUID.randomUUID();
        String strMessage = Node.GET_MESSAGE;
        strMessage += " ";
        strMessage += messageUuid;

        Message testMessage = createTestMessage(messageUuid);

        LoggingCache mockMessageCache = mock(LoggingCache.class);
        when(mockMessageCache.contains(messageUuid)).thenReturn(true);
        when(mockMessageCache.get(messageUuid)).thenReturn(testMessage);

        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(),"71.237.68.250",2020, channel);
        channel.pipeline().addLast("HEARTBEAT", new HeartBeatHandler(channel));

        ImprovedFile messages = new ImprovedFile("messages.log");
        LoggingCache cache = new LoggingCache(messages,104857600);

        node.handleGetMessage(strMessage);

        when(mockMessageCache.contains(messageUuid)).thenReturn(false);

        strMessage = Node.GET_MESSAGE;
        strMessage += " ";
        strMessage += messageUuid;

        String replyMessage = Node.MESSAGE_NOT_FOUND;

        node.handleGetMessage(strMessage);
    }
    */

    @Test
    public void handleMessage () throws LtsllcException, IOException {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        improvedFile.clear();

        MessageLog.defineStatics();
        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(),"71.237.68.250",2020, channel);

        ImprovedFile messages = new ImprovedFile("messages.log");
        node.pushState(ClusterConnectionStates.GENERAL);

        Message message = createTestMessage(UUID.randomUUID());
        String strMessage = message.longToString();

        node.handleMessage(strMessage);

        assert (node.getState() == ClusterConnectionStates.GENERAL);
    }

    @Test
    public void handleMessageDelivered () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast("HEARTBEAT", new HeartBeatHandler(channel));
        Node node = new Node(UUID.randomUUID(),"71.237.68.250",2021, channel);
        node.setUuid(UUID.randomUUID());

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
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(),"localhost", 2020, channel);
        node.setUuid(UUID.randomUUID());
        StringBuffer stringBuffer = new StringBuffer();

        MessageLog.defineStatics();

        Message message = createTestMessage(UUID.randomUUID());

        stringBuffer.append(Node.NEW_MESSAGE);
        stringBuffer.append(" ");
        stringBuffer.append(message.longToString());

        MessageLog mockMessageLog = mock(MessageLog.class);
        MessageLog.setInstance(mockMessageLog);

        node.handleNewMessage(stringBuffer.toString(),node.getUuid());

        Mockito.verify(mockMessageLog, Mockito.atLeastOnce()).add(message, node.getUuid());
    }

    @Test
    public void handleStart () throws LtsllcException, CloneNotSupportedException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        Cluster.defineStatics();
        EmbeddedChannel channel = new EmbeddedChannel();
        HeartBeatHandler heartBeatHandler = new HeartBeatHandler(channel);
        channel.pipeline().addLast("HEARTBEAT", heartBeatHandler);
        Node node = new Node(UUID.randomUUID(),"71.237.68.250",2021, channel);

        ImprovedFile messages = new ImprovedFile("messages.log");
        node.setUuid(UUID.randomUUID());

        UUID partnerID = UUID.randomUUID();

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.START);
        stringBuffer.append(" ");
        stringBuffer.append(partnerID);
        stringBuffer.append(" ");
        stringBuffer.append("192.168.0.20 2020 ");
        stringBuffer.append(Miranda.getInstance().getMyStart());


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

        node.handleStateStart(MessageType.START_START, stringBuffer.toString().toUpperCase());

        assert(node.getState() == ClusterConnectionStates.START);
    }


    @Test
    public void popState () throws LtsllcException {
        Cluster.defineStatics();
        Node node = new Node(UUID.randomUUID(),"71.237.68.250",2020, null);

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
        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(),"71.237.68.250",2020, channel);

        Message message = createTestMessage(UUID.randomUUID());

        node.sendMessage(message);
    }

    @Test
    public void constructor () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        MessageLog.defineStatics();

        Node node = new Node(UUID.randomUUID(), "71.237.68.250", 2021, null);
        node.setUuid(UUID.randomUUID());

        PropertiesHolder p = Miranda.getProperties();
        ImprovedFile messages = new ImprovedFile(p.getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        int loadLimit = p.getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT);

        assert (node.getHost().equals("71.237.68.250"));
        assert (node.getPort() == 2021);
        assert (MessageLog.getInstance().getCache().getFile().equals(messages));
        assert (MessageLog.getInstance().getCache().getLoadLimit() == loadLimit);
    }

    @Test
    public void handleError () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        miranda.setMyHost("192.168.0.18");
        miranda.setMyPort(2021);

        StringBuffer message = new StringBuffer();
        message.append(Node.ERROR);

        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(UUID.randomUUID(), "71.237.68.250", 2020, channel);
        UUID partnerID = UUID.randomUUID();
        node.setUuid(partnerID);

        PropertiesHolder p = Miranda.getProperties();

        node.handleError();

        assert(node.getState() == ClusterConnectionStates.START);
    }

    @Test
    public void handleSendOwners () throws LtsllcException, IOException {
        ImprovedFile messages = new ImprovedFile("messages.log");
        ImprovedFile owners = new ImprovedFile("owners.log");

        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();
            miranda.setMyHost("71.237.68.250");
            miranda.setMyPort(2021);

            MessageLog.defineStatics();

            String strUuid1 = "00000000-0000-0000-0000-000000000001";
            String strUuid2 = "00000000-0000-0000-0000-000000000002";
            String strUuid3 = "00000000-0000-0000-0000-000000000003";
            UUID message1 = UUID.fromString(strUuid1);
            UUID message2 = UUID.fromString(strUuid2);
            UUID owner1 = UUID.fromString(strUuid3);
            MessageLog.getInstance().setOwner(message1, owner1);
            MessageLog.getInstance().setOwner(message2, owner1);

            EmbeddedChannel channel = new EmbeddedChannel();
            Node node = new Node(UUID.randomUUID(),"localhost", 2020, channel);

            node.handleSendOwners();
            String s = channel.readOutbound();
            assert(s.equalsIgnoreCase("owners"));
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

            EmbeddedChannel channel = new EmbeddedChannel();
            Node node = new Node(UUID.randomUUID(),"localhost", 2020, channel);

            node.sendAllMessages();

            String s = channel.readOutbound();
            assert (s.equalsIgnoreCase("messages"));
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

            Node node = new Node(UUID.randomUUID(), "localhost", 2020, null);

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

          Node node = new Node(UUID.randomUUID(),"71.237.68.250", 2020, null);

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

        Node node = new Node(UUID.randomUUID(),"71.237.68.250", 2020, null);

        Cluster mockCluster = mock(Cluster.class);
        Cluster.setInstance(mockCluster);

        node.sendStart(false,false);

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.START_ACKNOWLEDGED);
        stringBuffer.append(" ");
        stringBuffer.append(UUID.randomUUID());
        stringBuffer.append(" 192.164.0.12 2020");

        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline().addLast("HEARTBEAT", new HeartBeatHandler(embeddedChannel));

        node.setChannel(embeddedChannel);

        node.messageReceived(stringBuffer.toString());

        StringBuffer stringBuffer2 = new StringBuffer();
        stringBuffer2.append(Node.START);
        stringBuffer2.append(" ");
        stringBuffer2.append(uuid);
        stringBuffer2.append(" 192.168.0.20 2020 ");
        stringBuffer2.append(Miranda.getInstance().getMyStart());

        synchronized (this) {
            wait(2 * Miranda.getProperties().getLongProperty(Miranda.PROPERTY_START_TIMEOUT));
        }
    }


    @Test
    public void sendDeadNode () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        UUID nodeUuid = UUID.randomUUID();

        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(nodeUuid, "71.237.68.250", 2020, channel);
        node.sendDeadNode (UUID.randomUUID());

        channel = channel;
    }

    /*
    @Test
    public void deadNodeTimeout () throws LtsllcException, InterruptedException, IOException, CloneNotSupportedException {
        UUID nodeUuid = UUID.randomUUID();

        Miranda miranda = new Miranda();
        miranda.loadProperties();
        Miranda.getProperties().setProperty(Miranda.PROPERTY_DEAD_NODE_TIMEOUT, "500");
        miranda.setMyUuid(nodeUuid);
        miranda.setMyHost("192.168.0.20");
        miranda.setMyPort(2020);

        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(nodeUuid, "192.168.0.20", 2020, channel);
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

        String s = channel.readOutbound();
        assert(s.startsWith("DEAD NODE START"));
        assert(node.getState() == ClusterConnectionStates.START);
    }


    @Test
    public void deadNodeTimeoutNot () throws InterruptedException, LtsllcException, IOException, CloneNotSupportedException {
        Cluster.defineStatics();
        UUID nodeUuid = UUID.randomUUID();

        SecureRandom random = new SecureRandom();

        Miranda miranda = new Miranda();
        miranda.loadProperties();
        Miranda.getProperties().setProperty(Miranda.PROPERTY_DEAD_NODE_TIMEOUT, "500");
        miranda.setMyUuid(nodeUuid);
        miranda.setMyHost("192.168.0.20");
        miranda.setMyPort(2020);

        EmbeddedChannel channel = new EmbeddedChannel();
        Node node = new Node(nodeUuid, "192.168.0.20", 2020, channel);
        node.setState(ClusterConnectionStates.GENERAL);

        UUID uuid = UUID.randomUUID();
        node.sendDeadNodeStart(uuid);
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Node.DEAD_NODE);
        stringBuffer.append(" ");
        stringBuffer.append(nodeUuid);
        stringBuffer.append(" ");
        stringBuffer.append(random.nextInt());

        ArrayList<Node> list = new ArrayList<Node>();
        list.add(node);
        Election election = new Election(list,uuid);
        Cluster.getInstance().setElection(election);

        node.messageReceived(stringBuffer.toString());

        synchronized (this) {
            wait(2 * Miranda.getProperties().getLongProperty(Miranda.PROPERTY_DEAD_NODE_TIMEOUT));
        }

        assert (node.state != ClusterConnectionStates.START);
    }

     */

}
