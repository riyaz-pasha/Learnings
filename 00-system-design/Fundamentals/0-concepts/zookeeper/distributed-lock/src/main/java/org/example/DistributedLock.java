package org.example;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DistributedLock implements Watcher, AutoCloseable {

    private final ZooKeeper zk;
    private final String lockBasePath;
    private String ourLockPath;
    private CountDownLatch latch;

    public DistributedLock(ZooKeeper zk, String lockBasePath) throws InterruptedException, KeeperException {
        this.zk = zk;
        this.lockBasePath = lockBasePath;

        // Ensure base path exists
        Stat stat = this.zk.exists(lockBasePath, false);
        if (stat == null) {
            try {
                this.zk.create(lockBasePath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } catch (KeeperException.NodeExistsException ignored) {
                // Ignore if created by others
            }
        }
    }

    public boolean acquire(long timeout, TimeUnit unit) throws InterruptedException, KeeperException {
        this.ourLockPath = this.zk.create(
                this.lockBasePath + "/lock-",
                Thread.currentThread().getName().getBytes(StandardCharsets.UTF_8),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Created lock node: " + this.ourLockPath);

        long startMillis = System.currentTimeMillis();
        long waitMillis = unit.toMillis(timeout);

        while (true) {
            List<String> children = this.zk.getChildren(this.lockBasePath, false)
                    .stream()
                    .filter(name -> name.startsWith("lock-"))
                    .sorted()
                    .toList();

            System.out.println("Children : " + children);
            String ourNodeName = this.ourLockPath.substring(this.lockBasePath.length() + 1);
            System.out.println("ourNodeName : " + ourNodeName);
            int ourIndex = children.indexOf(ourNodeName);
            System.out.println("ourIndex : " + ourIndex);

            if (ourIndex == -1) {
                throw new KeeperException.NoNodeException("Our lock node disappeared unexpectedly");
            }

            if (ourIndex == 0) {
                System.out.println("Lock acquired: " + ourLockPath);
                return true;
            } else {
                // Watch the node immediately before ours
                String prevNode = children.get(ourIndex - 1);
                latch = new CountDownLatch(1);

                Stat stat = zk.exists(lockBasePath + "/" + prevNode, this);
                if (stat != null) {
                    long elapsed = System.currentTimeMillis() - startMillis;
                    long remaining = waitMillis - elapsed;
                    if (remaining <= 0) {
                        // Timeout - cleanup
                        zk.delete(ourLockPath, -1);
                        System.out.println("Lock acquire timed out. Deleted lock node: " + ourLockPath);
                        return false;
                    }
                    latch.await(remaining, TimeUnit.MILLISECONDS);
                } else {
                    // Predecessor node gone, loop again to check if now we have the lock
                }
            }

        }
    }

    public void release() throws InterruptedException, KeeperException {
        if (this.ourLockPath != null) {
            this.zk.delete(this.ourLockPath, -1);
            System.out.println("Lock released: " + this.ourLockPath);
            this.ourLockPath = null;
        }
    }

    @Override
    public void close() throws Exception {
        this.release();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        // Trigger latch on predecessor deletion
        if (watchedEvent.getType() == Event.EventType.NodeDeleted && latch != null) {
            latch.countDown();
        }
    }
}
