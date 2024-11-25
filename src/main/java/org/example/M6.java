package org.example;

import java.net.SocketException;

public class M6 extends PaxosNode {
    public M6() throws SocketException {
        super(6, 5, "acceptor", null); // M4 is an Acceptor
        setDropMessages(false); // M4 doesn't drop messages
        setShortDelay(false); // M4 responds instantly
        setAlwaysAccept(true); // M5 will not accept proposals by default

    }

    public static void main(String[] args) throws Exception {
        M6 m6 = new M6();
        m6.start();
    }
}
