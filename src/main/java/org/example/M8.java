package org.example;

import java.net.SocketException;

public class M8 extends PaxosNode {
    public M8() throws SocketException {
        super(8, 5, "acceptor"); // M4 is an Acceptor
        setDropMessages(false); // M4 doesn't drop messages
        setDelay(false); // M4 responds instantly
        setAlwaysAccept(true); // M5 will not accept proposals by default

    }

    public static void main(String[] args) throws Exception {
        M8 m8 = new M8();
        m8.start();
    }
}