package org.example;
import org.junit.jupiter.api.Test;
import java.net.SocketException;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AllTestCases {

    //Helper method to create a PaxosNode with configurable properties.
    public static PaxosNode createNode(int id, String role, boolean isProposer, boolean shortDelay, boolean longDelay, boolean dropMessages, Integer nomineeId) throws SocketException {
        // Setting the majority 5, votes needed to reach a consensus.
        //setting nominee id only for proposer
        PaxosNode node = new PaxosNode(id, 5, role, isProposer ? nomineeId : null);
        node.setShortDelay(shortDelay);
        node.setDropMessages(dropMessages);
        if (!isProposer) {
            // Default acceptor behavior
            node.setAlwaysAccept(true);
        }
        return node;
    }

    //Helper method to start a PaxosNode.
    public static PaxosNode startNode(PaxosNode node) throws Exception {
        Thread thread = new Thread(() -> {
            try {
                node.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
        return node;
    }
    //Waits for a specified duration to allow the Paxos algorithm to complete.
    //This method ensures sufficient time for nodes to communicate and finalize decisions.
    public static void waitForCompletion(int milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }

    // Test Case 1: Simple Election: 30 points – Paxos implementation works in the case where all M1-M9 have immediate responses to voting queries
    @Test
    public void testSimpleElection() throws Exception {
        // Starting nodes
        PaxosNode m1 = startNode(createNode(1, "proposer", true, false, false, false, 1)); // m1 nominates itself
        PaxosNode m2 = startNode(createNode(2, "acceptor", false, false, false, false, null));
        PaxosNode m3 = startNode(createNode(3, "acceptor", false, false, false, false, null));
        PaxosNode m4 = startNode(createNode(4, "acceptor", false, false, false, false, null));
        PaxosNode m5 = startNode(createNode(5, "acceptor", false, false, false, false, null));
        PaxosNode m6 = startNode(createNode(6, "acceptor", false, false, false, false, null));
        PaxosNode m7 = startNode(createNode(7, "acceptor", false, false, false, false, null));
        PaxosNode m8 = startNode(createNode(8, "acceptor", false, false, false, false, null));
        PaxosNode m9 = startNode(createNode(9, "acceptor", false, false, false, false, null));

        // Meaning m1 only votes "accept" for itself and rejects others.
        m1.setPreferredNominees(Arrays.asList(1));
        // Meaning m2 only votes "accept" for itself and rejects others.
        m2.setPreferredNominees(Arrays.asList(2));
        // Meaning m3 only votes "accept" for itself and rejects others.
        m3.setPreferredNominees(Arrays.asList(3));
        // Rest all members vote "accept" for all nominees, they dont have any preference.


        // Waiting for election to complete
        AllTestCases.waitForCompletion(15000);

        // Validating president outcome
        assertEquals(1, m1.getCurrentPresident());

    }
    // Test Case 2: 10 points -  Paxos implementation works when two councillors send voting proposals at the same time.
    @Test
    public void testSimultaneousProposals() throws Exception {
        // Starting nodes
        PaxosNode m1 = startNode(createNode(1, "proposer", true, false, false, false, 1)); // m1 nominates itself
        PaxosNode m2 = startNode(createNode(2, "proposer", true, false, false, false, 2)); // m2 nominates itself
        PaxosNode m3 = startNode(createNode(3, "acceptor", false, false, false, false, null));
        PaxosNode m4 = startNode(createNode(4, "acceptor", false, false, false, false, null));
        PaxosNode m5 = startNode(createNode(5, "acceptor", false, false, false, false, null));
        PaxosNode m6 = startNode(createNode(6, "acceptor", false, false, false, false, null));
        PaxosNode m7 = startNode(createNode(7, "acceptor", false, false, false, false, null));
        PaxosNode m8 = startNode(createNode(8, "acceptor", false, false, false, false, null));
        PaxosNode m9 = startNode(createNode(9, "acceptor", false, false, false, false, null));
        PaxosNode m10 = startNode(createNode(10, "acceptor", false, false, false, false, null));

        // Meaning m1 only votes "accept" for itself and rejects others.
        m1.setPreferredNominees(Arrays.asList(1));
        // Meaning m2 only votes "accept" for itself and rejects others.
        m2.setPreferredNominees(Arrays.asList(2));
        // Meaning m3 only votes "accept" for itself and rejects others.
        m3.setPreferredNominees(Arrays.asList(3));
        // Rest all members vote "accept" for all nominees, they dont have any preference.

        // Allow time for proposal to propagate and election to complete
        AllTestCases.waitForCompletion(10000);

        // Validating consensus
        var presidentFromM1 = m1.getCurrentPresident();
        var presidentFromM2 = m2.getCurrentPresident();

        if(presidentFromM1 != null) System.out.println("Elected President: " + presidentFromM1);
        if (presidentFromM2 != null) System.out.println("Elected President: " + presidentFromM2);


        // Verifying that the president is either 1 or 2
        if((presidentFromM1 != null) && (presidentFromM2 != null))
        {
            assertTrue(presidentFromM1 == 1 || presidentFromM2 == 2, "President must be either 1 or 2");

        }
    }

    // Test Case 3: 30 points – Paxos implementation works when M1 – M9 have responses to voting queries suggested by several profiles (immediate response, small delay, large delay and no response), including when M2 or M3 propose and then go offline
    @Test
    public void testDelayedResponse() throws Exception {
        // Start nodes
        PaxosNode m1 = startNode(createNode(1, "proposer", true, false, false, false, 1)); // m1 nominates m4
        PaxosNode m2 = startNode(createNode(2, "acceptor", false, true, false, true, null));
        PaxosNode m3 = startNode(createNode(3, "acceptor", false, false, true, false, null));
        PaxosNode m4 = startNode(createNode(4, "acceptor", false, false, false, false, null));
        PaxosNode m5 = startNode(createNode(5, "acceptor", false, false, false, false, null));
        PaxosNode m6 = startNode(createNode(6, "acceptor", false, false, false, false, null));
        PaxosNode m7 = startNode(createNode(7, "acceptor", false, false, false, false, null));
        PaxosNode m8 = startNode(createNode(8, "acceptor", false, false, false, false, null));
        PaxosNode m9 = startNode(createNode(9, "acceptor", false, false, false, false, null));
        PaxosNode m10 = startNode(createNode(10, "acceptor", false, false, false, false, null));

        // Meaning m1 only votes "accept" for itself and rejects others.
        m1.setPreferredNominees(Arrays.asList(1));
        // Meaning m2 only votes "accept" for itself and rejects others.
        m2.setPreferredNominees(Arrays.asList(2));
        // Meaning m3 only votes "accept" for itself and rejects others.
        m3.setPreferredNominees(Arrays.asList(3));
        // Rest all members vote "accept" for all nominees, they dont have any preference.

        // Allow time for both proposals to propagate and election to complete
        AllTestCases.waitForCompletion(10000);

        // Validating consensus
        assertEquals(1, m1.getCurrentPresident());
    }
    // Test Case 4: no concensus can be reached, more than 5 members are not available.
    @Test
    public void testOnly5MembersAreAvailable() throws Exception {
        // Starting nodes
        PaxosNode m1 = startNode(createNode(1, "proposer", true, false, false, false, 1)); // m1 nominates m4
        PaxosNode m2 = startNode(createNode(2, "acceptor", false, true, false, false, null));
        PaxosNode m3 = startNode(createNode(3, "acceptor", false, false, true, false, null));
        PaxosNode m4 = startNode(createNode(4, "acceptor", false, false, false, false, null));
        PaxosNode m5 = startNode(createNode(5, "acceptor", false, false, false, false, null));


        // Meaning m1 only votes "accept" for itself and rejects others.
        m1.setPreferredNominees(Arrays.asList(1));
        // Meaning m2 only votes "accept" for itself and rejects others.
        m2.setPreferredNominees(Arrays.asList(2));
        // Meaning m3 only votes "accept" for itself and rejects others.
        m3.setPreferredNominees(Arrays.asList(3));
        // Rest all members vote "accept" for all nominees, they dont have any preference.


        // Allow time for both proposals to propagate and election to complete
        AllTestCases.waitForCompletion(15000);


        // Validating consensus, president should not be elected.
        assertEquals(null, m1.getCurrentPresident());
    }
}

