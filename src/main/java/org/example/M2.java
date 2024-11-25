package org.example;
//package org.example;

import java.net.SocketException;

public class M2 extends PaxosNode {
    public M2() throws SocketException {
        super(2, 5, "proposer", 2);
        setDropMessages(false); // Simulates dropping messages
        setShortDelay(false); // Simulates delay in responses
//        setAlwaysAccept(true); // M2 will not accept proposals by default
//        setPreferredNominees(Arrays.asList(2));
    }


    public static void main(String[] args) throws Exception {
        M2 m2 = new M2();
        m2.start();
    }
}
