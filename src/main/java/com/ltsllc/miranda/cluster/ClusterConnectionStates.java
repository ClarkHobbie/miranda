package com.ltsllc.miranda.cluster;

/**
 * A cluster connection state
 * <P>
 * A cluster connection exists in one of a limited number of states (see package documentation) in which it can only
 * receive a subset of messages.  This enum represents those states.
 * </P>
 */
public enum ClusterConnectionStates {
    AWAITING_ACK,
    AWAITING_ASSIGNMENTS,
    STATE_DEAD_NODE,
    ELECTION,
    GENERAL,
    MESSAGE,
    START,
    SYNCHRONIZING
}
