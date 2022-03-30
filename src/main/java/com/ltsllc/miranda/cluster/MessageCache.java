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
 * <P>
 * This class contains both in core Messages and Messages on disk.  It provides a single interface to get both online
 * and offline Messages.
 * </P>
 */
public class MessageCache {
    public static final Logger logger = LogManager.getLogger();
    public static final Logger events = LogManager.getLogger("events");

    /**
     * A map from message UUID to whether the memory or not.  A value of true indicates that the message is in memory.
     */
    protected Map<UUID, Boolean> uuidToOnline = new HashMap<>();

    /**
     * A map from message UUID to the message that has that uuid
     */
    protected Map<UUID, Message> uuidToMessage = new HashMap<>();

    /**
     * A map from message UUID to the message's offset in the message log
     */
    protected Map<UUID, Long> uuidToLocation = new HashMap<>();

    /**
     * The logfile where messages are stored
     */
    protected ImprovedFile logfile;

    /**
     * A map from message UUID to an integer that contains the number of times that message has been looked up.
     */
    protected Map<UUID, Integer> uuidToNumberOfTimesReferenced = new HashMap<>();

    /**
     * The number of bytes of all the messages in memory before it sheds the excess to the message log
     */
    protected int loadLimit;

     /**
      * The sum of all the message's contents that are in memory (i.e. in uuidToMessage).
      */
    protected int currentLoad = 0;



