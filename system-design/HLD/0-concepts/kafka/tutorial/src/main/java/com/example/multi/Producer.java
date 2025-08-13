package com.example.multi;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

// mvn compile exec:java -Dexec.mainClass="com.example.multi.Producer" -Dexec.args="test-topic Producer1 Hello1"
public class Producer {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("❌ Usage: java ProducerExample <topic> <key> <message>");
            System.exit(1);
        }

        String topicName = args[0];
        String key = args[1];
        String value = args[2];

        // Create topic if it doesn't exist
        createTopicIfNotExists(topicName);

        // Kafka producer properties
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ProducerRecord<String, String> record = new ProducerRecord<>(topicName, key, value);
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata = future.get();

            System.out.printf("✅ Message sent: topic=%s, key=%s, value=%s, partition=%d, offset=%d%n", metadata.topic(), key, value, metadata.partition(), metadata.offset());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createTopicIfNotExists(String topicName) {
        Properties adminProps = new Properties();
        adminProps.put("bootstrap.servers", "localhost:9092");

        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            NewTopic newTopic = new NewTopic(topicName, 3, (short) 1);
            adminClient.createTopics(Collections.singletonList(newTopic)).all().get();
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
