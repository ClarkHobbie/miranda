# Things to do in the com.ltsllc.miranda.message package
* Make MessageEventLogger persistent

## Make MessageEventLogger persistent
Currently, when miranda is shut down, all the events are lost.  Change MessageEventLogger so that
events are not lost, and persist through restarts. 


