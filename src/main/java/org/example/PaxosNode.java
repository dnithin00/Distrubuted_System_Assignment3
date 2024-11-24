package org.example;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PaxosNode {
    private int id;  // Node ID
    private int majority;  // Majority needed for consensus
    private DatagramSocket socket; // Socket for communication
    private String role; // Role of the node (proposer, acceptor)
    private int promiseCount = 0;


    private int proposalNumber; // Current proposal number (round identifier)
    private int rejectionCount;
    private Map<Integer, Integer> promises; // Store promises made by acceptors
    private int highestPromisedProposal; // Track the highest promised proposal number by this acceptor
    private List<Integer> acceptedValues; // Store accepted values from acceptors

    private boolean isDelayed; // Flag for simulating delays
    private boolean dropMessages; // Flag for simulating message drops
    private boolean alwaysAccept;  // Flag for custom acceptance rule
    private int preferredProposerId; // If the node only accepts from a specific proposer (e.g., M3 likes M1)
    private Integer nomineeId;  // New variable for the president nominee


    private List<Integer> preferredNominees;
    private List<Integer> preferredProposers;


    public PaxosNode(int id, int majority, String role, Integer nomineeId) throws SocketException {
        this.id = id;
        this.majority = majority;
        this.role = role;
        //this.nomineeId = nomineeId;
        // For proposers, nomineeId is required. For acceptors, we set it to -1 by default.
        if (role.equals("proposer") && nomineeId != null) {
            this.nomineeId = nomineeId;  // Nominee provided by proposer
        } else {
            this.nomineeId = -1;  // Default for acceptors
        }
        this.socket = new DatagramSocket(8000 + id); // Each node listens on a port based on its ID
        this.proposalNumber = 1000;
        this.highestPromisedProposal = 0; // Initialize the highest promised proposal
        this.promises = new HashMap<>();
        this.acceptedValues = new ArrayList<>();
        this.isDelayed = false; // Default is no delay
        this.dropMessages = false; // Default is not to drop messages
        this.alwaysAccept = true;  // Default to always accept
        this.preferredProposerId = -1; // No preference by default
    }

    public void setDelay(boolean isDelayed) {
        this.isDelayed = isDelayed;
    }

    public void setDropMessages(boolean dropMessages) {
        this.dropMessages = dropMessages;
    }

    public void start() {
        // Start node based on its role
        if (role.equals("proposer")) {
            startAsProposer();
        } else if (role.equals("acceptor")) {
            startAsAcceptor();
        } else {
            throw new IllegalArgumentException("Invalid role specified");
        }
    }

    private void startAsProposer() {
        System.out.println("Node " + id + " is starting as Proposer.");

        // Propose a value
        sendPrepareMessage();

        // Wait for responses from acceptors
        listenForResponses();
    }

    private void sendPrepareMessage() {
        // Increment proposal number (this acts as a round identifier)
        proposalNumber++;
        // Propose a new value (here we can just use the node's ID for simplicity)
        //here I should the president nominee id aswell? the acceptors should knwo right? which nominee they are voting for
        //no not here, in the proposal/accept message?
        //String message = "Prepare:" + proposalNumber + " From member:" + id;
        // The message now includes both the proposal number and the ID of the proposer
        String message = "Prepare:" + proposalNumber + ":FromMember:" + id + ":Nominee:" + nomineeId;



        System.out.println("Node " + id + " is proposing value with proposal number " + proposalNumber + "for member nominee: " + nomineeId);

        // Send Prepare message to all acceptors
        for (int i = 2; i <= 10; i++) { // Assume there are 10 nodes in total
            if (i != id) {
                sendMessage(message, 8000 + i);
            }
        }
    }

//    private void listenForResponses() {
//        promiseCount = 0;
//        rejectionCount = 0;  // Track rejections
//        int totalResponses = 0;
//
//        // Time-based waiting (timeout of 5 seconds)
//        long startTime = System.currentTimeMillis();
//        long timeout = 5000;  // 5 seconds
//
//        // Wait until all nodes respond or timeout is reached
//        while (System.currentTimeMillis() - startTime < timeout && totalResponses < 10) {  // Assume 10 nodes in total
//            try {
//                byte[] buffer = new byte[1024];
//                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//                socket.receive(packet); // Receive a packet
//
//                String receivedMessage = new String(packet.getData()).trim();
//                processResponse(receivedMessage, packet.getAddress(), packet.getPort());
//
//                totalResponses++;
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        // Evaluate promises and rejections after timeout or after all responses are received
//        if (promiseCount >= majority) {
//            // Now send Accept requests to those who promised
//            sendProposalMessage();
//        } else if (promiseCount < majority) {
//            // Retry with a higher proposal number if majority rejected
//            System.out.println("Node " + id + " proposal was rejected by majority. Retrying with a higher proposal number...");
//            proposalNumber++;
//            sendPrepareMessage();
//        } else {
//            System.out.println("Timed out before receiving enough responses, proceeding with available data.");
//            // Optionally retry or fail the proposal based on your protocol design
//        }
//
//        // After promises are gathered, wait for majority Accepted values
//        listenForAcceptedValues();
//    }
    private void listenForResponses() {
        promiseCount = 0;
        rejectionCount = 0;
        int totalResponses = 0;
        int expectedResponses = 9; // Total nodes in the cluster
        long startTime = System.currentTimeMillis();
        long timeout = 3000; // Timeout in milliseconds

        try {
            socket.setSoTimeout((int) timeout); // Set socket timeout to prevent indefinite blocking
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while ((System.currentTimeMillis() - startTime < timeout) && totalResponses < expectedResponses) {
            try {
                System.out.println("Waiting for responses...");
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.receive(packet); // Receive a packet

                String receivedMessage = new String(packet.getData()).trim();
                processResponse(receivedMessage, packet.getAddress(), packet.getPort());

                totalResponses++;
                System.out.println("Total Responses: " + totalResponses + " | Promise Count: " + promiseCount);

            } catch (SocketTimeoutException e) {
                System.out.println("Socket timeout reached while waiting for responses.");
                break; // Exit loop on socket timeout
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Evaluate promises and rejections
        if (promiseCount >= majority) {
            // Now send Accept requests to those who promised
            sendProposalMessage();
        } else if (rejectionCount >= majority) {
            // Retry with a higher proposal number if majority rejected
            System.out.println("Node " + id + " proposal was rejected by majority. Retrying with a higher proposal number...");
            proposalNumber++;
            sendPrepareMessage();
            //sendPrepareMessages();
        } else {
            System.out.println("Timed out before receiving enough responses, proceeding with available data.");
        }

        // **After promises are gathered, wait for majority Accepted values**
        listenForAcceptedValues();
    }

    private void listenForAcceptedValues() {
        int acceptedCount = 0;  // Count the number of accepted responses

        while (acceptedCount < majority) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Receive a packet

                String receivedMessage = new String(packet.getData()).trim();
                if (receivedMessage.startsWith("Accepted")) {
                    processResponse(receivedMessage, packet.getAddress(), packet.getPort());

                    acceptedCount = acceptedValues.size();  // Track the count of accepted values
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //Proposer method
    private void processResponse(String message, InetAddress address, int port) {
        // Handle responses from acceptors
        String[] parts = message.split(":");
        // Handle Promise response
        if (parts[0].equals("Promise")) {
            int promisedProposal = Integer.parseInt(parts[1]);
            promises.put(port, promisedProposal);
            promiseCount++;
//            System.out.println("totalResponse: " +);
            System.out.println( " PromiseCount: " +promiseCount);
            System.out.println("Node " + id + " received promise from Node " + (port - 8000) + " for proposal " + promisedProposal);

            // Handle Reject response
        } else if (parts[0].equals("Reject")) {
            int rejectedProposal = Integer.parseInt(parts[1]);
            System.out.println("Node " + id + " received rejection from Node " + (port - 8000) + " for proposal " + rejectedProposal);
            rejectionCount++;
        }

        // Handle Accepted response
        else if (parts[0].equals("Accepted")) {
            int presidentNominee = Integer.parseInt(parts[1]);
            int acceptorMemberId = Integer.parseInt(parts[2]);
            acceptedValues.add(presidentNominee);
            //here put in more detail, which member accepted and nominee number
            System.out.println("Node " + id + " learned that member " + acceptorMemberId + " accepted, member "+ presidentNominee + " as the president");

            // Check if the majority has accepted a value
            if (acceptedValues.size() >= majority) {
                announceElectionResult(presidentNominee);
            }
        }
    }

    private void sendProposalMessage() {
        // Send Accept messages to the acceptors who promised
        for (Map.Entry<Integer, Integer> entry : promises.entrySet()) {
            int port = entry.getKey();
            //This is a proposal from proposer, here I should mention the nominee id.
            //String message = "Accept:" + proposalNumber + ":" + " President Nominee is member number: "+ id; // Value to accept is the node's ID for simplicity
            String message = "Accept:" + proposalNumber + ":PresidentNominee:" + nomineeId;

            sendMessage(message, port);
        }
    }

    private void startAsAcceptor() {
        System.out.println("Node " + id + " is starting as Acceptor.");
        while (true) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Receive a packet

                // Simulate delay
                if (isDelayed) {
                    Thread.sleep(2000); // Simulating a 2-second delay
                }

                // Simulate message dropping
                if (dropMessages && new Random().nextBoolean()) {
                    System.out.println("Node " + id + " dropped a message.");
                    continue; // Skip processing this packet
                }

                // Process the packet
                String receivedMessage = new String(packet.getData()).trim();
                processAcceptRequest(receivedMessage, packet.getAddress(), packet.getPort());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Method to set acceptance preference based on proposer ID
    public void setAcceptancePreference(int preferredProposerId) {
        this.preferredProposerId = preferredProposerId;
    }

    public void setAlwaysAccept(boolean alwaysAccept) {
        this.alwaysAccept = alwaysAccept;
    }

    // Modify processAcceptRequest to consider acceptance preferences
    //Acceptor method
    private void processAcceptRequest(String message, InetAddress address, int port) {
        String[] parts = message.split(":");
        if (parts[0].equals("Prepare")) {
            int proposedNumber = Integer.parseInt(parts[1]);
            int fromNode = Integer.parseInt(parts[3]);

            // Respond with Promise
            if (proposedNumber > highestPromisedProposal) {
                highestPromisedProposal = proposedNumber;
                String response = "Promise:" + highestPromisedProposal;
                sendMessage(response, port);
                System.out.println("Node " + id + " promised for proposal number " + proposedNumber + " from member number:" + fromNode);
            } else {
                // Send a reject response if the proposed number is lower than the highest promised
                String rejectResponse = "Reject:" + proposedNumber;
                sendMessage(rejectResponse, port);
                System.out.println("Node " + id + " rejected proposal number " + proposedNumber + " (current highest promised is " + highestPromisedProposal + ")" + "from member number:" + fromNode);
            }
        } else if (parts[0].equals("Accept")) {
            int acceptedProposalNumber = Integer.parseInt(parts[1]);
            int presidentNomineeId = Integer.parseInt(parts[3]);

            if (parts[0].equals("Accept")) {
                 acceptedProposalNumber = Integer.parseInt(parts[1]);
                 presidentNomineeId = Integer.parseInt(parts[3]);

                // Check if nominee is in the preferred list
                if (preferredNominees != null && !preferredNominees.contains(presidentNomineeId)) {
                    System.out.println("Node " + id + " rejects nominee " + presidentNomineeId + " (not preferred).");
                    String rejectResponse = "Reject:" + acceptedProposalNumber;
                    sendMessage(rejectResponse, port);
                    return;
                }
            }
            // Accept based on proposal number
            if (acceptedProposalNumber >= highestPromisedProposal) {
                highestPromisedProposal = acceptedProposalNumber;
                //sending the acceptoes id to proposer along with "Accepted" message and presidentNomineeId
                String response = "Accepted:" + presidentNomineeId + ":" + id;
                sendMessage(response, port);
                System.out.println("Node " + id + " accepted value for president nomination of member " + presidentNomineeId);
            } else {
                // Send a reject response if the accepted proposal number is lower than the highest promised
                String rejectResponse = "Reject:" + acceptedProposalNumber;
                sendMessage(rejectResponse, port);
                System.out.println("Node " + id + " rejected acceptance of proposal number " + acceptedProposalNumber + " (current highest promised is " + highestPromisedProposal + ")");
            }
        } else if (parts[0].equals("PresidentElected")) {
            int presidentId = Integer.parseInt(parts[1]);
            System.out.println("Node " + id + " has learned that Node " + presidentId + " is the new president.");
            // Optionally, store this information for future use
        }
    }


    private void announceElectionResult(int presidentId) {
        System.out.println("Node " + id + " announces the new president is Member " + presidentId);

        // Broadcast the announcement to all nodes
        String message = "PresidentElected:" + presidentId;

        for (int i = 1; i <= 10; i++) { // Assume there are 10 nodes in total
            if (i != id) { // No need to send to itself
                sendMessage(message, 8000 + i); // Assuming each node listens on ports 8001 to 8010
            }
        }
    }

    private void sendMessage(String message, int port) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPreferredNominees(List<Integer> nominees) {
        this.preferredNominees = nominees;
    }

    public void setPreferredProposers(List<Integer> proposers) {
        this.preferredProposers = proposers;
    }
}
