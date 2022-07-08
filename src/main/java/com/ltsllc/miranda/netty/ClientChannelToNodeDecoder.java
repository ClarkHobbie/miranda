package com.ltsllc.miranda.netty;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.cluster.Node;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.nio.charset.Charset;

public class ClientChannelToNodeDecoder extends ChannelInboundHandlerAdapter {
    protected Node node;

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public void channelRead (ChannelHandlerContext ctx, Object message) throws LtsllcException, IOException, CloneNotSupportedException {
        if (node.getChannel() == null) {
            node.setChannel(ctx.channel());
        }

        String s = null;
        if (message instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) message;
            s = byteBuf.toString(Charset.defaultCharset());
        } else {
            s = (String) message;
        }

        node.messageReceived(s);
    }


}
