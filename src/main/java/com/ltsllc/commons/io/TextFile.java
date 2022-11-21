package com.ltsllc.commons.io;

import java.io.*;


/******************************************************************************
 *
 * A file containing text
 *
 * This class represents a text file.  When created, it reads in the file
 * and makes available via getReader, which returns a java.io.Reader.
 *
 ******************************************************************************
 */
public class TextFile {
    /* the associated file */

    protected File file;

    /* an image of the file in memory */

    protected char[] buffer;

    /*
     * Create a new instance of the class and read into memory if it exists.
     *
     * After calling this @readFile should return a java.io.reader to the
     * file.
     */
    public TextFile(File inFile) throws IOException {
        file = inFile;
        if (file.exists()) {
            readFile();
        }
    }


    /*
     * read in the associated text file
     *
     * @returns Nothing, calling this method simply reads the associated
     *          text file into memory
     */
    public void readFile() throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try {
            int fileSize = (int) file.length();
            buffer = new char[fileSize];

            int bytesRead = 0;
            int c = fis.read();

            while (c != -1) {
                buffer[bytesRead] = (char) c;
                bytesRead++;
                c = fis.read();
            }

        } finally {
            fis.close();
        }
    }

    /*
     * get a java.io.Reader for the file
     *
     * This method gets a java.io.Reader to the loaded file
     */
    public Reader getReader() {
        CharArrayReader car = new CharArrayReader(buffer);
        return car;
    }
}
