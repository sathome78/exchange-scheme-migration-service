FROM java:8
ARG JAR_NAME
WORKDIR /opt/migration
ADD ${JAR_NAME} migration.jar
RUN sh -c 'touch migration.jar'
ENTRYPOINT ["java", "-jar", "migration.jar"]