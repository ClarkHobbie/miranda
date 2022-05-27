package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;

import java.util.*;

/**
 * An election that makes one node the leader
 */
public class Election {
    /**
     * How each node voted
     */
    protected Map<UUID, Integer> votes = new HashMap<>();

    public Map<UUID, Integer> getVotes() {
        return votes;
    }

    public void setVotes(Map<UUID, Integer> votes) {
        this.votes = votes;
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

    /**
     * Get the results of the election
     *
     * <H>
     *     An election can be a tie or a node (called the president) can be elected the leader.
     * </H>
     * @return The results of the election.
     */
    public ElectionResults getResult () {
        boolean tie = false;
        Set<UUID> voters = votes.keySet();
        Iterator<UUID> iterator = voters.iterator();
        int highest = -1;
        UUID president = null;
        while (iterator.hasNext()) {
            UUID voter = iterator.next();
            int vote = votes.get(voter);

            if (president == null) {
                president = voter;
                highest = vote;
            }

            if (vote > highest) {
                highest = vote;
                tie = false;
            } else if (vote == highest) {
                tie = true;
            }
        }
        if (tie) {
            return ElectionResults.TIE;
        } else {
            return ElectionResults.LEADER_ELECTED;
        }
    }

    /**
     * Register a vote in the election
     *
     * @param uuid The node voting
     * @param vote Their vote
     */
    public void vote (UUID uuid, int vote) {
        votes.put(uuid, vote);
    }

    /**
     * Return true if all nodes have voted
     *
     * @return Weather all the nodes have voted
     */
    public boolean allVotesIn () {
        Collection col = votes.values();
        Iterator<Integer> iterator = col.iterator();
        while (iterator.hasNext()) {
            int vote = iterator.next();
            if (vote == -1) {
                return false;
            }

        }
        return true;
    }

    /**
     * In the case where a leader has been elected, the new leader
     * <H>
     *     Note this method assumes that a) all nodes have voted and b) that the outcome of the election was a leader
     *     getting elected.
     * </H>
     * @return The uuid of the new leader
     * @throws LtsllcException If a leader was not elected.
     */
    public UUID getLeader() throws LtsllcException {
        ElectionResults result = getResult();
        if (result != ElectionResults.LEADER_ELECTED) {
            throw new LtsllcException("the election did not result in a leader being selected");
        }

        Set<UUID> candidates = votes.keySet();
        Iterator<UUID> iterator = candidates.iterator();
        int highest = -1;
        UUID president = null;
        while (iterator.hasNext()) {
            UUID candidate = iterator.next();
            int vote = votes.get(candidate);

            if (president == null) {
                highest = votes.get(candidate);
                president = candidate;
            }

            if (vote > highest) {
                president = candidate;
                highest = vote;
            }
        }

        return president;
    }


    /**
     * Get a list of all the nodes that voted.
     *
     * @return A list of all the nodes that voted.
     */
    public List<UUID> getVoters() {
        List<UUID> newList = new ArrayList<>();
        Set<UUID> set = votes.keySet();
        Iterator<UUID> iterator = set.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            newList.add(uuid);
        }

        return newList;
    }


}
