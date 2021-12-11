package com.github.rajeevgurram.udemy.kafka.streams.wordcount;

import com.github.rajeevgurram.udemy.kafka.streams.config.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.Properties;

@Slf4j
public class Main {
    public static void main(String[] args) {
        log.info("Adding");
        Properties kafkaConfig = Configuration.getKafkaProperties("word-count-application");

        StreamsBuilder builder = new StreamsBuilder();

        //1 - Stream from Kafka
        KStream<String, String> wordCountInput = builder.stream("word-count-input");

        KTable<String, Long> wordCounts =
                //2 - MapValues to lowerCase
                wordCountInput.mapValues(textLine -> textLine.toLowerCase())
                //3 - FlatMapValues to split by space
                .flatMapValues(lowerCaseTextLine -> Arrays.asList(lowerCaseTextLine.split(" ")))
                //4 - select key to apply a key (we discard the old key)
                .selectKey((ignoredKey, word) -> word)
                //5 - group by key before the aggregation
                .groupBy((s, s2) -> s2)
                //6 - count occurrences
                .count(Named.as("Count"));

        //7 - to in order to write the results back to the kafka
        wordCounts.toStream().to("word-count-output", Produced.with(Serdes.String(), Serdes.Long()));

        KafkaStreams kafkaStreams = new KafkaStreams(builder.build(), kafkaConfig);
        kafkaStreams.start();

        //printing the topology
        System.out.println(kafkaStreams.toString());

        // shutdown hook to correctly close the stream application
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
    }
}
