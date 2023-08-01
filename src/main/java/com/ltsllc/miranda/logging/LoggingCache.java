package com.ltsllc.miranda.logging;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.UncheckedLtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.alarm.AlarmClock;
import com.ltsllc.miranda.alarm.Alarmable;
import com.ltsllc.miranda.alarm.Alarms;
import com.ltsllc.miranda.message.Message;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * An object that records objects as they come into the class, keeps a portion of those objects in memory, and recovers
 * itself when asked.
 *
 * <P>
 * A logging cache logs all objects that it takes ownership of to a file.  It then keeps some of those objects in memory
 * using a most frequently used algorithm.  Finally, the class can rebuild itself from a past file.
 * </P>
 */
public class LoggingCache implements Alarmable{
    public static final Logger logger = LogManager.getLogger(LoggingCache.class);

    /**
     * The file where it logs to
     */
    protected ImprovedFile file;

    /**
     * A message's UUID to the message itself the "main" part of the cache
     */
    protected TreeMap<UUID, Message> uuidToMessage = new TreeMap<>();

    /**
     * A map from a message's UUID to the location in the file where it can be found
     */
    protected Map<UUID, Long> uuidToLocation = new HashMap<>();

    /**
     * A map from a message's UUID to whether or not it is in memory.  If a message's UUID is not in the map then the
     * cache doesn't know anything about it.
     */
    protected Map<UUID, Boolean> uuidToInMemory = new HashMap<>();

    /**
     * A map from a message's UUID to the number of times it has been referenced
     */
    protected Map<UUID, Integer> uuidToTimesReferenced = new HashMap<>();

    /**
     * The maximum number of contents bytes that the cache may hold.  If the cache tries to store more than this then the
     * cache will start leaving least referenced message on disk rather than in memory.
     */
    protected int loadLimit = -1;

    /**
     * How many bytes of contents the cache currently has loaded
     */
    protected int currentLoad = 0;

    /**
     * Construct a new instance of the class
     *
     * @param logfile The file where new messages are logged.
     * @param loadLimit The total number of bytes of contents the instance can hold.  Attempts to go beyond this limit
     *                  will result in the lesser accessed messages being taken our of memory.
     */
    public LoggingCache (ImprovedFile logfile, int loadLimit) {
        file = logfile;
        this.loadLimit = loadLimit;
    }

    public int getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(int currentLoad) {
        this.currentLoad = currentLoad;
    }

    public int getLoadLimit() {
        return loadLimit;
    }

    public void setLoadLimit(int loadLimit) {
        this.loadLimit = loadLimit;
    }

    public Map<UUID, Integer> getUuidToTimesReferenced() {
        return uuidToTimesReferenced;
    }

    public void setUuidToTimesReferenced(Map<UUID, Integer> uuidToTimesReferenced) {
        this.uuidToTimesReferenced = uuidToTimesReferenced;
    }

    public Map<UUID, Boolean> getUuidToInMemory() {
        return uuidToInMemory;
    }

    public void setUuidToInMemory(Map<UUID, Boolean> uuidToInMemory) {
        this.uuidToInMemory = uuidToInMemory;
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

    public void setUuidToMessage(TreeMap<UUID, Message> uuidToMessage) {
        this.uuidToMessage = uuidToMessage;
    }

    public ImprovedFile getFile() {
        return file;
    }

    public void setFile(ImprovedFile file) {
        this.file = file;
    }


    /**
     * return if the value is outside the cache
     *
     * @param value the value
     * @return whether the value is outside the allowable range
     */
    public boolean outOfBounds (int value) {
        if ((value > -1) && (value < uuidToInMemory.size())) {
            return false;
        }
        else {
            return true;
        }
    }


    /**
     * Take ownership of a message.
     *
     * @param message The message to take ownership of.
     * @throws IOException If there is a problem adding the message.
     */
    public synchronized void add (Message message) throws IOException {
        //
        // first add the message to the file
        //
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        Long location = null;

        try {
            location = file.length();
            fileWriter = new FileWriter(file, true);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(message.longToString());
            bufferedWriter.newLine();
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }

            if (fileWriter != null) {
                fileWriter.close();
            }
        }

        //
        // if we will take too much space then migrate some messages to disk
        //
        if (currentLoad + message.getContents().length > loadLimit) {
            migrateLeastReferencedMessagesToDisk(loadLimit - message.getContents().length);
        }

        //
        // then add it to the cache
        //
        currentLoad += message.getContents().length;
        uuidToMessage.put (message.getMessageID(), message);
        uuidToLocation.put (message.getMessageID(), location);
        uuidToInMemory.put (message.getMessageID(), true);
        uuidToTimesReferenced.put (message.getMessageID(), 0);
    }

