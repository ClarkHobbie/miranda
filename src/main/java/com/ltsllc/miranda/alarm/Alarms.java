package com.ltsllc.miranda.alarm;

/**
 * The alarms that AlarmClock can use
 */
public enum Alarms {
    TEST, // for testing
    AUCTION, // timeout waiting for an auction message
    BID, // timeout waiting for a bid
    COMPACTION, // time for the MessageLog to compact
    CLUSTER, // time for the Cluster to reconnect
    DEAD_NODE, // the dead node timeout
    HEART_BEAT, // time to send a heart beat
    HEART_BEAT_TIMEOUT, // timeout waiting for a heart beat response
    START // the start timeout
}