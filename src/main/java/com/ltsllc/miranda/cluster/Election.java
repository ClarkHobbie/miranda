package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.message.MessageLog;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An election that makes one node the leader
 */
public class Election {
    protected ElectionResults result = ElectionResults.UNKNOWN;

    public ElectionResults getResult() {
        return result;
    }

    public void setResult(ElectionResults result) {
        this.result = result;
    }

    /**
     * The node that died thereby setting this election in motion
     */
    protected UUID deadNode;

    public UUID getDeadNode() {
        return deadNode;
    }

    public void setDeadNode(UUID deadNode) {
        this.deadNode = deadNode;
    }

    protected List<Voter> voters = new ArrayList<>();

    public List<Voter> getVoters() {
        return voters;
    }

    public void setVoters(List<Voter> voters) {
        this.voters = voters;
    }

    protected Voter leader = null;

    public Voter getLeader () {
        return leader;
    }

    public void setLeader(Voter leader) {
        this.leader = leader;
    }

    public Election (List<Node> list, UUID deadNode) {
        for (Node node : list) {
            if (node.getUuid().equals(deadNode)) {
                continue;
            }

            if ((node.getChannel() != null) && (node.getUuid() != null)) {
                Voter voter = new Voter();
                voter.node = node;
                voters.add(voter);
            }
        }
    }


    /**
     * Tally up the votes and put the result in result
     */
    public void countVotes()  {
        result = ElectionResults.UNKNOWN;
        int highest = Integer.MIN_VALUE;
        if (allVotesIn()) {
            for (Voter voter : voters) {
                if (voter.vote > highest) {
                    highest = voter.vote;
                    leader = voter;
                    result = ElectionResults.LEADER_ELECTED;
                } else if (voter.vote == highest) {
                    result = ElectionResults.TIE;
                }
            }
        } else {
            result = ElectionResults.STILL_TALLYING;
        }
    }


    /**
     * Divide up a node's messages among the still-connected nodes
     * <H>
     * This method assumes that there has been an election.
     * </H>
     */
    public synchronized void divideUpNodesMessages() throws LtsllcException, IOException {
        if (result == ElectionResults.TIE) {
            throw new LtsllcException("tie in divideUpNodesMessages");
        }

        if (null == deadNode) {
            throw new LtsllcException("null deadnode");
        }

        UUID uuid = deadNode;

        ImprovedRandom random = new ImprovedRandom(new SecureRandom());
        List<UUID> messages = MessageLog.getInstance().getAllMessagesOwnedBy(uuid);
        for (UUID message : messages) {
            Voter voter = random.choose(Voter.class, voters);
            voter.node.assignMessage(message);
        }
    }

    public boolean containsOneNode () {
        return voters.size() == 1;
    }


    /**
     * Register a vote in the election
     *
     * @param uuid The node voting
     * @param vote Their vote
     */
    public void vote (UUID uuid, int vote) {
        for (Voter voter:voters) {
            if (uuid == voter.node.getUuid()) {
                voter.vote = vote;
            }
        }
    }

    /**
     * Return true if all nodes have voted
     *
     * @return Weather all the nodes have voted
     */
    public boolean allVotesIn () {
        for (Voter voter: voters) {
            if (voter.vote == Integer.MIN_VALUE) {
                return false;
            }
        }
        return true;
    }


    public class Voter {
        public int vote = Integer.MIN_VALUE;

        protected Node node;

        public Node getNode() {
            return node;
        }

        public void setNode(Node node) {
            this.node = node;
        }
    }

}
