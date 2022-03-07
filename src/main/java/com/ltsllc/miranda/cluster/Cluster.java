package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.Miranda;
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

/**
 * A Miranda cluster
 *
 * A group of nodes exchanging heart beet messages. A heart beet message consists of:
 * <PRE>
 * HEARTBEET &lt;node UUID&gt;
 * </PRE>
 * A node that doesn't respond back with a heart beet in a configurable period of time is considered
 * dead. A dead node is announced via:
 * <PRE>
 * DEAD &lt;UUID of dead node&gt;
 * </PRE>
 * All other nodes must respond in a configurable period of time with:
 * <PRE>
 * DEAD &lt;UUID of dead node&gt;
 * </PRE>
 * The survivors then auction off the dead node's messages.  An auction consists of the survivors
 * creating bids for each message. A bid consists of:
 * <PRE>
 * BID &lt;UUID of message&gt; &lt;the writer's randomly generated integer&gt;
 * </PRE>
 * In the case of a tie the surviving nodes bid again until there is no tie. In the case where
 * there is no tie the winning node doesn't respond with a message: it merely takes possession
 * of the message and goes onto the next bid.  When the survivors have auctioned off all the
 * messages, they go back to heart beets.
 * <P>
 * When a new node joins the cluster it announces itself to all the cluster members via:
 * <PRE>
 * NEW NODE &lt;UUID of new node&gt;
 * </PRE>
 * The other nodes have a configurable amount of time to respond with:
 * <PRE>
 * CONFIRM NEW NODE &lt;UUID of new node&gt;
 * </PRE>
 * <P>
 * When a node creates a new message it informs the other node via:
 * <PRE>
 * NEW MESSAGE &lt;UUID of new message&gt; STATUS: &lt;URL of status URL&gt; DELIVERY: &lt;URL of delivery URL&gt;
 * CONTENTS:
 * &lt;Contents of the message&gt;
 * </PRE>
 * The other nodes make no reply.
 * <P>
 * When a node delivers a message, it tells the rest of the cluster via:
 * <PRE>
 * MESSAGE DELIVERED &lt;UUID of message&gt;
 * </PRE>
 * The other nodes make no reply.
 *
 */
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
    protected static List<IoSession> nodes = new ArrayList<>();

    public Cluster() throws LtsllcException {
    }

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

    protected MessageCache messageCache = new MessageCache();


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
        String contents = "MESSAGE CREATED " + message.getMessageID() + " CONTENTS: ";
        contents += message.getContents();

        logger.debug("POST contents = " + contents);
        for (IoSession ioSession : nodes) {
            WriteFuture future = ioSession.write(contents);
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

        int tcpPort = Miranda.getProperties().getIntProperty(Miranda.PROPERTY_CLUSTER_PORT);
        SocketAddress socketAddress = new InetSocketAddress(tcpPort);

        try {
            logger.debug("listening at port " + tcpPort);
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

    /**
     * Unbind all the ports that the cluster listens to
     *
     * Note that this method should unbind ALL our ports, not just those for the cluster
     */
    public synchronized void releasePorts() {
        logger.debug("entering releaseMessagePort");
        logger.debug("releasing all ports");
        IoAcceptor ioAcceptor = new NioSocketAcceptor();
        ioAcceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter( new TextLineCodecFactory( Charset.forName( "UTF-8" ))));

        ioAcceptor.unbind();
        logger.debug("leaving releaseMessagePort");
    }
 }
