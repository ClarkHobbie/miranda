package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.MessageLog;
import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.session.IoSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.UUID;

public class NodeTest {
    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

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


}
