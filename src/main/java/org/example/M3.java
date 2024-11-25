package org.example;
//package org.example;

import java.net.SocketException;
import java.util.Arrays;

public class M3 extends PaxosNode {
    public M3() throws SocketException {
        super(3, 5, "acceptor", null);
        setDropMessages(false); // M3 doesn't drop messages
        setShortDelay(false); // Simulates delay
        //setAcceptancePreference(1); // M3 will only accept proposals from M1
        setAlwaysAccept(true); // M2 will not accept proposals by default
        setPreferredNominees(Arrays.asList(3));

    }

    public static void main(String[] args) throws Exception {
        M3 m3 = new M3();
        m3.start();
    }
}
