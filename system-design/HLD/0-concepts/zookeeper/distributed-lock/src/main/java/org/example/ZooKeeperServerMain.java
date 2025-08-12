package org.example;

import java.io.IOException;

public class ZooKeeperServerMain {

    public static void main(String[] args) throws IOException {
        EmbeddedZooKeeper server = new EmbeddedZooKeeper(2181);
        server.start();
    }

}
