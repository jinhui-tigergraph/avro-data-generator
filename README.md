# avro-data-generator
Generate fake data in Avro format and produce them into Kafka

## Environment Requirement
Following are the environment on my local and works fine. Other versions of Java and Maven should be working as well, but not tested.
* Java
  ```shell
  $ java -version
  openjdk version "11.0.10" 2021-01-19
  OpenJDK Runtime Environment AdoptOpenJDK (build 11.0.10+9)
  OpenJDK 64-Bit Server VM AdoptOpenJDK (build 11.0.10+9, mixed mode)
  ```
* Maven
  ```shell
  $ mvn -version
  Apache Maven 3.8.4 (NON_CANONICAL)
  Maven home: /opt/app/apache-maven-3.8.4
  Java version: 11.0.10, vendor: AdoptOpenJDK, runtime: /home/tigergraph/.glocal/jdk-11.0.10+9
  Default locale: en_US, platform encoding: UTF-8
  OS name: "linux", version: "5.4.0-104-generic", arch: "amd64", family: "unix"
  ```

## Build
### Build Avro Schema File
```shell
java -jar avro-tools-1.11.0.jar compile schema ./avro/* ./src/main/java/
```

### Build Java Project
```shell
mvn clean compile assembly:single
```
Then a jar file named `avro-data-generator-1.0-SNAPSHOT-jar-with-dependencies.jar` will be generated in `target/` folder.

## Usage
```shell
java -jar ./target/avro-data-generator-1.0-SNAPSHOT.jar
 -b,--bootstrap-server <arg>      Kafka bootstrap server
 -c,--count <arg>                 Number of messages to generate
 -f,--file <arg>                  Avro file to produce
 -n,--no-schema-registry          Don't use schema registry
 -p,--properties <arg>            Producer properties file
 -s,--schema-registry-url <arg>   Schema registry url
 -t,--topic <arg>                 Kafka topic to produce messages into
```

### Sample Command Without Schema Registry
```shell
java -jar target/avro-data-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --bootstrap-server localhost:9092 --topic test --count 10 --no-schema-registry
```

### Sample Command With Schema Registry
```shell
java -jar target/avro-data-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --bootstrap-server localhost:9092 --topic test-with-registry --count 10 --schema-registry-url http://localhost:8081
```

### Sample Command with File Path
```shell
java -jar target/avro-data-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --bootstrap-server localhost:9092 --topic test-with-file --file /path/to/avro/test/file.avro
```

### Sample Command with Customized Producer Properties
Say there's a properties file named `producer.properties` with following content
```
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# see org.apache.kafka.clients.producer.ProducerConfig for more details

############################# Producer Basics #############################
bootstrap.servers=kafka-host.tigergraph.com:9092
security.protocol=SASL_SSL
sasl.mechanism=GSSAPI
sasl.kerberos.service.name=kafka
sasl.jaas.config=com.sun.security.auth.module.Krb5LoginModule required useKeyTab=true storeKey=true keyTab="/path/to/kafka-producer.keytab" principal="kafka-producer@TIGERGRAPH.COM";

ssl.endpoint.identification.algorithm=
ssl.keystore.location=/path/to/server.keystore.jks
ssl.keystore.password=********
ssl.key.password=********
ssl.truststore.location=/path/to/server.truststore.jks
ssl.truststore.password=********
# specify the compression codec for all data generated: none, gzip, snappy, lz4, zstd
compression.type=none

# name of the partitioner class for partitioning events; default partition spreads data randomly
#partitioner.class=

# the maximum amount of time the client will wait for the response of a request
#request.timeout.ms=

# how long `KafkaProducer.send` and `KafkaProducer.partitionsFor` will block for
#max.block.ms=

# the producer will wait for up to the given delay to allow other records to be sent so that the sends can be batched together
#linger.ms=

# the maximum size of a request in bytes
#max.request.size=

# the default batch size in bytes when batching multiple records sent to a partition
#batch.size=

# the total bytes of memory the producer can use to buffer records waiting to be sent to the server
#buffer.memory=
```
The corresponding command is
```shell
java -jar target/avro-data-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --bootstrap-server kafka-host.tigergraph.com:9092 --topic test --count 10 --no-schema-registry --properties /path/to/producer.properties
```
