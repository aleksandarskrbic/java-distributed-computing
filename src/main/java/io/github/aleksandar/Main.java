package io.github.aleksandar;

import org.apache.zookeeper.KeeperException;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        final var leaderElection = new LeaderElection();
        leaderElection.volunteerForLeadership();
        leaderElection.reelectLeader();
        leaderElection.run();
        leaderElection.close();
    }
}