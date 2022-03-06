package com.ltsllc.miranda;

import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.commons.util.ImprovedProperties;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The messages that this node is responsible for delivering
 */
public class SendQueueFile {
    protected static SendQueueFile instance;

    /**
     * The file where the messages are stored
     */
    protected ImprovedFile file;

    /**
     * A map from message uuid to the location where it is stored in the file
     */
    protected Map<UUID, Long> uuidToLocation = new HashMap<>();

    /**
     * The offset within the file where new messages go
     */
    protected long offset = 0;

    public static SendQueueFile getInstance() {
        return instance;
    }

    public static void setInstance(SendQueueFile instance) {
        SendQueueFile.instance = instance;
    }

    public ImprovedFile getFile() {
        return file;
    }

    public void setFile(ImprovedFile file) {
        this.file = file;
    }

    public Map<UUID, Long> getUuidToLocation() {
        return uuidToLocation;
    }

    public void setUuidToLocation(Map<UUID, Long> uuidToLocation) {
        this.uuidToLocation = uuidToLocation;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    /**
     * Define the classes static variables
     *
     * This is essentially an initialize for static members.  In particular, it defines the send queue file.
     */
    public static synchronized void defineStatics () {
        instance = new SendQueueFile();
        instance.file = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_SEND_FILE));
    }

    /**
     * Get the location for a message
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

    /**
     * Record the message in the file
     *
     * @param message The message to record.
     * @throws IOException If there was a problem manipulating the file.
     */
    public synchronized void record (Message message) throws IOException {
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;

        try {
            uuidToLocation.put(message.getMessageID(), offset);

            file = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_SEND_FILE));
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
}
