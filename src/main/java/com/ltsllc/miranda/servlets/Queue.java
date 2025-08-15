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
        out.println("  </TR>");
        for (int i = 0; i < messageList.size(); i++) {
            out.print("<TD>");
            out.println(i + "</TD>");

            Message message = messageList.get(i);
            out.println("<TD>");
            out.println("<FORM METHOD='POST' ACTION='/api/trackMessage'>");

            out.print("<INPUT TYPE='HIDDEN' NAME='messageId' VALUE='");
            out.print(message.getMessageID().toString());
            out.println("'>");

            out.print(message.getMessageID());

            out.println("<BUTTON TYPE='SUBMIT'>Track</BUTTON>");
        }
    }
}
