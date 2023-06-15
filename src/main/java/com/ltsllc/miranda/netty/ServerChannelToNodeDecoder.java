package com.ltsllc.miranda.netty;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.cluster.Node;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * This class takes care of a node connecting to us
 */
public class ServerChannelToNodeDecoder extends ChannelInboundHandlerAdapter {
    protected Node node;

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    /**
     * This method handles the situation where another node connects to us
     * @param ctx The context in which this took place.
     * @param message A String that is the message that the other node sent.
     * @throws LtsllcException This exception is thrown by the node when the message is delivered.
     * @throws IOException This exception is thrown by the node when the message is delivered.
     * @throws CloneNotSupportedException This exception is thrown by the node when the message is delivered.
     */
    public void channelRead(ChannelHandlerContext ctx, Object message) throws LtsllcException, IOException, CloneNotSupportedException {
        String s = (String) message;
        node.messageReceived(s);
    }
}