package com.ltsllc.miranda.netty;

import com.ltsllc.commons.LtsllcException;
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
    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }


    public ServerChannelToNodeDecoder (String newName) {
        setName(newName);
    }

    public void channelActive(ChannelHandlerContext context) {
        if (node == null) {
            node = new Node(null,null, -1, context.channel());
            Cluster.getInstance().addNode(node);
        }

        //
        // if this is a loopback then remove it
        //
        if (node.getIsLoopback()) {
            Cluster.getInstance().removeNode(node);
        }
    }

    /**
     * This method handles the situation where another node connects to us
     * @param ctx The context in which this took place.
     * @param message A String that is the message that the other node sent.
     * @throws LtsllcException This exception is thrown by the node when the message is delivered.
     * @throws IOException This exception is thrown by the node when the message is delivered.
     */
    public void channelRead(ChannelHandlerContext ctx, Object message) throws LtsllcException, IOException {
        if (message instanceof ByteBuf) {
            String s = ((ByteBuf) message).toString(Charset.defaultCharset());
            ((ByteBuf) message).release();

            node.messageReceived(s);
        }
    }
}