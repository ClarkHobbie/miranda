package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.netty.ClientChannelToNodeDecoder;
import com.ltsllc.miranda.netty.HeartBeatHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.AbstractEpollStreamChannel;

import java.net.InetSocketAddress;

/**
 * A simple class that contains the specification for a node
 */
public class SpecNode {
    protected String host;
    protected int port;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}

