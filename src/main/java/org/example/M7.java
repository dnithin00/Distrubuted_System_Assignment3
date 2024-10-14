package org.example;

import java.net.SocketException;

public class M7 extends PaxosNode {
    public M7() throws SocketException {
        super(7, 5, "acceptor"); // M4 is an Acceptor
        setDropMessages(false); // M4 doesn't drop messages
        setDelay(false); // M4 responds instantly
        setAlwaysAccept(true); // M5 will not accept proposals by default

    }

    public static void main(String[] args) throws Exception {
        M7 m7 = new M7();
        m7.start();
    }
}