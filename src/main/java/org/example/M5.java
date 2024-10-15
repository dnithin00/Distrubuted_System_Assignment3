package org.example;
//package org.example;

import java.net.SocketException;

public class M5 extends PaxosNode {
    public M5() throws SocketException {
        super(5, 5, "acceptor", null); // M4 is an Acceptor
        setDropMessages(false); // M4 doesn't drop messages
        setDelay(false); // M4 responds instantly
        setAlwaysAccept(true); // M5 will not accept proposals by default

    }

    public static void main(String[] args) throws Exception {
        M5 m5 = new M5();
        m5.start();
    }
}

