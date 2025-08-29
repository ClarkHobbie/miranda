package com.ltsllc.miranda.netty;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.cluster.Node;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;

/**
 * A ChannelInboundHandlerAdapter that translates ByteBufs to Strings
 */
public class ClientChannelToNodeDecoder extends ChannelInboundHandlerAdapter {
    protected Node node;

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    /**
     * A new message is available, translate it to a String and call messageReceived
     * @param ctx The context in which it was received
     * @param message The message
     */
    public void channelRead (ChannelHandlerContext ctx, Object message) throws LtsllcException, IOException, CloneNotSupportedException {
        try {
            if (node.getChannel() == null) {
                node.setChannel(ctx.channel());
            }

            String s = null;
            if (message instanceof ByteBuf) {
                ByteBuf byteBuf = (ByteBuf) message;
                s = byteBuf.toString();
            } else {
                s = (String) message;
            }

            node.messageReceived(s);
        } finally {
            if (message instanceof ByteBuf) {
                ByteBuf byteBuf = (ByteBuf) message;
                ReferenceCountUtil.release(byteBuf);
            }
        }
    }


}
