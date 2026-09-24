package com.example;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

// mvn compile exec:java -Dexec.mainClass="com.example.ConsumerExample"
public class ConsumerExample {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//        props.put("bootstrap.servers", "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
//        props.put("group.id", "test-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
//        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
//        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Start from the beginning
//        props.put("auto.offset.reset", "earliest"); // Start from the beginning

        // Create Kafka consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        try (consumer) {
            // Subscribe to the topic
            consumer.subscribe(Collections.singletonList("test-topic"));
            System.out.println("Waiting for messages...");
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("\uD83D\uDCAC Received message: key=%s, value=%s, partition=%d, offset=%d%n",
                            record.key(), record.value(), record.partition(), record.offset());
                }
            }
        }
    }

}
