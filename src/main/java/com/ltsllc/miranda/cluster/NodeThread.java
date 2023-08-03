package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;

public class NodeThread extends Thread {
    public NodeThread(Node theNode) {
        node = theNode;
    }

    protected Node node;

    public Node getNode () {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    protected boolean keepGoing;

    public boolean getKeepGoing () {
        return keepGoing;
    }

    public void setKeepGoing(boolean value) {
        keepGoing = value;
    }


    public void run () {
        try {
            boolean connected = Cluster.getInstance().connectToNode(node);
            keepGoing = true;
            if (connected) {
                while (getKeepGoing())
                    ;
            } else {
                throw new RuntimeException("connect failed");
            }
        } catch (LtsllcException e) {
            throw new RuntimeException(e);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
