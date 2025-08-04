package com.ltsllc.miranda.cluster;

public class NodeThread extends Thread {
    public NodeThread(Node theNode,boolean isLoopback) {
        node = theNode;
    }

    protected Node node;
    protected boolean isLoopback;

    public boolean getIsLoopback() {
        return isLoopback;
    }

    public void setIsLoopback(boolean loopback) {
        isLoopback = loopback;
    }

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
            boolean connected = Cluster.getInstance().connectToNode(node,getIsLoopback());
            keepGoing = true;
            if (connected) {
                while (getKeepGoing())
                    ;
            } else {
                throw new RuntimeException("connect failed");
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
