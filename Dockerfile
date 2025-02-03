FROM eclipse-temurin

#ENV RELSERVER_PARAMETERS "" #todo

ARG JAR_FILE=target/relserver-*-jar-with-dependencies.jar
COPY ${JAR_FILE} relserver.jar

ENTRYPOINT ["java", "-jar", "/relserver.jar"]