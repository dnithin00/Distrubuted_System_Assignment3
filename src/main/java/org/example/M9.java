package org.example;

import java.net.SocketException;

public class M9 extends PaxosNode {
    public M9() throws SocketException {
        super(9, 5, "acceptor", null); // M4 is an Acceptor
        setDropMessages(false); // M4 doesn't drop messages
        setDelay(false); // M4 responds instantly
        setAlwaysAccept(true); // M5 will not accept proposals by default

    }

    public static void main(String[] args) throws Exception {
        M9 m9 = new M9();
        m9.start();
    }
}