    public void forceInMemory(Message message) {
        uuidToMessage.put(message.getMessageID(), message);
        uuidToInMemory.put(message.getMessageID(), true);
        uuidToTimesReferenced.put(message.getMessageID(), 0);
    }


    /**
     * get the message, whether it is in memory or on the disk.
     *
     * @param uuid The UUID of the message
     * @return The message
     */
    public synchronized Message get (UUID uuid) throws LtsllcException, IOException {
        //
        // if we don't know of the message then say so
        //
        if (uuidToLocation.get(uuid) == null) {
            return null;
        }

        //
        // if the message is not in memory, then load it
        //
        Message message = null;
        if (!uuidToInMemory.get(uuid)) {
            loadMessage(uuid);
        }

        //
        // increment the number of times referenced
        //
        int numberOfTimesReferenced = uuidToTimesReferenced.get(uuid);
        numberOfTimesReferenced++;
        uuidToTimesReferenced.put (uuid, numberOfTimesReferenced);

        //
        // finally, return the message
        //
        return uuidToMessage.get(uuid);
    }

    public static class CopyMessagesResult {
        public List<Message> list;
        public int restartIndex;
    }
    /**
     * Copy messages up to some limit
     *
     * @param limit The number of .content.length in the list
     * @return The resulting list, and the restartIndexIn value for the next call
     */
    public synchronized CopyMessagesResult copyMessages (int limit,int restartIndexIn) {
        CopyMessagesResult temp = new CopyMessagesResult();
        temp.list = new ArrayList<>();

        Iterator<UUID> iter = uuidToMessage.keySet().iterator();

        int spaceUsed = 0;
        int index = 0;

        //
        // skip to restartIndexIn
        //
        while (iter.hasNext() && (restartIndexIn != index)) {
            index++;
            temp.restartIndex++;
            iter.next();
        }

        //
        // if we hit the end element then signal by returning null
        //
        if (!iter.hasNext()) {
            return null;
        }

        //
        // otherwise start copying
        //
        while (iter.hasNext()) {
            UUID u = iter.next();
            temp.restartIndex++;
            Message m = uuidToMessage.get(u);

            if ((spaceUsed + m.getContents().length) <= limit) {
                temp.list.add(m);
                temp.restartIndex = -1;
                spaceUsed += m.getContents().length;
            } else {
                break;
            }
        }

        temp.restartIndex++;
        return temp;
    }

    /**
     * Move the least referenced message to disk
     */
    public synchronized void migrateLeastReferencedMessagesToDisk(int desiredLoad) {
        //
        // sort the cache by number of times referenced
        //
        List<Message> list = new ArrayList<>(uuidToMessage.size());

        int index = 0;
        for (Message message : uuidToMessage.values()) {
            list.add(index, message);
        }

        Collections.sort(list, new TimesReferencedComparator(uuidToTimesReferenced));

        while (currentLoad > desiredLoad) {
            Message message = list.get(list.size() - 1);
            moveMessageToDisk(message);
        }
    }

    /**
     * Move a message to disk.
     *
     * Since we log all messages that we take ownership of, there is very little to do.  This is mostly a bookkeeping
     * action.
     *
     * @param message The message to be moved.
     */
    public synchronized void moveMessageToDisk(Message message) {
        uuidToMessage.remove(message.getMessageID());
        uuidToInMemory.put(message.getMessageID(), false);

        currentLoad -= message.getContents().length;
    }

