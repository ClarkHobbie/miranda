package com.ltsllc.miranda.misc;

public class StringComparrison {
    protected String s1 = null;
    protected String s2 = null;
    protected boolean areEqual = false;

    public boolean isAreEqual() {
        return areEqual;
    }

    public void setAreEqual(boolean areEqual) {
        this.areEqual = areEqual;
    }

    public void compare (String s1, String s2) {
        this.s1 = s1;
        this.s2 = s2;

        areEqual = s1.equals(s2);
    }

    public int getWhereTheyDiffer() {
        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return i;
            }
        }

        if (s1.length() == s2.length()) {
            return -1;
        } else {
            return s1.length() - 1;
        }
    }


}
