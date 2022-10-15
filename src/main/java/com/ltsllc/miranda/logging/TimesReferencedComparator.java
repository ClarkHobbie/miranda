package com.ltsllc.miranda.logging;

import com.ltsllc.miranda.message.Message;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

public class TimesReferencedComparator implements Comparator<Message> {
    Map<UUID, Integer> uuidToTimesReferanced;

    public TimesReferencedComparator(Map<UUID, Integer> uuidToTimesReferenced) {
        this.uuidToTimesReferanced = uuidToTimesReferenced;
    }


    @Override
    public int compare(Message m1,Message m2) {
        if (uuidToTimesReferanced.get(m1.getMessageID())>uuidToTimesReferanced.get(m2.getMessageID())) {
            return 1;
        } else if (uuidToTimesReferanced.get(m1.getMessageID()) < uuidToTimesReferanced.get(m2.getMessageID())) {
            return -1;
        } else {
            return 0;
        }
    }
}
