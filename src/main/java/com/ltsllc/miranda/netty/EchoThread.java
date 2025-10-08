package com.ltsllc.miranda.netty;

import io.netty.bootstrap.Bootstrap;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class EchoThread implements Runnable {
    public Bootstrap bootstrap;
    public String host;
    public int port;

    public void run() {
        Echo echo = new Echo("10.0.0.49", 4040);
        SocketAddress address = new InetSocketAddress(host, port);
        echo.connect(bootstrap);
    }
}
