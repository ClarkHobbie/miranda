package com.ltsllc.miranda.servlets;

import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.message.MessageLog;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class Queue extends HttpServlet {
    @Override
    public void doPost (HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
        response.setContentType("HTML");

        PrintWriter out = response.getWriter();
        List<Message> messageList = MessageLog.getInstance().copyAllMessages();

        out.println("<TABLE BORDER='1'>");
        out.println("  <TR>");
        out.println("    <TH>Queue Position</TH>");
        out.println("    <TH>Message ID</TH>");
        out.println("    <TH>Contents</TH>");
        out.println("    <TH>Actions</TH>");
        out.println("  </TR>");
        for (int i = 0; i < messageList.size(); i++) {
            out.print("<TD>");
            out.print(i);
            out.println("</TD>");

            out.println("<TD>");
            Message message = messageList.get(i);
            out.println(message.getMessageID().toString());
            out.println("</TD>");

            out.println("<TD>");
            String stringContents = new String(message.getContents());
            out.println(stringContents);
            out.println("</TD>");

            out.println("<TD>");
            out.println("<FORM METHOD='POST' ACTION='/api/trackMessage'>");

            out.print("<INPUT TYPE='HIDDEN' NAME='messageId' VALUE='");
            out.print(message.getMessageID().toString());
            out.println("'>");

            out.println("<BUTTON TYPE='SUBMIT'>Track</BUTTON>");
            out.println("</FORM>");

            out.println("<FORM METHOD='POST' ACTION='/api/deleteMessage'>");

            out.print("<INPUT TYPE='HIDDEN' NAME='messageId' VALUE='");
            out.print(message.getMessageID().toString());
            out.println("'>");

            out.println("<BUTTON TYPE='SUBMIT'>Delete</BUTTON>");
            out.println("</FORM>");

            out.println("</TD>");

        }
    }
}
