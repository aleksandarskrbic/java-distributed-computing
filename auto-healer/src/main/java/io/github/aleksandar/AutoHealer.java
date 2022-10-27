package io.github.aleksandar;

import org.apache.zookeeper.*;
import java.io.File;
import java.io.IOException;

public class AutoHealer implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;

    // Parent Znode where each worker stores an ephemeral child to indicate it is alive
    private static final String AUTOHEALER_ZNODES_PATH = "/workers";

    // Path to the worker jar
    private final String pathToProgram;

    // The number of worker instances we need to maintain at all times
    private final int numberOfWorkers;
    private final ZooKeeper zooKeeper;

    public AutoHealer(int numberOfWorkers, String pathToProgram) throws IOException {
        this.numberOfWorkers = numberOfWorkers;
        this.pathToProgram = pathToProgram;
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    public void startWatchingWorkers() throws KeeperException, InterruptedException {
        if (zooKeeper.exists(AUTOHEALER_ZNODES_PATH, false) == null) {
            zooKeeper.create(AUTOHEALER_ZNODES_PATH, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        launchWorkersIfNecessary();
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    @Override
    public void process(final WatchedEvent event) {
        switch (event.getType()) {
            case None -> handleEvent(event);
            case NodeChildrenChanged -> launchWorkersIfNecessary();
        }
    }

    private void handleEvent(final WatchedEvent event) {
        if (event.getState() == Event.KeeperState.SyncConnected) {
            System.out.println("Successfully connected to Zookeeper");
        } else {
            synchronized (zooKeeper) {
                System.out.println("Disconnected from Zookeeper event");
                zooKeeper.notifyAll();
            }
        }
    }

    private void launchWorkersIfNecessary() {
        try {
            var children = zooKeeper.getChildren(AUTOHEALER_ZNODES_PATH, this);
            if (children.size() < numberOfWorkers) {
                startNewWorker();
            }
        } catch (final KeeperException | IOException | InterruptedException ex) {
            ex.printStackTrace();
            System.out.println("Shutting down application...");
            System.exit(1);
        }
    }

    private void startNewWorker() throws IOException {
        var file = new File(pathToProgram);
        var command = "java -jar " + file.getCanonicalPath();
        System.out.println(String.format("Launching worker instance : %s ", command));
        Runtime.getRuntime().exec(command, null, file.getParentFile());
    }
}
