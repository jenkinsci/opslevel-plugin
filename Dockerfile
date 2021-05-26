FROM maven:3.8.1-adoptopenjdk-11

WORKDIR /jenkins-opslevel/

COPY src/ ./src/
COPY pom.xml .
COPY Jenkinsfile .

RUN mvn verify || true  # Install deps and lint code. TODO: remove || true once that last compiler warning is fixed
RUN mvn package  # Compile (if needed), run tests, and package.
RUN rm -f ./src/main/resources/config.properties  # remove after compile to avoid a bug where java loads from src rather than compiled


CMD ["mvn", "-Dhost=0.0.0.0", "-Djetty.port=8080", "hpi:run"]
