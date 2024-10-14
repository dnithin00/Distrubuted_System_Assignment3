package org.example;
//package org.example;

import java.net.SocketException;

public class M2 extends PaxosNode {
    public M2() throws SocketException {
        super(2, 5, "acceptor");
        setDropMessages(false); // Simulates dropping messages
        setDelay(false); // Simulates delay in responses
        setAlwaysAccept(true); // M2 will not accept proposals by default
    }

    public static void main(String[] args) throws Exception {
        M2 m2 = new M2();
        m2.start();
    }
}
