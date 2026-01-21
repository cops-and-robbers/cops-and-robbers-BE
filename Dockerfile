FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} cops-and-robbers.jar

ENTRYPOINT ["java", "-jar", "cops-and-robbers.jar"]
