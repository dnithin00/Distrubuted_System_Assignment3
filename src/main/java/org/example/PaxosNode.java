package org.example;
//package org.example;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.InetAddress;
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


    public PaxosNode(int id, int majority, String role) throws SocketException {
        this.id = id;
        this.majority = majority;
        this.role = role;
        this.socket = new DatagramSocket(8000 + id); // Each node listens on a port based on its ID
        this.proposalNumber = 0;
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
        proposeValue();

        // Wait for responses from acceptors
        listenForResponses();
    }

    private void proposeValue() {
        // Increment proposal number (this acts as a round identifier)
        proposalNumber++;
        // Propose a new value (here we can just use the node's ID for simplicity)
        String message = "Prepare:" + proposalNumber;

        System.out.println("Node " + id + " is proposing value with proposal number " + proposalNumber);

        // Send Prepare message to all acceptors
        for (int i = 2; i <= 10; i++) { // Assume there are 10 nodes in total
            if (i != id) {
                sendMessage(message, 8000 + i);
            }
        }
    }

    private void listenForResponses() {
         promiseCount = 0;
        rejectionCount = 0;  // Track rejections
        int totalResponses = 0;

        // Time-based waiting (timeout of 3 seconds for example)
        long startTime = System.currentTimeMillis();
        long timeout = 5000;  // 5 seconds

//        while (promiseCount < majority) {
//            try {
//                byte[] buffer = new byte[1024];
//                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//                socket.receive(packet); // Receive a packet
//
//                // Simulate delay
////                if (isDelayed) {
////                    Thread.sleep(2000); // Simulating a 2-second delay
////                }
////
////                // Simulate message dropping
////                if (dropMessages && new Random().nextBoolean()) {
////                    System.out.println("Node " + id + " dropped a message.");
////                    continue; // Skip processing this packet
////                }
//
//                // Process the packet
//                String receivedMessage = new String(packet.getData()).trim();
//                processResponse(receivedMessage, packet.getAddress(), packet.getPort());
//                //promiseCount++;
//
//                // Check if enough promises are received this was moved within processResponse method
////                if (promiseCount >= majority) {
////                    // Now send Accept requests to those who promised
////                    sendAcceptMessages();
////                }
//                //processResponse(receivedMessage, packet.getAddress(), packet.getPort());
//
//
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        while (System.currentTimeMillis() - startTime < timeout && totalResponses < 9) {  // Wait until all 9 nodes or timeout
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Receive a packet

                String receivedMessage = new String(packet.getData()).trim();
                processResponse(receivedMessage, packet.getAddress(), packet.getPort());

                totalResponses++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Evaluate promises and rejections
        if (promiseCount >= majority) {
            // Now send Accept requests to those who promised
            sendAcceptMessages();
        } else if (rejectionCount >= majority) {
            // Retry with a higher proposal number if majority rejected
            System.out.println("Node " + id + " proposal was rejected by majority. Retrying with a higher proposal number...");
            proposalNumber++;
            proposeValue();
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

    private void processResponse(String message, InetAddress address, int port) {
        // Handle responses from acceptors
        String[] parts = message.split(":");
        // Handle Promise response
        if (parts[0].equals("Promise")) {
            int promisedProposal = Integer.parseInt(parts[1]);
            promises.put(port, promisedProposal);
            promiseCount++;
            System.out.println("Node " + id + " received promise from Node " + (port - 8000) + " for proposal " + promisedProposal);

            // Handle Reject response
        } else if (parts[0].equals("Reject")) {
            int rejectedProposal = Integer.parseInt(parts[1]);
            System.out.println("Node " + id + " received rejection from Node " + (port - 8000) + " for proposal " + rejectedProposal);
            rejectionCount++;
        }

        // Handle Accepted response
        else if (parts[0].equals("Accepted")) {
            int value = Integer.parseInt(parts[1]);
            acceptedValues.add(value);
            System.out.println("Node " + id + " learned that value " + value + " was accepted.");

            // Check if the majority has accepted a value
            if (acceptedValues.size() >= majority) {
                announceElectionResult(value);
            }
        }
    }

    private void sendAcceptMessages() {
        // Send Accept messages to the acceptors who promised
        for (Map.Entry<Integer, Integer> entry : promises.entrySet()) {
            int port = entry.getKey();
            String message = "Accept:" + proposalNumber + ":" + id; // Value to accept is the node's ID for simplicity
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
//                if (isDelayed) {
//                    Thread.sleep(2000); // Simulating a 2-second delay
//                }

                // Simulate message dropping
//                if (dropMessages && new Random().nextBoolean()) {
//                    System.out.println("Node " + id + " dropped a message.");
//                    continue; // Skip processing this packet
//                }

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
    private void processAcceptRequest(String message, InetAddress address, int port) {
        String[] parts = message.split(":");
        if (parts[0].equals("Prepare")) {
            int proposedNumber = Integer.parseInt(parts[1]);

            // Respond with Promise
            if (proposedNumber > highestPromisedProposal) {
                highestPromisedProposal = proposedNumber;
                String response = "Promise:" + highestPromisedProposal;
                sendMessage(response, port);
                System.out.println("Node " + id + " promised for proposal number " + proposedNumber);
            } else {
                // Send a reject response if the proposed number is lower than the highest promised
                String rejectResponse = "Reject:" + proposedNumber;
                sendMessage(rejectResponse, port);
                System.out.println("Node " + id + " rejected proposal number " + proposedNumber + " (current highest promised is " + highestPromisedProposal + ")");
            }
        } else if (parts[0].equals("Accept")) {
            int acceptedProposalNumber = Integer.parseInt(parts[1]);
            int proposerId = Integer.parseInt(parts[2]);

            // Accept based on proposal number
            if (acceptedProposalNumber >= highestPromisedProposal) {
                highestPromisedProposal = acceptedProposalNumber;
                String response = "Accepted:" + proposerId;
                sendMessage(response, port);
                System.out.println("Node " + id + " accepted value from proposer " + proposerId);
            } else {
                // Send a reject response if the accepted proposal number is lower than the highest promised
                String rejectResponse = "Reject:" + acceptedProposalNumber;
                sendMessage(rejectResponse, port);
                System.out.println("Node " + id + " rejected acceptance of proposal number " + acceptedProposalNumber + " (current highest promised is " + highestPromisedProposal + ")");
            }
        }
    }

//    private void processAcceptRequest(String message, InetAddress address, int port) {
//        String[] parts = message.split(":");
//        if (parts[0].equals("Prepare")) {
//            int proposedNumber = Integer.parseInt(parts[1]);
//            // Respond with Promise
//            if (proposedNumber > highestPromisedProposal) {
//                highestPromisedProposal = proposedNumber;
//                String response = "Promise:" + highestPromisedProposal;
//                sendMessage(response, port);
//                System.out.println("Node " + id + " promised for proposal number " + proposedNumber);
//            }
//        } else if (parts[0].equals("Accept")) {
//            int acceptedProposalNumber = Integer.parseInt(parts[1]);
//            int proposerId = Integer.parseInt(parts[2]);
//
//            // Custom acceptance rule based on preferred proposer
////            if (preferredProposerId != -1 && proposerId != preferredProposerId) {
////                System.out.println("Node " + id + " rejects value from proposer " + proposerId + " (prefers proposer " + preferredProposerId + ")");
////                return; // Reject the proposal
////            }
//
//            // Accept based on proposal number
//            if (acceptedProposalNumber >= highestPromisedProposal) {
//                highestPromisedProposal = acceptedProposalNumber;
//                String response = "Accepted:" + proposerId;
//                sendMessage(response, port);
//                System.out.println("Node " + id + " accepted value from proposer " + proposerId);
//            }
//        }
//    }

//
//    private void processAcceptRequest(String message, InetAddress address, int port) {
//        String[] parts = message.split(":");
//        if (parts[0].equals("Prepare")) {
//            int proposedNumber = Integer.parseInt(parts[1]);
//            // Respond with Promise
//            if (proposedNumber > highestPromisedProposal) { // Only promise if the proposal is higher
//                highestPromisedProposal = proposedNumber; // Update the highest promised proposal
//                String response = "Promise:" + highestPromisedProposal;
//                sendMessage(response, port); // Send promise back to proposer
//                System.out.println("Node " + id + " promised for proposal number " + proposedNumber);
//            }
//        } else if (parts[0].equals("Accept")) {
//            int acceptedProposalNumber = Integer.parseInt(parts[1]);
//            int value = Integer.parseInt(parts[2]);
//            // Accept only if the proposal number is greater than or equal to the highest promised proposal
//            if (acceptedProposalNumber >= highestPromisedProposal) {
//                highestPromisedProposal = acceptedProposalNumber; // Update the highest promised
//                String response = "Accepted:" + value;
//                sendMessage(response, port); // Notify proposer
//                System.out.println("Node " + id + " accepted value " + value);
//            }
//        }
//    }

    private void announceElectionResult(int presidentId) {
        System.out.println("Node " + id + " announces the new president is Node " + presidentId);
        // Optionally, you could broadcast this announcement to all nodes
        // to inform them of the elected president.
    }

    private void sendMessage(String message, int port) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), port);
//            System.out.println("sendMessage() line 261");
//            System.out.println(InetAddress.getLocalHost());
//            System.out.println(port );
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
