package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.miranda.cluster.MessageCache;
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
            instance = new OtherMessages();
            instance.messageCache.setLoadLimit(Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT)/10);
        } catch (LtsllcException e) {
            logger.error("Encountered exception during initialization",e);
        }
    }

    protected MessageCache messageCache;
    protected Map<UUID, UUID> uuidToOwner = new HashMap<>();

    protected OtherMessages () throws LtsllcException {
        messageCache = new MessageCache();
    }

    public Map<UUID, UUID> getUuidToOwner() {
        return uuidToOwner;
    }

    public void setUuidToOwner(Map<UUID, UUID> uuidToOwner) {
        this.uuidToOwner = uuidToOwner;
    }

    public MessageCache getMessageCache() {
        return messageCache;
    }

    public void setMessageCache(MessageCache messageCache) {
        this.messageCache = messageCache;
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
        ImprovedFile temp = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OWNER_FILE));

        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;

        try {
            fileWriter = new FileWriter(temp, true);
            bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write("MESSAGE_ID: ");
            bufferedWriter.write(messageUuid.toString());
            bufferedWriter.write(" OWNER: ");
            bufferedWriter.write(ownerUuid.toString());
            bufferedWriter.newLine();
        } finally {
            if (null != bufferedWriter) {
                bufferedWriter.close();
            }

            if (fileWriter != null) {
                fileWriter.close();
            }
        }
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

    public void record (Message message, UUID owner) throws LtsllcException {
        messageCache.add(message);
        uuidToOwner.put(message.getMessageID(), owner);
    }
}
