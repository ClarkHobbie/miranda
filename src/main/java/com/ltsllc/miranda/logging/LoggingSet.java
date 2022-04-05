package com.ltsllc.miranda.logging;

import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.Message;

import java.io.*;
import java.util.*;

/**
 * A set that logs its members
 */
public class LoggingSet {
    /**
     * The logfile for the class
     */
    protected ImprovedFile file;

    /**
     * The actual set
     */
    protected Set<Message> set = new HashSet<>();

    /**
     * Construct a new instance of the class
     * <P>
     *     Note that no check is made to ensure that the does not already exist.
     * </P>
     * @param logfile Where the messages will be stored.
     */
    public LoggingSet (ImprovedFile logfile) {
        file = logfile;
    }

    public ImprovedFile getFile() {
        return file;
    }

    public void setFile(ImprovedFile file) {
        this.file = file;
    }

    public Set<Message> getSet() {
        return set;
    }

    public void setSet(Set<Message> set) {
        this.set = set;
    }

    public synchronized boolean contains (Message message) {
        return set.contains(message);
    }

    /**
     * Log the message to the classes logfile and add it to the set
     *
     * @param message The message to be added.
     * @return Whether the message was already in the set.  Note that a return value of true means that the logfile will
     * contain two copies of the message.
     * @throws IOException If there is a problem logging the message.
     */
    public synchronized boolean add (Message message) throws IOException {
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new FileWriter(file, true);
            bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write(message.longToString());
            bufferedWriter.newLine();
            return set.add(message);
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
     * Remove a message from the set as well as the logfile
     *
     * <P>
     *     In the case where a message is in the logfile more than once; ALL instances of the message are removed from
     *     the logfile.
     * </P>
     * <P>
     *     The method works by first removing the backup file.  The backup file has the same name as the logfile,
     *     with ".backup" appended to it.  It then renames the logfile to the backup file, and then copies the contents
     *     of the backup, minus the removed element, to the logfile.
     * </P>
     *
     * @param message The message to be removed.
     * @return true if the message was in the set, false otherwise.
     * @throws IOException If there is a problem when removing the message from the logfile.
     */
    public synchronized boolean remove (Message message) throws IOException {
        ImprovedFile backup = new ImprovedFile(file.getName() + ".backup");
        if (backup.exists()) {
            backup.delete();
        }

        file.renameTo(backup);

        FileReader fileReader = null;
        FileWriter fileWriter = null;
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileReader = new FileReader(backup);
            bufferedReader = new BufferedReader(fileReader);
            fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);

            String line = bufferedReader.readLine();
            while (line != null) {
                Message temp = Message.readLongFormat(line);

                //
                // only write a message to the new file if the line represents a message that is NOT being removed
                //
                if (!temp.equals(message)) {
                    bufferedWriter.write (line);
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

        return set.remove(message);
    }


    /**
     * Is the set empty?
     *
     * @return true if the set is empty, false otherwise.
     */
    public synchronized boolean isEmpty () {
        return set.isEmpty();
    }

    /**
     * Should the recover method be called?
     *
     * <P>
     *     Basically, return true if the logfile or a backup exists.
     * </P>
     *
     * @param logfile The logfile and the backup file that are checked.  In this case, the backup file name is just the
     *                logfile name followed by ".backup"
     * @return True if recover should be called, false otherwise.
     * @see #recover(ImprovedFile)
     */
    public static boolean shouldRecover (ImprovedFile logfile) {
        ImprovedFile backup = new ImprovedFile(logfile.getName() + ".backup");
        return logfile.exists() || backup.exists();
    }

    /**
     * Recover from a crash
     *
     * <P>
     *     It is suggested but not required for the caller to have already called shouldRecover.
     * </P>
     *
     * <P>
     *     Recovery just consists of loading the set members into memory.  In the case where both exist, use the backup
     *     file.  The logfile may be incomplete or otherwise damaged, but the backup is just the contents of the set.
     *     The backup file is created when we are about to copy the contents of the logfile, minus the removed
     *     message, to the logfile.
     * </P>
     *
     * @param logfile The logfile and backup file to be used in a recovery.  The name of the backup file is just the
     *                name of the logfile with ".backup" appended to it.
     * @throws IOException If there is a problem accessing the logfile.
     */
    public static LoggingSet recover (ImprovedFile logfile) throws IOException {
        ImprovedFile theLogfile = new ImprovedFile(logfile);
        ImprovedFile backup = new ImprovedFile(theLogfile.getName() + ".backup");
        LoggingSet returnValue = null;

        if (theLogfile.exists() && !backup.exists()) {
            FileReader fileReader = null;
            BufferedReader bufferedReader = null;
            Set<Message> set = new HashSet<>();

            try {
                fileReader = new FileReader(theLogfile);
                bufferedReader = new BufferedReader(fileReader);
                String line = bufferedReader.readLine();
                while (line != null) {
                    Message message = Message.readLongFormat(line);
                    set.add(message);

                    line = bufferedReader.readLine();
                }

                returnValue = new LoggingSet(theLogfile);
                returnValue.addAll(set);
            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }

                if (fileReader != null) {
                    fileReader.close();
                }
            }
        } else if (!logfile.exists() && !backup.exists()) {
            //
            // another pathological case: what are doing here?
            //
            returnValue = null;
        } else if (!logfile.exists() && backup.exists()) {
            //
            // this means that we were going to remove something --- but right after we renamed the logfile to the
            // backup file the system crashed.  In this case rename the backup file to the logfile and load the logfile
            // into memory
            //
            backup.renameTo(logfile);
            returnValue = recover(logfile);
        } else if (logfile.exists() && backup.exists()) {
            logfile.delete();
            backup.renameTo(logfile);
            returnValue = recover(logfile);
        }

        return returnValue;
    }

    /**
     * Add all the elements of a collection
     *
     * @param collection The collection to add.
     * @throws IOException If a problem is encountered with the logfile while adding an element.
     */
    public synchronized void addAll (Collection<Message> collection) throws IOException {
        Iterator<Message> iterator = collection.iterator();
        while (iterator.hasNext()) {
            Message message = iterator.next();
            add(message);
        }
    }
}
