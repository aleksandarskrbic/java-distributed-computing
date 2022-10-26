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
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case None -> {
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to Zookeeper");
                } else {
                    synchronized (zooKeeper) {
                        System.out.println("Disconnected from Zookeeper event");
                        zooKeeper.notifyAll();
                    }
                }
            }
            /**
             * Add states code here to respond to the relevant events
             */
        }
    }

    private void launchWorkersIfNecessary() {
        /**
         * Implement this method to watch and launch new workers if necessary
         */
    }

    /**
     * Helper method to start a single worker
     * @throws IOException
     */
    private void startNewWorker() throws IOException {
        File file = new File(pathToProgram);
        String command = "java -jar " + file.getCanonicalPath();
        System.out.println(String.format("Launching worker instance : %s ", command));
        Runtime.getRuntime().exec(command, null, file.getParentFile());
    }
}
