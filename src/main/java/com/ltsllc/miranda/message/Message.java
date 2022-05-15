package com.ltsllc.miranda.message;

import com.ltsllc.commons.util.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;

/**
 * A class that represents a POST to the message port.  The caller is expected to supply a status URL and a delivery
 * URL to which this instance will be delivered to.
 */
public class Message implements Comparable<Message> {
    protected static Logger logger = LogManager.getLogger();

    protected int status;
    protected String deliveryURL;
    protected String statusURL;
    protected byte[] contents;
    protected UUID messageID;

    public Message () {
        super();
    }

    public Message (Message message) {
        status = message.status;
        deliveryURL = message.deliveryURL;
        statusURL = message.statusURL;
        contents = Arrays.copyOf(message.contents, message.contents.length);
        messageID = new UUID(message.messageID.getMostSignificantBits(), message.messageID.getLeastSignificantBits());
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
     * is this Message equal to another Message?
     */
    public boolean equals (Object obj) {
        if (!(obj instanceof  Message)) {
            logger.debug("is " + obj + " an instance of " + Message.class + ", return false");
            return false;
        }

        Message other = (Message) obj;
        if (!messageID.equals(other.messageID)) {
            return false;
        } else if (!statusURL.equals(other.statusURL)) {
            return false;
        } else if (!deliveryURL.equals(other.deliveryURL)) {
            return false;
        } else {
            return contentsAreEquivalent(contents, other.contents);
        }
    }

    /**
     * Are the two byte arrays equivalent?
     *
     * @param ba1 The first byte array.
     * @param ba2 The second byte array.
     * @return True if the two arrays are equivalent.
     */
    public static boolean contentsAreEquivalent (byte[] ba1, byte[] ba2) {
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
     * Return the internals string
     *
     * The string returned has the form: ID: &lt;UUID of message&gt; STATUS: &lt;status URL&gt; DELIVERY: &lt;delivery
     * URL&gt; CONTENTS: &lt;hex encoded contents&gt;
     *
     * @return The string as discussed.
     */
    public String internalsToString () {
        String returnValue = null;

        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append ("ID: ");
        stringBuffer.append(messageID);
        stringBuffer.append(" STATUS: ");
        stringBuffer.append(statusURL);
        stringBuffer.append(" DELIVERY: ");
        stringBuffer.append(deliveryURL);
        stringBuffer.append(" CONTENTS: ");
        stringBuffer.append(Utils.hexEncode(contents));

        returnValue = stringBuffer.toString();

        return returnValue;
    }

    /**
     * Return a message in "long" format
     *
     * Long format includes all the information we have on the message; and has the format MESSAGE ID: &lt;UUID of
     * message&gt; STATUS: &lt;status URL&gt; DELIVERY: &lt;delivery URL&gt;
     *  CONTENTS: &lt;hex encoded contents&GT;
     *
     * @return The message in the long format.
     */
    public String longToString() {
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append ("MESSAGE ");
        stringBuffer.append(internalsToString());

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
        scanner.next();scanner.next(); // MESSAGE ID:
        newMessage.messageID = UUID.fromString(scanner.next());
        scanner.next(); // <UUID>
        newMessage.statusURL = scanner.next();
        scanner.next(); // weird bug
        newMessage.deliveryURL = scanner.next();
        scanner.next(); // weird bug
        newMessage.contents = Utils.hexDecode(scanner.next());

        logger.debug("leaving readLongFormat with newMessage = " + newMessage);

        return newMessage;
    }

    /**
     * Read a message from a Scanner
     *
     * @param scanner The scanner to read from.
     * @return The message.
     * @see Scanner The scanner.
     */
    public static Message readLongFormat (Scanner scanner) {
        logger.debug ("entering readLong with scanner = " + scanner.toString());

        Message message = new Message();
        scanner.next(); scanner.next(); // weird bug MESSAGE ID
        message.setMessageID(UUID.fromString(scanner.next()));
        scanner.next(); // weird bug STATUS:
        message.setStatusURL(scanner.next());
        scanner.next(); // weird bug DELIVERY:
        message.setDeliveryURL(scanner.next());
        scanner.next(); // weird bug CONTENTS
        message.contents = Utils.hexDecode(scanner.next());

        return message;
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
        scanner.next();scanner.next(); // MESSAGE ID:
        newMessage.messageID = UUID.fromString(scanner.next());
        scanner.next(); // STATUS:
        newMessage.statusURL = scanner.next();
        scanner.next(); // DELIVERY:
        newMessage.deliveryURL = scanner.next();
        scanner.next(); scanner.next(); // LAST STATUS:
        newMessage.status = Integer.parseInt(scanner.next());
        scanner.next(); // CONTENTS:
        newMessage.contents = Utils.hexDecode(scanner.next());

        return newMessage;
    }

    /**
     * Are two messages equivalent?
     *
     * <P>
     *     The problem with this method is that it uses the deprecated class java.lang.Long  This is because the author
     *     couldn't find an alternative class/
     * </P>
     *
     * @param message The message to compare with.
     * @return True if the message is equivalent,
     */
    @Override
    public int compareTo(Message message) {
        //
        // TODO: find a replacement for Long
        //
        Long l1 = new Long(messageID.getMostSignificantBits());
        Long l2 = new Long(messageID.getLeastSignificantBits());

        int result = l1.compareTo(message.messageID.getMostSignificantBits());

        if (result == 0) {
            return l2.compareTo(message.messageID.getLeastSignificantBits());
        } else {
            return result;
        }
    }

    /**
     * Generate a hash code that is based on the message's ID.
     *
     * <P>
     *     This is required because two objects can be equals equivalent, but, with the default algorithm, two objects
     *     are not hashCode equivalent.  Thus classes like HashSet will return false to the contains method even when
     *     it contains another object that is equals equivalent to the object.
     * </P>
     *
     * @return The hashCode for this object.
     */
    @Override
    public int hashCode () {
        return (int) (messageID.getMostSignificantBits() & messageID.getLeastSignificantBits());
    }
}
