package com.ltsllc.miranda.logcache;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.cluster.MessageCacheTest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

public class TestLoggingCache extends TestSuperclass {
    @BeforeAll
    public static void setup() {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void add() throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Message message = createTestMessage(UUID.randomUUID());

        ImprovedFile logfile = new ImprovedFile("testLogFile.msg");

        LoggingCache loggingCache = new LoggingCache(logfile, Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT));

        loggingCache.add(message);
        Message temp = loggingCache.get(message.getMessageID());

        assert (temp.equals(message));

        FileReader fileReader = null;
        BufferedReader bufferedReader = null;

        try {
            fileReader = new FileReader(logfile);
            bufferedReader = new BufferedReader(fileReader);

            String line = bufferedReader.readLine();
            Message temp2 = Message.readLongFormat(line);

            assert (temp2.equals(message));
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }

            if (fileReader != null) {
                fileReader.close();
            }

            logfile.delete();
        }
    }

    @Test
    public void compact() throws LtsllcException, IOException {
        ImprovedFile logfile = null;
        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();

            Message message1 = MessageCacheTest.createTestMessage(UUID.randomUUID());
            Message message2 = MessageCacheTest.createTestMessage(UUID.randomUUID());
            Message message3 = MessageCacheTest.createTestMessage(UUID.randomUUID());

            logfile = new ImprovedFile("testLogfile.msg");

            LoggingCache loggingCache = new LoggingCache(logfile, Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT));
            loggingCache.add(message1);
            loggingCache.remove(message1.getMessageID());
            loggingCache.add(message2);
            loggingCache.add(message3);
            loggingCache.remove(message3.getMessageID());

            long size1 = logfile.length();
            loggingCache.compact();
            long size2 = logfile.length();

            assert (size1 > size2);

            FileReader fileReader = null;
            BufferedReader bufferedReader = null;

            try {
                fileReader = new FileReader(logfile);
                bufferedReader = new BufferedReader(fileReader);

                String line = bufferedReader.readLine();
                Message message4 = Message.readLongFormat(line);

                assert (message4.equals(message2));
            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }

                if (fileReader != null) {
                    fileReader.close();
                }
            }
        } finally {
            if (logfile != null) {
                logfile.delete();
            }
        }
    }

    @Test
    public void get() throws IOException, LtsllcException {
        Message message1 = createTestMessage(UUID.randomUUID());
        Message message2 = createTestMessage(UUID.randomUUID());
        Message message3 = createTestMessage(UUID.randomUUID());
        Message message4 = createTestMessage(UUID.randomUUID());

        ImprovedFile improvedFile = new ImprovedFile("tempfile.msg");
        LoggingCache loggingCache = new LoggingCache(improvedFile, 6);

        loggingCache.add(message1);
        loggingCache.add(message2);
        loggingCache.add(message3);

        Message temp = loggingCache.get(message1.getMessageID());

        assert (temp != null);

        temp = loggingCache.get(message2.getMessageID());

        assert (temp != null);

        temp = loggingCache.get(message3.getMessageID());

        assert (temp != null);

        temp = loggingCache.get(message4.getMessageID());
        assert (temp == null);

        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(improvedFile);
            bufferedReader = new BufferedReader(fileReader);

            String line = bufferedReader.readLine();

            assert (line != null);

            temp = Message.readLongFormat(line);

            assert (temp.equals(message1));

            line = bufferedReader.readLine();
            temp = Message.readLongFormat(line);
            assert (temp.equals(message2));

            line = bufferedReader.readLine();
            temp = Message.readLongFormat(line);
            assert (temp.equals(message3));
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }

            if (fileReader != null) {
                fileReader.close();
            }

            improvedFile.delete();
        }
    }

    @Test
    public void loadMessage() throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        Miranda.getInstance().loadProperties();

        Message message = createTestMessage(UUID.randomUUID());

        ImprovedFile logfile = new ImprovedFile("tempfile.msg");
        LoggingCache loggingCache = new LoggingCache(logfile, Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT));

        Message message1 = createTestMessage(UUID.randomUUID());
        Message message2 = createTestMessage(UUID.randomUUID());
        Message message3 = createTestMessage(UUID.randomUUID());
        Message message4 = createTestMessage(UUID.randomUUID());

        loggingCache.setLoadLimit(6);

        loggingCache.add(message1);
        loggingCache.add(message2);
        loggingCache.add(message3);

        Message temp = loggingCache.loadMessage(message1.getMessageID());
        assert (temp.equals(message1));
        logfile.delete();
    }

    @Test
    public void migrateLeastReferencedMessageToDisk() throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile logfile = new ImprovedFile("tempfile.msg");

        LoggingCache loggingCache = new LoggingCache(logfile, 6);

        Message message1 = createTestMessage(UUID.randomUUID());
        loggingCache.add(message1);
        loggingCache.get(message1.getMessageID());
        loggingCache.get(message1.getMessageID());
        loggingCache.get(message1.getMessageID());

        assert (3 == loggingCache.getUuidToTimesReferenced().get(message1.getMessageID()));
        assert (true == loggingCache.getUuidToInMemory().get(message1.getMessageID()));

        Message message2 = createTestMessage(UUID.randomUUID());
        loggingCache.add(message2);
        loggingCache.get(message2.getMessageID());
        loggingCache.get(message2.getMessageID());

        assert (2 == loggingCache.getUuidToTimesReferenced().get(message2.getMessageID()));
        assert (true == loggingCache.getUuidToInMemory().get(message2.getMessageID()));

        Message message3 = createTestMessage(UUID.randomUUID());
        loggingCache.add(message3);
        loggingCache.get(message3.getMessageID());
        loggingCache.get(message3.getMessageID());
        loggingCache.get(message3.getMessageID());
        loggingCache.get(message3.getMessageID());

        assert (4 == loggingCache.getUuidToTimesReferenced().get(message3.getMessageID()));
        assert (true == loggingCache.getUuidToInMemory().get(message3.getMessageID()));
        assert (false == loggingCache.getUuidToInMemory().get(message2.getMessageID()));

        logfile.delete();
    }


    @Test
    public void moveMessageToDisk() throws LtsllcException {
        ImprovedFile logfile = null;
        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();
            Message message = createTestMessage(UUID.randomUUID());

            logfile = new ImprovedFile("tempfile.msg");
            LoggingCache loggingCache = new LoggingCache(logfile, Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT));

            loggingCache.moveMessageToDisk(message);
            assert (false == loggingCache.getUuidToInMemory().get(message.getMessageID()));
        } finally {
            if (logfile != null) {
                logfile.delete();
            }
        }
    }

    @Test
    public void recover() throws LtsllcException, IOException {
        ImprovedFile logfile = null;
        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();

            Message message = createTestMessage(UUID.randomUUID());
            logfile = new ImprovedFile("tempfile.msg");

            LoggingCache loggingCache = new LoggingCache(logfile, Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT));
            loggingCache.add(message);

            loggingCache.recover();

            assert (true == loggingCache.getUuidToInMemory().get(message.getMessageID()));
        } finally {
            if (null != logfile) {
                logfile.delete();
            }
        }
    }

    @Test
    public void remove() throws LtsllcException, IOException {
        ImprovedFile logfile = null;
        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();

            logfile = new ImprovedFile("tempfile.msg");
            LoggingCache loggingCache = new LoggingCache(logfile, Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT));

            Message message = createTestMessage(UUID.randomUUID());
            loggingCache.add(message);
            loggingCache.remove(message.getMessageID());

            assert (null == loggingCache.getUuidToMessage().get(message.getMessageID()));
            assert (null == loggingCache.getUuidToInMemory().get(message.getMessageID()));
            assert (null == loggingCache.getUuidToTimesReferenced().get(message.getMessageID()));
            assert (null == loggingCache.getUuidToLocation().get(message.getMessageID()));
        } finally {
            if (logfile != null) {
                logfile.delete();
            }
        }
    }

    @Test
    public void shouldRecover () throws IOException, LtsllcException {
        ImprovedFile logfile1 = null;
        ImprovedFile logfile2 = null;
        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();
            logfile1 = new ImprovedFile("tempfile.msg");
            logfile1.touch();

            LoggingCache loggingCache = new LoggingCache(logfile1, Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT));
            assert (true == loggingCache.shouldRecover());

            logfile2 = new ImprovedFile("otherfile.msg");
            loggingCache = new LoggingCache(logfile2, Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT));

            assert (false == loggingCache.shouldRecover());
        } finally {
            if (logfile1 != null) {
                logfile1.delete();
            }

            if (logfile2 != null) {
                logfile2.delete();
            }
        }
    }
}