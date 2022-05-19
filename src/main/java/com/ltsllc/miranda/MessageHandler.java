package com.ltsllc.miranda;


import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.message.MessageLog;
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
import java.util.Map;
import java.util.UUID;


/**
 * A connection to a client
 */
public class MessageHandler extends AbstractHandler {
    public static final String PARAM_DESTINATION_URL = "DELIVER_URL";
    public static final String PARAM_STATUS_URL = "STATUS_URL";

    protected static final Logger logger = LogManager.getLogger(MessageHandler.class);

    @Override
    public void handle (String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String destinationURL = jettyRequest.getParameter(PARAM_DESTINATION_URL);
        String statusURL = jettyRequest.getParameter(PARAM_STATUS_URL);
        if ((null == destinationURL) || (null == statusURL)) {
            Map map = jettyRequest.getParameterMap();
            logger.error("missing destination or status URL");
            return;
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

        UUID uuid = UUID.randomUUID();
        message.setMessageID(uuid);

        MessageLog.getInstance().add(message, Miranda.getInstance().getMyUuid());
        response.setStatus(200);
    }

}
