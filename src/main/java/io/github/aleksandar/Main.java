package io.github.aleksandar;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        final LeaderElection leaderElection = new LeaderElection();
        leaderElection.run();
        leaderElection.close();
    }
}