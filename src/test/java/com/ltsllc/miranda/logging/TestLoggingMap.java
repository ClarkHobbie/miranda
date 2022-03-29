package com.ltsllc.miranda.logging;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.TestSuperclass;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

public class TestLoggingMap extends TestSuperclass {
    @Test
    public void constructor () {
        ImprovedFile logfile = new ImprovedFile("tempfile");
        LoggingMap loggingMap = new LoggingMap("tempfile");
        assert (loggingMap.getFile().equals(logfile));

        loggingMap = new LoggingMap(logfile);
        assert (loggingMap.getFile().equals(logfile));
    }

    @Test
    public void add () throws LtsllcException, IOException {
        ImprovedFile logfile = null;
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();
            Message message = createTestMessage(UUID.randomUUID());

            logfile = new ImprovedFile("tempfile");

            LoggingMap loggingMap = new LoggingMap(logfile);
            UUID uuid = UUID.randomUUID();
            loggingMap.add(message.getMessageID(), uuid);
            UUID temp = loggingMap.get (message.getMessageID());
            assert (temp.equals(uuid));

            fileReader = new FileReader(logfile);
            bufferedReader = new BufferedReader(fileReader);
            String line = bufferedReader.readLine();

            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(message.getMessageID());
            stringBuffer.append(" ");
            stringBuffer.append(uuid.toString());
            assert (line.equals(stringBuffer.toString()));
        } finally {
            if (null != fileReader) {
                fileReader.close();
            }

            if (null != bufferedReader) {
                bufferedReader.close();
            }

            if (null != logfile) {
                logfile.delete();
            }
        }
    }

    @Test
    public void compact () throws LtsllcException, IOException {
        ImprovedFile logfile = null;
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();

            UUID messageUuid = UUID.randomUUID();
            UUID ownerUuid = UUID.randomUUID();

            logfile = new ImprovedFile("tempfile");

            LoggingMap loggingMap = new LoggingMap(logfile);

            UUID otherMessageID = UUID.randomUUID();

            loggingMap.add(messageUuid, ownerUuid);
            loggingMap.add(otherMessageID, ownerUuid);
            loggingMap.remove(otherMessageID);



            loggingMap.compact ();
            fileReader = new FileReader(logfile);
            bufferedReader = new BufferedReader(fileReader);

            String line = bufferedReader.readLine();
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(messageUuid);
            stringBuffer.append (" ");
            stringBuffer.append(ownerUuid);
            assert (line.equals(stringBuffer.toString()));

            line = bufferedReader.readLine();
            assert (line == null);
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }

            if (fileReader != null) {
                fileReader.close();
            }

            if (logfile != null) {
                logfile.delete();
            }
        }
    }


    @Test
    public void get () throws LtsllcException, IOException {
        ImprovedFile logfile = null;
        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();

            logfile = new ImprovedFile("tempfile");
            Message message = createTestMessage(UUID.randomUUID());
            UUID owner = UUID.randomUUID();

            LoggingMap loggingMap = new LoggingMap(logfile);
            loggingMap.add(message.getMessageID(), owner);

            UUID temp = loggingMap.get(message.getMessageID());
            assert (temp.equals(owner));
        } finally {
            if (logfile != null) {
                logfile.delete();
            }
        }
    }

    @Test
    public void recover () throws LtsllcException, IOException {
        ImprovedFile logfile = null;
        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();

            logfile = new ImprovedFile("tempfile");

            UUID message = UUID.randomUUID();
            UUID owner = UUID.randomUUID();

            LoggingMap loggingMap = new LoggingMap(logfile);
            loggingMap.add(message, owner);

            LoggingMap newMap = new LoggingMap(logfile);
            newMap.recover();

            UUID temp = newMap.get(message);
            assert (temp.equals(owner));
        } finally {
            if (logfile != null) {
                logfile.delete();
            }
        }
    }

    @Test
    public void remove () throws IOException {
        ImprovedFile logfile = null;
        try {
            UUID message = UUID.randomUUID();
            UUID owner = UUID.randomUUID();
            logfile = new ImprovedFile("tempfile");

            LoggingMap loggingMap = new LoggingMap(logfile);
            loggingMap.add(message, owner);
            loggingMap.remove(message);
            UUID temp = loggingMap.get(message);

            assert (temp == null);
        } finally {
            if (logfile != null) {
                logfile.delete();
            }
        }
    }

    @Test
    public void shouldRecover () throws IOException {
        ImprovedFile logfile = null;
        try {
            UUID message = UUID.randomUUID();
            UUID owner = UUID.randomUUID();
            logfile = new ImprovedFile("tempfile");

            LoggingMap loggingMap = new LoggingMap(logfile);
            loggingMap.add(message, owner);

            assert (true == loggingMap.shouldRecover());

            logfile.delete();

            assert (false == loggingMap.shouldRecover());
        } finally {
            if (logfile != null) {
                logfile.delete();
            }
        }

    }
}
