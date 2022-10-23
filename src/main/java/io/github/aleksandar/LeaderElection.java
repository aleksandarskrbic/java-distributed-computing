package io.github.aleksandar;

import org.apache.zookeeper.*;

import java.io.IOException;

public class LeaderElection implements Watcher {
    private static final int SESSION_TIMEOUT = 3000;
    private static final String ZK_ADDRESS = "localhost:2181";
    private static final String ELECTION_NAMESPACE = "/election";

    private final ZooKeeper zk;
    private String currentZkNodeName;

    public LeaderElection() throws IOException {
        this.zk = new ZooKeeper(ZK_ADDRESS, SESSION_TIMEOUT, this);
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
        }
    }

    public void volunteerForLeadership() throws InterruptedException, KeeperException {
        final String zkNodePrefix = ELECTION_NAMESPACE + "/c_";
        final String zkNodeFullPath = zk.create(zkNodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

        System.out.println("zkNodeFullPath: " + zkNodeFullPath);
        this.currentZkNodeName = zkNodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
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
