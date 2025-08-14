package com.ltsllc.miranda.servlets;

import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.message.MessageLog;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.UUID;

/**
 * A {@link Servlet} that allows the creation of new messages.
 */
public class NewMessage extends HttpServlet {
    public static Logger logger = LogManager.getLogger(NewMessage.class);
    public static final String PARAM_DESTINATION_URL = "DELIVERY_URL";
    public static final String PARAM_STATUS_URL = "STATUS_URL";


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String statusURL = request.getParameter(PARAM_STATUS_URL);
        String deliveryURL = request.getParameter(PARAM_DESTINATION_URL);
        byte[] contents = request.getInputStream().readAllBytes();

        com.ltsllc.miranda.message.Message message = new com.ltsllc.miranda.message.Message();

        message.setStatusURL(statusURL);
        message.setDeliveryURL(deliveryURL);
        message.setContents(contents);
        message.setMessageID(UUID.randomUUID());

        MessageLog.getInstance().add(message, Miranda.getInstance().getMyUuid());
        Cluster.getInstance().informOfNewMessage(message);

        response.getWriter().write(message.getMessageID().toString());
    }

}
