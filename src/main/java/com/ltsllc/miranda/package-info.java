/**
 * <IMG SRC="doc-files/fig1.jpg" ALT="The custer protocol">
 * <PRE>
 * &lt;message&gt; ::= &lt;auction&gt; | &lt;auction over&gt; | &lt;bid&gt; | &lt;dead node&gt; | &lt;error&gt;
 *         | &lt;get message&gt; | &lt;heart beat&gt; | &lt;message&gt; | &lt;messages&gt; | &lt;message delivered&gt;
 *         | &lt;new message&gt; | &lt;new node&gt; | &lt;new node confirmed&gt; | &lt;new node over&gt; | &lt;start&gt;
 *         | &lt;timeout&gt;
 * 
 *         &lt;auction&gt; ::= AUCTION &lt;UUID of the node getting auctioned&gt;
 *         &lt;auction over&gt; ::= AUCTION OVER
 *         &lt;bid&gt; := BID &lt;UUID of message&gt; &lt;an integer value&gt;
 *         &lt;dead node&gt; := DEAD &lt;UUID of node&gt;
 *         &lt;error&gt; ::= ERROR
 *         &lt;get message&gt; ::= GET MESSAGE &lt;UUID of message&gt;
 *         &lt;heart beat&gt; := Heart beat &lt;UUID of node&gt;
 *         &lt;message&gt; ::= MESSAGE STATUS: &lt;URL&gt; DELIVERY: &lt;URL&gt; CONTENTS: &lt;contents of message&gt;
 *         &lt;message delivered&gt; := MESSAGE DELIVERED &lt;UUID of message&gt;
 *         &lt;new message&gt; ::= NEW MESSAGE &lt;UUID of message&gt; STATUS: &lt;status URL&gt; DELIVERY: &lt;delivery URL&gt; CONTENT:&lt;newline&gt;
 *         &lt;contents of message&gt;
 *         &lt;new node&gt; ::= NEW NODE &lt;UUID of new node&gt;
 *         &lt;new node confirmed&gt; ::= NEW NODE CONFIRMED &lt;UUID of new node&gt; &lt;UUID of sender&gt;
 *         &lt;new node over&gt; ::= NEW NODE OVER
 *         &lt;start&gt; ::= START
 *         &lt;timeout&gt; ::= TIMEOUT
 * </PRE>
 * <P>
 *     A connection starts in the "start state;"  As shown in figure 1.  A connection can progress to the "general
 *     state" by receiving a start message; or it can transition to the new node state by receiving a new node message.
 *     heart beats, new messages, delivered messages, dead messages as well as being done
 *     with certain exchanges result in the connection remaining in the general state.
 *
 * <P>
 *     An auction message causes the connection to enter the auction state.  The connection remains in that state
 *     through any number of bid messages before it receives an auction over message and returns to the general state.
 *
 * <P>
 *     If the connection receives a start message then it has a configurable amount of time to send a start message of
 *     its
 *     own.  The connection then enters the general state.  This is the "ground state" for a connection and it can
 *     receive
 *     basically any message.  A timeout or error message causes it to re-enter the start state.
 *
 * <P>
 *     A connection enters the new node state by receiving a new node message when in the start state.  The node then
 *     tells
 *     its new "partner" about all the messages it knows about by issuing a message message until it runs out of
 *     messages
 *     at which time it issues a new node over message and returns to the start state.
 *
 * <P>
 *     <PRE>
 *     &lt;dead node&gt; ::= DEAD &lt;UUID of dead node&gt;
 *     &lt;heart beat&gt; ::= HEART BEAT &lt;node UUID&gt;
 *     </PRE>
 *     A node that doesn't respond back with a heart beat message of its own in a configurable period of time is
 *     considered dead.
 *
 * <P>
 *     When a node receives a dead node message,
 *     it must respond in a configurable period of time with a dead node message its own.  If the node
 *     responds to the dead mode message then the connection reenters the general state.  It is suggested that the node
 *     that sent the dead message starts an auction immediately but this not a requirement.
 *
 * 
 * <P>
 *     <PRE>
 *     &lt;auction&gt; ::= AUCTION &lt;UUID of node getting auctioned&gt;
 *     &lt;auction over&gt; ::= AUCTION OVER
 *     &lt;bid&gt; ::= BID &lt;UUID of message&gt; &lt;an integer value&gt;
 *     </PRE>
 *     An auction consists of the two nodes "bidding" on all the messages that a node had.  It is suggested that the
 *     auction take place directly after declaring a node dead, but this is not a requirement.
 *     When a node receives an auction it should respond with its own auction message.  The connection then enters
 *     the auction state; and continues to be in that state until an auction over message is received.
 * 
 * <P>
 *     The integer values in the bid messages are expected to be random values that the nodes create for each bid.  If
 *     the node's value is
 *     greater than the other node's value then the node has "won" the message and it is added to that node's send
 *     queue.
 *     In the case of a tie, both nodes are expected to reissue bids immediately.  If another
 *     tie occurs then the nodes reissue bids until there is no tie. If the other node in the connection issues a bid
 *     for a
 *     message that the node does not have, then if the node wins the message then it should issue a get message message
 *     to get the status and delivery URLs and the message's contents, but this is not a requirement.
 *
 * 
 * <P>
 *     <PRE>
 *     &lt;message&gt; ::= MESSAGE STATUS: &lt;status URL&gt; DELIVERY: &lt;delivery URL&gt; CONTENTS: &lt;contents of message&gt;
 *     &lt;new node&gt; ::= NEW NODE &lt;UUID of new node&gt; &lt;IP address of the new node&gt;
 *     &lt;new node confirmed&gt; ::= NEW NODE CONFIRMED &lt;UUID of new node&gt; &lt;UUID of sender&gt;
 *     &lt;new node over&gt; ::= NEW NODE OVER
 *     </PRE>
 *     When a new node joins the cluster it connects to any node's cluster port and issues a new node message. It then
 *     tries to "catch up" on the messages in the cluster by receiving one or more message messages (the other
 *     node is required to issue one message message for each message it knows about.  When the node has exhausted all
 *     its
 *     it is required to issue a new node over message and then reenters the start state).
 * <P>
 *     <PRE>
 *     &lt;new message&gt; ::= NEW MESSAGE &lt;UUID of new message&gt; STATUS: &lt;status URL&gt; DELIVERY: &lt;delivery URL&gt;
 *             CONTENTS: &lt;Contents of the message&gt;
 *     &lt;message delivered&gt; ::= MESSAGE DELIVERED &lt;UUID of message&gt;
 *     </PRE>
 *     When a node creates a new message it informs the other node via a new message message.  The other node makes no
 *     reply.
 * <P>
 *     When a node delivers a message it informs the other nodes via the message delivered message.  The other
 *     node makes no reply.
 *
 * <P>
 *     <PRE>
 *     &lt;error&gt; ::= ERROR
 *     </PRE>
 *     When a node receives an error message the other node has detected a protocol error.  Unless otherwise noted, the
 *     connection reenters the start state
 * <P>
 *     <PRE>
 *     &lt;get message&gt; ::= GET MESSAGE &lt;UUID of message&gt;
 *     </PRE>
 *     When a node receives this message it must respond with a message message if it has the message.  The time-out for
 *     this is configurable.
 *
 * <P>
 *     <PRE>
 *     &lt;start message&gt; ::= START
 *     </PRE>
 *     When a node receives a start message it has a configurable amount of time to respond with a start message of its
 *     own.  It then enters the general state.
 *
 * <P>
 *     <PRE>
 *     &lt;timeout&gt; ::= TIMEOUT
 *     </PRE>
 *     When a node receives a timeout message it signals that the other node in the connection has timed out waiting for
 *     a reply from the node.  Unless otherwise stated receiving a timeout message transfers the connection, in whatever
 *     state it was in, to go to the start state.
 *
 */
 package com.ltsllc.miranda;
