package com.ltsllc.miranda;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Message {
    protected static Logger logger = LogManager.getLogger();

    protected Result status;
    protected String deliveryURL;
    protected String statusURL;
    protected byte[] contents;
    protected UUID messageID;

    public Message () {
        super();
    }

    public String getDeliveryURL() {
        return deliveryURL;
    }

    public void setDeliveryURL(String deliveryURL) {
        this.deliveryURL = deliveryURL;
    }

    public UUID getMessageID() {
        return messageID;
    }

    public void setMessageID(UUID messageID) {
        this.messageID = messageID;
    }

    public String getStatusURL() {
        return statusURL;
    }

    public void setStatusURL(String statusURL) {
        this.statusURL = statusURL;
    }

    public byte[] getContents() {
        return contents;
    }

    public void setContents(byte[] contents) {
        this.contents = contents;
    }

    public Result getStatus() {
        return status;
    }

    public String toString () {
        if (messageID == null) {
            return "not initialized";
        } else {
            return messageID.toString();
        }
    }

    /*
     * Tell the client that we have created their message
     *
     * In the situation that the client doesn't return a 200 code this method logs an error.
     */
    public void informOfCreated() {
        logger.debug("entering informOfCreated");
        HttpClient httpClient = HttpClient.newBuilder().build();
        String postContents = "MESSAGE CREATED " + messageID;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getDeliveryURL()))
                .POST(HttpRequest.BodyPublishers.ofString(postContents))
                .build();
        logger.debug("built message, " + postContents);
        CompletableFuture<HttpResponse<String>> response =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        int statusCode;
        try {
            statusCode = response.thenApply(HttpResponse::statusCode).get(5, TimeUnit.SECONDS);
            logger.debug("status code = " + statusCode);
            if (statusCode != 200) {
                logger.error("Status returned " + statusCode + " instead of a 200");
            }
        } catch (Exception e) {
            logger.error("Exception during informOfCreated", e);
        }
        logger.debug("leaving informOfCreated");
    }

    public Result deliver() {
        logger.debug("entering deliver");
        HttpClient httpClient = HttpClient.newBuilder().build();
        String postContents = "MESSAGE DELIVERED " + messageID;
        logger.debug("built message, " + postContents);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getDeliveryURL()))
                .POST(HttpRequest.BodyPublishers.ofString(postContents))
                .build();
        CompletableFuture<HttpResponse<String>> response =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = 0;
        try {
            statusCode = response.thenApply(HttpResponse::statusCode).get(5, TimeUnit.SECONDS);
            logger.debug("statusCode = " + statusCode);
            if (statusCode != 200) {
                logger.info("deliver failed");
            }
        } catch (Exception e) {
            logger.info("Exception during deliver", e);
        }

        Result result = new Result();
        result.setStatus(statusCode);
        logger.debug("leaving deliver, status code = " + statusCode);
        return result;
    }


    public void informOfDelivery() {
        logger.debug("entering informOfDelivery");
        HttpClient httpClient = HttpClient.newBuilder().build();
        String postContents = "MESSAGE DELIVERED " + messageID;
        logger.debug("POST contents = " + postContents);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getDeliveryURL()))
                .POST(HttpRequest.BodyPublishers.ofString(postContents))
                .build();
        CompletableFuture<HttpResponse<String>> response =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        int statusCode;
        try {
            statusCode = response.thenApply(HttpResponse::statusCode).get(5, TimeUnit.SECONDS);
            logger.debug("statusCode = " + statusCode);
            if (statusCode != 200) {
                logger.error("informOfDelivery failed");
            }
        } catch (Exception e) {
            logger.error("Exception during informOfCreated", e);
        }

        logger.debug("leaving informOfDelivery");
    }

    /*
     * is this Message equal to another Message?
     */
    public boolean equals (Object obj) {
        if (!(obj instanceof  Message)) {
            logger.debug("is " + obj + " an instance of " + Message.class + ", return false");
            return false;
        }

        Message other = (Message) obj;

        if (!(other.messageID.equals(messageID))) {
            return false;
        } else if (!(other.statusURL.equals(statusURL))) {
            return false;
        } else if (status != null && !status.equals(other.status)) {
            return false;
        } else if (!other.deliveryURL.equals(deliveryURL)) {
            return false;
        } else if (contents != null) {
            if (other.contents == null) {
                return false;
            }
            if (contents.length != other.contents.length) {
                return false;
            } else {
                for (int i = 0; i < contents.length; i++) {
                    if (contents[i] != other.contents[i]) {
                        return false;
                    }
                }
            }

        }

        return true;

    }
}
