package org.example;

import java.net.SocketException;

public class M10 extends PaxosNode {
    public M10() throws SocketException {
        super(10, 5, "acceptor", null  ); // M4 is an Acceptor
        setDropMessages(false); // M4 doesn't drop messages
        setDelay(false); // M4 responds instantly
        setAlwaysAccept(true); // M5 will not accept proposals by default

    }

    public static void main(String[] args) throws Exception {
        M10 m10 = new M10();
        m10.start();
    }
}