package com.ltsllc.miranda.logcache;

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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TestLoggingSet extends TestSuperclass {
    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void testConstructor () throws IOException {
        ImprovedFile logfile = new ImprovedFile("logfile");
        LoggingSet loggingSet = new LoggingSet(logfile);
        try {
            logfile.touch();
            loggingSet = new LoggingSet(logfile);
        } finally {
            logfile.delete();
        }
    }

    @Test
    public void add () throws IOException {
        ImprovedFile logfile = new ImprovedFile("logfile");
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;

        try {
            LoggingSet loggingSet = new LoggingSet(logfile);
            Message message = createTestMessage(UUID.randomUUID());
            loggingSet.add(message);

            assert (loggingSet.contains(message));
            assert (logfile.exists());

            fileReader = new FileReader(logfile);
            bufferedReader = new BufferedReader(fileReader);
            String line = bufferedReader.readLine();
            while (line != null) {
                Message temp = Message.readLongFormat(line);

                assert(temp.equals(message));

                line = bufferedReader.readLine();
            }
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
    public void addAll () throws IOException {
        ImprovedFile logfile = new ImprovedFile("logfile");

        try {
            LoggingSet loggingSet = new LoggingSet(logfile);
            Set<Message> set = new HashSet<>();
            Message message1 = createTestMessage(UUID.randomUUID());
            set.add (message1);
            Message message2 = createTestMessage(UUID.randomUUID());
            set.add(message2);
            Message message3 = createTestMessage(UUID.randomUUID());
            set.add(message3);
            loggingSet.addAll(set);

            Message message4 = createTestMessage(UUID.randomUUID());

            assert (loggingSet.contains(message1));
            assert (loggingSet.contains(message2));
            assert (loggingSet.contains(message3));
            assert (!loggingSet.contains(message4));
        } finally {
            logfile.delete();
        }
    }

    @Test
    public void contains () throws IOException {
        ImprovedFile logfile = new ImprovedFile("logfile");
        try {
            LoggingSet loggingSet = new LoggingSet(logfile);
            Message message1 = createTestMessage(UUID.randomUUID());
            Message message2 = createTestMessage(UUID.randomUUID());

            loggingSet.add(message1);

            assert (loggingSet.contains(message1));
            assert (!loggingSet.contains(message2));
        } finally {
            logfile.delete();
        }
    }

    @Test
    public void isEmpty () throws IOException {
        ImprovedFile logfile = new ImprovedFile("logfile");
        try {
            LoggingSet loggingSet = new LoggingSet(logfile);
            assert(loggingSet.isEmpty());

            Message message = createTestMessage(UUID.randomUUID());
            loggingSet.add(message);

            assert (!loggingSet.isEmpty());
        } finally {
            logfile.delete();
        }
    }

    @Test
    public void recover () throws IOException, LtsllcException {
        ImprovedFile logfile = new ImprovedFile("logfile");
        ImprovedFile backup = new ImprovedFile(logfile.getName() + ".backup");
        try {
            LoggingSet loggingSet = new LoggingSet(logfile);
            Message message = createTestMessage(UUID.randomUUID());
            loggingSet.add(message);

            assert (loggingSet.contains(message));
            Message temp = new Message(message);
            Message temp2 = new Message(temp);

            loggingSet = LoggingSet.recover(logfile);

            assert(loggingSet != null);
            assert(loggingSet.contains(message));

            //
            // a backup file is created when removing a message --- but that depends on an item in memory so we can't
            // recover from it.  Just make sure we remove it.
            //
            logfile.renameTo(backup);

            assert (backup.exists() && !logfile.exists());

            loggingSet = LoggingSet.recover(logfile);

            assert (!backup.exists()  && logfile.exists());
            assert (loggingSet != null && loggingSet.contains(message));

            //
            // a logfile doesn't exist but a backup file does --- this means that we were getting ready to delete
            // something but before we could do anything the system crashed.  When presented with this situation
            // the system should rename the backup file to the logfile and load the logfile into memory
            //
            loggingSet = new LoggingSet(logfile);
            loggingSet.add (message);
            logfile.renameTo(backup);

            loggingSet = LoggingSet.recover(logfile);

            assert (!backup.exists());
            assert (logfile.exists());
            assert (loggingSet != null);
            assert (loggingSet.contains(message));
        } finally {
            logfile.delete();
            backup.delete();
        }
    }

    @Test
    public void remove () throws IOException {
        ImprovedFile logfile = new ImprovedFile("logfile");
        ImprovedFile backup = new ImprovedFile(logfile.getName() + ".backup");
        try {
            LoggingSet loggingSet = new LoggingSet(logfile);
            Message message = createTestMessage(UUID.randomUUID());
            loggingSet.add(message);

            assert (loggingSet.contains(message));

            loggingSet.remove(message);

            assert (!loggingSet.contains(message));
        } finally {
            logfile.delete();
            backup.delete();
        }
    }

    @Test
    public void shouldRecover () throws IOException {
        ImprovedFile logfile = new ImprovedFile("logfile");
        ImprovedFile backup = new ImprovedFile(logfile.getName() + ".backup");

        try {
            LoggingSet loggingSet = new LoggingSet(logfile);
            Message message = createTestMessage(UUID.randomUUID());
            loggingSet.add(message);

            assert(LoggingSet.shouldRecover(logfile));

            logfile.renameTo(backup);

            assert(LoggingSet.shouldRecover(logfile));

            backup.delete();

            assert (!LoggingSet.shouldRecover(logfile));
        } finally {
            logfile.delete();
            backup.delete();
        }
    }
}
