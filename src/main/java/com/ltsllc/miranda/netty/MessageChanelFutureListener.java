package com.ltsllc.miranda.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MessageChanelFutureListener implements ChannelFutureListener {
    public static Logger logger = LogManager.getLogger(MessageChanelFutureListener.class);

    public String message;


    public MessageChanelFutureListener (String message) {
        this.message = message;
    }

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if  (channelFuture.isSuccess()) {
            logger.debug ("successfully sent " + message);
        } else {
            logger.error("error sending " + message);
            channelFuture.cause().printStackTrace();
        }
    }
}
