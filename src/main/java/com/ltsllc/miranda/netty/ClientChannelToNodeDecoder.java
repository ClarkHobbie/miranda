package com.ltsllc.miranda.netty;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.cluster.Node;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * A ChannelInboundHandlerAdapter that translates ByteBufs to Strings
 */
public class ClientChannelToNodeDecoder extends ChannelInboundHandlerAdapter {
    public static Logger logger = LogManager.getLogger(ClientChannelToNodeDecoder.class);
    protected Node node;

    public ClientChannelToNodeDecoder (ChannelPipeline pipeline) {
        node = node;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public void channelActive (ChannelHandlerContext ctx) {
        setNode(Cluster.getNode(ctx.channel()));
        ctx.fireChannelActive();
    }
    /**
     * A new message is available, translate it to a String and call messageReceived
     * @param ctx The context in which it was received
     */
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws LtsllcException, IOException, CloneNotSupportedException {

        try {
            logger.debug("entering channelRead");

            if (node == null) {
                node = new Node(null, null, -1, ctx.channel());
                Cluster.getInstance().addNode(node);
            }

            if (node.getChannel() == null) {
                node.setChannel(ctx.channel());
            }

            String s = null;
            if (msg instanceof ByteBuf) {
                ByteBuf byteBuf = (ByteBuf) msg;
                s = byteBuf.toString(Charset.defaultCharset());
            } else {
                s = (String) msg;
            }

            node.messageReceived(s);
            ctx.fireChannelRead(msg);
        } finally {
            //if (msg instanceof ByteBuf) {
                //ByteBuf byteBuf = (ByteBuf) msg;
                //ReferenceCountUtil.release(byteBuf);
            //}
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("caught exception");
        logger.error(cause);
        cause.printStackTrace();
    }
}
