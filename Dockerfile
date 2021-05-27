FROM maven:3.8.1-adoptopenjdk-11

WORKDIR /jenkins-opslevel/

COPY src/ ./src/
COPY pom.xml .
COPY Jenkinsfile .

RUN mvn verify  # install deps, compile, test, and lint the plugin
RUN mvn package
RUN rm -f ./src/main/resources/config.properties  # remove after compile to avoid a bug where java loads from src instead of target


CMD ["mvn", "-Dhost=0.0.0.0", "-Djetty.port=8080", "hpi:run"]
