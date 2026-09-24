package org.example;

import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

// mvn clean compile exec:java -Dexec.mainClass="org.example.TestConnection"
public class TestConnection {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("------------------------------------------------------------------------");
        ZooKeeperConnection connection = new ZooKeeperConnection();
        ZooKeeper zooKeeper = connection.connect("localhost:2181");

        System.out.println("ZooKeeper state: " + zooKeeper.getState());

        connection.close();
        System.out.println("------------------------------------------------------------------------");
    }

}

