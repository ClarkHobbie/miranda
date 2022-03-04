package com.ltsllc.miranda.cluster;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;

import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.util.*;


/**
 * A cache of messages
 *
 * This class contains both in core Messages and Messages on disk.  It provides a single interface to get both online
 * and offline Messages.
 */
public class MessageCache {
    public static final Logger logger = LogManager.getLogger();
    protected static final Gson ourGson;

    protected Map<UUID, Boolean> uuidToOnline = new HashMap<>();
    protected Map<UUID, Message> uuidToMessage = new HashMap<>();
    protected Map<UUID, Long> uuidToLocation = new HashMap<>();
    protected ImprovedFile offLineMessages;
    protected Map<UUID, Integer> uuidToNumberOfTimesReferenced = new HashMap<>();
    protected int loadLimit;

    protected int currentLoad = 0;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        ourGson = gsonBuilder.create();
    }

    public void initialize () {
        ImprovedProperties instance = Miranda.getProperties();
        try {
            loadLimit = instance.getIntProperty(Miranda.PROPERTY_CACHE_LOAD_LIMIT);
        } catch (Exception e) {
            logger.error("Exception trying to get " + Miranda.PROPERTY_CACHE_LOAD_LIMIT, e);
        } finally {
            if (loadLimit == 0) {
                logger.warn(Miranda.PROPERTY_CACHE_LOAD_LIMIT + " not initialized");
            }
        }

        offLineMessages = null;
        try {
            offLineMessages = ImprovedFile.createImprovedTempFile("temp_");
        } catch (IOException e) {
            logger.error ("Exception getting temp file",e);
        } finally {
            if (null == offLineMessages) {
                logger.warn("Tempfile for messages is null");
            }
        }
    }


    public Map<UUID, Boolean> getUuidToOnline() {
        return uuidToOnline;
    }

    public void setUuidToOnline(Map<UUID, Boolean> uuidToOnline) {
        this.uuidToOnline = uuidToOnline;
    }

    /**
     * Create a new, empty cache
     *
     * @return The new, empty cache
     */
    public static MessageCache empty () {
        return new MessageCache();
    }

    /**
     * Is the Message in core memory?
     *
     * @param uuid The UUID that the caller is asking about
     * @return Whether the message is online, or false if we don't know
     */
    public boolean isOnline(UUID uuid) {
        if (uuidToOnline.containsKey(uuid)) {
            return uuidToOnline.get(uuid);
        }

        return false;
    }

    /**
     * Get a Message regardless of where it is.
     *
     * NOTE THAT THE MESSAGE MUST EXIST IN THE CACHE!
     * This method get an online or offline message.
     *
     * @param uuid The UUID of the message
     * @return The message
     * @exception IOException If the temp file is not found or if skip throws an Exception
     */
    public Message get (UUID uuid) throws IOException {
        if (isOnline(uuid)) {
            return uuidToMessage.get (uuid);
        } else {
            //
            // the message is offline
            //
            FileInputStream fileInputStream = null;
            InputStreamReader inputStreamReader = null;
            try {
                long location = uuidToLocation.get(uuid);
                fileInputStream = new FileInputStream(offLineMessages);
                fileInputStream.skip(location);
                inputStreamReader = new InputStreamReader(fileInputStream);
                return ourGson.fromJson(inputStreamReader, Message.class);
            } finally {
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }

                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            }
        }
    }

    /**
     * Add a Message to the cache
     *
     * @param newMessage The message to add.
     * @exception LtsllcException If there are problems accommodating the new message.  This method also throws the
     * exception if the new message's contents are larger than the load limit.
     */
    public synchronized void add (Message newMessage) throws LtsllcException {
        while (currentLoad + newMessage.getContents().length > loadLimit && currentLoad != 0) {
            migrateLeastReferencedMessage();
        }

        if (newMessage.getContents().length > loadLimit) {
            logger.error("a new message is larger than the loadLimit, " + loadLimit);
            throw new LtsllcException("a new message is larger than the loadLimit, " + loadLimit);
        }

        uuidToMessage.put(newMessage.getMessageID(), newMessage);
        currentLoad += newMessage.getContents().length;
    }

    /**
     * Remove a message from the cache
     *
     * NOTE: this method expects the message to be in the cache.
     *
     * @param uuid The UUID of the message to remove.
     * @exception LtsllcException If there is a problem removing the message.
     */
    public synchronized void remove (UUID uuid) throws LtsllcException {
        if (!contains(uuid)) {
            return;
        }

        Message message = uuidToMessage.get(uuid);

        uuidToMessage.put(uuid, null);
        uuidToOnline.put(uuid, null);
        uuidToLocation.put(uuid, null);
        uuidToNumberOfTimesReferenced.put(uuid, null);

        if (uuidToOnline.get(uuid)) {
            currentLoad -= message.getContents().length;
        }
    }

    /**
     * Put a new message in the cache
     *
     * NOTE: the message must not already be in the cache.
     *
     * @param message The new message.
     * @throws LtsllcException If the "new" message is already in the cache; o if there is a problem putting it in the
     * cache.
     */
    public synchronized void putMessage (Message message) throws LtsllcException {
        if (uuidToMessage.get(message.getMessageID()) != null) {
            throw new LtsllcException("messageID, " + message.getMessageID() + ", is already present in the cache");
        }

        while (message.getContents().length + currentLoad > loadLimit) {
            migrateLeastReferencedMessage();
        }
        uuidToMessage.put(message.getMessageID(), message);
        uuidToOnline.put(message.getMessageID(), true);
        currentLoad += message.getContents().length;
    }

    /**
     * Migrate the least referenced Message to the offline temp file
     */
    public synchronized void migrateLeastReferencedMessage () throws LtsllcException {
        Set<UUID> set = uuidToNumberOfTimesReferenced.keySet();
        Iterator<UUID> iterator = set.iterator();
        UUID uuid = null;
        int smallest = Integer.MAX_VALUE;
        Message smallestMessage = null;

        while (iterator.hasNext()) {
            uuid = iterator.next();
            if ((smallestMessage == null) || (uuidToNumberOfTimesReferenced.get(smallestMessage.getMessageID()) < smallest)) {
                smallest = uuidToNumberOfTimesReferenced.get(uuid);
                smallestMessage = uuidToMessage.get(uuid);
            }
        }

        migrateInCoreMessageToOffline(smallestMessage.getMessageID());
    }

    /**
     * Migrate the designated message to the temp file
     *
     * All the variables, e.g. uuidToOnline, are updated to reflect this.
     *
     * @param uuid The designated message.  This message must be present in uuidToMessage.
     * @exception LtsllcException The method throws this if there is an error opening offlineMessage or if there is an
     * error writing to the file, or the UUID does not exist in uuidToMessage
     */
    protected synchronized void migrateInCoreMessageToOffline (UUID uuid) throws LtsllcException {
        if (uuidToMessage.get(uuid) == null) {
            throw new LtsllcException("uuid does not exist in uuidToMessage");
        }

        String json = ourGson.toJson(uuidToMessage.get(uuid));
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(offLineMessages);
            uuidToLocation.put(uuid, offLineMessages.length());
            fileWriter.append(json);
            currentLoad = currentLoad - uuidToMessage.get(uuid).getContents().length;
            uuidToMessage.put(uuid, null);
            uuidToOnline.put(uuid, false);
        } catch (IOException e) {
            throw new LtsllcException("Exception opening or reading message " + uuid,e);
        }finally {
            try {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (IOException e) {
                throw new LtsllcException("Exception trying to close offLineMessages",e);
            }
        }
    }

    /**
     * Read a Message from the offline file
     *
     * NOTE: the file must exist in uuidToLocation for this method to work.
     *
     * @param uuid The message to read.  This UUID must exist in uuidToLocation
     * @return The specified message
     * @throws com.ltsllc.commons.LtsllcException If there is a problem opening the file, or reading the message.
     */
    protected synchronized Message readOfflineMessage (UUID uuid) throws LtsllcException {
        if (uuidToLocation.get(uuid) == null) {
            throw new LtsllcException("message does not exist in uuidToLocation");
        }
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(offLineMessages);
            fileReader.skip(uuidToLocation.get(uuid));
            return ourGson.fromJson(fileReader, Message.class);
        } catch (IOException e) {
            throw new LtsllcException("Execption opening or reading offline file", e);
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
            } catch (IOException e) {
                throw new LtsllcException("Exception closing offline file",e);
            }
        }
    }

    /**
     * Migrate an offline message to online
     *
     * The UUID must exist in uuidToLocation.  All the variables, e.g. uuidToMessages are updated to reflect this.
     *
     * @param uuid The Message to read.
     * @throws LtsllcException If the uuid does not exist in uuidToLocation, or there is a problem accessing the
     * Message.
     */
    public synchronized void migrateMessageToOnline (UUID uuid) throws LtsllcException {
        if (uuidToLocation.get(uuid) == null) {
            throw new LtsllcException("uuid, " + uuid + ", does not exist in uuidToLocation");
        }

        Message message = readOfflineMessage(uuid);
        while (currentLoad + message.getContents().length > loadLimit) {
            migrateLeastReferencedMessage();
        }

        uuidToMessage.put (uuid, message);
        uuidToLocation.put (uuid, null);
        uuidToOnline.put (uuid, true);
    }

    public synchronized boolean contains (UUID uuid) {
        return uuidToOnline.containsKey(uuid);
    }

    public synchronized void undefineMessage(Message message) {
        uuidToMessage.put(message.getMessageID(), null);
        uuidToLocation.put(message.getMessageID(), null);
        uuidToNumberOfTimesReferenced.put(message.getMessageID(), null);
    }
}
