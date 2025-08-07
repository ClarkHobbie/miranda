package com.ltsllc.miranda.message;

/**
 * The type of message
 * <p>
 * This class represents the type of message we are dealing with.
 */
public enum MessageType {
    ASSIGN,
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
    LEADER,
    MESSAGE_DELIVERED,
    NEW_NODE,
    NEW_NODE_CONFIRMED,
    NEW_NODE_OVER,
    OWNER,
    OWNER_END,
    OWNERS,
    OWNERS_END,
    MESSAGES,
    MESSAGES_END,
    START,
    START_START,
    START_ACKNOWLEDGED,
    SYNCHRONIZE,
    SYNCHRONIZE_START,
    TAKE,
    TIMEOUT,
    UNKNOWN
}
