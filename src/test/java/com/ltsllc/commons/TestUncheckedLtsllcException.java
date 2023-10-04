package com.ltsllc.commons;

import com.ltsllc.commons.UncheckedLtsllcException;
import org.junit.Test;

public class TestUncheckedLtsllcException {
    @Test
    public void constructor () {
        throw new UncheckedLtsllcException("hi there");
    }
}
