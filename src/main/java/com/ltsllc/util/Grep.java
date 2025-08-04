package com.ltsllc.util;

import com.ltsllc.commons.io.ImprovedFile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Grep
{



    public int matches (String pattern, ImprovedFile file) throws IOException {
        if (pattern.contains("-")) {
            Scanner scanner = new Scanner(pattern);
            pattern = pattern;
        }
        int returnValue = 0;
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);

            Pattern pat = Pattern.compile(pattern);
            for (
                    String line = bufferedReader.readLine();
                    line != null;
                    line = bufferedReader.readLine()
            ) {
                if (line.contains(pattern)) {
                    returnValue++;
                }
            }
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
}