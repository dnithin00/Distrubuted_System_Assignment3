package org.example;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PaxosNode {
    private int id;  // Unique ID for each node (e.g., M1, M2)
    private int highestProposal = -1; // Highest proposal number seen by acceptor
    private int acceptedProposal = -1; // The proposal accepted by this node
    private String acceptedValue = null; // Candidate that was accepted
    private int majority; // Number of votes needed for majority

    private DatagramSocket socket;
    private Map<String, Integer> acceptedMessages = new HashMap<>(); // For learners
    private Random random = new Random();

    public PaxosNode(int id, int majority) throws SocketException {
        this.id = id;
        this.majority = majority;
        socket = new DatagramSocket(9000 + id);  // Each node will listen on a different port
    }

    // Proposer sends Prepare message
    public void sendPrepare(int proposalId, String candidate, String[] acceptorAddresses) throws IOException {
        String prepareMessage = "prepare:" + proposalId + ":" + candidate;

        // Send to all acceptors
        for (String address : acceptorAddresses) {
            sendMessage(prepareMessage, address, 9000);
        }

        // Wait for promises and handle response
        waitForPromises(proposalId, candidate);
    }

    // Acceptor handles Prepare and responds with Promise
    public void handlePrepare(int proposalId, String proposerAddress, int proposerPort) throws IOException {
        if (shouldDropMessage()) {
            System.out.println("Node " + id + " dropped the prepare message.");
            return;  // Simulate M3 being offline or M2 dropping messages
        }

        if (proposalId > highestProposal) {
            // Add artificial delay for M2
            simulateNetworkIssues();

            highestProposal = proposalId;
            // Respond with promise
            String promiseMessage = "promise:" + proposalId;
            sendMessage(promiseMessage, proposerAddress, proposerPort);
        }
    }

    // Acceptor handles Accept request
    public void handleAccept(int proposalId, String candidate) throws IOException {
        if (shouldDropMessage()) {
            System.out.println("Node " + id + " dropped the accept message.");
            return;  // Simulate M3 being offline or M2 dropping messages
        }

        if (proposalId >= highestProposal) {
            // Add artificial delay for M2
            simulateNetworkIssues();

            acceptedProposal = proposalId;
            acceptedValue = candidate;
            System.out.println("Node " + id + " accepted proposal " + proposalId + " with candidate: " + candidate);

            // Send accepted message to learner
            String acceptedMessage = "accepted:" + proposalId + ":" + candidate;
            // Assuming learners are at address "localhost" port 9009 (you can change)
            sendMessage(acceptedMessage, "localhost", 9009);
        }
    }

    // Learner receives Accepted messages
    public void handleAcceptedMessage(int proposalId, String candidate) {
        // Increment the number of accepted messages for this candidate
        acceptedMessages.put(candidate, acceptedMessages.getOrDefault(candidate, 0) + 1);

        int currentCount = acceptedMessages.get(candidate);
        System.out.println("Candidate " + candidate + " has " + currentCount + " accepted votes.");

        // If the candidate has reached the majority, declare them the president
        if (currentCount >= majority) {
            System.out.println("Candidate " + candidate + " has been elected as the new council president!");
        }
    }

    // Utility to send messages over UDP
    private void sendMessage(String message, String address, int port) throws IOException {
        byte[] buffer = message.getBytes();
        InetAddress recipient = InetAddress.getByName(address);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, recipient, port);
        socket.send(packet);
    }

    // Utility to receive messages over UDP and parse
    public void receiveMessages() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (true) {
            socket.receive(packet);
            String message = new String(packet.getData(), 0, packet.getLength());
            String[] parts = message.split(":");

            if (parts[0].equals("prepare")) {
                int proposalId = Integer.parseInt(parts[1]);
                String proposerAddress = packet.getAddress().getHostAddress();
                int proposerPort = packet.getPort();
                handlePrepare(proposalId, proposerAddress, proposerPort);
            } else if (parts[0].equals("accept")) {
                int proposalId = Integer.parseInt(parts[1]);
                String candidate = parts[2];
                handleAccept(proposalId, candidate);
            } else if (parts[0].equals("promise")) {
                // Proposer handles promise and moves to accept phase
                int proposalId = Integer.parseInt(parts[1]);
                // Handle promise (you can store it and count for majority)
            } else if (parts[0].equals("accepted")) {
                int proposalId = Integer.parseInt(parts[1]);
                String candidate = parts[2];
                handleAcceptedMessage(proposalId, candidate);
            }
        }
    }

    private void waitForPromises(int proposalId, String candidate) throws IOException {
        // Wait for promises and send accept requests when enough promises are received
        // Simulated logic here; in real code, you would wait for responses from acceptors
        sendAcceptRequest(proposalId, candidate, new String[]{"localhost", "localhost"});
    }

    private boolean shouldDropMessage() {
        // Simulate M3 or M2 dropping messages occasionally
        if (id == 3) {
            return random.nextDouble() < 0.3;  // 30% chance to drop a message for M3
        }
        if (id == 2) {
            return random.nextDouble() < 0.1;  // 10% chance to drop a message for M2
        }
        return false;
    }

    private void simulateNetworkIssues() {
        if (id == 2) {
            // Simulate network delay for M2
            try {
                Thread.sleep(3000);  // M2 delays by 3 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Proposer sends Accept request
    public void sendAcceptRequest(int proposalId, String candidate, String[] acceptorAddresses) throws IOException {
        String acceptMessage = "accept:" + proposalId + ":" + candidate;

        for (String address : acceptorAddresses) {
            sendMessage(acceptMessage, address, 9000);
        }
    }
}
