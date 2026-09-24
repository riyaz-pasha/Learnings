package com.example;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

// mvn compile exec:java -Dexec.mainClass="com.example.KafkaTopicCreator"
public class KafkaTopicCreator {

    public static void main(String[] args) {
        Properties props = new Properties();
        // Use 127.0.0.1 instead of localhost for better cross-platform compatibility
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        // Optional: Increase connection timeout to avoid timeouts on slower machines
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 20000); // 20 seconds
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 20000); // 20 seconds

        int numPartitions = 1;
        short replicationFactor = 1;

        try (AdminClient adminClient = AdminClient.create(props)) {
            NewTopic topic = new NewTopic("test-topic", numPartitions, replicationFactor);

            adminClient.createTopics(Collections.singletonList(topic))
                    .all()
                    .get();

            System.out.println("Topic 'test-topic' created successfully!");
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to create topic: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
