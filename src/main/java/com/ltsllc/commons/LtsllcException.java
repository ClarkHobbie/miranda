package com.ltsllc.commons;

public class LtsllcException extends Exception {

    public LtsllcException () {
        super();
    }

    public LtsllcException (String message, Throwable cause) {
        super(message, cause);
    }

    public LtsllcException (Throwable cause) {
        super(cause);
    }

    public LtsllcException (String message) {
        super(message);
    }
}
