package com.ltsllc.miranda.servlets;

import com.ltsllc.miranda.cluster.Cluster;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.*;
import java.util.Map;
import java.util.UUID;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.cluster.Cluster;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.*;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Map;
import java.util.UUID;


public class NumberOfConnectionsHandler extends AbstractHandler {



        protected static final Logger logger = LogManager.getLogger(com.ltsllc.miranda.MessageHandler.class);
        public static final Logger events = LogManager.getLogger("events");


        public NumberOfConnectionsHandler() {
        }

        /**
         * A new message has arrived
         * <p>
         * This method takes care of all the record-keeping that is associated with a new message.  This consists of
         * creating a new message and adding it to the send queue, informing the cluster of the new message and telling
         * the client that we created a new message.
         *
         * @param target       The "target" of the request, ignored.
         * @param jettyRequest This contains the request that came in on jetty, used.
         * @param request      The servlet request, ignored.
         * @param response
         */
        @Override
        public void handle(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
            int numberOfConnections = Cluster.getInstance().getNumberOfConnections();



            response.setStatus(200);
            response.setContentType("html");
            Writer writer = null;
            try {
                writer = response.getWriter();

                response.getWriter().println("<html>");
                response.getWriter().println(numberOfConnections);
                response.getWriter().println("</html>");
            } finally {
                               if (writer != null) {
                    writer.close();
                }
            }

        }


    }

