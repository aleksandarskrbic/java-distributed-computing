package io.github.aleksandar;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;

public class LeaderElection implements Watcher {
    private static final int SESSION_TIMEOUT = 3000;
    private static final String ZK_ADDRESS = "localhost:2181";
    private static final String ELECTION_NAMESPACE = "/election";

    private final ZooKeeper zk;
    private String currentZkNodeName;

    public LeaderElection() throws IOException {
        System.out.println("Creating Leader Election");
        this.zk = new ZooKeeper(ZK_ADDRESS, SESSION_TIMEOUT, this);
        System.out.println("Created Leader Election");
    }

    @Override
    public void process(final WatchedEvent event) {
        switch (event.getType()) {
            case None -> {
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to Zookeeper");
                } else {
                    synchronized (zk) {
                        System.out.println("Disconnected. Event " + event.getState());
                        zk.notifyAll();
                    }
                }
            }
            case NodeDeleted -> {
                    try {
                        reelectLeader();
                    } catch (final InterruptedException | KeeperException e) {
                        throw new RuntimeException(e);
                    }
            }
        }
    }

    public void volunteerForLeadership() throws InterruptedException, KeeperException {
        final var zkNodePrefix = ELECTION_NAMESPACE + "/c_";
        final var zkNodeFullPath = zk.create(zkNodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        System.out.println("zkNodeFullPath: " + zkNodeFullPath);
        this.currentZkNodeName = zkNodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    public void reelectLeader() throws InterruptedException, KeeperException {
        Stat predecessorStat = null;
        var predecessorZkNodeName = "";

        while (predecessorStat == null) {
            final var children = zk.getChildren(ELECTION_NAMESPACE, false);

            Collections.sort(children);
            final var smallestChild = children.get(0);

            if (smallestChild.equals(currentZkNodeName)) {
                System.out.println("I am the leader");
                break;
            } else {
                System.out.println("I am not the leader. " + smallestChild + " is the leader.");
                int predecessorIndex = Collections.binarySearch(children, currentZkNodeName) - 1;
                predecessorZkNodeName = children.get(predecessorIndex);
                predecessorStat = zk.exists(ELECTION_NAMESPACE + "/" + predecessorZkNodeName, this);
            }
        }

        System.out.println("Watching znode: " + predecessorZkNodeName);
        System.out.println();
    }

    public void run() throws InterruptedException {
        synchronized (zk) {
            zk.wait();
        }
    }

    public void close() throws InterruptedException {
        zk.close();
        System.out.println("Disconnected from Zookeeper");
    }
}
