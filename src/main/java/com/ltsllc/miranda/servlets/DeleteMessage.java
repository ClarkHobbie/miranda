package com.ltsllc.miranda.servlets;

import com.ltsllc.miranda.message.MessageLog;
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
        try {
            MessageLog.getInstance().remove(messageID);
        } catch (IOException e) {
            throw new ServletException(e);
        }

        out.println("<H1>Success</H1>");

        out.println("");
    }
}
