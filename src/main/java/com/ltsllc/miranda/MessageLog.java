package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.logging.LoggingCache;
import com.ltsllc.miranda.logging.LoggingMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A central location for all messages
 *
 * <P>
 *     This class replaces the queues that other classes used to keep track of, for example, the messages that Miranda
 *     was going to send.
 * </P>
 * <P>
 *     This class consists of a LoggingCache, and a LoggingMap of the ownership information.  The ownership information
 *     is always kept in memory, but there is a maximum size to the LoggingMap.  That size is the total aggregate
 *     number of bytes of all the messages in the log.  If a message would exceed that limit then a least frequently
 *     referenced message is "forgotten" until enough space is freed up to store the new message.
 * </P>
 */
public class MessageLog {
    protected static MessageLog instance;

    protected LoggingCache cache;
    protected LoggingMap uuidToOwner;

    /**
     * Create a new instance
     *
     * @param logfile Where the class will put the messages that are added to it.
     * @param loadLimit The max bytes of content that the class will hold in memory.  This is required because Java
     *                  does not have the equivalent of C/C++'s sizeof function.
     * @param ownersFile The file where the ownership data of a message is stored.
     */
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

    /**
     * Essentially an equivalent to an initialize method for static variables
     *
     * @param logfile Where the class will put the messages that are added to it.
     * @param loadLimit The max bytes of content that the class will hold in memory.  This is required because Java
     *                  does not have the equivalent of C/C++'s sizeof function.
     * @param ownerLogfile The file where the ownership data of a message is stored.
     */
    public static void defineStatics (ImprovedFile logfile, int loadLimit, ImprovedFile ownerLogfile) {
        instance = new MessageLog(logfile, loadLimit, ownerLogfile);
    }

    /**
     * Should the caller call recover?
     *
     * <P>
     *     Are there logfiles and/or backups that the system creates when it works.  Note that it is suggested, but not
     *     required, that a caller should invoke the recover method if this method returns true.  If it is ignored,
     *     then this class could generate errors and exceptions when it operates.
     * </P>
     *
     * <P>
     *     This method returns true if the logfile already exists, making it equivalent to calling exists on the input
     *     parameter.
     * </P>
     *
     * @param logfile The logfile to check for.
     * @return True if the logfile exists, false otherwise.
     * @see #recover(ImprovedFile, int, ImprovedFile)
     */
    public static boolean shouldRecover (ImprovedFile logfile) {
        return logfile.exists();
    }

    /**
     * Recover from a crash
     *
     * <P>
     *     It is suggested, but not required, that the caller called the shouldRecover (and that shouldRecover returned
     *     true) before calling this method.
     * </P>
     *
     * <P>
     *     This method is just a wrapper for the performRecover method.
     * </P>
     *
     * @param logfile Where the class stores messages that are added to it.
     * @param loadLimit The max aggregate size of all the message's contents.  Any additional message are left on disk.
     * @param ownersFile The owners file that the class should use.
     * @return A new instance, based on the parameters passed to the method.
     * @throws IOException If there are problems reading the various input logfiles.
     */
    public static MessageLog recover(ImprovedFile logfile, int loadLimit, ImprovedFile ownersFile) throws IOException {
        return getInstance().performRecover(logfile, loadLimit, ownersFile);
    }

    /**
     * Recover from a crash
     *
     * <P>
     *     It is suggested, but not required, that the caller called the shouldRecover (and that shouldRecover returned
     *     true) before calling this method.
     * </P>
     *
     * <P>
     *     This method is just a wrapper for the performRecover method.
     * </P>
     *
     * @param logfile Where the class stores messages that are added to it.
     * @param loadLimit The max aggregate size of all the message's contents.  Any additional message are left on disk.
     * @param ownersFile The owners file that the class should use.
     * @return A new instance, based on the parameters passed to the method.
     * @throws IOException If there are problems reading the various input logfiles.
     */
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
                messageLog.add(message, UUID.randomUUID());

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

    public synchronized void setOwner(UUID message, UUID owner) throws IOException {
        uuidToOwner.add(message, owner);
    }

    public synchronized void removeOwnerOf (UUID message) {
        uuidToOwner.remove(message);
    }

    /**
     * Add the message to the logfile and record the ownership of the message
     *
     * @param message The message to be added.
     * @param owner The UUID of the owner of the message
     * @throws IOException If a problem is encountered while reading or writing the logfiles
     */
    public void add (Message message, UUID owner) throws IOException {
        cache.add(message);
        uuidToOwner.add(message.getMessageID(), owner);
    }

    /**
     * Remove a message
     *
     * <P>
     *     Note that this method does not remove the message from the message logfile or the owners file.
     * </P>
     *
     * @param uuid The UUID of the message to remove.
     * @throws IOException If there are problem reading from, or writing to the logfiles.
     */
    public void remove (UUID uuid) throws IOException {
        cache.remove(uuid);
        uuidToOwner.remove(uuid);
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

    public long getLocationFor (UUID message) {
        return cache.getLocationFor(message);
    }
}
