package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import jakarta.servlet.ServletException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

/**
 * A connection to a client
 */
public class MessageHandler extends AbstractHandler {
    public static final String PARAM_DESTINATION_URL = "DESTINATION_URL";
    public static final String PARAM_STATUS_URL = "STATUS_URL";

    protected static final Logger logger = LogManager.getLogger(MessageHandler.class);

    @Override
    public void handle (String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) {
        String destinationURL = jettyRequest.getParameter(PARAM_DESTINATION_URL);
        String statusURL = jettyRequest.getParameter(PARAM_STATUS_URL);
        if ((null == destinationURL) || (null == statusURL)) {
            logger.error("missing destination or status URL");
        }

        byte[] contents = new byte[jettyRequest.getContentLength()];
        InputStream inputStream;
        try {
            inputStream = request.getInputStream();
            inputStream.read(contents);
        } catch (IOException exception) {
            logger.error("exception with InputStream", exception);
        }

        Message message = new Message();
        message.setDeliveryURL(destinationURL);
        message.setStatusURL(statusURL);
        message.setContents(contents);

        Date date = new Date();
        Date date2 = new Date();
        UUID uuid = new UUID(date.getTime(), date2.getTime());
        message.setMessageID(uuid);

        Miranda.addNewMessage(message);
        response.setStatus(200);
    }

}
