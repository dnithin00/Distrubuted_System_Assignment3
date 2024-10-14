package org.example;

public class Main {
    public static void main(String[] args) throws Exception {

        System.out.println("Hello world");
//        if (args.length < 2) {
//            System.out.println("Usage: java Main <node_id> <role>");
//            System.out.println("Roles: proposer, acceptor, learner");
//            return;
//        }
//
//        int nodeId = Integer.parseInt(args[0]);
//        String role = args[1];
//        PaxosNode node = new PaxosNode(nodeId, 5);  // Assume majority is 5 for 9 council members
//
//        if (role.equals("proposer")) {
//            String candidate = args[2];  // Candidate to propose
//            node.sendPrepare(1, candidate, new String[]{"localhost", "localhost"});
//        } else if (role.equals("acceptor")) {
//            node.receiveMessages();  // Start receiving messages
//        } else if (role.equals("learner")) {
//            node.receiveMessages();  // Start receiving messages and learn the result
//        } else {
//            System.out.println("Unknown role: " + role);
//        }
   }
}
