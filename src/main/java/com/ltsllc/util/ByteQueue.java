package com.ltsllc.util;

import java.util.ArrayList;
import java.util.List;

public class ByteQueue {
    protected List<Byte> bytes = new ArrayList<>();

    public void append(byte b) {
        Byte B = new Byte(b);
        bytes.add(B);
    }

    public int toInt() {
        byte[] byteArray = new byte[bytes.size()];
        Byte[] byteArray2 = new Byte[bytes.size()];
        bytes.toArray(byteArray2);

        for (int i = 0;i < byteArray2.length; i++) {
            byteArray[i] = byteArray2[i];
        }

        String s = new String(byteArray);
        return Integer.parseInt(s);
    }

    public int size() {
        return bytes.size();
    }
}