    /**
     * Load a message from disk to memory
     *
     * This method affects the currentLoad and uuidToMessage.
     *
     * @param uuid The uuid of the message to load
     * @throws LtsllcException This method throws this exception if it doesn't recognize the UUID.
     * @throws IOException If there is a problem loading the message.
     */
    public synchronized Message loadMessage (UUID uuid) throws LtsllcException, IOException {
        Long location = uuidToLocation.get(uuid);

        if (null == location) {
            throw new LtsllcException("message not found in loadMessage");
        }

        Message message = null;
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            bufferedReader.skip(location);
            String line = bufferedReader.readLine();
            message = Message.readLongFormat(line);
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }

            if (fileReader != null) {
                fileReader.close();
            }
        }

        currentLoad += message.getContents().length;
        if (currentLoad > loadLimit) {
            
        }
        uuidToInMemory.put (uuid, true);
        uuidToMessage.put (uuid, message);
        return message;
    }

    /**
     * Make it so that this object has no record of the message
     *
     * @param uuid The message to be removed
     */
    public synchronized void remove (UUID uuid) {
        if (null == uuidToLocation.get(uuid)) {
            return;
        }

        Message message = uuidToMessage.get(uuid);
        uuidToMessage.remove(uuid);
        if (message != null) {
            currentLoad -= message.getContents().length;
        }

        uuidToInMemory.remove(uuid);
        uuidToTimesReferenced.remove(uuid);
        uuidToLocation.remove(uuid);
    }

    /**
     * Compact the logfile by keeping out the messages not in the system
     * <P>
     *     A message is not part of the system if it does not appear in any of the uuidTo variables, which happens when
     *     a message is delivered.
     * <P>
     *     This method works by going through the logfile and copying those messages that are referenced elsewhere to
     *     another, temporary file.  The original logfile is then deleted and then the temporary file is renamed to the
     *     logfile.
     *
     * @throws IOException If there is a problem reading or writing a file.
     */
    public synchronized void compact () throws IOException {
        logger.debug("entering compact");

        ImprovedFile messages = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_MESSAGE_LOG));
        if (!messages.exists()) {
            logger.debug("message log, " +
                    messages
                    + " does not exist, returning");
            return;
        }

        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        String outputFileName = file.toString() + ".temp";
        ImprovedFile outputFile = new ImprovedFile(outputFileName);

        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            fileWriter = new FileWriter(outputFile);
            bufferedWriter = new BufferedWriter(fileWriter);

            String line = bufferedReader.readLine();
            while (line != null) {
                Message message = Message.readLongFormat(line);
                if (null != uuidToLocation.get(message.getMessageID())) {
                    long location = file.length();
                    uuidToLocation.put(message.getMessageID(), location);
                    bufferedWriter.write(line);
                    bufferedWriter.newLine();
                }

                line = bufferedReader.readLine();
            }
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

        file.delete();
        outputFile.renameTo(file);

        logger.debug("leaving compact");
    }

    /**
     * Return true if we should recover.
     *
     * The rule for recovery is that, if the logfile exists, then we should recover.
     *
     * @return true if we should recover, false otherwise.
     */
    public boolean shouldRecover () {
        return file.exists();
    }

    /**
     * Recover from a crash
     *
     * <P>
     * This method reads in the logfile and then deletes it.
     * <P>
     * A recover should immediately followed by a compaction.
     * <P>
     * This method depends on the ability to get a location in
     * the file where an entry takes place.  Unfortunately, Java doesn't provide that capability in a BufferedReader or
     * a FileReader, but it sort of does in an InputStream.  Hence, the use of FileInputStream and InputStreamReader.
     */
    public synchronized void recover () throws IOException, LtsllcException {

        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader bufferedReader = null;

        try {
            String oldFileName = file.toString();
            String newFileName = oldFileName + ".backup";
            ImprovedFile newFile = new ImprovedFile(newFileName);
            file.copyTo(newFile);
            file.delete();

            fis = new FileInputStream(newFile);
            isr = new InputStreamReader(fis);
            bufferedReader = new BufferedReader(isr);

            long location = 0; // the location within the file of the current entry
            long lastLocation = 0; // the location within the file of the previous entry

            String line = bufferedReader.readLine();
            while (null != null) {
                Message message = Message.readLongFormat(line);
                location = lastLocation;
                lastLocation = fis.getChannel().position();

                uuidToLocation.put (message.getMessageID(), location);
                uuidToMessage.put (message.getMessageID(), message);
                uuidToTimesReferenced.put (message.getMessageID(), 0);
                uuidToInMemory.put (message.getMessageID(), true);
            }
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }

            if (isr != null) {
                isr.close();
            }

            if (fis != null) {
                fis.close();
            }
        }
    }

    public boolean contains(UUID uuid) {
        return (null != uuidToLocation.get(uuid));
    }

    /**
     * Make a copy of all the messages the class "knows about."
     *
     * <P>
     *     This method makes a copy of all the messages, in memory or on disk, that the object "knows about."  That is,
     *     all the messages that have been passed via add.
     * </P>
     * <P>
     *     This method makes no check to see that all the messages can fit into memory, so care must be taken when
     *     calling it.
     * </P>
     * @return A list of all the messages.
     * @throws IOException If there is a problem relating to any on-disk messages.
     */
    public synchronized List<Message> copyAllMessages () throws IOException {
        List<Message> list = new ArrayList<>();
        if (allInMemory()) {
            list.addAll(uuidToMessage.values());
        } else {
            if (!file.exists()) {
                return new ArrayList<>();
            }

            FileReader fileReader = null;
            BufferedReader bufferedReader = null;
            try {
                fileReader = new FileReader(file);
                bufferedReader = new BufferedReader(fileReader);
                String line = bufferedReader.readLine();
                while (line != null) {
                    Message message = Message.readLongFormat(line);
                    list.add(message);
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

        return list;
    }

    public synchronized boolean isInMemory (UUID uuid) {
        return uuidToInMemory.get(uuid);
    }

    /**
     * Remove a message from the cache
     * <P>
     *     Thereafter, the object will not "know" about the message.
     * </P>
     * <P>
     *     This method does not check to see that it contains the message.
     * </P>
     * @param uuid The UUID of the message to be removed.
     */
    public synchronized void undefine (UUID uuid) {
        uuidToMessage.remove(uuid);
        uuidToLocation.remove(uuid);
        uuidToTimesReferenced.remove(uuid);
        uuidToInMemory.remove(uuid);
    }

    /**
     * Are all of the messages in memory?
     *
     * @return Whether all of the messages are in memory.
     */
    public boolean allInMemory () {
        for (Boolean b : uuidToInMemory.values()) {
            if (!b) {
                return false;
            }
        }

        return true;
    }

    public long getLocationFor (UUID message) {
        Long l = uuidToLocation.get(message);
        if (l == null) {
            return -1;
        } else {
            return l.longValue();
        }
    }

    /**
     * Return a collection of all the message UUIDs in the object
     *
     * @return A collection of all the message UUIDs in the object
     */
    public synchronized Collection<Message> getAllMessages () {
        return uuidToMessage.values();
    }

    /**
     * An alarm which the class previously registered for went off
     *
     * @param alarm The alarm that the class previously registered for.
     * @throws IOException If the method (this method calls the compact method) this method calls throws an
     *                     exception.
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

    public void setupCompaction() {
        AlarmClock.getInstance().scheduleOnce(this, Alarms.COMPACTION,
                Miranda.getProperties().getLongProperty(Miranda.PROPERTY_COMPACTION_TIME));

    }


    public void loadMessageAndRebalance (Message message) throws IOException {
        if (uuidToInMemory.get(message.getMessageID()) == null) {
            add(message);
        }

        uuidToMessage.put (message.getMessageID(), message);
        uuidToInMemory.put (message.getMessageID(), true);

        currentLoad += message.getContents().length;

        if (currentLoad > loadLimit) {
            migrateLeastReferencedMessagesToDisk(loadLimit);
        }
    }
}
