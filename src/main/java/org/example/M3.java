package org.example;
//package org.example;

import java.net.SocketException;

public class M3 extends PaxosNode {
    public M3() throws SocketException {
        super(3, 5, "acceptor");
        setDropMessages(false); // M3 doesn't drop messages
        setDelay(false); // Simulates delay
        //setAcceptancePreference(1); // M3 will only accept proposals from M1
        setAlwaysAccept(true); // M2 will not accept proposals by default

    }

    public static void main(String[] args) throws Exception {
        M3 m3 = new M3();
        m3.start();
    }
}
