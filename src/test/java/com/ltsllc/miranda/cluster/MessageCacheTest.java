package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Collection;
import java.util.UUID;

public class MessageCacheTest {
    public static final Logger logger = LogManager.getLogger();

    @After
    public static void shutdown () {
        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();

            ImprovedFile improvedFile = new ImprovedFile(miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
            if (improvedFile.exists()) {
                improvedFile.delete();
            }
        } catch (LtsllcException e) {
            logger.error("threw exception",e);
        }
    }

    @Before
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
        Miranda miranda = new Miranda();
        try {
            miranda.loadProperties();
        } catch (LtsllcException e) {
            logger.error("LtsllcException while trying to load properties", e);
        }

        ImprovedFile file = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void addNoMigrate () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
        if (improvedFile.exists()) {
            improvedFile.delete();
        }

        MessageCache messageCache = new MessageCache();

        Message message = createTestMessage(UUID.randomUUID());

        messageCache.setLoadLimit(1048576);
        messageCache.add(message);

        assert (true == messageCache.contains(message.getMessageID()));
        assert (message.equals(messageCache.get(message.getMessageID())));
    }

    @Test
    public void addMigrate () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
        if (improvedFile.exists()) {
            improvedFile.delete();
        }

        MessageCache messageCache = new MessageCache();
        messageCache.setLoadLimit(4);

        Message message = createTestMessage(UUID.randomUUID());
        Message message2 = createTestMessage(UUID.randomUUID());

        messageCache.add(message);
        messageCache.add(message2);

        assert (messageCache.getLocationFor(message.getMessageID()) != -1);
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
    public void contains () throws LtsllcException {
        MessageCache messageCache = new MessageCache();
        messageCache.setLoadLimit(1048576);

        Message message1 = createTestMessage(UUID.randomUUID());
        Message message2 = createTestMessage(UUID.randomUUID());

        messageCache.add(message1);

        assert (true == messageCache.contains(message1.getMessageID()));
        assert (true == messageCache.getUuidToOnline().get(message1.getMessageID()));
        assert (false == messageCache.contains(message2.getMessageID()));
        assert (false == messageCache.isOnline(message2.getMessageID()));
    }

    @Test
    public void empty () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
        if (improvedFile.exists()) {
            improvedFile.delete();
        }

        MessageCache messageCache = new MessageCache();

        assert (true == messageCache.empty());
    }

    @Test
    public void getNotPresent () throws IOException, LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
        if (improvedFile.exists()) {
            improvedFile.delete();
        }

        MessageCache messageCache = new MessageCache();
        messageCache.setOffLineMessages(new ImprovedFile("c:/Users/Clark/offlineMessages.msg"));
        assert (null == messageCache.get(UUID.randomUUID()));
    }

    @Test
    public void getIsOnline () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
        if (improvedFile.exists()) {
            improvedFile.delete();
        }

        MessageCache messageCache = new MessageCache();

        Message message1 = createTestMessage(UUID.randomUUID());
        Message message2 = createTestMessage(UUID.randomUUID());
        Message message3 = createTestMessage(UUID.randomUUID());

        messageCache.setLoadLimit(4);

        messageCache.add(message1);
        messageCache.add(message2);
        messageCache.add(message3);

        assert (message3.equals(messageCache.get(message3.getMessageID())));
        assert (true == messageCache.getUuidToOnline().get(message3.getMessageID()));
    }
    
    @Test
    public void getIsOffline() throws LtsllcException, IOException {
        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();

            MessageCache messageCache = new MessageCache();
            messageCache.setLoadLimit(4);

            Message message1 = createTestMessage(UUID.randomUUID());
            Message message2 = createTestMessage(UUID.randomUUID());
            Message message3 = createTestMessage(UUID.randomUUID());

            messageCache.add(message1);
            messageCache.add(message2);
            messageCache.add(message3);

            assert (true == messageCache.get(message1.getMessageID()).equals(message1));
            assert (false == messageCache.isOnline(message1.getMessageID()));
        } finally {

           ImprovedFile tempFile = new ImprovedFile(Miranda.PROPERTY_DEFAULT_OFFLINE_MESSAGES);
           if (tempFile.exists()) {
               logger.debug("removing " + tempFile);
               tempFile.delete();
           }

        }
    }

    @Test
    public void intializeOfflineDoesNotExist () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
        if (improvedFile.exists()) {
            improvedFile.delete();
        }

        MessageCache messageCache = new MessageCache();
        messageCache.initialize();

        assert(!messageCache.getOffLineMessages().exists());
    }

    @Test
    public void initializeFileExists () throws LtsllcException, IOException {
        ImprovedFile improvedFile = null;
        Throwable t = null;
        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();

            improvedFile = new ImprovedFile(miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
            FileWriter fileWriter = new FileWriter(improvedFile);
            fileWriter.write("hi there");
            fileWriter.close();

            try {
                MessageCache messageCache = new MessageCache();
                messageCache.initialize();
            } catch (Throwable throwable) {
                t = throwable;
            }
        } finally {
            if (improvedFile != null && improvedFile.exists()) {
                improvedFile.delete();
            }
        }

        assert (t != null);
    }

    @Test
    public void migrateInCoreMessageToOfflineFileWrongLocation () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));

        if (improvedFile.exists()) {
            improvedFile.delete();
        }

        miranda.getProperties().setProperty(Miranda.PROPERTY_OFFLINE_MESSAGES, "c:/offlineMessages.msg");
        Throwable t = null;
        try {
            MessageCache messageCache = new MessageCache();
            Message message = createTestMessage(UUID.randomUUID());
            messageCache.add(message);

            if (improvedFile.exists()) {
                improvedFile.delete();
            }

            messageCache.migrateInCoreMessageToOffline(message);
        } catch (Throwable throwable) {
            t = throwable;
        }

        assert (t != null);
    }

    @Test
    public void migrateInCoreMessageToOfflineEverythingOK () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));

        MessageCache messageCache = new MessageCache();

        Message message = createTestMessage(UUID.randomUUID());
        messageCache.add(message);

        messageCache.migrateInCoreMessageToOffline(message);

        assert (improvedFile.exists());
        assert (messageCache.get(message.getMessageID()).equals(message));
    }

    @Test
    public void migrateLeastReferencedMessageMultipleMessagesInCore () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Message message1 = createTestMessage(UUID.randomUUID());
        Message message2 = createTestMessage(UUID.randomUUID());
        Message message3 = createTestMessage(UUID.randomUUID());

        MessageCache messageCache = new MessageCache();
        messageCache.add(message1);
        messageCache.add(message2);
        messageCache.add(message3);

        messageCache.get(message1.getMessageID());
        messageCache.get(message1.getMessageID());
        messageCache.get(message1.getMessageID());

        messageCache.get(message3.getMessageID());
        messageCache.get(message3.getMessageID());

        messageCache.migrateLeastReferencedMessage();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));

        assert (4 == messageCache.getUuidToNumberOfTimesReferenced().get(message1.getMessageID()));
        assert (3 == messageCache.getUuidToNumberOfTimesReferenced().get(message3.getMessageID()));
        assert (1 == messageCache.getUuidToNumberOfTimesReferenced().get(message2.getMessageID()));

        assert (false == messageCache.isOnline(message2.getMessageID()));
        assert (true == improvedFile.exists());
    }

    @Test
    public void migrateMessageToOnline() throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
        if (improvedFile.exists()) {
            improvedFile.delete();
        }

        MessageCache messageCache = new MessageCache();
        messageCache.setLoadLimit(4);

        Message message1 = createTestMessage(UUID.randomUUID());
        messageCache.add(message1);
        Message message2 = createTestMessage(UUID.randomUUID());
        messageCache.add(message2);
        Message message3 = createTestMessage(UUID.randomUUID());
        messageCache.add(message3);

        assert (false == messageCache.isOnline(message1.getMessageID()));

        messageCache.migrateMessageToOnline(message1.getMessageID());

        assert (true == messageCache.isOnline(message1.getMessageID()));
    }

    @Test
    public void putMessage () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
        if (improvedFile.exists()) {
            improvedFile.delete();
        }

        MessageCache messageCache = new MessageCache();
        messageCache.setLoadLimit(4);

        Message message1 = createTestMessage(UUID.randomUUID());
        messageCache.add(message1);

        Message message2 = createTestMessage(UUID.randomUUID());
        messageCache.add(message2);

        Message message3 = createTestMessage(UUID.randomUUID());
        messageCache.add(message3);

        Message message4 = createTestMessage(UUID.randomUUID());
        messageCache.putMessage(message4);

        assert (true == messageCache.isOnline(message4.getMessageID()));
        assert (false == messageCache.isOnline(message1.getMessageID()));
    }

    @Test
    public void readOfflineMessage () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
        if (improvedFile.exists()) {
            improvedFile.delete();
        }

        MessageCache messageCache = new MessageCache();
        messageCache.setLoadLimit(4);

        Message message1 = createTestMessage(UUID.randomUUID());
        messageCache.add(message1);

        Message message2 = createTestMessage(UUID.randomUUID());
        messageCache.add(message2);

        Message message3 = createTestMessage(UUID.randomUUID());
        messageCache.add(message3);

        Message message4 = messageCache.readOfflineMessage(message1.getMessageID());

        assert (message4.equals(message1));
        assert (false == messageCache.isOnline(message1.getMessageID()));
    }

    @Test
    public void remove () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
        if (improvedFile.exists()) {
            improvedFile.delete();
        }

        MessageCache messageCache = new MessageCache();

        Message message = createTestMessage(UUID.randomUUID());
        messageCache.add(message);

        assert (messageCache.getUuidToMessage().get(message.getMessageID()) != null);
        assert (messageCache.getUuidToLocation().get(message.getMessageID()) == null);
        assert (messageCache.getUuidToNumberOfTimesReferenced().get(message.getMessageID()) != null);
        assert (messageCache.getUuidToOnline().get(message.getMessageID()) != null);

        messageCache.remove(message.getMessageID());

        assert (messageCache.getUuidToMessage().get(message.getMessageID()) == null);
        assert (messageCache.getUuidToLocation().get(message.getMessageID()) == null);
        assert (messageCache.getUuidToNumberOfTimesReferenced().get(message.getMessageID()) == null);
        assert (messageCache.getUuidToOnline().get(message.getMessageID()) == null);
    }

    @Test
    public void undefine () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile improvedFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
        if (improvedFile.exists()) {
            improvedFile.delete();
        }

        MessageCache messageCache = new MessageCache();
        Message message = createTestMessage(UUID.randomUUID());
        messageCache.add(message);

        assert (messageCache.getUuidToMessage().get(message.getMessageID()) != null);
        assert (messageCache.getUuidToLocation().get(message.getMessageID()) == null);
        assert (messageCache.getUuidToNumberOfTimesReferenced().get(message.getMessageID()) != null);
        assert (messageCache.getUuidToOnline().get(message.getMessageID()) != null);

        messageCache.undefineMessage(message);

        assert (messageCache.getUuidToMessage().get(message.getMessageID()) == null);
        assert (messageCache.getUuidToLocation().get(message.getMessageID()) == null);
        assert (messageCache.getUuidToNumberOfTimesReferenced().get(message.getMessageID()) == null);
        assert (messageCache.getUuidToOnline().get(message.getMessageID()) == null);
    }

}
