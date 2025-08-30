package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.logging.MessageLog;
import com.ltsllc.miranda.message.Message;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageCacheTest {

    @Test
    void addNullMessage() throws LtsllcException {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        MessageCache messageCache = new MessageCache(improvedFile, 1048576);

        messageCache.add(null);

        assert (true);
    }

    @Test
    void addCurrentLoadRequiresMigration () throws LtsllcException {
        MessageCache messageCache = null;

        try {
            ImprovedFile improvedFile = new ImprovedFile("messages.log");

            messageCache = new MessageCache(improvedFile, 1024);

            Message message1 = new Message();
            message1.setContents(new byte[512]);
            message1.setMessageID(UUID.randomUUID());
            message1.setDeliveryURL("http://localhost");
            message1.setStatusURL("http://localhost");
            message1.setStatusURL("http://localhost");

            messageCache.add(message1);

            Message message = new Message();
            message.setContents(new byte[548]);
            message.setMessageID(UUID.randomUUID());
            message.setDeliveryURL("http://localhost");
            message.setStatusURL("http://localhost");
            message.setStatusURL("http://localhost");

            messageCache.add(message);

            assert (messageCache.getUuidToOnline().get(message1.getMessageID()) == false);
        } finally {
            if (messageCache != null) {
                messageCache.clear();
            }
        }
    }

    @Test
    void addTooLarge () throws LtsllcException {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        MessageCache messageCache = new MessageCache(improvedFile, 128);

        Message message = new Message();
        message.setContents(new byte[512]);
        message.setMessageID(UUID.randomUUID());
        message.setDeliveryURL("http://localhost");
        message.setStatusURL("http://localhost");
        message.setStatusURL("http://localhost");

        Exception exception = null;
        try {
            messageCache.add(message);
        } catch (LtsllcException e) {
            exception = e;
        }

        assert (exception != null);
    }

    @Test
    void add () throws LtsllcException {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        MessageCache messageCache = new MessageCache(improvedFile, 1024);
        int load1 = messageCache.getLoadLimit();

        Message message = new Message();
        message.setContents(new byte[512]);
        message.setMessageID(UUID.randomUUID());
        message.setDeliveryURL("http://localhost");
        message.setStatusURL("http://localhost");
        message.setStatusURL("http://localhost");

        messageCache.add(message);

        int load2 = messageCache.getCurrentLoad();

        assert (load1 != load2);
    }

    @Test
    void getUUIDIsNull () throws IOException, LtsllcException {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        MessageCache messageCache = new MessageCache(improvedFile, 1024);

        Message message = messageCache.get(null);

        assert (message == null);
    }

    @Test
    void getMessageIsNotOnline () throws LtsllcException, IOException {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        MessageCache messageCache = new MessageCache(improvedFile, 1024);

        Message message1 = new Message();
        message1.setContents(new byte[512]);
        message1.setMessageID(UUID.randomUUID());
        message1.setDeliveryURL("http://localhost");
        message1.setStatusURL("http://localhost");
        message1.setStatusURL("http://localhost");

        messageCache.add(message1);

        Message message2 = new Message();
        message2.setContents(new byte[512]);
        message2.setMessageID(UUID.randomUUID());
        message2.setDeliveryURL("http://localhost");
        message2.setStatusURL("http://localhost");
        message2.setStatusURL("http://localhost");

        messageCache.add(message2);

        Message message = messageCache.get(message1.getMessageID());

        assert (message != null);
    }

    @Test
    void get () throws LtsllcException, IOException {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        MessageCache messageCache = new MessageCache(improvedFile, 1024);
        int load1 = messageCache.getLoadLimit();

        Message message = new Message();
        message.setContents(new byte[512]);
        message.setMessageID(UUID.randomUUID());
        message.setDeliveryURL("http://localhost");
        message.setStatusURL("http://localhost");
        message.setStatusURL("http://localhost");

        messageCache.add(message);

        Message message2 = messageCache.get(message.getMessageID());

        assert (message.getMessageID().equals(message2.getMessageID()));

    }


    @Test
    void removeNullUuid () throws LtsllcException, IOException {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        MessageCache messageCache = new MessageCache(improvedFile, 1024);
        messageCache.remove(null);

        assert (true);
    }

    @Test
    void migrateMessageToOnline() throws LtsllcException {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        MessageCache messageCache = new MessageCache(improvedFile, 1024);

        Message message1 = new Message();
        message1.setContents(new byte[512]);
        message1.setMessageID(UUID.randomUUID());
        message1.setDeliveryURL("http://localhost");
        message1.setStatusURL("http://localhost");
        message1.setStatusURL("http://localhost");

        messageCache.add(message1);

        Message message = new Message();
        message.setContents(new byte[548]);
        message.setMessageID(UUID.randomUUID());
        message.setDeliveryURL("http://localhost");
        message.setStatusURL("http://localhost");
        message.setStatusURL("http://localhost");

        messageCache.add(message);

        messageCache.migrateMessageToOnline(message1.getMessageID());

        assert (messageCache.isOnline(message1.getMessageID()));
    }

    @Test
    void migrateInCoreMessageToOffline() throws LtsllcException {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        MessageCache messageCache = new MessageCache(improvedFile, 1024);

        Message message1 = new Message();
        message1.setContents(new byte[512]);
        message1.setMessageID(UUID.randomUUID());
        message1.setDeliveryURL("http://localhost");
        message1.setStatusURL("http://localhost");
        message1.setStatusURL("http://localhost");

        messageCache.add(message1);

        Message message = new Message();
        message.setContents(new byte[548]);
        message.setMessageID(UUID.randomUUID());
        message.setDeliveryURL("http://localhost");
        message.setStatusURL("http://localhost");
        message.setStatusURL("http://localhost");

        messageCache.add(message);

        assert (messageCache.isOnline(message1.getMessageID()) == false);

        messageCache.migrateMessageToOnline(message1.getMessageID());

        assert (messageCache.isOnline(message1.getMessageID()));
    }

    @Test
    void readOfflineMessage() throws LtsllcException {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        MessageCache messageCache = new MessageCache(improvedFile, 1024);

        byte[] buff = new byte[512];
        buff[0] = 7;

        Message message1 = new Message();
        message1.setContents(buff);
        message1.setMessageID(UUID.randomUUID());
        message1.setDeliveryURL("http://localhost");
        message1.setStatusURL("http://localhost");
        message1.setStatusURL("http://localhost");

        messageCache.add(message1);

        Message message = new Message();
        message.setContents(new byte[548]);
        message.setMessageID(UUID.randomUUID());
        message.setDeliveryURL("http://localhost");
        message.setStatusURL("http://localhost");
        message.setStatusURL("http://localhost");

        messageCache.add(message);

        message = messageCache.readOfflineMessage(message1.getMessageID());

        assert (message.getContents()[0] == 7);
    }
}