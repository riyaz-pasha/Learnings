package org.example;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

// mvn clean compile exec:java -Dexec.mainClass="org.example.Main"
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        System.out.println("------------------------------------------------------------------------");
        ZooKeeperConnection connection = new ZooKeeperConnection();
        ZooKeeper zooKeeper = connection.connect("localhost:2181");

        connection.createNode("/locks", new byte[0]);
        connection.createNode("/locks", new byte[0]);

        DistributedLock lock = new DistributedLock(zooKeeper, "/locks/mylock");

        if (lock.acquire(10, TimeUnit.SECONDS)) {
            System.out.println("Critical section: Lock is held, doing work...");
            Thread.sleep(3000); // simulate work
            lock.release();
        } else {
            System.out.println("Failed to acquire lock");
        }

        connection.close();
        System.out.println("------------------------------------------------------------------------");
    }

}