package com.ltsllc.miranda.netty;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.cluster.Node;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.nio.charset.Charset;

public class ServerChannelToNodeDecoder extends ChannelInboundHandlerAdapter {
    protected Node node;

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public void channelRead(ChannelHandlerContext ctx, Object message) throws LtsllcException, IOException, CloneNotSupportedException {
        if (this.node == null) {
            Node node = new Node(null, null, -1, ctx.channel());
            this.node = node;
            Cluster.getInstance().addNode(node);
        }
        String s = (String) message;
        node.messageReceived(s);
        Cluster.getInstance().coalesce();
    }


}