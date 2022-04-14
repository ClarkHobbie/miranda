package com.ltsllc.miranda.logging;

import com.ltsllc.commons.io.ImprovedFile;

import java.io.*;
import java.util.*;

/**
 * A map that logs all its items to a file
 */
public class LoggingMap {
    /**
     * The file where the UUIDs are logged
     */
    protected ImprovedFile file;

    /**
     * A map from message ID to the UUID of the owner of that message
     */
    protected Map<UUID, UUID> uuidToUuid = new HashMap<>();

    public Map<UUID, UUID> getUuidToUuid() {
        return uuidToUuid;
    }

    public void setUuidToUuid(Map<UUID, UUID> uuidToUuid) {
        this.uuidToUuid = uuidToUuid;
    }

    public ImprovedFile getFile() {
        return file;
    }

    public void setFile(ImprovedFile file) {
        this.file = file;
    }

    public LoggingMap(String filename) {
        file = new ImprovedFile(filename);
    }

    /**
     * Construct a new instance of the class
     * <P>
     *     The method makes no check to see if the file already exists.
     * </P>
     * @param file The file where the UUIDs will be logged.
     */
    public LoggingMap (File file) {
        this.file = new ImprovedFile(file);
    }

    /**
     * Add a message/owner relationship to the object
     *
     * This method will add the relationship to the logfile then add it to the in memory map.
     *
     * @param message The UUID for the message.
     * @param owner The UUID for the owner.
     * @throws IOException If there is a problem writing the relationship to the logfile.
     */
    public synchronized void add (UUID message, UUID owner) throws IOException {
        //
        // first log it
        //
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;

        try {
            fileWriter = new FileWriter(file, true);
            bufferedWriter = new BufferedWriter(fileWriter);

            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append (message.toString());
            stringBuffer.append (" ");
            stringBuffer.append (owner.toString());

            bufferedWriter.write(stringBuffer.toString());
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
        // then add it to the map
        //
        uuidToUuid.put(message, owner);
    }

    /**
     * Get the owner of a message
     *
     * @param message The UUID for the message for which the caller wants to know the owner UUID.
     * @return The owner UUID for the message or null if we do not know.
     */
    public synchronized UUID get (UUID message) {
        return uuidToUuid.get(message);
    }

    /**
     * Go through the logfile and copy only those messageIDs that exist in uuidToUuid
     */
    public synchronized void compact () throws IOException {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;

        ImprovedFile newFile = null;

        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);

            String oldFileName = file.toString();
            newFile = new ImprovedFile(file.toString() + ".temp");
            fileWriter = new FileWriter(newFile);
            bufferedWriter = new BufferedWriter(fileWriter);

            String line = bufferedReader.readLine();
            while (line != null) {
                Scanner scanner = new Scanner(line);
                UUID message = UUID.fromString(scanner.next());
                if (uuidToUuid.containsKey(message)) {
                    bufferedWriter.write(line);
                    bufferedWriter.newLine();
                } else {
                    remove (message);
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
        newFile.renameTo(file);
    }

    /**
     * Remove an association from memory
     */
    public synchronized void remove (UUID uuid) {
        uuidToUuid.remove(uuid);
    }

    /**
     * Does the object think we should recover?
     *
     * The method returns true if the logfile exists
     *
     * @return true if the object thinks we should recover false otherwise
     */
    public boolean shouldRecover () {
        if (file.exists()) {
            return true;
        }

        return false;
    }

    /**
     * Recover from a crash
     *
     * This method reads the logfile into memory.  Note that this method assumes that there is a logfile to recover
     * from.  A recover should immediately be followed by a compaction but this is not a requirement.
     */
    public synchronized void recover () throws IOException {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;

        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);

            String line = bufferedReader.readLine();
            while (line != null) {
                Scanner scanner = new Scanner(line);
                UUID message = UUID.fromString(scanner.next());
                UUID owner = UUID.fromString(scanner.next());
                uuidToUuid.put(message, owner);

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
     * Return the collection of all keys to the map
     *
     * @return The collection of all keys
     */
    public synchronized Collection<UUID> getAllKeys () {
        return uuidToUuid.keySet();
    }

    /**
     * Get a collection of all values
     */
    public synchronized Collection<UUID> values () {
        return uuidToUuid.values();
    }
}
