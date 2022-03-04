package com.ltsllc.miranda;

import com.ltsllc.commons.util.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Message {
    protected static Logger logger = LogManager.getLogger();

    protected int status;
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int newStatus) { status = newStatus;}

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

        if (!(Utils.bothEqualCheckForNull(messageID, other.messageID))) {
            return false;
        } else if (!(Utils.bothEqualCheckForNull(statusURL, other.statusURL))) {
            return false;
        } else if (!Utils.bothEqualCheckForNull(deliveryURL, other.deliveryURL)) {
            return false;
        } else if (contents == other.contents) {
            return true;
        } else if ((contents == null) || (other.contents == null)) {
            return false;
        } else {
            return this.contentsAreEquivalent(contents, other.contents);
        }
    }

    public boolean contentsAreEquivalent (byte[] ba1, byte[] ba2) {
        if (ba1 == ba2) {
            return true;
        } else if ((ba1 == null) || (ba2 == null)) {
            return false;
        } else if (ba1.length != ba2.length) {
            return false;
        } else {
            for (int i = 0; i < ba1.length; i++) {
                if (ba1[i] != ba2[i]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return a message in "long" format
     *
     * Long format includes all the information we have on the message; and has the format MESSAGE ID: &lt;UUID of
     * message&gt; STATUS: &lt;status URL&gt; DELIVERY: &lt;delivery URL&gt;
     *  CONTENTS: lt;hex encoded contents&GT;
     *
     * @return The message in the long format.
     */
    public String longToString() {
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append ("MESSAGE ID: ");
        stringBuffer.append(messageID);
        stringBuffer.append(" STATUS: ");
        stringBuffer.append(statusURL);
        stringBuffer.append(" DELIVERY: ");
        stringBuffer.append(deliveryURL);
        stringBuffer.append(" CONTENTS: ");
        stringBuffer.append(Utils.hexEncode(contents));

        return stringBuffer.toString();
    }

    /**
     * Read a Message in long format.
     *
     * @param s The string to read from.
     * @return The encoded message.
     */
    public static Message readLongFormat (String s) {
        logger.debug("entering readLongFormat with s = " + s);
        Message newMessage = new Message();
        Scanner scanner = new Scanner(s);
        scanner.skip ("MESSAGE ID: ");
        newMessage.messageID = UUID.fromString(scanner.next());
        scanner.next(); // weird bug
        newMessage.statusURL = scanner.next();
        scanner.next(); // weird bug
        newMessage.deliveryURL = scanner.next();
        scanner.next(); // weird bug
        newMessage.contents = Utils.hexDecode(scanner.next());

        logger.debug("leaving readLongFormat with newMessage = " + newMessage);

        return newMessage;
    }

    /**
     * Return a string with all the information we have about the message
     *
     * This method is very similar to {@link Message#longToString()} except it includes information that
     * {@link Message#longToString()} leaves out.  The format for the string is:
     * <PRE>
     * MESSAGE ID: &lt;UUID of message&gt; STATUS: &lt;status URL&gt; DELIVERY: &lt;delivery URL&gt; LAST STATUS: &lt;status code from last delivery attempt&gt; CONTENTS: &lt;hex encoded contents&gt;
     * </PRE>
     *
     * @return The message, in the format discussed above
     */
    public String everythingToString () {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("MESSAGE ID: ");
        stringBuffer.append(messageID);
        stringBuffer.append(" STATUS: ");
        stringBuffer.append(statusURL);
        stringBuffer.append(" DELIVERY: ");
        stringBuffer.append(deliveryURL);
        stringBuffer.append(" LAST STATUS: ");
        stringBuffer.append(status);
        stringBuffer.append(" CONTENTS: ");
        stringBuffer.append(Utils.hexEncode(contents));

        return stringBuffer.toString();
    }

    /**
     * Read a message in everything format
     *
     * The everything format is:
     * <PRE>
     *
     * </PRE>
     * MESSAGE ID: &lt;UUID of message&gt; STATUS: &lt;status URL&gt; DELIVERY: &lt;delivery URL&gt; LAST STATUS: &lt;status code from last delivery attempt&gt; CONTENTS: &lt;hex encoded contents&gt;
     * @param s The string to read from
     * @return The encoded format
     */
    public static Message readEverything (String s) {
        Message newMessage = new Message();

        Scanner scanner = new Scanner(s);
        scanner.skip ("MESSAGE ID: ");
        newMessage.messageID = UUID.fromString(scanner.next());
        scanner.skip ("STATUS:");
        newMessage.statusURL = scanner.next();
        scanner.skip("DELIVERY:");
        newMessage.deliveryURL = scanner.next();
        scanner.skip("LAST STATUS:");
        newMessage.status = Integer.parseInt(scanner.next());
        scanner.skip("CONTENTS:");
        newMessage.contents = Utils.hexDecode(scanner.next());

        return newMessage;
    }
}
