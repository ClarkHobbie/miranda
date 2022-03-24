package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.cluster.Cluster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.*;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Map;
import java.util.UUID;


/**
 * A connection to a client
 */
public class MessageHandler extends AbstractHandler {
    public static final String PARAM_DESTINATION_URL = "DELIVER_URL";
    public static final String PARAM_STATUS_URL = "STATUS_URL";

    protected static final Logger logger = LogManager.getLogger(MessageHandler.class);

    /**
     * A new message has arrived
     *
     * This method takes care of all the record-keeping that is associated with a new message.  This consists of
     * creating a new message and adding it to the send queue, informing the cluster of the new message and telling
     * the client that we created a new message.
     *
     * @param target The "target" of the request, ignored.
     * @param jettyRequest This contains the request that came in on jetty, used.
     * @param request The servlet request, ignored.
     * @param response
     */
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

        Miranda.getInstance().addNewMessage(message);
        response.setStatus(200);
        Writer writer = null;
        BufferedWriter bufferedWriter = null;
        try {
            writer = response.getWriter();
            bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write(message.getMessageID().toString());
            bufferedWriter.newLine();
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }

            if (writer != null) {
                writer.close();
            }
        }

        try {
            Cluster.getInstance().informOfNewMessage(message);
            //tell the client (again!) that we created the message
            tellClient(message.getStatusURL(), message.getMessageID());
        } catch (LtsllcException e) {
            logger.error("Encoutered exception while telling cluster of new message",e);
        }
    }

    public void tellClient (String url, UUID uuid) {
        AsyncHttpClient httpClient = Dsl.asyncHttpClient();
        BoundRequestBuilder brb = httpClient.preparePost(url);

        brb.setBody("CREATED " + uuid.toString())
           .execute(new AsyncCompletionHandler<Response>() {
               @Override
               public Response onCompleted (Response response) {
                   if (response.getStatusCode() != 200) {
                       logger.error("Client returned non-200 (" + response.getStatusCode() + ") when we tried to use URL (" + url + ")");
                   } // otherwise they accepted it

                   return response;
                }
           });
    }
}
