package com.ltsllc.miranda.netty;

import com.ltsllc.miranda.Miranda;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Echo {
    public String host;
    public int port;
    public Channel channel;

    Logger logger = LogManager.getLogger(Echo.class);

    public Echo (String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect(Bootstrap bootstrap) {
        boolean result = false;

        try {
            InetSocketAddress address = new InetSocketAddress(host, port);

            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new EchoClientHandler());
                }
            });

            ChannelFuture future = bootstrap
                    .connect(address);
            future.await();
            if (!future.isSuccess()) {
                result = false;
            } else {
                channel = future.channel();
                result = true;
            }
        } catch (Exception e) {
            Miranda.out.println(e);
            logger.error(e);
        }

        return result;
    }

    public void send(String line) {
        try {
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeBytes(line.getBytes());
            ChannelFuture future = channel.writeAndFlush(byteBuf);
            future.await();

            if (!future.isSuccess()) {
                Miranda.out.println(future.exceptionNow());
                throw new RuntimeException("send failed", future.exceptionNow());
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
