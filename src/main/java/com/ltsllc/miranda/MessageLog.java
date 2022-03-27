package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.miranda.logcache.LoggingCache;
import com.ltsllc.miranda.logcache.LoggingMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * A central location for all messages
 */
public class MessageLog {
    protected static MessageLog instance;

    protected LoggingCache cache;
    protected LoggingMap uuidToOwner;

    protected MessageLog (ImprovedFile logfile, int loadLimit, ImprovedFile ownersFile) {
        cache = new LoggingCache(logfile, loadLimit);
        uuidToOwner = new LoggingMap(ownersFile);
    }

    public static MessageLog getInstance() {
        return instance;
    }

    public static void setInstance(MessageLog instance) {
        MessageLog.instance = instance;
    }

    public static void defineStatics (ImprovedFile logfile, int loadLimit, ImprovedFile ownerLogfile) {
        instance = new MessageLog(logfile, loadLimit, ownerLogfile);
    }

    public static boolean shouldRecover (ImprovedFile logfile) {
        return logfile.exists();
    }

    public static MessageLog recover(ImprovedFile logfile, int loadLimit, ImprovedFile ownersFile) throws IOException {
        return getInstance().performRecover(logfile, loadLimit, ownersFile);
    }

    public MessageLog performRecover (ImprovedFile logfile, int loadLimit, ImprovedFile  ownersFile) throws IOException {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;

        try {
            MessageLog messageLog = new MessageLog(logfile, loadLimit, ownersFile);
            fileReader = new FileReader(logfile);
            bufferedReader = new BufferedReader(fileReader);

            String line = bufferedReader.readLine();

            while (line != null) {
                Message message = Message.readLongFormat(line);
                messageLog.add(message);

                line = bufferedReader.readLine();
            }

            return messageLog;
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }

            if (fileReader != null) {
                fileReader.close();
            }
        }
    }

    public synchronized void recordOwner (UUID message, UUID owner) throws IOException {
        uuidToOwner.add(message, owner);
    }

    public synchronized void removeOwnerOf (UUID message) {
        uuidToOwner.remove(message);
    }

    public void add (Message message) throws IOException {
        cache.add(message);
    }

    public void remove (UUID uuid) throws IOException {
        cache.remove(uuid);
    }

    public Message get (UUID uuid) throws LtsllcException, IOException {
        return cache.get (uuid);
    }

    public List<Message> copyAllMessages () throws IOException {
        return cache.copyAllMessages();
    }

    public UUID getOwnerOf (UUID message) {
        return uuidToOwner.get(message);
    }

    public void clearAllMessages (ImprovedFile logfile, int loadLimit, ImprovedFile ownersFile) {
        cache = new LoggingCache(logfile, loadLimit);
        uuidToOwner = new LoggingMap(ownersFile);
    }
}
