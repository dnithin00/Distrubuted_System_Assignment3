package org.example;
//package org.example;

import java.net.SocketException;

public class M4 extends PaxosNode {
    public M4() throws SocketException {
        super(4, 5, "acceptor", null);
        setDropMessages(false); // M4 doesn't drop messages
        setDelay(false); // No delay
        setAlwaysAccept(true);

        //setAcceptancePreference(2); // M4 will only accept proposals from M2
    }

    public static void main(String[] args) throws Exception {
        M4 m4 = new M4();
        m4.start();
    }
}
