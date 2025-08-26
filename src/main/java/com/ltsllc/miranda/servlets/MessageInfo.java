package com.ltsllc.miranda.servlets;

import com.ltsllc.commons.HexConverter;
import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.logging.MessageLog;
import com.ltsllc.miranda.message.Message;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.asynchttpclient.Param;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

/**
 * A servlet that gets the host and port properties,
 */
public class MessageInfo extends HttpServlet {
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String strUuid = req.getParameter("ID");
        if (null == strUuid) {
            throw new RuntimeException("no ID provided");
        }

        UUID uuid = UUID.fromString(strUuid);
        Message message = null;
        try {
            message = MessageLog.getInstance().get(uuid);
        } catch (LtsllcException e) {
            throw new RuntimeException("exception during get", e);
        }

        if (null == message) {
            throw new RuntimeException("ID: " + uuid.toString() + " not found");
        }

        PrintWriter out = res.getWriter();

        res.setContentType("HTML");
        out.println("<H1>Information for message " + message.getMessageID().toString() + "</H1>");

        out.println("<TABLE BORDER='1'>");
        out.println("<TR>");
        out.println("<TH>Name</TH>");
        out.println("<TH>Value</TH>");
        out.println("</TR>");

        out.println("<TR>");
        out.println("<TD>Message ID</TD>");
        out.println("<TD>");
        out.println(message.getMessageID().toString());
        out.println("</TD>");
        out.println("</TR>");

        out.println("<TR>");
        out.println("<TD>");
        out.println("Delivery URL");
        out.println("</TD>");
        out.println("<TD>");
        out.println(message.getDeliveryURL());
        out.println("</TD>");
        out.println("</TR>");

        out.println("<TR>");
        out.println("<TD>Status URL</TD>");
        out.println("<TD>");
        out.println(message.getStatusURL());
        out.println("</TD>");
        out.println("</TR>");

        out.println("<TR>");
        out.println("<TD>Params</TD>");
        out.println("</TR>");
        for (Param param : message.getParamList()) {
            out.println("<TR>");
            out.println("<TD>");
            out.println(param.getName());
            out.println("</TD>");
            out.println("<TD>");
            out.println(param.getValue());
            out.println("</TD>");
            out.println("</TR>");
        }

        out.println("</TR");

        out.println("<TR>");
        out.println("<TD>Contents</TD>");
        out.println("<TD>");
        out.println(HexConverter.toHexString(message.getContents()));
        out.println("</TD>");
        out.println("</TR>");

        out.println("</TABLE>");
    }

    @Override
    public String getServletInfo() {
        return "";
    }

    @Override
    public void destroy() {

    }
}
