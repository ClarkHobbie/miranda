package com.ltsllc.util;

import com.ltsllc.miranda.TestSuperclass;
import org.junit.jupiter.api.Test;

public class ByteQueueTest extends TestSuperclass {
    @Test
    public void toInt01() {
        ByteQueue bq = new ByteQueue();
        String s = "1000";
        byte[] byteArray = s.getBytes();
        for (int i = 0; i < byteArray.length; i++) {
            bq.append(byteArray[i]);
        }

        assert (bq.toInt() == 1000);
    }

    @Test
    public void toInt02() {
        ByteQueue bq = new ByteQueue();
        String s = "1";
        byte[] byteArray = s.getBytes();
        for (int i = 0;i < byteArray.length; i++) {
            bq.append(byteArray[i]);
        }

        assert (bq.toInt() == 1);
    }

    @Test
    public void toInt03() {
        ByteQueue bq = new ByteQueue();
        String s = "30";
        byte[] byteArray = s.getBytes();
        for (int i = 0;i < byteArray.length; i++) {
            bq.append(byteArray[i]);
        }

        assert (bq.toInt() == 30);
    }

    @Test
    public void toInt04() {
        ByteQueue bq = new ByteQueue();
        String s = "300";
        byte[] byteArray = s.getBytes();
        for (int i = 0;i < byteArray.length; i++) {
            bq.append(byteArray[i]);
        }

        assert (bq.toInt() == 300);
    }

    @Test
    public void toInt05() {
        ByteQueue bq = new ByteQueue();
        String s = "124";
        byte[] byteArray = s.getBytes();
        for (int i = 0;i < byteArray.length; i++) {
            bq.append(byteArray[i]);
        }

        assert (bq.toInt() == 124);
    }


}
