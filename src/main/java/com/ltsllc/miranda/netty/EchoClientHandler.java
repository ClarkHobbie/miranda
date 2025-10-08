package com.ltsllc.miranda.netty;

import com.ltsllc.miranda.Miranda;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;

public class EchoClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    public EchoClientHandler() {
        String wharever = "hello, world!";
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // Send a message to the server when connected
        ctx.writeAndFlush(Unpooled.copiedBuffer("Hello Netty Echo Server!", StandardCharsets.UTF_8));
    }

    @Override
    public void channelInactive (ChannelHandlerContext context) {
        Miranda.out.println("Client connection closed");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        // Print the received echoed message
        Miranda.out.println("Client received: " + msg.toString(StandardCharsets.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        //ctx.close();
    }
}