    public MessageCache(ImprovedFile logfile, int loadLimit) throws LtsllcException {
        initialize(logfile, loadLimit);
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

    public ImprovedFile getLogfile() {
        return logfile;
    }

    public void setLogfile(ImprovedFile logfile) {
        this.logfile = logfile;
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

    public void initialize(ImprovedFile logfile, int loadLimit) throws LtsllcException {
        this.logfile = logfile;
        this.loadLimit = loadLimit;
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
    public boolean empty() {
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
     * <p>
     * NOTE THAT THE MESSAGE MUST EXIST IN THE CACHE!
     * This method get an online or offline message.
     *
     * @param uuid The UUID of the message
     * @return The message
     * @throws IOException If the temp file is not found or if skip throws an Exception
     */
    public Message get(UUID uuid) throws IOException {
        Message returnValue = null;

        if (null == uuidToOnline.get(uuid)) {
            return null;
        }

        if (isOnline(uuid)) {
            returnValue = uuidToMessage.get(uuid);
            Integer numberOfTimesReffernced = uuidToNumberOfTimesReferenced.get(uuid);
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
                fileReader = new FileReader(logfile);
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
     * @throws LtsllcException If there are problems accommodating the new message.  This method also throws the
     *                         exception if the new message's contents are larger than the load limit.
     */
    public synchronized void add(Message newMessage) throws LtsllcException {
        if (newMessage == null) {
            logger.error("null message in add");
            return;
        }
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
     * <p>
     * NOTE: this method expects the message to be in the cache.  It will try to delete the message from
     * offlineMessages.
     *
     * @param uuid The UUID of the message to remove.
     * @throws LtsllcException If there is a problem removing the message.
     */
    public synchronized void remove(UUID uuid) throws LtsllcException, IOException {
        uuidToMessage.remove(uuid);
        uuidToOnline.remove(uuid);
        uuidToLocation.remove(uuid);

        uuidToNumberOfTimesReferenced.remove(uuid);

        if (logfile.exists()) {
            FileReader fileReader = null;
            BufferedReader bufferedReader = null;
            FileWriter fileWriter = null;
            BufferedWriter bufferedWriter = null;

            try {
                fileReader = new FileReader(logfile);
                bufferedReader = new BufferedReader(fileReader);
                ImprovedFile tempfile = logfile.copy();
                fileWriter = new FileWriter(tempfile);
                bufferedWriter = new BufferedWriter(fileWriter);
                for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                    Message readMessage = Message.readLongFormat(line);
                    if (!readMessage.getMessageID().equals(uuid)) {
                        bufferedWriter.write(line);
                        bufferedWriter.newLine();
                    }
                }

                String tempFileName = logfile.toString() + ".backup";
                ImprovedFile tempFile = new ImprovedFile(tempFileName);
                // rename the offLineMessages to a backup file
                if (!logfile.renameTo(tempFile)) {
                    logger.error("Could not rename " + logfile + " to " + tempFile + " aborting.");
                    tempfile.delete();
                    throw new LtsllcException("Could not rename " + logfile + " to " + tempFile);
                }

                // try to rename the tempfile to offLineMessages
                if (!tempfile.renameTo(logfile)) {
                    logger.error("Could not rename " + tempfile + " to " + logfile);
                    logger.error("tempfile," + tempfile + ", may still be around. Aborting");
                    tempfile.delete();
                    throw new LtsllcException("could not rename " + tempfile + " to " + logfile);
                }

                // remove tempFile
                if (!tempFile.delete()) {
                    logger.error("Could not remove " + tempFile + ". Aborting.");
                    throw new LtsllcException("could not remove " + tempFile);
                }

                // we successfully finished!
            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }

                if (fileReader != null) {
                    fileReader.close();
                }

                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }

                if (fileWriter != null) {
                    fileWriter.close();
                }
            }
        }
    }

    /**
     * Put a new message in the cache
     * <p>
     * NOTE: the message must not already be in the cache.
     *
     * @param message The new message.
     * @throws LtsllcException If the "new" message is already in the cache; o if there is a problem putting it in the
     *                         cache.
     */
    public synchronized void putMessage(Message message) throws LtsllcException {
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
    public synchronized void migrateLeastReferencedMessage() throws LtsllcException {
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
     * <p>
     * All the variables, e.g. uuidToOnline, are updated to reflect this.
     *
     * @param message The designated message.  This message must be present in uuidToMessage.
     * @throws LtsllcException The method throws this if there is an error opening offlineMessage or if there is an
     *                         error writing to the file, or the UUID does not exist in uuidToMessage
     */
    protected synchronized void migrateInCoreMessageToOffline(Message message) throws LtsllcException {
        if (uuidToMessage.get(message.getMessageID()) == null) {
            throw new LtsllcException("uuid does not exist in uuidToMessage");
        }
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new FileWriter(logfile.toString(), true);
            bufferedWriter = new BufferedWriter(fileWriter);
            long location = logfile.length();
            bufferedWriter.write(message.longToString());
            bufferedWriter.newLine();
            currentLoad = currentLoad - message.getContents().length;
            uuidToMessage.put(message.getMessageID(), null);
            uuidToOnline.put(message.getMessageID(), false);
            uuidToLocation.put(message.getMessageID(), location);
        } catch (IOException e) {
            throw new LtsllcException("Exception opening or reading message " + message.getMessageID(), e);
        } finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }

                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (IOException e) {
                throw new LtsllcException("Exception trying to close offLineMessages", e);
            }
        }
    }

    /**
     * Read a Message from the offline file
     * <p>
     * NOTE: the file must exist in uuidToLocation for this method to work.
     *
     * @param uuid The message to read.  This UUID must exist in uuidToLocation
     * @return The specified message
     * @throws com.ltsllc.commons.LtsllcException If there is a problem opening the file, or reading the message.
     */
    protected synchronized Message readOfflineMessage(UUID uuid) throws LtsllcException {
        if (uuidToLocation.get(uuid) == null) {
            throw new LtsllcException("message does not exist in uuidToLocation");
        }
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(logfile);
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
                throw new LtsllcException("Exception closing offline file", e);
            }
        }
    }

    /**
     * Migrate an offline message to online
     * <p>
     * The UUID must exist in uuidToLocation.  All the variables, e.g. uuidToMessages are updated to reflect this.
     *
     * @param uuid The Message to read.
     * @throws LtsllcException If the uuid does not exist in uuidToLocation, or there is a problem accessing the
     *                         Message.
     */
    public synchronized void migrateMessageToOnline(UUID uuid) throws LtsllcException {
        if (uuidToLocation.get(uuid) == null) {
            throw new LtsllcException("uuid, " + uuid + ", does not exist in uuidToLocation");
        }

        Message message = readOfflineMessage(uuid);
        while (currentLoad + message.getContents().length > loadLimit) {
            migrateLeastReferencedMessage();
        }

        uuidToMessage.put(uuid, message);
        uuidToLocation.put(uuid, null);
        uuidToOnline.put(uuid, true);
    }

    public synchronized boolean contains(UUID uuid) {
        return uuidToOnline.containsKey(uuid);
    }

    public synchronized void undefineMessage(Message message) {
        uuidToMessage.put(message.getMessageID(), null);
        uuidToLocation.put(message.getMessageID(), null);
        uuidToOnline.put(message.getMessageID(), null);
        uuidToNumberOfTimesReferenced.put(message.getMessageID(), null);
    }

    public synchronized long getLocationFor(UUID uuid) {
        return uuidToLocation.get(uuid);
    }

    public static boolean shouldRecover(ImprovedFile logfile) {
        logger.debug("entering shouldRecover");
        logger.debug("leaving shouldRecover with " + logfile.exists());

        return logfile.exists();
    }

    /**
     * Make a copy of all the messages
     * <p>
     * This method returns an independent list of messages.
     *
     * @return The new list of messages.
     * @throws LtsllcException If there is a problem copying the messages
     * @throws IOException     If these is a problem copying the messages
     */
    public synchronized List<Message> copyAllMessages() throws IOException, LtsllcException {
        logger.debug("entering copyAllMessages");
        ImprovedProperties p = Miranda.getProperties();

        List<Message> list = new ArrayList<>(uuidToOnline.size());

        Collection<Message> onlineMessages = uuidToMessage.values();
        list.addAll(onlineMessages);


        if (logfile.exists()) {
            FileReader fileReader = null;
            BufferedReader bufferedReader = null;
            try {
                fileReader = new FileReader(logfile);
                bufferedReader = new BufferedReader(fileReader);

                for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                    list.add(Message.readLongFormat(line));
                }
            } catch (IOException e) {
                logger.error("Encountered exception while copying messages", e);
                throw new LtsllcException(e);
            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }

                if (fileReader != null) {
                    fileReader.close();
                }
            }
        }
        logger.debug("leaving copyAllMessages");

        return list;
    }

    /**
     * Try to recover from a crash.
     * <p>
     * The message cache looks for the file specified by Miranda.PROPERTY_OFFLINE_MESSAGES and, if it exists, the class
     * tries to take it as its offline messages file.  It cannot recover those messages that were in memory when the
     * crash occurred: those are lost.
     */
    public synchronized void recover(ImprovedFile logfile) {
        if (!logfile.exists()) {
            logger.warn("asked to recover when the file, " + logfile + ", does not exist");
        } else {
            this.logfile = logfile;
        }
    }

    public synchronized void removeNulls() {
        clearMapping(uuidToMessage);
        clearMapping(uuidToLocation);
        clearMapping(uuidToOnline);
        clearMapping(uuidToNumberOfTimesReferenced);
    }

    public static void clearMapping(Map map) {
        Set set = map.keySet();
        for (
                Object key : set) {
            if (map.get(key) == null) {
                map.put(key, null);
            }
        }
    }
}
