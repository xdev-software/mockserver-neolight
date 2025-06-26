# syntax=docker/dockerfile:1-labs
ARG JAVA_VERSION=21
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine AS jre-base

# Build the JRE ourself and exclude stuff from Eclipse-Temurin that we don't need
#
# Derived from https://github.com/adoptium/containers/blob/91ea190c462741d2c64ed2f8f0a0efdb3e77c49d/21/jre/alpine/3.21/Dockerfile
FROM alpine:3 AS jre-minimized

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH=$JAVA_HOME/bin:$PATH

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

RUN set -eux; \
    # DO NOT INSTALL:
    # gunpg - only required to verify download of jre from eclipse-temurin
    # fontconfig ttf-dejavu - No fonts are needed as nothing is rendered/using AWT
    apk add --no-cache \
        ca-certificates p11-kit-trust coreutils openssl \
        musl-locales musl-locales-lang \
        tzdata

COPY --from=jre-base /opt/java/openjdk /opt/java/openjdk

RUN set -eux; \
    echo "Verifying install ..."; \
    echo "java --version"; java --version; \
    echo "Complete."

# Renamed as cacerts functionality is disabled
COPY --from=jre-base /__cacert_entrypoint.sh /entrypoint.sh
RUN chmod 775 /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]


FROM eclipse-temurin:${JAVA_VERSION}-jdk-alpine AS builder

RUN apk add --no-cache bash

WORKDIR /builder

# Copy & Cache wrapper
COPY --parents mvnw .mvn/** ./
RUN ./mvnw --version

# Copy & Cache poms/dependencies
COPY --parents **/pom.xml ./
# Resolve jars so that they can be cached and don't need to be downloaded when a Java file changes
ARG MAVEN_GO_OFFLINE_COMMAND='./mvnw -B dependency:go-offline -pl server -am -DincludeScope=runtime -T2C'
RUN echo "Executing '$MAVEN_GO_OFFLINE_COMMAND'"
RUN ${MAVEN_GO_OFFLINE_COMMAND}

# Copying all other files
COPY . ./
# Run the actual build
ARG MAVEN_BUILD_COMMAND='./mvnw -B package -pl "server" -am -T2C -Dmaven.test.skip'
RUN echo "Executing '$MAVEN_BUILD_COMMAND'"
RUN ${MAVEN_BUILD_COMMAND}


FROM jre-minimized

ARG user=app
ARG group=app
ARG uid=1000
ARG gid=1000
ARG APP_DIR=/opt/app

# Create user + group + home
RUN mkdir -p ${APP_DIR} \
  && chown ${uid}:${gid} ${APP_DIR} \
  && addgroup -g ${gid} ${group} \
  && adduser -h "$APP_DIR" -u ${uid} -G ${group} -s /bin/sh -D ${user}

WORKDIR ${APP_DIR}

USER ${user}

COPY --from=builder --chown=${user}:${group} builder/server/target/server-standalone.jar ${APP_DIR}/app.jar

# CDS
RUN java -XX:ArchiveClassesAtExit=app.jsa -Dexit-immediately-after-start=1 -jar app.jar -serverPort 1080

# MaxRAMPercentage: Default value is 25% -> we want to use available memory optimal -> increased, but enough is left for other RAM usages like e.g. Metaspace
# Min/MaxHeapFreeRatio: Default values cause container reserved memory not to shrink properly/waste memory -> decreased
# https://stackoverflow.com/questions/16058250/what-is-the-purpose-of-xxminheapfreeratio-and-xxmaxheapfreeratio
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=30 -Djava.awt.headless=true"
ENV JAVA_CDS_OPTS="-XX:SharedArchiveFile=app.jsa"
ENV ARGS="-serverPort 1080"

CMD [ "/bin/sh", "-c", "java $JAVA_OPTS $JAVA_CDS_OPTS -jar app.jar ${ARGS}" ]
