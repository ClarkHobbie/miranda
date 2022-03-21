package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.miranda.cluster.MessageCache;
import com.ltsllc.miranda.logcache.LoggingCache;
import com.ltsllc.miranda.logcache.LoggingMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A file that contains messages we are not responsible for delivering.
 */
public class OtherMessages {
    public static final Logger logger = LogManager.getLogger();

    protected static OtherMessages instance;

    static {
        try {
            ImprovedFile logfile = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OTHER_MESSAGES));
            instance = new OtherMessages(logfile);
            instance.messageCache.setLoadLimit(Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT)/10);
        } catch (LtsllcException e) {
            logger.error("Encountered exception during initialization of OtherMessages",e);
        }
    }

    protected LoggingCache messageCache;
    protected LoggingMap uuidToOwner = null;

    protected OtherMessages (ImprovedFile logfile) throws LtsllcException {
        uuidToOwner = new LoggingMap(logfile);
        ImprovedFile logfile2 = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OTHER_MESSAGES));
        int loadlimit = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_OTHER_MESSAGES_LOAD_LIMIT);
        messageCache = new LoggingCache(logfile2, loadlimit);
    }

    public LoggingCache getMessageCache() {
        return messageCache;
    }

    public void setMessageCache(LoggingCache messageCache) {
        this.messageCache = messageCache;
    }

    public LoggingMap getUuidToOwner() {
        return uuidToOwner;
    }

    public void setUuidToOwner(LoggingMap uuidToOwner) {
        this.uuidToOwner = uuidToOwner;
    }

    public static OtherMessages getInstance() {
        return instance;
    }

    public static void setInstance(OtherMessages instance) {
        OtherMessages.instance = instance;
    }

    /**
     * Return whether this method thinks we should recover
     *
     * This method thinks we should recover if the other or the owner file exists.
     *
     * @return See above.
     */
    public static boolean shouldRecover () {
        ImprovedProperties p = Miranda.getProperties();
        ImprovedFile otherFile = new ImprovedFile(p.getProperty(Miranda.PROPERTY_OTHER_MESSAGES));
        ImprovedFile ownerFile = new ImprovedFile(p.getProperty(Miranda.PROPERTY_OWNER_FILE));
        return otherFile.exists() || ownerFile.exists();
    }


    /**
     * Record the owner of a message
     *
     * Note that the method uses Miranda#PROPERTY_OWNER_FILE as the file name for the file.
     *
     * @param messageUuid The uuid of a message.
     * @param ownerUuid The uuid of the node that owns the message.
     * @throws IOException If there is a problem manipulating the owner file.
     * @see Miranda#PROPERTY_OWNER_FILE
     */
    public synchronized void recordOwner (UUID messageUuid, UUID ownerUuid) throws IOException {
        uuidToOwner.add(messageUuid, ownerUuid);
    }

    /**
     * Return the owner of a message, or null if we don't know
     *
     * @param uuid The uuid of the message we are interested in.
     * @return The uuid of the node that owns the message or null if we don't know.
     */
    public synchronized UUID getOwnerOf (UUID uuid) {
        return uuidToOwner.get(uuid);
    }

    public void record (Message message, UUID owner) throws LtsllcException, IOException {
        messageCache.add(message);
        uuidToOwner.add(message.getMessageID(), owner);
    }

}
