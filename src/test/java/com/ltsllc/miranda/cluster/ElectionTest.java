package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.HexConverter;
import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.logging.MessageLog;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class ElectionTest {

    public Election buildElection () {
        List<Node> nodes = new ArrayList<>();
        EmbeddedChannel embeddedChannel = new EmbeddedChannel();

        Node node1 = new Node(UUID.randomUUID(), "71.237.68.250", 2021, embeddedChannel);
        nodes.add(node1);
        Node node2 = new Node(UUID.randomUUID(), "192.168.0.20", 2021, embeddedChannel);
        nodes.add(node2);
        Node node3 = new Node(UUID.randomUUID(), "192.168.0.21", 2021, embeddedChannel);
        nodes.add(node3);

        Election election = new Election(nodes, UUID.randomUUID());

        return election;
    }


    @Test
    void countVotesStillTallying() {
        Election election = buildElection();

        election.countVotes();
        assert (election.getResult() == ElectionResults.STILL_TALLYING);
    }

    @Test
    public void countVotesLeaderElected () {
        ImprovedRandom improvedRandom = new ImprovedRandom();
        Election election = buildElection();
        for (Election.Voter voter : election.getVoters()) {
            Node node = voter.getNode();
            election.vote(node.getUuid(), improvedRandom.nextInt());
        }
        election.countVotes();

        assert (election.getResult() == ElectionResults.LEADER_ELECTED);
    }

    @Test
    public void countVotesTie () {
        Election election = buildElection();
        for (Election.Voter voter : election.getVoters()) {
            Node node = voter.getNode();
            election.vote(node.getUuid(), Integer.MAX_VALUE);
        }
        election.countVotes();

        assert (election.getResult() == ElectionResults.TIE);
    }

    public Message buildMessage () {
        Message message = new Message();
        message.setMessageID(UUID.randomUUID());
        message.setContents(HexConverter.toByteArray("Hi there"));
        message.setStatusURL("http://localhost");
        message.setDeliveryURL("http://localhost");

        return message;
    }


    @Test
    void divideUpNodesMessages() throws IOException, LtsllcException {
        try {
            Election election = buildElection();
            Node deadNode = election.getVoters().getFirst().getNode();
            election.getVoters().remove(0);
            Node node1 = election.getVoters().get(0).getNode();
            Node node2 = election.getVoters().get(1).getNode();

            MessageLog.defineStatics();
            Message message = buildMessage();
            message.setContents(HexConverter.regularStringToByteArray("Hi there"));
            MessageLog.getInstance().add(message, deadNode.getUuid());

            message = buildMessage();
            byte[] array = HexConverter.regularStringToByteArray("low there");
            message.setContents(array);
            MessageLog.getInstance().add(message, deadNode.getUuid());

            message = buildMessage();
            message.setContents("medium there".getBytes());
            MessageLog.getInstance().add(message, deadNode.getUuid());

            election.setDeadNode(deadNode.getUuid());
            election.divideUpNodesMessages();

            EmbeddedChannel channel = (EmbeddedChannel) node1.getChannel();
            String string1 = channel.readOutbound();
            channel = (EmbeddedChannel) node2.getChannel();
            String string2 = channel.readOutbound();

            assert (string1.length() > 0 || string2.length() > 0);
        } finally {
            MessageLog.getInstance().clear();
        }
    }

    @Test
    void vote() {
        Election election = buildElection();
        Node node = election.getVoters().get(0).getNode();
        election.vote(node.getUuid(), 3);

        Election.Voter voter = election.getVoters().get(0);

        assert (voter.vote == 3);
    }
}