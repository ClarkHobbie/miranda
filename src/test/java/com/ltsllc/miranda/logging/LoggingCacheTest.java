package com.ltsllc.miranda.logging;

import com.ltsllc.commons.util.Utils;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.message.MessageLog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static com.ltsllc.miranda.message.MessageLog.getInstance;
import static org.junit.jupiter.api.Assertions.*;

class LoggingCacheTest {

    public Message createMessage () {
        Message m = new Message();
        m.setMessageID(UUID.randomUUID());
        m.setStatus(0);

        String contents = "hi there";
        String contentsHex = Utils.hexEncode(contents.getBytes());
        m.setContents(contents.getBytes());

        m.setStatusURL("google.com");
        m.setDeliveryURL("google.com");

        return m;
    }


    @Test
    void copyMessages1() throws IOException {
        Message m1 = createMessage();
        Message m2 = createMessage();

        MessageLog.defineStatics();

        UUID u = UUID.randomUUID();

        getInstance().add(m1, u);
        getInstance().add(m2, u);

        int junk = 0;
        List<Message> l = MessageLog.getInstance().copyMessages(9, junk, junk);

        assert (l.size() == 1);
    }

    @Test
    void copyMessages2() throws Exception {
        Message m1 = createMessage();
        Message m2 = createMessage();

        MessageLog.defineStatics();

        UUID u = UUID.randomUUID();

        getInstance().add(m1, u);
        getInstance().add(m2, u);

        int restartIndexOut = 0;
        int restartIndexIn = 1;

        List<Message> l = MessageLog.getInstance().copyMessages(9, restartIndexIn, restartIndexOut);

        assert (l.get(0).equals(m2));
    }

    @Test
    void copyMessages3() throws Exception {
        Message m1 = createMessage();
        Message m2 = createMessage();

        MessageLog.defineStatics();

        UUID u = UUID.randomUUID();

        getInstance().add(m1, u);
        getInstance().add(m2, u);

        int restartIndexOut = 0;
        int restartIndexIn = 2;

        List<Message> l = MessageLog.getInstance().copyMessages(9, restartIndexIn, restartIndexOut);

        assert (l == null);
    }

    @Test
    void copyMessages4() throws Exception {
        Message m1 = createMessage();
        Message m2 = createMessage();

        MessageLog.defineStatics();

        UUID u = UUID.randomUUID();

        getInstance().add(m1, u);
        getInstance().add(m2, u);

        int restartIndexOut = 0;
        int restartIndexIn = -1;

        List<Message> l = MessageLog.getInstance().copyMessages(9, restartIndexIn, restartIndexOut);

        assert (l == null);
    }

}