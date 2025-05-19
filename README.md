
# Dependable Distributed Systems Project 1 (Byzantine Fault Tolerant Decentralized Ledger) Intructions

The project was developed using Java 24 + Spring (Boot) Framework + MySQL + Maven -- which takes care of all dependencies, including BFT-SMaRT, BouncyCastle, etc.

To quickly get the project up and running, simply import the git repository's project1 branch into IntelliJ as a Spring Boot + Maven project.

## Server

### application.properties Files

All application[0, 1, 2, 3].properties files are already setup for quick execution -- the only required step is creating 4 new SQL schemas, matching the names in each .properties file, and adding your own DB username and password.

### Running via IntelliJ

To easily configure and run 4 servers simultaneously via IntelliJ, go to `Run > Edit Configurations...` and add 4 new Spring Boot configurations pointing to the `csd.server.ServerApplication` class, and add the following Java arguments (updating, for each, the --spring.config.name argument with the appropriate application index):

`--spring.config.name=application0 -Djava.security.properties="./config/java.security" -Dlogback.configurationFile="./config/logback.xml" -cp "lib/*"`

All 4 servers are now easily and simultaneously executable through the Services tab, located at the bottom of the IDE left sidebar. 

### Compiling, packaging, and running JAR

`mvn clean package -Dmaven.test.skip`

copy `config` and `lib` folders into the `target` directory which contains the .jar

`java -jar server-0.0.1-SNAPSHOT.jar --spring.config.location=classpath:/application0.properties`

## Client

The Client is a Spring Command Line Application, which can be run as-is with the provided configurations.

If the user wants to change the contacted ledger server, simply update the `ledger.address=https://localhost:8092` property between executions, using one of the following ports: 8090, 8091, 8092, 8093 (as per the standard configuration).