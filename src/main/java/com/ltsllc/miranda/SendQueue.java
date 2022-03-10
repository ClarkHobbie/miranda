package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.miranda.cluster.MessageCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * The messages that this node is responsible for delivering
 */
public class SendQueue {
    protected static SendQueue instance;

    static {
        try {
            instance = new SendQueue();
        } catch (LtsllcException e) {
            throw new RuntimeException("encountered exception while in static block", e);
        }
    }

    public static final Logger logger = LogManager.getLogger();


    /**
     * The messages in the sendQueue
     */
    protected MessageCache messageCache = null;

    public MessageCache getMessageCache() {
        return messageCache;
    }

    public void setMessageCache(MessageCache messageCache) {
        this.messageCache = messageCache;
    }

    public static SendQueue getInstance() {
        return instance;
    }

    public static void setInstance(SendQueue instance) {
        SendQueue.instance = instance;
    }

    protected SendQueue () throws LtsllcException {
        messageCache = new MessageCache();
    }
     /**
     * Define the classes static variables
     *
     * This is essentially an initialize for static members.  In particular, it defines the send queue file.
     */
    public static synchronized void defineStatics () throws LtsllcException {
        instance = new SendQueue();
        instance.messageCache = new MessageCache();
    }

    /**
     * Record the message in the file
     *
     * @param message The message to record.
     * @throws IOException If there was a problem manipulating the file.
     */
    public synchronized void record (Message message) throws LtsllcException {
        messageCache.add(message);
    }

    /**
     * Return whether this method thinks we should recover
     *
     * This method thinks we should recover if the send file exists.
     *
     * @return See above.
     */
    public static boolean shouldRecover () {
        ImprovedProperties p = Miranda.getProperties();
        ImprovedFile sendFile = new ImprovedFile(p.getProperty(Miranda.PROPERTY_SEND_FILE));
        return sendFile.exists();
    }

    /**
     * Make a copy of the list of messages
     *
     * This method makes a duplicate of the list of messages that the class maintains and returns it.  The caller can
     * then iterate over the list while adding and removing elements of the list without getting a
     * ConcurrentModification exception.
     *
     * @return The copy of the list
     */
    public synchronized List<Message> copyMessages () throws LtsllcException, IOException {
        return messageCache.copyAllMessages();
    }

    /**
     * Recover the send queue from the file
     */
    public static void recover () throws LtsllcException, IOException {
        getInstance().instanceRecover();
    }

    public void instanceRecover () {
        messageCache.recover();
    }

    /**
     * Add a message to the send queue
     *
     * This method take care of all the record-keeping associated with messages.
     *
     * @param message The message to add.
     */
    public synchronized void add(Message message) throws LtsllcException {
        messageCache.add(message);
    }

    /**
     * Remove a message from the send queue.
     *
     * @param message The message to remove.  This doesn't have to exist in the class.
     */
    public synchronized void remove(Message message) throws LtsllcException, IOException {
        messageCache.remove(message.getMessageID());
    }

    /**
     * Clear the entire object
     *
     * @throws LtsllcException Thrown if there is a problem
     */
    public synchronized void clearAll () throws LtsllcException {
        messageCache = new MessageCache();
    }

    /**
     * Remove any null messages from the list
     */
    public synchronized void removeNulls () {
        messageCache.removeNulls();
    }
}









