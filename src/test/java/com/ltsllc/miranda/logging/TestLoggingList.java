package com.ltsllc.miranda.logging;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.TestSuperclass;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

public class TestLoggingList extends TestSuperclass {
    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void add () throws IOException {
        ImprovedFile logfile = new ImprovedFile("logfile");
        try {
            LoggingList loggingList = new LoggingList(logfile);

            Message message = createTestMessage(UUID.randomUUID());
            loggingList.add(message);

            Message temp = loggingList.get(0);
            assert (temp.equals(message));
            assert (logfile.exists());

            FileReader fileReader = null;
            BufferedReader bufferedReader = null;

            try {
                fileReader = new FileReader(logfile);
                bufferedReader = new BufferedReader(fileReader);

                String line = bufferedReader.readLine();
                Message fileMessage = Message.readLongFormat(line);

                assert (fileMessage.equals(message));
            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }

                if (fileReader != null) {
                    fileReader.close();
                }
            }
        } finally {
            logfile.delete();
        }
    }

    @Test
    public void isEmpty () throws IOException {
        ImprovedFile logfile = new ImprovedFile("logfile");
        try {
            LoggingList loggingList = new LoggingList(logfile);
            boolean result = loggingList.isEmpty();
            assert (result);

            Message message = createTestMessage(UUID.randomUUID());
            loggingList.add(message);
            result = loggingList.isEmpty();
            assert(!result);
        } finally {
            logfile.delete();
        }
    }

    @Test
    public void get () throws IOException {
        ImprovedFile logfile = new ImprovedFile("logfile");
        try {
            LoggingList loggingList = new LoggingList(logfile);

            Message message = createTestMessage(UUID.randomUUID());
            loggingList.add(message);
            Message temp = loggingList.get(0);
            assert (temp.equals(message));
        } finally {
            logfile.delete();
        }
    }

    @Test
    public void recover () throws IOException, LtsllcException {
        ImprovedFile logfile = new ImprovedFile("logfile");
        ImprovedFile backup = new ImprovedFile("logfile.backup");
        try {
            LoggingList loggingList = new LoggingList(logfile);

            Message message = createTestMessage(UUID.randomUUID());
            loggingList.add (message);

            LoggingList newLoggingList = LoggingList.recover(logfile);
            Message temp = newLoggingList.get(0);
            assert (temp.equals(message));
        } finally {
            logfile.delete();
            backup.delete();
        }
    }

    @Test
    public void remove () throws IOException {
        ImprovedFile logfile = new ImprovedFile("logfile");
        try {
            LoggingList loggingList = new LoggingList(logfile);
            Message message = createTestMessage(UUID.randomUUID());
            loggingList.add(message);
            Message temp = loggingList.get(0);
            assert (temp.equals(message));
            loggingList.remove(message);
            assert (loggingList.isEmpty());
        } finally {
            logfile.delete();
        }
    }

    @Test
    public void shouldRecover () throws IOException {
        ImprovedFile logfile = new ImprovedFile("logfile");
        try {
            LoggingList loggingList = new LoggingList(logfile);
            assert (!loggingList.shouldRecover(logfile));

            Message message = createTestMessage(UUID.randomUUID());
            loggingList.add(message);

            assert (LoggingList.shouldRecover(logfile));
        } finally {
            logfile.delete();
        }
    }
}
