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
