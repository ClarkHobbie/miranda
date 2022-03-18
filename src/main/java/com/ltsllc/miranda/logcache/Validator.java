package com.ltsllc.miranda.logcache;

import java.util.UUID;

/**
 * An object that identifies those UUIDs that are valid
 */
public interface Validator {
    /**
     * Return true if the validator deems that the UUID is valid
     *
     * @param uuid The UUID in question.
     * @return true if the UUID is valid, false otherwise.
     */
    public boolean isValid (UUID uuid);
}
