package com.ltsllc.miranda.netty;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.cluster.Node;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * A ChannelInboundHandlerAdapter that translates ByteBufs to Strings
 */
public class ClientChannelToNodeDecoder extends ChannelInboundHandlerAdapter {
    public static Logger logger = LogManager.getLogger(ClientChannelToNodeDecoder.class);
    protected Node node;
    protected ChannelPipeline  pipeline;

    public ClientChannelToNodeDecoder (ChannelPipeline pipeline) {
        this.pipeline =  pipeline;
        node = node;
    }

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
    public void channelRead (ChannelHandlerContext ctx, ByteBuf message) throws LtsllcException, IOException, CloneNotSupportedException {
        try {
            logger.debug("entering channelRead");
            if (node.getChannel() == null) {
                node.setChannel(ctx.channel());
            }

            String s = null;
            /*
            if (message instanceof ByteBuf) {
                ByteBuf byteBuf = (ByteBuf) message;

             */
                s = message.toString();
            /*
            } else {
                s = (String) message;
            }


             */
            node.messageReceived(s);
            ctx.fireChannelRead(message);
        } finally {
            if (message instanceof ByteBuf) {
                ByteBuf byteBuf = (ByteBuf) message;
                ReferenceCountUtil.release(byteBuf);
            }
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause);
    }
}
