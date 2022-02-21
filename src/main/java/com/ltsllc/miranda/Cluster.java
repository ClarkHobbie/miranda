package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.ImprovedRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cluster {
    public static final long BID_WRITE_TIMEOUT = 1000;
    public static final TimeUnit BID_WRITE_TIMEOUT_UNITS = TimeUnit.MILLISECONDS;
    public static final long BID_READ_TIMEOUT = 1000;
    public static final TimeUnit BID_READ_TIMEOUT_UNITS = TimeUnit.MILLISECONDS;
    public static final long IOD_TIMEOUT = 1000;
    public static final TimeUnit IOD_TIMEOUT_UNITS = TimeUnit.MILLISECONDS;
    public static final long IONM_TIMEOUT = 1000;
    public static final TimeUnit IONM_TIMEOUT_UNITS = TimeUnit.MILLISECONDS;

    protected static ImprovedRandom randomNumberGenerator = new ImprovedRandom();
    protected static final Logger logger = LogManager.getLogger();
    protected static List<IoSession> nodes;

    public static List<IoSession> getNodes() {
        return nodes;
    }

    public static void setNodes(List<IoSession> n) {
        nodes = n;
    }

    public static synchronized void addNode (IoSession session) {
        logger.debug("Adding new node, " + session + " to nodes");
        nodes.add (session);
    }

    public static ImprovedRandom getRandomNumberGenerator() {
        return randomNumberGenerator;
    }

    /*
     * Set the random number generator that this node uses
     *
     * The random number generator is used mostly in bidding
     */
    public static void setRandomNumberGenerator(ImprovedRandom randomNumberGenerator) {
        Cluster.randomNumberGenerator = randomNumberGenerator;
    }

    /*
     * remove a node from the cluster
     *
     * This method does so synchronously, so it is thread safe.
     */
    public static synchronized void removeNode(IoSession ioSession) {
        logger.debug("removing node, " + ioSession + " from nodes");
        nodes.remove(ioSession);
    }

    /*
     * inform the cluster of this node receiving a new message
     */
    public void informOfNewMessage(Message message) throws LtsllcException {
        logger.debug("entering informOfNewMessage with message = " + message);
        String contents = "MESSAGE CREATED " + message;
        logger.debug("POST contents = " + contents);
        for (IoSession ioSession : nodes) {
            WriteFuture  future= ioSession.write(contents);
            try {
                if (!future.await(IONM_TIMEOUT, IONM_TIMEOUT_UNITS)) {
                    logger.error("write timed out informing another node of message delivery");
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted during wait in informOfNewMessage", e);
            }
            IoSession nodeSession = future.getSession();
            ReadFuture readFuture = nodeSession.read();
            try {
                if (!readFuture.await(IONM_TIMEOUT, IONM_TIMEOUT_UNITS)) {
                    logger.error("read timed out informing another node of message delivery");
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted during wait in informOfNewMessage", e);
            }
            String statusCodeStr = (String) readFuture.getMessage();
            logger.debug("came back from read with " + statusCodeStr);

            if (Integer.parseInt(statusCodeStr) != 200) {
                logger.error("got back a non-200 code for inform of new message. status = " + statusCodeStr);
            }
        }
        logger.debug("leaving informOfNewMessage");
    }


    /*
     * Tell the cluster that we delivered a message
     */
    public void informOfDelivery(Message message) {
        logger.debug("entering informOfDelivery with message = " + message);

        String contents = "MESSAGE DELIVERED " + message.getMessageID();
        logger.debug("POST contents = " + contents);
        for (IoSession ioSession: nodes) {
            WriteFuture  future = ioSession.write(contents);
            try {
                future.await(IOD_TIMEOUT, IOD_TIMEOUT_UNITS);
            } catch (InterruptedException e) {
                logger.error("Interrupted during wait for write to complete for some in informOfDelivery", e);
            }
            IoSession nodeSession = future.getSession();
            ReadFuture readFuture = nodeSession.read();
            try {
                if (!readFuture.await(IONM_TIMEOUT, IONM_TIMEOUT_UNITS)) {
                    logger.error("read timed out informing another node of message delivery");
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted during wait in informOfNewMessage", e);
            }
            String statusCodeStr = (String) readFuture.getMessage();
            logger.debug("came back from read with " + statusCodeStr);

            if (Integer.parseInt(statusCodeStr) != 200) {
                logger.error("got back a non-200 code for inform of message delivery. status = " + statusCodeStr);
            }
        }

        logger.debug("leaving informOfDelivery");
    }

    /*
     * connect to the cluster
     *
     * This method tries to connect to the other nodes of the cluster.  It sets up a listener for
     * new nodes
     */
    public void connect () throws LtsllcException {
        logger.debug("entering connect with nodes = " + nodes);
        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        logger.debug("building IoAcceptor");
        IoAcceptor ioAcceptor = new NioSocketAcceptor();
        ioAcceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter( new TextLineCodecFactory( Charset.forName( "UTF-8" ))));
        ioAcceptor.setHandler(new ClusterHandler());

        int socketNumber = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        SocketAddress socketAddress = new InetSocketAddress(socketNumber);

        try {
            logger.debug("listening at port " + socketNumber);
            ioAcceptor.bind(socketAddress);
        } catch (IOException e) {
            logger.error ("exception binding to cluster port, " + Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT), e);
            throw new LtsllcException(
                    "exception binding to cluster port, "
                            + Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT),
                    e
            );
        }
        logger.debug("leaving connect with nodes = " + nodes);
    }

     /*
     * bid on a message and return it if our bid is higher than the other nodes in the cluster
     *
     * This method returns a message if we "won" it in a bid with all the nodes in a cluster.
     * Basically, just chose a number.  This is our bid.  If our bid is larger than everyone
     * else's then we won the bid so return the message.  If another node has a higher bid then
     * return null.  In the case of a tie we just go through the process again.
     *
     * @param  message The message we are bidding on.
     * @return         The message if we "won" it, otherwise null
     */
    public Message bid (Message message) throws LtsllcException {
        logger.debug("entering bid with message = " + message);
        for (IoSession ioSession : nodes) {
            int ourBid = randomNumberGenerator.nextInt(0, Integer.MAX_VALUE);
            logger.debug("our bid is " + ourBid);
            logger.debug("sending our bid");
            sendBid(ioSession, ourBid, message);

            int remoteBid = getBid(ioSession, message);
            logger.debug("remote bid is " + remoteBid);

            while (ourBid == remoteBid) {
                ourBid = randomNumberGenerator.nextInt(0, Integer.MAX_VALUE);
                logger.debug("our bid is " + ourBid);
                logger.debug("sending our bid");
                sendBid(ioSession, ourBid, message);

                remoteBid = getBid(ioSession, message);
                logger.debug("remote bid is " + remoteBid);
            }

            if (ourBid < remoteBid) {
                logger.debug("we were outbid, returning null");
                return null;
            }
        }

        logger.debug("we won the message");
        logger.debug("leaving bid, message = " + message);

        return message;
    }

    /*
     * Get a remote bid from the cluster
     *
     * @param ioSession The node that we should get the bid from
     * @param message   The message that we should get the bid for
     */
    protected int getBid (IoSession ioSession, Message message) throws LtsllcException {
        try {
            ReadFuture readFuture = ioSession.read();

            if (!readFuture.await((long) BID_READ_TIMEOUT, BID_READ_TIMEOUT_UNITS)) {
                throw new LtsllcException("exception during await");
            }

            String temp = (String) readFuture.getMessage();
            logger.debug("got " + temp);
            Pattern pattern = Pattern.compile("BID (\\d+) " + message.getDeliveryURL() + ":" + message.getStatusURL());
            Matcher matcher = pattern.matcher(temp);
            String remoteBidString = matcher.group(1);
            if (null == remoteBidString) {
                throw new LtsllcException("malformed bid");
            }

            return Integer.parseInt(remoteBidString);
        } catch (Exception e) {
            throw new LtsllcException("exception during getBid", e);
        }

    }

    /*
     * Send a bid for a message to a node
     *
     * @param ioSession the node to which we should send the bid
     * @param bid       our bid
     * @param message   the message that we are sending the bid for
     */
    protected void sendBid (IoSession ioSession, int bid, Message message) throws LtsllcException {
        String ourMessage = "BID " + Integer.toString(bid, 10) + message.getDeliveryURL() + ":" + message.getStatusURL();
        WriteFuture writeFuture = ioSession.write(ourMessage);
        if (!writeFuture.awaitUninterruptibly(BID_WRITE_TIMEOUT, BID_WRITE_TIMEOUT_UNITS)) {
            throw new LtsllcException("write bid timed out");
        }

    }




}
