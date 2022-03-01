package com.ltsllc.miranda.cluster;

/**
 * A cluster connection state
 *
 * A cluster connection exists in one of a limited number of states (see package documentation) in which it can only
 * receive a subset of messages.  This enum represents those states.
 */
public enum ClusterConnectionStates {
    AUCTION,
    GENERAL,
    MESSAGE,
    NEW_NODE,
    START
}