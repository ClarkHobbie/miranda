package com.ltsllc.miranda.cluster;

/**
 * A cluster connection state
 * <P>
 * A cluster connection exists in one of a limited number of states (see package documentation) in which it can only
 * receive a subset of messages.  This enum represents those states.
 * </P>
 */
public enum ClusterConnectionStates {
    AUCTION,
    GENERAL,
    GET_OWNERS,
    GET_MESSAGES,
    MESSAGE,
    START
}
