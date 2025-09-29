package com.ltsllc.miranda.logging;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.TestSuperclass;
import com.ltsllc.miranda.message.Message;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageLogTest extends TestSuperclass {

    @Test
    void shouldRecoverLogfileExists() throws IOException, LtsllcException {
        ImprovedFile logfile = null;
        ImprovedFile ownerFile = null;
        ImprovedFile eventsFile = null;
        try {
            MessageLog.defineStatics();
            logfile = MessageLog.getInstance().getCache().getFile();
            ownerFile = MessageLog.getInstance().getOwnersFile();
            eventsFile = MessageLog.getInstance().getEventsFile();

            Message message = createTestMessage(UUID.randomUUID());
            MessageLog.getInstance().add(message, UUID.randomUUID());

            assert (MessageLog.getInstance().contains(message.getMessageID()));

            boolean result = MessageLog.shouldRecover(logfile,ownerFile, eventsFile);

            assert (result);
        } finally {
            logfile.delete();
            ownerFile.delete();
            eventsFile.delete();
        }
    }

    @Test
    void performRecover() throws LtsllcException, IOException {
        ImprovedFile logfile = null;
        try {
            MessageLog.defineStatics();
            Miranda miranda = new Miranda();
            logfile = MessageLog.getInstance().getLogfile();
            Message message = createTestMessage(UUID.randomUUID());

            MessageLog.getInstance().add(message, UUID.randomUUID());

            assert (MessageLog.getInstance().contains(message.getMessageID()));

            MessageLog.getInstance().performRecover(logfile, Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT),
                    MessageLog.getInstance().getOwnersFile(), MessageLog.getInstance().getEventsFile());

            assert (MessageLog.getInstance().contains(message.getMessageID()));
        } finally {
            logfile.delete();
        }
    }

    @Test
    void remove() throws LtsllcException, IOException {
        MessageLog.defineStatics();
        Message message = createTestMessage(UUID.randomUUID());
        MessageLog.getInstance().add(message, UUID.randomUUID());

        assert (MessageLog.getInstance().contains(message.getMessageID()));

        MessageLog.getInstance().remove(message.getMessageID());

        assert (!MessageLog.getInstance().contains(message.getMessageID()));
    }

    @Test
    void getOwnerOf() {
    }

    @Test
    void compact() {
    }

    @Test
    void getAllMessagesOwnedBy() {
    }

    @Test
    void propertyChanged() {
    }

    @Test
    void contains() {
    }
}