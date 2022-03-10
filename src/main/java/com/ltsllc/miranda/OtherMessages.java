package com.ltsllc.miranda;

import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A file that contains messages we are not responsible for delivering.
 */
public class OtherMessages {
    protected static OtherMessages instance = new OtherMessages();

    protected ImprovedFile file;
    protected Map<UUID, Long> uuidToLocation = new HashMap<>();
    protected Map<UUID,UUID> uuidToOwner = new HashMap<>();
    protected long offset = 0;

    /**
     * Define the classes static variables
     *
     * This is essentially an initialize for static members.  In particular, it
     */
    public static synchronized void defineStatics () {
        instance.file = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OTHER_MESSAGES));
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public Map<UUID, UUID> getUuidToOwner() {
        return uuidToOwner;
    }

    public void setUuidToOwner(Map<UUID, UUID> uuidToOwner) {
        this.uuidToOwner = uuidToOwner;
    }

    /**
     * Get the location for a UUID
     *
     * NOTE: this method returns -1 in the case where the location is undefined.
     *
     * @param uuid The UUID the caller wants a location for.
     * @return The location that the caller wants a location for or -1 if the location is undefined.
     */
    public long getLocationFor (UUID uuid) {
        Long location = uuidToLocation.get(uuid);
        if (null == location) {
            return -1;
        }

        return location.longValue();
    }

    public Map<UUID, Long> getUuidToLocation() {
        return uuidToLocation;
    }

    public void setUuidToLocation(Map<UUID, Long> uuidToLocation) {
        this.uuidToLocation = uuidToLocation;
    }

    public ImprovedFile getFile() {
        return file;
    }

    public void setFile(ImprovedFile file) {
        this.file = file;
    }

    public static OtherMessages getInstance() {
        return instance;
    }

    public static void setInstance(OtherMessages instance) {
        OtherMessages.instance = instance;
    }

    /**
     * Record the message in the file
     *
     * @param message The message to record.
     * @param uuid The owner of the message.
     * @throws IOException If there was a problem manipulating the file.
     */
    public synchronized void record (Message message, UUID uuid) throws IOException {
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;

        try {
            uuidToLocation.put(message.getMessageID(), offset);
            recordOwner(message.getMessageID(), uuid);

            recordOwner (message.getMessageID(), uuid);

            file = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OTHER_MESSAGES));
            fileWriter = new FileWriter(file.toString(), true);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(message.longToString());
            bufferedWriter.newLine();

            offset = file.length();
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }

            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }

    /**
     * Read a message from a certain location
     *
     * Note that the method throws an exception rather than returning a value.
     *
     * @param location Where to read.
     * @return The message at that location.
     * @throws IOException If there is a problem manipulating the file.
     */
    public Message read (long location) throws IOException {
        Message returnValue = null;
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            file = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_OTHER_MESSAGES));
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            bufferedReader.skip(location);
            String inline = bufferedReader.readLine();
            returnValue = Message.readLongFormat(inline);
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }

            if (fileReader != null) {
                fileReader.close();
            }
        }

        return returnValue;
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
}
