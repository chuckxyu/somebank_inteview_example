#
# Build stage
#
FROM maven:3.6.1-jdk-8-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml compile package

#
# Package stage
#
FROM openjdk:8-jre-slim
COPY --from=build /home/app/target/OperationAuthorizer-0.0.1-SNAPSHOT-jar-with-dependencies.jar /usr/local/lib/OperationAuthorizer-0.0.1-SNAPSHOT-jar-with-dependencies.jar
#ENTRYPOINT exec java -jar /usr/local/lib/OperationAuthorizer-0.0.1-SNAPSHOT-jar-with-dependencies.jar