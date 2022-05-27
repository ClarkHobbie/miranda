package com.ltsllc.miranda.cluster;

/**
 * The outcome of an election
 */
public enum ElectionResults {
    /**
     * The outcome was a tie.
     */
    TIE,

    /**
     * The outcome was a leader being elected.
     */
    LEADER_ELECTED,

    /**
     * The outcome  is still in play.
     */
    STILL_TALLYING
}
