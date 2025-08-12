package org.example;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;

public class ZkDistributedLock {

    private final ZooKeeper zooKeeper;
    private final String locksRoot = "/locks";
    private final String lockName;
    private final String basePath;
    private String ourPath;

    public ZkDistributedLock(String connectString, String lockName, String sessionId) throws IOException, InterruptedException, KeeperException {
        this.lockName = lockName;
        this.basePath = this.locksRoot + "/" + this.lockName;
        Watcher watcher = new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
                    try {
                        getLockOrWait(sessionId, locksRoot);
                    } catch (InterruptedException | KeeperException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        };
        this.zooKeeper = new ZooKeeper(connectString, 20000, watcher);

        this.ensurePathExists(this.locksRoot, CreateMode.PERSISTENT);
        this.ensurePathExists(this.basePath, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    private void ensurePathExists(String path, CreateMode createMode) throws InterruptedException, KeeperException {
        Stat stat = this.zooKeeper.exists(path, false);
        if (stat == null) {
            try {
                this.zooKeeper.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
            } catch (KeeperException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void getLockOrWait(String sessionId, String locksRoot) throws InterruptedException, KeeperException {
        List<String> children = this.zooKeeper.getChildren(this.locksRoot, false);
        children.sort(String::compareTo);

        byte[] data = this.zooKeeper.getData(this.locksRoot + "/" + children.getFirst(), false, null);
        if (data != null && new String(data).equalsIgnoreCase(sessionId)) {
            System.out.println("I acquired a lock :). will leave it in 10 seconds");
            for (int i = 0; i < 10; i++) {
                System.out.println("leaving in " + i + "seconds");
                Thread.sleep(1000);
            }
            zooKeeper.delete(this.locksRoot + "/" + children.getFirst(), -1);
        } else {
            System.out.println("i could not acquire a lock. So will wait");
            zooKeeper.getChildren(this.locksRoot, true);
        }

        Thread.sleep(100_100_100);
    }

}
