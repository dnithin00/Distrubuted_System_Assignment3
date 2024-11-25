package org.example;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class PaxosNode {
    // Variable declaration
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
    private boolean isShortDelay; // Flag for simulating delays
    private boolean dropMessages; // Flag for simulating message drops
    private boolean alwaysAccept;  // Flag for custom acceptance rule
    private int preferredProposerId; // If the node only accepts from a specific proposer (M1, M2, M3 only votes for itself)
    private Integer nomineeId;  // New variable for the president nominee
    private List<Integer> preferredNominees;
    private List<Integer> preferredProposers;
    private Integer currentPresident; // Store the ID of the elected president
    private boolean isLongDelay;

    // Constructor
    public PaxosNode(int id, int majority, String role, Integer nomineeId) throws SocketException {
        this.id = id;
        this.majority = majority;
        this.role = role;
        if (role.equals("proposer") && nomineeId != null) {
            this.nomineeId = nomineeId;  // Nominee provided by proposer
        } else {
            this.nomineeId = -1;  // Default for acceptors
        }
        this.socket = new DatagramSocket(8000 + id); // Each node listens on a port based on its ID
        proposalNumber = id * 10 + 1;
        this.highestPromisedProposal = 0; // Initializing the highest promised proposal
        this.promises = new HashMap<>();
        this.acceptedValues = new ArrayList<>();
        this.isShortDelay = false; // Default is no delay
        this.dropMessages = false; // Default is not to drop messages
        this.alwaysAccept = true;  // Default to always accept
        this.preferredProposerId = -1; // No preference by default
        this.isLongDelay = false;
        System.out.println("Welcome to Adelaide Suburbs Council Election !");

    }

    public void setShortDelay(boolean isDelayed) {
        this.isShortDelay = isDelayed;
    }
    public void setLongDelay(boolean isDelayed) {
        this.isLongDelay = isDelayed;
    }


    public void setDropMessages(boolean dropMessages) {
        this.dropMessages = dropMessages;
    }

    public void start() {
        // Starting node based on its role
        if (role.equals("proposer")) {
            startAsProposer();
        } else if (role.equals("acceptor")) {
            startAsAcceptor();
        } else {
            throw new IllegalArgumentException("Invalid role specified");
        }
    }
    // Method for nodes acting as proposer
    private void startAsProposer() {
        System.out.println("Node " + id + " is starting as Proposer.");
        // Propose a value
        sendPrepareMessage();
        // Wait for responses from acceptors
        listenForResponses();
    }

    //The Proposer sends prepare message to all nodes.
    private void sendPrepareMessage() {
        // Increment proposal number (this acts as a round identifier)
        proposalNumber = id * 10 + 1;
        proposalNumber++;
        String message = "Prepare:" + proposalNumber + ":FromMember:" + id + ":Nominee:" + nomineeId;
        System.out.println("Node " + id + " is proposing value with proposal number " + proposalNumber + "for member nominee: " + nomineeId);
        // Send Prepare message to all acceptors
        for (int i = 2; i <= 10; i++) {
            if (i != id) {
                sendMessage(message, 8000 + i);
            }
        }
    }
    // Method for the proposer to listen to responses from all nodes.
    private void listenForResponses() {
        promiseCount = 0;
        rejectionCount = 0;
        int totalResponses = 0;
        int expectedResponses = 9; // Total nodes in the cluster
        long startTime = System.currentTimeMillis();
        long timeout = 10000; // Timeout in milliseconds

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
            } catch (SocketTimeoutException e) {
                System.out.println("Socket timeout reached while waiting for responses. Waited for 10 seconds");
                if(totalResponses < 5)
                {
                    System.out.println("Not enough members are available, consensus could not be reached");
                    return;
                }

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
        } else {
            System.out.println("Timed out before receiving enough responses, proceeding with available data.");
        }

        // After promises are gathered, wait for majority Accepted values
        listenForAcceptedValues();
    }

    // Method for proposer to listen for Accept or Reject response from node.
    private void listenForAcceptedValues() {
        int acceptedCount = 0;  // Counting the number of accepted responses

        while (acceptedCount < majority) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Receive a packet

                String receivedMessage = new String(packet.getData()).trim();
                if (receivedMessage.startsWith("Accepted")) {
                    processResponse(receivedMessage, packet.getAddress(), packet.getPort());

                    acceptedCount = acceptedValues.size();  // Tracking the count of accepted values
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //Proposer Method: to process responses from acceptor nodes.
    private void processResponse(String message, InetAddress address, int port) {
        // Handling responses from acceptors
        String[] parts = message.split(":");
        // Handling Promise response
        if (parts[0].equals("Promise")) {
            int promisedProposal = Integer.parseInt(parts[1]);
            promises.put(port, promisedProposal);
            promiseCount++;
           System.out.println("Node " + id + " received promise from Node " + (port - 8000) + " for proposal " + promisedProposal);

            // Handling Reject response
        } else if (parts[0].equals("Reject")) {
            int rejectedProposal = Integer.parseInt(parts[1]);
            System.out.println("Node " + id + " received rejection from Node " + (port - 8000) + " for proposal " + rejectedProposal);
            rejectionCount++;
        }

        // Handling Accepted response
        else if (parts[0].equals("Accepted")) {
            int presidentNominee = Integer.parseInt(parts[1]);
            int acceptorMemberId = Integer.parseInt(parts[2]);
            acceptedValues.add(presidentNominee);
            System.out.println("Node " + id + " learned that member " + acceptorMemberId + " accepted, member "+ presidentNominee + " as the president");

            // Check if the majority has accepted a value
            if (acceptedValues.size() >= majority) {
                announceElectionResult(presidentNominee);
            }
        }
    }

    // Proposer method to send proposal to promised acceptors
    private void sendProposalMessage() {
        // Sending Accept messages to the acceptors who promised
        for (Map.Entry<Integer, Integer> entry : promises.entrySet()) {
            int port = entry.getKey();
            String message = "Accept:" + proposalNumber + ":PresidentNominee:" + nomineeId;

            sendMessage(message, port);
        }
    }

    // Acceptor Method to start node as acceptor
    private void startAsAcceptor() {
        System.out.println("Member " + id + " is starting as Acceptor.");
        while (true) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Receive a packet

                // Simulating delay
                if (isShortDelay) {
                    Thread.sleep(2000); // Simulating a 2-second delay
                    System.out.println("Member " + id + " has a short delay");
                }
                if (isLongDelay) {
                    Thread.sleep(10000); // Simulating a 3-second delay
                    System.out.println("Member " + id + " has a long delay");
                }
                // Simulating message dropping
                if (dropMessages && new Random().nextBoolean()) {
                    System.out.println("Member " + id + " dropped a message.");
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

    //Acceptor method to propcess messages from Proposer node.
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
                System.out.println("Member " + id + " promised for proposal number " + proposedNumber + " from member number:" + fromNode);
            } else {
                // Sending a reject response if the proposed number is lower than the highest promised
                String rejectResponse = "Reject:" + proposedNumber;
                sendMessage(rejectResponse, port);
                System.out.println("Member " + id + " rejected proposal number " + proposedNumber + " (current highest promised is " + highestPromisedProposal + ")" + "from member number:" + fromNode);
            }
        } else if (parts[0].equals("Accept")) {
            int acceptedProposalNumber = Integer.parseInt(parts[1]);
            int presidentNomineeId = Integer.parseInt(parts[3]);

            if (parts[0].equals("Accept")) {
                 acceptedProposalNumber = Integer.parseInt(parts[1]);
                 presidentNomineeId = Integer.parseInt(parts[3]);

                // Checking if nominee is in the preferred list
                if (preferredNominees != null && !preferredNominees.contains(presidentNomineeId)) {
                    System.out.println("Member " + id + " rejects nominee " + presidentNomineeId + " (not preferred).");
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
                System.out.println("Member " + id + " accepted value for president nomination of member " + presidentNomineeId);
            } else {
                // Sending a reject response if the accepted proposal number is lower than the highest promised
                String rejectResponse = "Reject:" + acceptedProposalNumber;
                sendMessage(rejectResponse, port);
                System.out.println("Member " + id + " rejected acceptance of proposal number " + acceptedProposalNumber + " (current highest promised is " + highestPromisedProposal + ")");
            }
        } else if (parts[0].equals("PresidentElected")) {
            int presidentId = Integer.parseInt(parts[1]);
            System.out.println("Member " + id + " has learned that Node " + presidentId + " is the new president.");
            // Optionally, store this information for future use
        }
    }

    // Method used by proposer to broadcast new president to all nodes.
    private void announceElectionResult(int presidentId) {
        System.out.println("Member " + id + " announces the new president is Member " + presidentId );

        // Update the elected president
        this.currentPresident = presidentId;

        // Broadcasting the announcement to all nodes
        String message = "PresidentElected:" + presidentId;

        for (int i = 1; i <= 10; i++) { // Assume there are 10 nodes in total
            if (i != id) { // No need to send to itself
                sendMessage(message, 8000 + i); // Assuming each node listens on ports 8001 to 8010
            }
        }
    }

    // Method used to send messages through socket connection
    private void sendMessage(String message, int port) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Setter for preferred nominees
    public void setPreferredNominees(List<Integer> nominees) {
        this.preferredNominees = nominees;
    }

    public void setPreferredProposers(List<Integer> proposers) {
        this.preferredProposers = proposers;
    }
    // Getter for currentPresident
    public Integer getCurrentPresident() {
        return currentPresident;
    }
    // Setter for nomineeId
    public void setNomineeId(int nomineeId) {
        this.nomineeId = nomineeId;
    }

    public Integer getNomineeId() {
        return nomineeId;
    }
}
