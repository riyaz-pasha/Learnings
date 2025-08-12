package org.example;

import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public class EmbeddedZooKeeper {

    private ServerCnxnFactory factory;
    private int port;
    private File snapshotDir;
    private File logDir;

    public EmbeddedZooKeeper(int port) {
        this.port = port;
        this.snapshotDir = new File(System.getProperty("java.io.tmpdir"), "zk-snapshots");
        this.logDir = new File(System.getProperty("java.io.tmpdir"), "zk-logs");
        snapshotDir.mkdirs();
        logDir.mkdirs();
    }

    public void start() throws IOException {
        try {
            ZooKeeperServer zkServer = new ZooKeeperServer(snapshotDir, logDir, 2000);
            factory = ServerCnxnFactory.createFactory(new InetSocketAddress("localhost", port), 100);
            factory.startup(zkServer);
            System.out.println("Embedded ZooKeeper started on port " + port);
        } catch (Exception e) {
            throw new IOException("Failed to start embedded ZooKeeper", e);
        }
    }

    public void stop() {
        if (factory != null) {
            factory.shutdown();
            System.out.println("Embedded ZooKeeper stopped");
        }
    }

}
