package org.strauteka.kafka.test.consumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.StickyAssignor;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

public class App {
    public static void main(String[] args) {
        System.out.println("Starting simple consumer");
        Properties props = new Properties();
        final String topic = Optional.ofNullable(args).filter(Objects::nonNull).filter(e -> e.length > 0).map(e -> e[0])
                .orElseGet(() -> "test");
        final String kafka_consumer = Optional.ofNullable(args).filter(Objects::nonNull).filter(e -> e.length > 1)
                .map(e -> e[1]).orElseGet(() -> "kafka-basic-consumer");

        final String kafka_group = Optional.ofNullable(args).filter(Objects::nonNull).filter(e -> e.length > 2)
                .map(e -> e[2]).orElseGet(() -> "test-group");
        // "localhost:9092,localhost:9093"
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9091,localhost:9092,localhost:9093");
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, kafka_consumer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafka_group);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "5000"); // not more than 1/3 of
                                                                        // SESSION_TIMEOUT_MS_CONFIG
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, StickyAssignor.class.getName());
        // if new group introduced gets all offset topics
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // returns at max 1 item in request
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");
        // if consumer not gracefully shutdown, not messaged broker
        // If poll() is not called before expiration of this timeout, then the consumer
        // is considered failed and the group will rebalance in order to reassign the
        // partitions to another member.
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "30000");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        final Thread mainThread = Thread.currentThread();

        // Registering a shutdown hook so we can exit cleanly
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Starting exit...");
                // Note that shutdownhook runs in a separate thread, so the only thing we can
                // safely do to a consumer is wake it up
                consumer.wakeup();
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            consumer.subscribe(Collections.singletonList(topic));
            System.out.println(consumer.groupMetadata());
            java.util.Map<String, java.util.List<PartitionInfo>> listTopics = consumer.listTopics();
            System.out.println("list of topic size :" + listTopics.size());

            for (String t : listTopics.keySet()) {
                System.out.println("topic name :" + t);
            }
            System.out.println("Subscribed to topic " + topic);
            while (true) {
                System.out.println("Waiting for messages!");
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf(
                            "consumer id:%s, partition id= %s, key = %s, value = %s, offset = %s, timestamp = %s%n",
                            kafka_consumer, record.partition(), record.key(), record.value(), record.offset(),
                            record.timestamp());

                }
                for (TopicPartition tp : consumer.assignment())
                    System.out.println(
                            "Committing offset at position:" + consumer.position(tp) + ", topic: " + tp.topic());
                // autocmmit set
                consumer.commitSync();
            }
        } catch (WakeupException e) {
            // ignore for shutdown 2
        } finally {
            consumer.close();
            System.out.println("Closed consumer and we are done");
        }

    }
}
