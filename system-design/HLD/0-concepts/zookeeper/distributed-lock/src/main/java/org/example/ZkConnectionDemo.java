package org.example;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

public class ZkConnectionDemo {

    public static void main(String[] args) throws IOException, InterruptedException {
        String zkConnect = "localhost:2181";

        ZooKeeper zooKeeper = new ZooKeeper(zkConnect, 3000, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                System.out.println("Watcher event: " + watchedEvent);
            }
        });

        // Just wait for connection to be established
        Thread.sleep(2000);

        System.out.println("ZooKeeper state: " + zooKeeper.getState());

        // Close connection
        zooKeeper.close();
    }

}
