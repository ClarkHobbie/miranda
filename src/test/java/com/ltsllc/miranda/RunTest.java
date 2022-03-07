package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import org.junit.jupiter.api.Test;

public class RunTest {
    @Test
    public void go () throws Exception {
        Miranda miranda = new Miranda();
        String[] args = {};
        miranda.startUp(args);
        while (true) {
            miranda.mainLoop();
        }
    }
}
