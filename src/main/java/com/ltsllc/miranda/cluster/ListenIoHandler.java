package com.ltsllc.miranda.cluster;

import com.ltsllc.miranda.MessageLog;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;

public class ListenIoHandler implements IoHandler {
    @Override
    public void sessionCreated(IoSession session) throws Exception {

    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        Node node = new Node("unknown", -1);
        node.setConnected(true);

        node.setIoSession(session);
        node.sendStart(node.getIoSession());

        Cluster.getInstance().addNode(node, node.getIoSession());
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {

    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {

    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {

    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        int i = 5;
        i++;
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {

    }

    @Override
    public void inputClosed(IoSession session) throws Exception {

    }

    @Override
    public void event(IoSession session, FilterEvent event) throws Exception {

    }
}
