java run configurations: --spring.config.name=application3 -Djava.security.properties="./config/java.security"
-Dlogback.configurationFile="./config/logback.xml" -cp "lib/*"

mvn clean package -Dmaven.test.skip

copy config and lib folders into target directory

java -jar server-0.0.1-SNAPSHOT.jar --spring.config.location=classpath:/application0.properties
