package com.ltsllc.miranda.logcache;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.Message;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A list that saves its elements to a logfile
 */
public class LoggingList {
    /**
     * The logfile
     */
    protected ImprovedFile file;

    /**
     * The elements
     */
    protected List<Message> list = new ArrayList<>();

    public LoggingList (ImprovedFile logfile) {
        file = logfile;
    }

    public ImprovedFile getFile() {
        return file;
    }

    public void setFile(ImprovedFile file) {
        this.file = file;
    }

    public List<Message> getList() {
        return list;
    }

    public void setList(List<Message> list) {
        this.list = list;
    }

    /**
     * Add an element to the list, first logging the element to the logfile
     *
     * @param message The message to be added.
     * @throws IOException If an error occurs while manipulating the logfile
     */
    public synchronized void add (Message message) throws IOException {
        list.add(message);

        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;

        try {
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
    }

    /**
     * Remove a message from the list and the logfile
     *
     * <P>
     *     Note that this method works by renaming the logfile to "&lt;logfile&gt;.backup" and then creating a new
     *     logfile that does not contain the message.
     * </P>
     *
     * @param message The message to be removed.
     * @return Whether the element was in the list.
     * @throws IOException If there was a problem with the logfile.
     */
    public synchronized boolean remove (Message message) throws IOException {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;

        try {
            ImprovedFile temp = new ImprovedFile(file.getName() + ".backup");
            file.renameTo(temp);
            fileReader = new FileReader(temp);
            fileWriter = new FileWriter(file);
            bufferedReader = new BufferedReader(fileReader);
            bufferedWriter = new BufferedWriter(fileWriter);

            String line = bufferedReader.readLine();
            while (line != null) {
                Message fileMessage = Message.readLongFormat(line);
                if (!fileMessage.equals(message)) {
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

        return list.remove(message);
    }

    /**
     * Get an element from the list
     *
     * <P>
     *     The method throws IndexOutOfBoundsException if the index is outside the range 0..<=size
     * </P>
     * @param index The index of the element to get.
     * @return The element at that location.
     */
    public synchronized Message get (int index) {
        return list.get(index);
    }

    /**
     * Should the caller try to recover?
     *
     * <P>
     *     A caller should recover if the logfile for the class already exists.
     * </P>
     * @param recoveryFile The logfile to be used.
     * @return Whether the caller should recover.
     */
    public static boolean shouldRecover (ImprovedFile recoveryFile) {
        return recoveryFile.exists();
    }

    /**
     * Try to recover the list
     *
     * <P>
     *     This method tries to recover a list from its logfile.  It simply reads the logfile into memory.
     * </P>
     * @param recoveryFile The logfile to recover from.
     * @return The list corresponding to the logfile.
     * @throws LtsllcException If there is a problem copying the file to the backup file.
     * @throws IOException If there is a problem reading the logfile.
     */
    public static LoggingList recover (ImprovedFile recoveryFile) throws  LtsllcException, IOException {
        ImprovedFile temp = new ImprovedFile(recoveryFile);
        LoggingList newList = new LoggingList(recoveryFile);
        newList.file = temp;

        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        List<Message> list = new ArrayList<>();

        try {
            ImprovedFile backup = new ImprovedFile(recoveryFile.getName() + ".backup");
            recoveryFile.copyTo(backup);
            fileReader = new FileReader(recoveryFile);
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
        newList.list = list;
        return newList;
    }

    /**
     * Is the LoggingList empty?
     *
     * @return true if the LoggingList is empty, false otherwise
     */
    public synchronized boolean isEmpty () {
        return list.isEmpty();
    }
}
