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
    protected long location = 0;

    protected int currentLoad = 0;

    public MessageCache() throws LtsllcException {
        initialize();
    }

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        ourGson = gsonBuilder.create();
    }

    public int getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(int currentLoad) {
        this.currentLoad = currentLoad;
    }

    public Map<UUID, Integer> getUuidToNumberOfTimesReferenced() {
        return uuidToNumberOfTimesReferenced;
    }

    public void setUuidToNumberOfTimesReferenced(Map<UUID, Integer> uuidToNumberOfTimesReferenced) {
        this.uuidToNumberOfTimesReferenced = uuidToNumberOfTimesReferenced;
    }

    public ImprovedFile getOffLineMessages() {
        return offLineMessages;
    }

    public void setOffLineMessages(ImprovedFile offLineMessages) {
        this.offLineMessages = offLineMessages;
    }

    public Map<UUID, Long> getUuidToLocation() {
        return uuidToLocation;
    }

    public void setUuidToLocation(Map<UUID, Long> uuidToLocation) {
        this.uuidToLocation = uuidToLocation;
    }

    public Map<UUID, Message> getUuidToMessage() {
        return uuidToMessage;
    }

    public void setUuidToMessage(Map<UUID, Message> uuidToMessage) {
        this.uuidToMessage = uuidToMessage;
    }

    public void initialize () throws LtsllcException {
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

        offLineMessages = new ImprovedFile(instance.getProperty(Miranda.PROPERTY_OFFLINE_MESSAGES));
        if (offLineMessages.exists()) {
            logger.error("Offline messages file (" + offLineMessages + ") exists aborting");
            throw new LtsllcException("offline messages file (" + offLineMessages + ") exists");
        }

        location = 0;
    }

    public int getLoadLimit() {
        return loadLimit;
    }

    public void setLoadLimit(int loadLimit) {
        this.loadLimit = loadLimit;
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
    public boolean empty () {
        return uuidToOnline.isEmpty();
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
        Message returnValue = null;

        if (null == uuidToOnline.get(uuid)) {
            return null;
        }

        if (isOnline(uuid)) {
            returnValue = uuidToMessage.get (uuid);
            Integer numberOfTimesReffernced = uuidToNumberOfTimesReferenced.get (uuid);
            numberOfTimesReffernced++;
            uuidToNumberOfTimesReferenced.put(uuid, numberOfTimesReffernced);
        } else {
            //
            // the message is offline
            //
            FileReader fileReader = null;
            BufferedReader bufferedReader = null;
            try {
                long location = uuidToLocation.get(uuid);
                fileReader = new FileReader(offLineMessages);
                bufferedReader = new BufferedReader(fileReader);
                bufferedReader.skip(location);
                String line = bufferedReader.readLine();
                returnValue = Message.readLongFormat(line);
            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }

                if (fileReader != null) {
                    fileReader.close();
                }
            }

        }

        return returnValue;
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
        uuidToOnline.put(newMessage.getMessageID(), true);
        Integer numberOfTimesReferened = uuidToNumberOfTimesReferenced.get(newMessage.getMessageID());
        if (null == numberOfTimesReferened) {
            uuidToNumberOfTimesReferenced.put(newMessage.getMessageID(), 1);
        } else {
            int newNumberOfTimesReferrenced = numberOfTimesReferened + 1;
            uuidToNumberOfTimesReferenced.put(newMessage.getMessageID(), newNumberOfTimesReferrenced);
        }

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

        uuidToMessage.put(uuid, null);
        uuidToOnline.put(uuid, null);
        uuidToLocation.put(uuid, null);
        uuidToNumberOfTimesReferenced.put(uuid, null);
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
            if ((smallestMessage == null) || (uuidToNumberOfTimesReferenced.get(uuid) < smallest)) {
                smallest = uuidToNumberOfTimesReferenced.get(uuid);
                smallestMessage = uuidToMessage.get(uuid);
            }
        }

        migrateInCoreMessageToOffline(smallestMessage);
    }

    /**
     * Migrate the designated message to the temp file
     *
     * All the variables, e.g. uuidToOnline, are updated to reflect this.
     *
     * @param message The designated message.  This message must be present in uuidToMessage.
     * @exception LtsllcException The method throws this if there is an error opening offlineMessage or if there is an
     * error writing to the file, or the UUID does not exist in uuidToMessage
     */
    protected synchronized void migrateInCoreMessageToOffline (Message message) throws LtsllcException {
        if (uuidToMessage.get(message.getMessageID()) == null) {
            throw new LtsllcException("uuid does not exist in uuidToMessage");
        }
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new FileWriter(offLineMessages.toString(), true);
            bufferedWriter = new BufferedWriter(fileWriter);
            location = offLineMessages.length();
            bufferedWriter.write(message.longToString());
            bufferedWriter.newLine();
            currentLoad = currentLoad - message.getContents().length;
            uuidToMessage.put(message.getMessageID(), null);
            uuidToOnline.put(message.getMessageID(), false);
            uuidToLocation.put(message.getMessageID(), location);
        } catch (IOException e) {
            throw new LtsllcException("Exception opening or reading message " + message.getMessageID(),e);
        }finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }

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
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(offLineMessages);
            bufferedReader = new BufferedReader(fileReader);
            bufferedReader.skip(uuidToLocation.get(uuid));
            String inline = bufferedReader.readLine();
            return Message.readLongFormat(inline);
        } catch (IOException e) {
            throw new LtsllcException("Execption opening or reading offline file", e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
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
        uuidToOnline.put(message.getMessageID(), null);
        uuidToNumberOfTimesReferenced.put(message.getMessageID(), null);
    }

    public synchronized long getLocationFor (UUID uuid) {
        return uuidToLocation.get(uuid);
    }
}
