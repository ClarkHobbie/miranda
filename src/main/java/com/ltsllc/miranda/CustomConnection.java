package com.ltsllc.miranda;

import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.EndPoint;

import java.util.concurrent.Executor;

class CustomConnection extends AbstractConnection
{
    public CustomConnection(EndPoint endPoint, Executor executor)
    {
        super(endPoint, executor);
    }

    @Override
    public void onOpen()
    {
        super.onOpen();
        // System.getLogger("connection").log(INFO, "Opened connection {0}", this);
    }

    @Override
    public void onFillable()
    {
    }
}

