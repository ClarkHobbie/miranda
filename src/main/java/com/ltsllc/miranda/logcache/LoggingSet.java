package com.ltsllc.miranda.logcache;

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
     *
     * <P>
     *     Note that a TreeSet is used instead of a HashSet.  This is because you can have two objects be equals
     *     equivalent but not hashCode equivalent.  TreeSet uses Compareable whereas HashSet uses hashCode.
     * </P>
     */
    protected Set<Message> set = new TreeSet<>();

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

    public synchronized boolean remove (Message message) throws IOException {
        ImprovedFile backup = new ImprovedFile(file.getName() + ".backup");
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

    public synchronized boolean isEmpty () {
        return set.isEmpty();
    }

    public static boolean shouldRecover (ImprovedFile logfile) {
        ImprovedFile backup = new ImprovedFile(logfile.getName() + ".backup");
        return logfile.exists() || backup.exists();
    }

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
        }

        return returnValue;
    }

    public synchronized void addAll (Collection<Message> collection) throws IOException {
        Iterator<Message> iterator = collection.iterator();
        while (iterator.hasNext()) {
            Message message = iterator.next();
            add(message);
        }
    }
}
