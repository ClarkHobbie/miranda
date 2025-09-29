package com.ltsllc.miranda.logging;

import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.TestSuperclass;
import org.junit.jupiter.api.Test;
import com.ltsllc.miranda.message.Message;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoggingListTest extends TestSuperclass {

    @Test
    void add() throws IOException {
        Message message = createTestMessage(UUID.randomUUID());
        ImprovedFile file = ImprovedFile.createImprovedTempFile("abc");
        LoggingList list = new LoggingList(file);

        try {
            list.add(message);

            assert (list.getList().contains(message));
        } finally {
            file.delete();
        }
    }

    @Test
    void remove() {
    }

    @Test
    void shouldRecover() {
    }

    @Test
    void recover() {
    }
}