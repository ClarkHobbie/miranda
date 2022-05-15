package com.ltsllc.miranda.message;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.UncheckedLtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.alarm.AlarmClock;
import com.ltsllc.miranda.alarm.Alarmable;
import com.ltsllc.miranda.alarm.Alarms;
import com.ltsllc.miranda.logging.LoggingCache;
import com.ltsllc.miranda.logging.LoggingMap;
import com.ltsllc.miranda.properties.Properties;
import com.ltsllc.miranda.properties.PropertyChangedEvent;
import com.ltsllc.miranda.properties.PropertyListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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
public class MessageLog implements Alarmable, PropertyListener {
    public static final Logger logger = LogManager.getLogger();
    public static final Logger events = LogManager.getLogger("events");

    protected static MessageLog instance;

    protected LoggingCache cache;

    public LoggingCache getCache() {
        return cache;
    }

    public void setCache(LoggingCache cache) {
        this.cache = cache;
    }

    protected LoggingMap uuidToOwner;

    public LoggingMap getUuidToOwner() {
        return uuidToOwner;
    }

    public void setUuidToOwner(LoggingMap uuidToOwner) {
        this.uuidToOwner = uuidToOwner;
    }

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

    protected MessageLog () {}

    public static MessageLog getInstance() {
        return instance;
    }

    public static void setInstance(MessageLog instance) {
        MessageLog.instance = instance;
    }

    /**
     * Essentially an equivalent to an initialize method for static variables
     */
    public static void defineStatics () {
        instance = new MessageLog();
        instance.setupMessageLog();
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
     * @param ownerFile The owners file to check for.
     * @return True if the logfile exists, false otherwise.
     * @see #recover(ImprovedFile, int, ImprovedFile)
     */
    public static boolean shouldRecover (ImprovedFile logfile, ImprovedFile ownerFile) {
        logger.debug("entering shouldRecover");
        boolean returnValue = logfile.exists();
        if (returnValue) {
            logger.debug("message log, " + logfile + ", exists");
            events.info ("message log, " + logfile + ", exists");
        } else {
            returnValue = ownerFile.exists();

            if (returnValue) {
                logger.debug ("owner log, " + ownerFile + ", exists");
                events.info("owner log, " + ownerFile + ", exists");
            }
        }

        logger.debug("leaving shouldRecover with " + returnValue);
        return returnValue;
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
    public static MessageLog recover(ImprovedFile logfile, int loadLimit, ImprovedFile ownersFile) throws IOException, LtsllcException {
        return getInstance().performRecover(logfile, loadLimit, ownersFile);
    }

    /**
     * Recover from a crash
     *
     * <P>
     *     It is suggested, but not required, that the caller called shouldRecover (and that shouldRecover returned
     *     true) before calling this method.
     * </P>
     * <P>
     *     The method first attempts to copy the files to backups, then it attempts to read in the message and owners
     *     information.
     * </P>
     *
     * @param logfile Where the class stores the messages that were added to it.
     * @param loadLimit The max aggregate size of all the message's contents.  Any additional message are left on disk.
     * @param ownersFile The owners file that contains the ownership information.
     * @return A new instance, based on the parameters passed to the method.
     * @throws IOException If there are problems reading the various input logfiles.
     * @throws LtsllcException If backup files already exist.
     */
    public MessageLog performRecover (ImprovedFile logfile, int loadLimit, ImprovedFile  ownersFile) throws IOException, LtsllcException {
        ImprovedFile messageBackup = new ImprovedFile(logfile.getName() + ".backup");
        ImprovedFile ownersBackup = new ImprovedFile(ownersFile.getName() + ".backup");

        if (messageBackup.exists()) {
            logger.error("Backup file, " + messageBackup + ", already exists");
            throw new LtsllcException("backup file already exists " + messageBackup);
        }

        if (ownersBackup.exists()) {
            logger.error("Backup file, " + ownersBackup + ", already exists");
            throw new LtsllcException("backup file already exists " + ownersBackup);
        }

        logfile.renameTo(messageBackup);
        ownersFile.renameTo(ownersBackup);
        cache = new LoggingCache(logfile, loadLimit);
        uuidToOwner = new LoggingMap(ownersFile);

        restoreOwnersFile(ownersBackup);
        restoreMessages(messageBackup);

        return this;
    }

    /**
     * Restore the owners information
     *
     * <P>
     *     This method attempts to restore ownership information by reading in a file containing that information.
     * </P>
     *
     * @param file The file to restore from.
     * @throws IOException If there is a problem reading the file
     */
    public void restoreOwnersFile (ImprovedFile file) throws IOException {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;

        try {

            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);

            String line = bufferedReader.readLine();

            while (line != null) {
                Scanner scanner = new Scanner(line);
                UUID messageUuid = UUID.fromString(scanner.next());
                UUID ownerUuid = UUID.fromString(scanner.next());

                uuidToOwner.add(messageUuid, ownerUuid);

                line = bufferedReader.readLine();
            }
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }

            if (fileReader != null) {
                fileReader.close();
            }
        }
    }

    /**
     * Restore the message cache
     * <P>
     *     This method attempts to recover from a crash by restoring the message information that the system had when it
     *     crashed.  It does this by reading an old messages file into the cache.
     * </P>
     *
     * @param file  The old message logfile.
     * @throws IOException If there is a problem reading in the old message file.
     */
    public void restoreMessages (ImprovedFile file) throws IOException {

        FileReader fileReader = null;
        BufferedReader bufferedReader = null;

        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);

