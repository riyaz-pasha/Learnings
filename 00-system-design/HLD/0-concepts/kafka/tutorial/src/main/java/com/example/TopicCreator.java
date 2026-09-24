package com.example;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

// mvn compile exec:java -Dexec.mainClass="com.example.TopicCreator" -Dexec.args="new-topic-1"
public class TopicCreator {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("❌ Please provide a topic name as the first argument.");
            System.exit(1);
        }

        String topicName = args[0]; // accept topic name from command line

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");

        try (AdminClient adminClient = AdminClient.create(props)) {
            // Create topic with 1 partition, replication factor 1
            NewTopic newTopic = new NewTopic(topicName, 1, (short) 1);

            adminClient.createTopics(Collections.singleton(newTopic)).all().get();

            System.out.println("✅ Topic '" + topicName + "' created successfully!");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                System.out.println("⚠ Topic already exists: " + topicName);
            } else {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
