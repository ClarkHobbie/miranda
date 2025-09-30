package com.ltsllc.miranda.message;

import com.ltsllc.commons.HexConverter;
import com.ltsllc.miranda.TestSuperclass;
import org.asynchttpclient.Param;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest extends TestSuperclass {
    @Test
    public void readLongFormat () {
        StringBuilder builder = new StringBuilder();
        builder.append("MESSAGE ID: ");
        UUID messageId = UUID.randomUUID();
        builder.append(messageId.toString());
        builder.append(' ');
        builder.append("OWNER: ");
        UUID ownerID = UUID.randomUUID();
        builder.append(ownerID.toString());
        builder.append(" PARAMS: one = a two = b STATUS: http://foo.bar.com DELIVERY: http://foo.bar.com ");
        builder.append("NUMBER_OF_SENDS: 3 LAST_SEND: 1759179281507 NEXT_SEND: 1759180281246 CONTENTS: 6869");

        Message message = Message.readLongFormat(builder.toString());

        assert (message.getMessageID().equals(messageId));
        assert (message.getOwner().equals(ownerID));
        List<Param> list = message.getParamList();
        Param param = list.getFirst();

        assert (param.getName().equalsIgnoreCase("one"));
        assert (param.getValue().equalsIgnoreCase("a"));

        param = list.getLast();

        assert (param.getName().equalsIgnoreCase("two"));
        assert (param.getValue().equalsIgnoreCase("b"));

        assert (message.getStatusURL().equalsIgnoreCase("http://foo.bar.com"));
        assert (message.getDeliveryURL().equalsIgnoreCase("http://foo.bar.com"));

        assert (Arrays.equals(message.getContents(), HexConverter.toByteArray("6869")));
    }

    @Test
    public void longFormatToString () {

    }

    @Test
    void compareTo() {
    }

    @Test
    void convertToUpperCase() {
    }
}