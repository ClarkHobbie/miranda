package com.ltsllc.miranda.servlets;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.logging.MessageEventLogger;
import com.ltsllc.miranda.logging.MessageLog;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

public class DeleteMessage extends HttpServlet {
    public static final String PARAM_MESSAGE_ID = "messageId";

    @Override
    public void doPost (HttpServletRequest request, HttpServletResponse response)
        throws ServletException
    {
        PrintWriter out = null;
        try {
            out = response.getWriter();
        } catch (IOException e) {
            throw new ServletException(e);
        }
        UUID messageID = UUID.fromString(request.getParameter(PARAM_MESSAGE_ID));
        Message message = null;
        try {
            message = MessageLog.getInstance().get(messageID);
        } catch (LtsllcException|IOException e) {
            throw new RuntimeException(e);
        }
        try {
            MessageLog.getInstance().remove(messageID);
        } catch (IOException e) {
            throw new ServletException(e);
        }

        MessageEventLogger.deleted(message);

        out.println("<H1>Success</H1>");

        out.println("<A HREF='/'>Back</A>");
    }
}