            String line = bufferedReader.readLine();

            while (line != null) {
                Message message = Message.readLongFormat(line);
                cache.add(message);

                line = bufferedReader.readLine();
            }
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
     * @param owner The UUID of the owner of the message.  If this parameter is null, then it is assumed that the
     *              ownership information was previously entered, and only the message will be added.
     * @throws IOException If a problem is encountered while reading or writing the logfiles
     */
    public void add (Message message, UUID owner) throws IOException {
        logger.debug("entering add with message");
        if (null == owner) {
            cache.add(message);
        } else {
            cache.add(message);
            uuidToOwner.add(message.getMessageID(), owner);
        }
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

    /**
     * Return the collection of all owner keys (message UUIDs) in the object
     *
     * @return The collection of all keys (message UUIDs) of all the ownership data in the object
     */
    public Collection<UUID> getAllOwnerKeys () {
        return uuidToOwner.getAllKeys();
    }

    /**
     * Return the collection of all messages in the object
     *
     * @return The collection of all message in the object
     */
    public Collection<Message> getAllMessages () {
        return cache.getAllMessages();
    }

    /**
     * Compact the various log files
     */
    public void compact () throws IOException {
        ImprovedFile messages = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        ImprovedFile owners = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OWNER_FILE));
        if (!messages.exists() || !owners.exists()) {
            logger.debug("message file or owners file does not exist aborting compaction");
            events.info("Messages file or owners file does not exist aborting compaction");
            return;
        }
        cache.compact();
        uuidToOwner.compact();
    }

    /**
     * An alarm which the class previously registered for went off
     *
     * @param alarm The alarm that the class previously registered for.
     * @throws IOException If the method (this method calls the compact method) this method calls throws an
     * exception.
     */
    @Override
    public void alarm(Alarms alarm) throws IOException {
        if (alarm == Alarms.COMPACTION) {
            compact();
            setupCompaction();
        } else {
            throw new UncheckedLtsllcException("alarm called with " + alarm);
        }
    }

    /**
     * Get all the messages the a provided owner owns
     *
     * @param owner The owner we need to search for.
     * @return A List of the messages the owner owns.
     */
    public synchronized List<UUID> getAllMessagesOwnedBy (UUID owner) {
        List<UUID> results = new ArrayList<>();

        Collection<UUID> collection = uuidToOwner.getAllKeys();
        for (UUID message : collection) {
            if (getOwnerOf(message) == owner) {
                results.add(message);
            }
        }

        return results;
    }

    /**
     * Setup the messageLog
     * <H>
     *     Calling this method sets up the compaction alarm via setupCompaction.
     * </H>
     */
    public void setupMessageLog () {
        ImprovedFile messageLog = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        ImprovedFile ownersFile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OWNER_FILE));
        int cacheLoadLimit = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT);
        instance = new MessageLog(messageLog, cacheLoadLimit, ownersFile);
        Miranda.getProperties().listen(this, com.ltsllc.miranda.properties.Properties.messageLogfile);
        Miranda.getProperties().listen(this, com.ltsllc.miranda.properties.Properties.cacheLoadLimit);
        Miranda.getProperties().listen(this, com.ltsllc.miranda.properties.Properties.ownerFile);
        setupCompaction();
    }

    public void setupCompaction() {
        AlarmClock.getInstance().scheduleOnce(this, Alarms.COMPACTION,
                Miranda.getProperties().getLongProperty(Miranda.PROPERTY_COMPACTION_TIME));
    }

    /**
     * This method calls another method to deal with a property being changed.
     *
     * <H>
     *     The different types of property change that this method can deal with are:
     *     <TABLE border="1">
     *         <TR>
     *             <TH>Property changed</TH>
     *             <TH>method</TH>
     *         </TR>
     *         <TR>
     *             <TD>messageLogfile</TD>
     *             <TD>setupMessageLog</TD>
     *         </TR>
     *         <TR>
     *             <TD>cacheLoadLimit</TD>
     *             <TD>setupMessageLog</TD>
     *         </TR>
     *         <TR>
     *             <TD>ownerLog</TD>
     *             <TD>setupMessageLog</TD>
     *         </TR>
     *         <TR>
     *             <TD>compaction</TD>
     *             <TD>setupCompaction</TD>
     *         </TR>
     *         <TR>
     *             <TD>anything else</TD>
     *             <TD>throw an LtsllcException
     *         </TR>
     *     </TABLE>
     * </H>
     * @param propertyChangedEvent The property which has changed.
     * @throws LtsllcException If an unrecognized property is passed to the method.
     */
    public void propertyChanged (PropertyChangedEvent propertyChangedEvent) throws LtsllcException {
        switch (propertyChangedEvent.getProperty()) {
            case messageLogfile : {
                setupMessageLog();
                break;
            }

            case cacheLoadLimit: {
                setupMessageLog();
                break;
            }

            case ownerFile: {
                setupMessageLog();
                break;
            }

            case compaction: {
                setupCompaction();
                break;
            }

            default: {
                throw new LtsllcException("propertyChanged called with " + propertyChangedEvent.getProperty());
            }
        }
    }

    @Override
    public Set<Properties> listeningTo() {
        return null;
    }
}