package org.example;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class ZooKeeperConnection {

    private ZooKeeper zooKeeper;
    private final CountDownLatch connectionLatch = new CountDownLatch(1);

    public ZooKeeper connect(String host) throws IOException, InterruptedException {
        this.zooKeeper = new ZooKeeper(host, 3000, watchedEvent -> {
            if (Objects.requireNonNull(watchedEvent.getState()) == Watcher.Event.KeeperState.SyncConnected) {
                System.out.println("✅ Connected to ZooKeeper");
                connectionLatch.countDown();
            }
        });

        this.connectionLatch.await();
        return zooKeeper;
    }

    public void close() throws InterruptedException {
        if (this.zooKeeper != null) {
            this.zooKeeper.close();
            zooKeeper.close();
            System.out.println("🔒 Connection closed");
        }
    }

    public void createNode(String path, byte[] data) throws InterruptedException, KeeperException {
        if (this.zooKeeper.exists(path, false) == null) {
            this.zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            System.out.println("✅ Node created at: " + path);
        } else {
            System.out.println("⚠ Node already exists at: " + path);
        }
    }

}
