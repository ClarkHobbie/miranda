package com.ltsllc.commons;


public class UncheckedLtsllcException extends RuntimeException {

    public UncheckedLtsllcException () {
        super();
    }

    public UncheckedLtsllcException (String message, Throwable cause) {
        super(message, cause);
    }

    public UncheckedLtsllcException (Throwable cause) {
        super(cause);
    }

    public UncheckedLtsllcException (String message) {
        super(message);
    }
}
