package com.ltsllc.commons.util;

import com.ltsllc.commons.test.TestCase;
import org.junit.Test;

import java.security.SecureRandom;

public class ImprovedRandomTest extends TestCase {
    @Test
    public void choose () {
        ImprovedRandom improvedRandom = new ImprovedRandom(new SecureRandom());
        Integer[] candidates = { 0,1,2,3};
        Integer i = improvedRandom.choose(Integer.class, candidates);

        assert(i == 0 || i == 1 || i == 2 || i == 3);
    }
}
