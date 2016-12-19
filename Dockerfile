FROM azul/zulu-openjdk:7
COPY entry mvnw pom.xml /app/
COPY .mvn /app/.mvn
COPY code /app/code
COPY resources /app/resources
WORKDIR /app
RUN ./mvnw install
RUN ./mvnw exec:java -pl code/iaas/model -Dexec.mainClass=io.cattle.platform.core.cleanup.Main || true
ENTRYPOINT ["/app/entry"]
