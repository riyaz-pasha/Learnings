package org.example;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;

public class ZookeeperWorkerIdAssigner {

    private final String ZK_SERVERS = "localhost:2181,localhost:2182,localhost:2183";
    private final int SESSION_TIMEOUT = 5000;
    private final String ID_GENERATOR = "/id_generator";
    private final String WORKER_PATH = ID_GENERATOR + "/worker-";

    private final ZooKeeper zk;
    private final long workerId;

    public ZookeeperWorkerIdAssigner() throws IOException, InterruptedException, KeeperException {
        this.zk = new ZooKeeper(ZK_SERVERS, SESSION_TIMEOUT, event -> {
            if (event.getState() == Watcher.Event.KeeperState.Expired) {
                System.out.println("ZooKeeper session expired!...");
            }
        });

        // Ensure parent node exists
        Stat stat = this.zk.exists(ID_GENERATOR, false);
        if (stat == null) {
            this.zk.create(ID_GENERATOR, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }

        // Create an ephemeral sequential node
        String path = this.zk.create(WORKER_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        String substring = path.substring(path.lastIndexOf("-") + 1);
        this.workerId = Long.parseLong(substring);

        System.out.println("Assigned worker id : " + this.workerId);
    }

    public long getWorkerId() {
        return workerId;
    }

}
