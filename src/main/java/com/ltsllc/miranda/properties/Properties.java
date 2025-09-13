package com.ltsllc.miranda.properties;

import com.ltsllc.miranda.message.Message;

/**
 * An enum that represents the properties that a class can listen to for changes.
 */
public enum Properties {
    unknown,

    auctionTimeout,
    bidTimeout,
    cacheLoadLimit,
    cluster,
    cluster1,
    cluster2,
    cluster3,
    cluster4,
    cluster5,
    clusterPort,
    clusterRetry,
    compaction,
    coalescePeriod,
    deadNodeTimeout,
    events,
    heartBeat,
    heartBeatTimeout,
    hostName,
    leaderAckTimeout,
    loggingLevel,
    maxWaitBetweenSends,
    messageLogfile,
    messagePort,
    ownerFile,
    propertiesFile,
    scanPeriod,
    startTimeout,
    thisHost,
    thisPort,
    useHeartbeats,
    waitBetweenSends,
    uuid;
}
