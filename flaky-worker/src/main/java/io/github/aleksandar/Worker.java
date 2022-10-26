package io.github.aleksandar;

import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;

public class Worker {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final float CHANCE_TO_FAIL = 0.1F;

    // Parent Znode where each worker stores an ephemeral child to indicate it is alive
    private static final String AUTOHEALER_ZNODES_PATH = "/workers";

    private final Random random = new Random();
    private final ZooKeeper zooKeeper;

    public Worker() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, __ -> {});
    }

    public void start() throws KeeperException, InterruptedException {
        addChildZnode();

        while (true) {
            System.out.println("Working...");
            LockSupport.parkNanos(1000);
            if (random.nextFloat() < CHANCE_TO_FAIL) {
                System.out.println("Critical error happened");
                throw new RuntimeException("Oops");
            }
        }
    }

    private void addChildZnode() throws KeeperException, InterruptedException {
        zooKeeper.create(
                AUTOHEALER_ZNODES_PATH + "/worker_",
                new byte[]{},
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL
        );
    }
}