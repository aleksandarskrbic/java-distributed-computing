package io.github.aleksandar;

import org.apache.zookeeper.KeeperException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        var worker = new Worker();
        worker.start();
    }
}