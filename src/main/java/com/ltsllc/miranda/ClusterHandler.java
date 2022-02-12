package com.ltsllc.miranda;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;

public class ClusterHandler implements IoHandler {
    protected static Logger logger = LogManager.getLogger();

    public ClusterHandler () {
        logger.debug("New instance of ClusterHandler");
    }

    public void sessionCreated (IoSession ioSession) {
        logger.debug("adding new node, " + ioSession + " to cluster");
        Cluster.addNode(ioSession);
    }

    @Override
    public void sessionOpened(IoSession ioSession) throws Exception {

    }

    @Override
    public void sessionClosed(IoSession ioSession) throws Exception {
        logger.debug("removing instance, " + ioSession + " from cluster");
        Cluster.removeNode(ioSession);
    }

    @Override
    public void sessionIdle(IoSession ioSession, IdleStatus idleStatus) throws Exception {

    }

    @Override
    public void exceptionCaught(IoSession ioSession, Throwable throwable) throws Exception {

    }

    @Override
    public void messageReceived(IoSession ioSession, Object o) throws Exception {

    }

    @Override
    public void messageSent(IoSession ioSession, Object o) throws Exception {

    }

    @Override
    public void inputClosed(IoSession ioSession) throws Exception {

    }

    @Override
    public void event(IoSession ioSession, FilterEvent filterEvent) throws Exception {

    }

}
