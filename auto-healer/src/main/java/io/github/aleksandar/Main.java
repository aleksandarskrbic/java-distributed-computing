package io.github.aleksandar;

import org.apache.zookeeper.KeeperException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws InterruptedException, KeeperException, IOException {
        if (args.length != 2) {
            System.out.println("Expecting parameters <number of workers> <path to worker jar file>");
            System.exit(1);
        }

        int numberOfWorkers = Integer.parseInt(args[0]);
        var pathToWorkerProgram = args[1];
        var autoHealer = new AutoHealer(numberOfWorkers, pathToWorkerProgram);
        autoHealer.startWatchingWorkers();
        autoHealer.run();
        autoHealer.close();
    }
}