package com.example.multi;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

// mvn compile exec:java -Dexec.mainClass="com.example.multi.Consumer" -Dexec.args="test-topic group1"
public class Consumer {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("❌ Please provide topic name as the first argument.");
            System.exit(1);
        }

        String topicName = args[0];
        String groupId = args.length > 1 ? args[1] : "test-group";

        KafkaConsumer<String, String> consumer = getKafkaConsumer(groupId);

        try (consumer) {
            consumer.subscribe(Collections.singletonList(topicName));
            System.out.println("Waiting for messages on topic '" + topicName + "' in group '" + groupId + "'...");

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("💬 Received message: key=%s, value=%s, partition=%d, offset=%d%n",
                            record.key(), record.value(), record.partition(), record.offset());
                }
            }
        }
    }

    private static KafkaConsumer<String, String> getKafkaConsumer(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new KafkaConsumer<>(props);
    }

}
