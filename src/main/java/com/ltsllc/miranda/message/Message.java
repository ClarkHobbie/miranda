package com.ltsllc.miranda.message;

import com.ltsllc.commons.HexConverter;
import com.ltsllc.commons.io.ScannerWithUnget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.Param;
import org.asynchttpclient.Response;

import java.util.*;

/**
 * A class that represents a POST to the message port.  The caller is expected to supply a status URL and a delivery
 * URL to which this instance will be delivered to.
 */
public class Message implements Comparable<Message> {
    public static int Delivered = 1;
    public static int Pending = 0;

    protected AsyncCompletionHandler<Response> completionHandler;

    public AsyncCompletionHandler<Response> getCompletionHandler() {
        return completionHandler;
    }

    public void setCompletionHandler(AsyncCompletionHandler<Response> completionHandler) {
        this.completionHandler = completionHandler;
    }

    protected static Logger logger = LogManager.getLogger(Message.class);

    protected int status;
    protected String deliveryURL;
    protected String statusURL;
    protected List<Param> paramList = new ArrayList<>();
    protected byte[] contents;
    protected UUID messageID;
    protected long lastSend = 0;
    protected int numberOfSends = 0;
    protected long nextSend = 0;

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

    public List<Param> getParamList() {
        return paramList;
    }

    public void setParamList(List<Param> paramList) {
        this.paramList = paramList;
    }

    public synchronized long getLastSend() {
        return lastSend;
    }

    public void setLastSend(long lastSend) {
        this.lastSend = lastSend;
    }

    public int getNumberOfSends() {
        return numberOfSends;
    }

    public void setNumberOfSends(int numberOfSends) {
        this.numberOfSends = numberOfSends;
    }

    public long getNextSend() {
        return nextSend;
    }

    public void setNextSend(long nextSend) {
        this.nextSend = nextSend;
    }

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
        stringBuffer.append(" PARAMS: ");
        for (Param param : paramList) {
            if (param.getValue() == null || param.getValue().trim().equals("")) {
                logger.debug("invalid parameter: " + param.getName() + " value is null or the empty string");
                continue;
            }

            stringBuffer.append(param.getName());
            stringBuffer.append(" = ");
            stringBuffer.append(param.getValue());
            stringBuffer.append(' ');
        }
        stringBuffer.append("STATUS: ");
        stringBuffer.append(statusURL);
        stringBuffer.append(" DELIVERY: ");
        stringBuffer.append(deliveryURL);
        stringBuffer.append(" NUMBER_OF_SENDS: ");
        stringBuffer.append(numberOfSends);
        stringBuffer.append(" LAST_SEND: ");
        stringBuffer.append(lastSend);
        stringBuffer.append(" NEXT_SEND: ");
        stringBuffer.append(nextSend);
        stringBuffer.append(" CONTENTS: ");
        stringBuffer.append(HexConverter.toHexString(contents));

        returnValue = stringBuffer.toString();

        return returnValue;
    }

    /**
     * Return a message in "long" format
     * <p>
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
        logger.debug("entering readLongFormat with " + s);
        Message newMessage = new Message();
        Scanner scanner = new Scanner(s);

        return readLongFormat(scanner);
    }

    public static Message readLongFormat (Scanner scanner) {
        ScannerWithUnget scannerWithUnget = new ScannerWithUnget(scanner);
        Message newMessage = new Message();
        scannerWithUnget.next();scannerWithUnget.next(); // MESSAGE ID:
        String temp;
        String strID = scannerWithUnget.next();;
        newMessage.setMessageID(UUID.fromString(strID));
        List<Param> paramList = new ArrayList<>();
        String name = null;
        temp = scannerWithUnget.next(); // PARAMS:
        for (name = scannerWithUnget.next(); !name.equals("STATUS:"); name = scannerWithUnget.next()) {
            temp = scannerWithUnget.next();  // =
            String value = scannerWithUnget.next();
            Param param = new Param(name,value);
            paramList.add(param);
        }

        newMessage.paramList = paramList;

        newMessage.statusURL = scannerWithUnget.next();
        temp = scannerWithUnget.next(); // DELIVERY:
        newMessage.deliveryURL = scannerWithUnget.next();
        temp = scannerWithUnget.next(); // NUMBER_OF_SENDS:
        newMessage.numberOfSends = Integer.parseInt(scannerWithUnget.next());
        temp = scannerWithUnget.next(); // LAST_SEND:
        newMessage.lastSend = Long.parseLong(scannerWithUnget.next());
        temp = scannerWithUnget.next(); // NEXT_SEND:
        newMessage.nextSend = Long.parseLong(scannerWithUnget.next());
        temp = scannerWithUnget.next(); // CONTENTS:
        newMessage.contents = HexConverter.toByteArray(scannerWithUnget.next());

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
        Long l1 = Long.valueOf(this.messageID.getMostSignificantBits());
        Long l2 = Long.valueOf(this.messageID.getLeastSignificantBits());

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
