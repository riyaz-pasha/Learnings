package org.example;

import org.apache.zookeeper.KeeperException;

import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        System.out.printf("Hello and welcome!");

        // Get a unique Worker ID from ZooKeeper
        ZookeeperWorkerIdAssigner assigner = new ZookeeperWorkerIdAssigner();
        long workerId = assigner.getWorkerId();

        // Create Snowflake generator with the assigned Worker ID
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(workerId);

        // Generate some IDs
        for (int i = 1; i <= 5; i++) {

            long id = idGenerator.nextId();//TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
            // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
            System.out.println("Generated id : = " + id);
            System.out.println("Short id : = " + Base62Encoder.encode(id));
        }

        Thread.sleep(100_000);
    }
}