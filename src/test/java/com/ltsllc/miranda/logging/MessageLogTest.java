package com.ltsllc.miranda.logging;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
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
    void deliveryAttempted() {
    }

    @Test
    void recover() {
    }

    @Test
    void performRecover() {
    }

    @Test
    void restoreOwnersFile() {
    }

    @Test
    void restoreMessages() {
    }

    @Test
    void removeOwnerOf() {
    }

    @Test
    void outOfBounds() {
    }

    @Test
    void remove() {
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