package com.ltsllc.miranda;

/**
 * The type of message
 *
 * This class represents the type of message we are dealing with.
 */
public enum MessageType {
    AUCTION,
    AUCTION_START,
    AUCTION_OVER,
    BID,
    DEAD_NODE,
    ERROR,
    ERROR_START,
    GET_MESSAGE,
    HEART_BEAT_START,
    HEART_BEAT,
    MESSAGE,
    MESSAGE_NOT_FOUND,
    NEW_MESSAGE,
    MESSAGE_DELIVERED,
    NEW_NODE,
    NEW_NODE_CONFIRMED,
    NEW_NODE_OVER,
    OWNER,
    OWNERS,
    OWNERS_END,
    MESSAGES,
    MESSAGES_END,
    START,
    START_ACKNOWLEDGED,
    SYNCHRONIZE,
    SYNCHRONIZE_START,
    TIMEOUT,
    UNKNOWN
}
