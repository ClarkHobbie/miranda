# Introduction
Miranda is about making HTML POSTs reliable.  You POST a message to Miranda, and it worries
about making sure it's delivered.

## How the thing works
* POST to /api/newMessage
* miranda POSTs to the delivery URL
* miranda POSTs to the status URL with the outcome 

### POST to /api/newMessage
Miranda will look for a DELIVERY_URL where the message should be delivered and
a STATUS_URL parameter where status updates will go.  The other parameters
and the content of the message are passed to the delivery URL.  The servlet
responds with the message's new ID.

### miranda POSTs to the delivery URL
Miranda will attempt to deliver the message by POSTing to the delivery URL.
The parameters (other than DELIVERY_URL and STATUS_URL) that were passed to 
the servlet are passed to the delivery URL along with the content of
the original message, are sent as well.

### miranda POSTs to the status URL with the outcome
Miranda will attempt to POST a status update to the status URL. If no status 
URL was given then no update is sent.  The message
sets the MESSAGE_ID parameter to the ID of the message for the 
message that a delivery was attempted.  Miranda set the SUBJECT parameter to
MESSAGE_DELIVERED for a delivery and MESSAGE_DELIVERY_FAILED for a failed
attempt.  In any case, miranda sets the STATUS parameter to the status code
returned to it.  

In the case of a failure, miranda will keep trying until it succeeds.

## Messages
The system revolves around messages.  They have a number of
attributes including
* A delivery URL
* A status URL
* Some parameters and some values
* The contents of the message

### The delivery URL
The delivery URL is the URL that miranda tries to POST to in order
to deliver the message.

### The status URL
The status URL is the URL that miranda POSTs to in order to
inform the client that a message has been delivered.  Such a POST
includes the MESSAGE_ID parameter set to the message's UUID, a
parameter SUBJECT set to MESSAGE_DELIVERED, and the STATUS parameter
set to the status code the delivery URL got when it delivered
the message.  If miranda fails it sends the MESSAGE_ID parameter
set to the message's UUID, the SUBJECT parameter set to
MESSAGE_DELIVERY_FAILED, and the STATUS parameter set to the return
code miranda got when it tried to deliver the message.

### Some parameters and their values
The parameters and their values that were used when the message
was POSTed to miranda are saved along with the message and passed
when making a POST to the delivery URL.

### The contents of the message
The contents of the message, as bytes, is past to the delivery
URL.

# Details
## Ports
Miranda listens to many port for messages including
* Port 3030
* Port 2020

### Port 3030
Miranda listens to this port for various URLs.  These include
* / or index.html
* /api/coalesce
* /api/connections/details
* /api/deleteMessage
* /api/deliver
* /api/newMessage
* /api/numberOfConnections
* /api/numberOfMessages
* /api/properties
* /api/receiveStatus
* /api/saveProperties
* /api/status
* /api/trackMessage
* /api/queue 

#### / or index.html
Also called the index and the welcome page this URL.

#### /api/coalesce
Causes miranda to 'coalesce' it's nodes.  If two nodes point to the
same node then they are merged together.  Two nodes point at the
same thing if they have the same ID. When two nodes merge, the most recent time of last activity
becomes the node's time of last activity.

#### /api/connections/details
This servlet prints out a table with a row for each connection to other nodes in the 
cluster.  Each row contains:
* a representation of the connection to the node
* the host for the node
* the node's id
* the node's state

#### /api/deleteMessage
Delete a message given by its message ID, specified by the parameter messageId.

#### /api/deliver
A URL for testing, this servlet simply prints out the parameters and content
of the POST.

#### /api/newMessage
Create a new message.  This servlet receives a new POST.  It looks for the 
parameters DELIVERY_URL, which becomes the URL that miranda tries to deliver
the message, and STATUS_URL, the URL that miranda will POST status updates to.
The contents of the POST become the content that miranda uses when delivering
the message.

#### /api/numberOfConnections

### Port 2020
This port is where miranda listens for new connections with the rest of the 
cluster.  A new node connects to this port and announces itself with a start
message.  Messages include:
* dead node
* error
* heart beat
* message delivered
* messages
* new message
* new node
* new node confirmed
* owner
* owners
* start
* start acknowledged
* synchronize start
* timeout

This message tells the receiver to take ownship (try to deliver) a message.

#### dead node
````
DEAD NODE <UUID of dead node> <UUID of sender> <random int>
````
This message says that a node is dead who the sender's uuid, and a random
number that is the sender's "bid" to become the new leader.

#### error
```
ERROR <start>
```

#### error start
```
ERROR_START <start>
```

#### message delivered
````
MESSAGE DELIVERED <UUID of message>
````

#### new message
````
MESSAGE CREATED ID: <UUID> PARAMS: <param list> STATUS: <URL> DELIVERY: <URL> NUMBER_OF_SENDS: <number of sends> LAST SEND: <time of last send> NEXT SEND: <time of next send> CONTENTS: <contents of message, hex encoded>
````

#### owner
````
OWNER <message UUID> <owner UUUID>
````

#### start
````
START START <UUID> <host> <port> <time>
````

#### start acknowledged
```
START ACKNOWLEDGED <UUID> <host> <port>
```

#### owners
```
OWNERS <owners> OWNERS END
```

#### messages
```
MESSAGES <messages> MESSAGES END
```

#### synchronization start
```
SYNCHRONIZE START <UUID> <host> <port> <time>
```

## Files
Miranda uses files to do a number of things.  Theses files include
* messages.log
* owners.log
* miranda.properties
* messages.log.backup
* owners.log.backup

### messages.log
This is the systems logfile.  It contains all the messages that the
system delivered or will deliver along with the delivery URL, the
status URL, the parameters and the contents of the message.

