package com.tigergraph.fake;

import com.github.javafaker.Faker;
import com.tigergraph.fake.avro.Address;
import com.tigergraph.fake.avro.Person;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Random;

public class Generator {

    private static final String BOOTSTRAP_SERVER_SHORT_OPTION = "b";
    private static final String BOOTSTRAP_SERVER_LONG_OPTION = "bootstrap-server";
    private static final String TOPIC_SHORT_OPTION = "t";
    private static final String TOPIC_LONG_OPTION = "topic";
    private static final String NO_SCHEMA_REGISTRY_SHORT_OPTION = "n";
    private static final String NO_SCHEMA_REGISTRY_LONG_OPTION = "no-schema-registry";
    private static final String SCHEMA_REGISTRY_URL_SHORT_OPTION = "s";
    private static final String SCHEMA_REGISTRY_URL_LONG_OPTION = "schema-registry-url";
    private static final String COUNT_SHORT_OPTION = "c";
    private static final String COUNT_LONG_OPTION = "count";

    private static String topic;
    private static int count;
    private static KafkaProducer producer;

    private static Faker faker;
    private static Random rand;

    private static CommandLine parseCommandLine(String[] args) {
        Options options = new Options();

        Option bootstrapServer = new Option(BOOTSTRAP_SERVER_SHORT_OPTION, BOOTSTRAP_SERVER_LONG_OPTION, true, "Kafka bootstrap server");
        bootstrapServer.setRequired(true);
        options.addOption(bootstrapServer);

        Option topic = new Option(TOPIC_SHORT_OPTION, TOPIC_LONG_OPTION, true, "Kafka topic to produce messages into");
        topic.setRequired(true);
        options.addOption(topic);

        Option noSchemaRegistry = new Option(NO_SCHEMA_REGISTRY_SHORT_OPTION, NO_SCHEMA_REGISTRY_LONG_OPTION, false, "Don't use schema registry");
        options.addOption(noSchemaRegistry);

        Option schemaRegistry = new Option(SCHEMA_REGISTRY_URL_SHORT_OPTION, SCHEMA_REGISTRY_URL_LONG_OPTION, true, "Schema registry url");
        options.addOption(schemaRegistry);

        Option count = new Option(COUNT_SHORT_OPTION, COUNT_LONG_OPTION, true, "Number of messages to generate");
        options.addOption(count);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine cmd = parser.parse(options, args);
            validateCmd(cmd);
            return cmd;
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar ./target/avro-data-generator-1.0-SNAPSHOT-jar-with-dependencies.jar", options);

            System.exit(1);
        }
        return null;
    }

    private static void validateCmd(CommandLine cmd) throws ParseException {
        if (!cmd.hasOption(NO_SCHEMA_REGISTRY_LONG_OPTION) && !cmd.hasOption(SCHEMA_REGISTRY_URL_LONG_OPTION)) {
            throw new ParseException("--schema-registry-url MUST be set if --no-schema-registry is NOT provided");
        }
    }

    private static Properties buildProps(CommandLine cmd) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cmd.getOptionValue(BOOTSTRAP_SERVER_LONG_OPTION));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.ByteArraySerializer.class);

        if (!cmd.hasOption(NO_SCHEMA_REGISTRY_SHORT_OPTION)) {
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class);
            props.put("schema.registry.url", cmd.getOptionValue(SCHEMA_REGISTRY_URL_LONG_OPTION));
        } else {
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.ByteArraySerializer.class);
        }
        return props;
    }

    private static Person generateFakePerson(int id) {
        Person p = new Person();
        p.setId(id);
        p.setFirstName(faker.name().firstName());
        p.setLastName(faker.name().lastName());
        p.setAge(rand.nextInt(100));

        Address a = new Address();
        com.github.javafaker.Address fakeAddress = faker.address();
        a.setLine1(fakeAddress.streetAddress());
        a.setCity(fakeAddress.city());
        a.setState(fakeAddress.state());
        a.setZipcode(fakeAddress.zipCode());
        p.setAddress(a);

        return p;
    }

    public static void main(String[] args) {
        CommandLine cmd = parseCommandLine(args);

        topic = cmd.getOptionValue(TOPIC_LONG_OPTION);
        if (!cmd.hasOption(COUNT_LONG_OPTION)) {
            count = 10;
        } else {
            try {
                count = Integer.parseInt(cmd.getOptionValue(COUNT_LONG_OPTION));
            } catch (NumberFormatException e) {
                System.out.printf("--count must be an integer, but got: %s", cmd.getOptionValue(COUNT_LONG_OPTION));
                System.exit(1);
            }
        }
        Properties props = buildProps(cmd);
        producer = new KafkaProducer(props);

        faker = new Faker();
        rand = new Random();

        if (!cmd.hasOption(NO_SCHEMA_REGISTRY_LONG_OPTION)) {
            for (int i = 0; i < count; i++) {
                Person p = generateFakePerson(i);
                byte[] key = (p.getFirstName().toString() + p.getLastName().toString()).getBytes(StandardCharsets.UTF_8);
                ProducerRecord<byte[], Person> record = new ProducerRecord<>(topic, key, p);
                producer.send(record);
            }
        } else {
            for (int i = 0; i < count; i++) {
                Person p = generateFakePerson(i);
                byte[] key = (p.getFirstName().toString() + p.getLastName().toString()).getBytes(StandardCharsets.UTF_8);
                // serialize Avro data manually
                DatumWriter recordDatumWriter = new SpecificDatumWriter<>(Person.class);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                BinaryEncoder binaryEncoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
                try{
                    recordDatumWriter.write(p, binaryEncoder);
                    binaryEncoder.flush();
                    outputStream.close();
                }
                catch(IOException e){
                    e.printStackTrace();
                }
                ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(topic, key, outputStream.toByteArray());
                producer.send(record);
            }
        }
        producer.flush();
        producer.close();

        System.out.printf("Sent %d messages to topic [%s]", count, topic);
    }
}
