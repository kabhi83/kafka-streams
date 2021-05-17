package ak.home.kafka.streams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class StreamsStarterApp {

    public static void main(String[] args) {
        Properties configs = new Properties();
        configs.put(StreamsConfig.APPLICATION_ID_CONFIG, "word-count");
        configs.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        configs.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,  Serdes.String().getClass());

        //Operations to perform word count
        // 1. Read data from kafka
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> streamInput = builder.stream("word-count-input");

        // 2. Convert the values to lowercase using map
        KTable<String, Long> wordCounts = streamInput.mapValues(value -> value.toLowerCase())
        // 3. Split the words using flatmap
        .flatMapValues(value -> Arrays.asList(value.split(" ")))
        // 4. SelectKey to apply a key - discard the old key if any
        .selectKey((oldKey, key) -> key)
        // 5. Perform aggregation with GroupByKey
        .groupByKey()
        // 6. Count occurrences in each group
        .count();
        // 7. Write the result back to kafka
        wordCounts.toStream().to("word-count-output", Produced.with(Serdes.String(), Serdes.Long()));


        //Start the stream application
        final KafkaStreams streams = new KafkaStreams(builder.build(), configs);
        streams.start();

        //print the topology
        System.out.println(streams.toString());

        //Add the shutdown hook to close the streams thread gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
