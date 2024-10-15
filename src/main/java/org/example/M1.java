package org.example;
//package org.example;
import java.net.SocketException;

public class M1 extends PaxosNode {
    public M1() throws SocketException {
        super(1, 5, "proposer", 2); // M1 is a Proposer
        setDropMessages(false); // M1 doesn't drop messages
        setDelay(false); // M1 responds instantly
    }

    public static void main(String[] args) throws Exception {
        M1 m1 = new M1();
        m1.start();
    }
}
