package com.ltsllc.miranda.servlets;

import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.logging.MessageEvent;
import com.ltsllc.miranda.logging.MessageEventLogger;
import com.ltsllc.miranda.logging.MessageLog;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TrackMessage extends HttpServlet {
    public static final String PARAM_MESSAGE_ID = "messageId";
    @Override
    public void doPost (HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("HTML");
        PrintWriter out = null;
        try {
            out = response.getWriter();
        } catch (IOException e) {
            throw new RuntimeException("error getting output", e);
        }

        String stringMessageID = request.getParameter(PARAM_MESSAGE_ID);
        if (null == stringMessageID) {
            out.println("<H1>Error</H1>");
            out.println("<P>The system requires a message ID to track.</P>");
            out.println("<P><BUTTON ONCLICK='window.location.href='/'>Back</P>");
            return;
        }

        UUID messageID = null;

        try {
            messageID = UUID.fromString(stringMessageID);
        } catch (Exception e) {
            out.println("<H1>Error</H1>");
            out.println("<P>Invalid message ID: " + stringMessageID);
            out.println("<P><BUTTON ONCLICK='window.location.href='/'>Back</P>");
            return;
        }

        List<MessageEvent> messageEventList = MessageLog.getInstance().getMessageEventLogger().getEventsFor(messageID);
        out.println("<H1>Events for " + messageID.toString() + "</H1>");
        out.println("<TABLE border=\"1\">");
        out.println("<TR>");
        out.println("<TH>Type</TH>");
        out.println("<TH>When</TH>");
        out.println("<TH>Where</TH>");
        out.println("</TR>");

        for (MessageEvent event : messageEventList) {
            out.print("<TD>");
            out.print(event.getType().toString());
            out.println("</TD>");

            out.print("<TD>");
            printDateTime(out, event.getTime());
            out.println("</TD>");

            out.print("<TD>");
            printStackTrace(out, event.getWhere());
            out.println("</TD>");

            out.println("</TR>");
        }

        out.println("</TABLE>");

        out.println("<P><A HREF='/'>Back</A></P>");
    }

    public void printStackTrace(PrintWriter out, Exception e) {
        StackTraceElement[] stackTrace = e.getStackTrace();

        for (StackTraceElement element : stackTrace) {
            // out.println("<P>");
            out.print(element.getClassName());
            out.print(".");
            if ('<' != element.getMethodName().charAt(0)) {
                out.print(element.getMethodName());
            } else {
                out.print("<I>init</I>");
            }
            out.print(" ");
            out.println(element.getLineNumber());
            out.println("<BR>");
            // out.println("</P>");
        }
    }

    public void printDateTime(PrintWriter out, long time) {
        Calendar calendar = Calendar.getInstance();
        Date date = new Date(time);
        calendar.setTime(date);

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        out.println(formatter.format(calendar.getTime()));
    }
}
