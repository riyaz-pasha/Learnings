package org.example;

import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// mvn clean compile exec:java -Dexec.mainClass="org.example.LockCompetitionTest"
public class LockCompetitionTest {

    private static final int CLIENT_COUNT = 5;

    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CLIENT_COUNT);

        for (int i = 0; i < CLIENT_COUNT; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    ZooKeeperConnection zkConnection = new ZooKeeperConnection();
                    ZooKeeper zk = zkConnection.connect("localhost:2181");

                    DistributedLock lock = new DistributedLock(zk, "/locks/mylock");

                    System.out.println("Client " + clientId + " trying to acquire lock...");
                    boolean acquired = lock.acquire(15, TimeUnit.SECONDS);

                    if (acquired) {
                        System.out.println("Client " + clientId + " acquired the lock!");
                        Thread.sleep(2000);  // Simulate critical section
                        lock.release();
                        System.out.println("Client " + clientId + " released the lock.");
                    } else {
                        System.out.println("Client " + clientId + " failed to acquire the lock.");
                    }

                    zkConnection.close();
                } catch (Exception e) {
                    System.err.println("Client " + clientId + " encountered error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
}
