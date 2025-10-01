package com.ltsllc.miranda.logging;

import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.TestSuperclass;
import com.ltsllc.miranda.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class LoggingSetTest extends TestSuperclass {
    protected LoggingSet set = null;

    @BeforeEach
    public void setup () throws IOException {
        ImprovedFile file = ImprovedFile.createImprovedTempFile("abc");
        set = new LoggingSet(file);
    }

    @AfterEach
    public void tearDown () {
        if (set != null) {
            set.getFile().delete();
        }
    }

    @Test
    public void add () throws IOException {
        Message message = createTestMessage();

        set.add(message);

        assert (set.getFile().exists());
        assert (set.contains(message));
    }
}
