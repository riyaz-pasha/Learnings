package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;
import java.util.concurrent.Future;

// mvn compile exec:java -Dexec.mainClass="com.example.ProducerExample"
public class ProducerExample {

    public static void main(String[] args) {

        // Kafka producer properties
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//        props.put("bootstrap.servers", "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
//        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
//        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // Create Kafka producer

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            // Create a message
            ProducerRecord<String, String> record = new ProducerRecord<>("test-topic", "key1", "Hello Kafka!");

            // Send message asynchronously
            Future<RecordMetadata> future = producer.send(record);

            // Optionally wait for the result
            RecordMetadata metadata = future.get();
            System.out.printf("Message sent to topic:%s partition:%d offset:%d%n",
                    metadata.topic(), metadata.partition(), metadata.offset());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
