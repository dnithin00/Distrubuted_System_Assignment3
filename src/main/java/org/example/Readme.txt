Instructions to run the code:
Steps to Run the Paxos Code
1. Setup the Project
    Import the project into IntelliJ IDEA
    Ensure you have Maven installed and properly configured.
2. Install Dependencies
    Reload Maven configuration in IntelliJ IDEA:
3. Running:
    1. You can ignore the M1 to M9 files, I used these to run each node manually and test.
    2. So Basically, if you run the test cases in "AllTestCases.java" ony by one, because there may be port already in use if you run two test cases simultaneously, it should be fine,
        it will automatically initialize and start nodes.
    3. But if you want start each 9 nodes manually with manual configuration, first run the PaxosNode file,
       then run the acceptors first, in my zip code, m1 is the proposer, rest all are acceptors, so run rest and at last run m1.
       using the run button of IDE intellij.

Readme file also in main directory
