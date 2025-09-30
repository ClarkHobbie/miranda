package com.ltsllc.miranda.logging;

import com.ltsllc.commons.HexConverter;
import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.TestSuperclass;
import com.ltsllc.miranda.message.Message;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static com.ltsllc.miranda.logging.MessageLog.getInstance;

class LoggingCacheTest extends TestSuperclass {

    public Message createMessage () {
        return createTestMessage(UUID.randomUUID());
    }


    @Test
    void copyMessages1() throws IOException, LtsllcException {
        Message m1 = createMessage();
        Message m2 = createMessage();

        Miranda.getProperties().setProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT, String.valueOf(m1.getContents().length));

        MessageLog.defineStatics();

        UUID u = UUID.randomUUID();

        MessageLog.getInstance().add(m1, u);
        MessageLog.getInstance().add(m2, u);

        LoggingCache.CopyMessagesResult temp = MessageLog.getInstance().copyMessages(m1.getContents().length + 1, 0);

        assert (MessageLog.getInstance().getCache().getUuidToMessage().size() == 1);
    }

    @Test
    void copyMessages2() throws Exception {
        Message m1 = createMessage();
        Message m2 = createMessage();

        Miranda.getProperties().setProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT,String.valueOf(m1.getContents().length));
        MessageLog.defineStatics();

        UUID u = UUID.randomUUID();

        MessageLog.getInstance().add(m1, u);
        MessageLog.getInstance().add(m2, u);

        LoggingCache.CopyMessagesResult temp = MessageLog.getInstance().copyMessages(m1.getContents().length,
                0);

        // int temp2 = MessageLog.getInstance().getCache().getU().size();
        assert (temp.list.size() == 1);
    }

    @Test
    void copyMessages3() throws Exception {
        Message m1 = createMessage();
        Message m2 = createMessage();

        MessageLog.defineStatics();

        UUID u = UUID.randomUUID();

        getInstance().add(m1, u);
        getInstance().add(m2, u);

        LoggingCache.CopyMessagesResult temp = MessageLog.getInstance().copyMessages(m1.getContents().length + 1, 3);

        assert (temp == null);
    }

    @Test
    void copyMessages4() throws Exception {
        Message m1 = createMessage();
        Message m2 = createMessage();

        MessageLog.defineStatics();

        UUID u = UUID.randomUUID();

        MessageLog.getInstance().add(m1, u);
        getInstance().add(m2, u);

        LoggingCache.CopyMessagesResult temp = MessageLog.getInstance().copyMessages(
                m1.getContents().length + 1,
                m1.getContents().length + 1
        );

        assert (temp == null);
    }


    @Test
    void copyMessages5()throws Exception {
        Message m1 = createMessage();
        Message m2 = createMessage();
        Message m3 = createMessage();

        ImprovedFile logFile = new ImprovedFile("whatever.log");

        if (logFile.exists()) {
            logFile.delete();
        }

        //
        // Force the cache to have only enough space for m1 & m2
        //
        LoggingCache lc = new LoggingCache(logFile, (m1.getContents().length + m2.getContents().length + 1));

        lc.add(m1);
        lc.add(m2);
        lc.add(m3);

        LoggingCache.CopyMessagesResult temp =  lc.copyMessages(
                m1.getContents().length + m2.getContents().length + 1,
                0
        );

        if (logFile.exists()) {
            logFile.delete();
        }

        assert (2 == lc.getUuidToMessage().size());
    }

    @Test
    void loadMessages() throws IOException, LtsllcException {
        ImprovedFile improvedFile = ImprovedFile.createImprovedTempFile("abc");
        try {
            LoggingCache cache = new LoggingCache(improvedFile, 1024);
            Message one = createMessage();
            Message two = createMessage();
            cache.add(one);
            cache.add(two);

            cache.recover();

            assert(cache.getAllMessages().contains(one) && cache.getAllMessages().contains(two));
        } finally {
            improvedFile.delete();
        }
    }

    @Test
    void get() {
    }

    @Test
    void clear() {
    }

    @Test
    void copyMessages() {
    }

    @Test
    void migrateLeastReferencedMessagesToDisk() {
    }

    @Test
    void moveMessageToDisk() {
    }

    @Test
    void loadMessage() {
    }

    @Test
    void remove() {
    }

    @Test
    void compact() {
    }

    @Test
    void shouldRecover() {
    }

    @Test
    void recover() {
    }

    @Test
    void copyAllMessages() {
    }

    @Test
    void getLocationFor() {
    }

    @Test
    void getAllMessages() {
    }

    @Test
    void alarm() {
    }

    @Test
    void loadMessageAndRebalance() {
    }
}