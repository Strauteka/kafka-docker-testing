package org.strauteka.kafka.test.producer;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

public class App {
    public static void main(String[] args) {
        System.out.println("Run basic Producer!");
        Properties props = new Properties();
        // "localhost:9092,localhost:9093"
        final String topic = Optional.ofNullable(args).filter(Objects::nonNull).filter(e -> e.length > 0).map(e -> e[0])
                .orElseGet(() -> "test");

        final String clientId = Optional.ofNullable(args).filter(Objects::nonNull).filter(e -> e.length > 1)
                .map(e -> e[0]).orElseGet(() -> "kafka-basic-producer");

        final Optional<Integer> partition = Optional.ofNullable(args).filter(Objects::nonNull).filter(e -> e.length > 2)
                .map(e -> asInteger(e[2])).filter(e -> e.isPresent()).map(e -> e.get());

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9091,localhost:9092,localhost:9093");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);// 64MB
        // {this
        // props.put(ProducerConfig.ACKS_CONFIG, "all");
        // props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // or this
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        // }
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        // retries to send message
        props.put(ProducerConfig.RETRIES_CONFIG, "3");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1000);

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        // Registering a shutdown hook so we can exit cleanly
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Starting exit...");
                producer.close();
            }
        });

        Random rnd = new Random();
        try {
            while (true) {
                for (int i = 0; i < 1 + rnd.nextInt(4); i++) {
                    if (partition.isPresent()) {
                        producer.send(
                                new ProducerRecord<>(topic, partition.get(), String.valueOf(i), "Hello-Kafka-" + i),
                                callback);
                    } else {
                        producer.send(new ProducerRecord<>(topic, String.valueOf(i), "Hello-Kafka-" + i), callback);
                    }
                }
                Thread.sleep(5000 + rnd.nextInt(5000));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            producer.close();
            System.out.println("Closed producer and we are done");
        }

    }

    public static Optional<Integer> asInteger(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private static Callback callback = new Callback() {
        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e == null) {
                System.out.println("Partition: " + recordMetadata.partition() + " Offset: " + recordMetadata.offset()
                        + " Timestamp: " + recordMetadata.timestamp());
            } else {
                System.out.println("The message can't be produced" + e.getMessage() + Stream.of(e.getStackTrace())
                        .map(StackTraceElement::toString).collect(Collectors.joining("\n")));
            }
        }
    };
}
