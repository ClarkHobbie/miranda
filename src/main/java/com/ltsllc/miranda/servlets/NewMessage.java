package com.ltsllc.miranda.servlets;

import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.logging.MessageLog;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.Servlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.Param;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

/**
 * A {@link Servlet} that allows the creation of new messages.
 */
public class NewMessage extends HttpServlet {
    public static Logger logger = LogManager.getLogger(NewMessage.class);
    public static final String PARAM_DESTINATION_URL = "DELIVERY_URL";
    public static final String PARAM_STATUS_URL = "STATUS_URL";
    public static final String PARAM_CONTENTS = "CONTENTS";


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String statusURL = request.getParameter(PARAM_STATUS_URL);
        String deliveryURL = request.getParameter(PARAM_DESTINATION_URL);
        String contents = request.getParameter(PARAM_CONTENTS);

        com.ltsllc.miranda.message.Message message = new com.ltsllc.miranda.message.Message();

        message.setStatusURL(statusURL);
        message.setDeliveryURL(deliveryURL);
        message.setParamList(captureParams(request));
        message.setContents(contents.getBytes());
        message.setMessageID(UUID.randomUUID());

        MessageLog.getInstance().add(message, Miranda.getInstance().getMyUuid());
        Cluster.getInstance().informOfNewMessage(message);

        PrintWriter out = response.getWriter();
        response.setContentType("HTML");
        out.println("<H1>Message ID</H1>");
        out.println("Message ID: ");
        out.println(message.getMessageID().toString());
        out.println("<FORM METHOD='POST' ACTION='/api/trackMessage'>");
        out.print("<INPUT TYPE='HIDDEN' NAME='messageId' VALUE='");
        out.print(message.getMessageID().toString());
        out.println("'>");
        out.println("<BUTTON TYPE='SUBMIT'>Track</BUTTON>");
        out.println("</FORM>");
    }

    private List<Param> captureParams(HttpServletRequest request) {
        List<Param> list = new ArrayList<>();

        Enumeration<String> enumeration = request.getParameterNames();
        while (enumeration.hasMoreElements()) {
            String name = enumeration.nextElement();
            if (name.equalsIgnoreCase(PARAM_DESTINATION_URL) || name.equalsIgnoreCase(PARAM_STATUS_URL) ||
                    name.equalsIgnoreCase(PARAM_CONTENTS)) {
                continue;
            }

            String value = request.getParameter(name);
            Param param = null;
            if (value != null) {
                param = new Param(name, value);
                list.add(param);
            }
        }

        return list;
    }

}
