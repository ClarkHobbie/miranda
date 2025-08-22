# Things to do
* Make a timer for message sends

## Make a timer for message sends
Currently, miranda just sends out messages every iteration of the
mainLoop.  Change things so that miranda waits a period of time
before sending a message out.  When a message delivery attempt ends
in failure, it waits exponentially longer before trying again. 