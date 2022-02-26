package com.ltsllc.miranda;

/**
 * A cluster connection state
 *
 * A cluster connection exists in one of a limited number of states (see package documentation) in which it can only
 * receive a subset of messages.  This enum represents those states.
 */
public enum ClusterConnectionStates {
    STATE_AUCTION,
    STATE_GENERAL,
    STATE_NEW_NODE,
    STATE_START

}
