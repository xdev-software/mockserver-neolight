# Stage 1: Build the dummy app
FROM eclipse-temurin:21-jdk-alpine AS build-env

RUN apk add --no-cache git bash

# Create non root user
ARG userName=limitedbuild
ARG groupName=limitedbuild
ARG userId=1000

RUN addgroup --system ${groupName} \
	&& adduser --uid ${userId} --system --disabled-password --shell /bin/bash ${userName} \
	&& adduser ${userName} ${groupName}

# Create build dir
RUN mkdir /build \
    && chown ${userName}:${groupName} /build
WORKDIR /build

USER ${userName}

# Copying context is prepared by Testcontainers
COPY --chown=${userName}:${groupName} . ./

ARG mvncmd='clean package -pl "server" -am -T2C -Dmaven.test.skip'

RUN echo "Executing '$mvncmd'"
RUN chmod +x ./mvnw \
  && ./mvnw -B ${mvncmd}

# Stage 2: Build the executable image
FROM eclipse-temurin:21-jre-alpine

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

EXPOSE 1080

USER ${user}

COPY --from=build-env --chown=${user}:${group} build/server/target/server-standalone.jar ${APP_DIR}/server-standalone.jar

# MaxRAMPercentage: Default value is 25% -> we want to use available memory optimal -> increased, but enough is left for other RAM usages like e.g. Metaspace
# Min/MaxHeapFreeRatio: Default values cause container reserved memory not to shrink properly/waste memory -> decreased
# https://stackoverflow.com/questions/16058250/what-is-the-purpose-of-xxminheapfreeratio-and-xxmaxheapfreeratio
ENV JAVA_OPTS "-XX:MaxRAMPercentage=75 -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=30 -Djava.awt.headless=true"
ENV ARGS "-serverPort 1080"

CMD java ${JAVA_OPTS} -jar /opt/app/server-standalone.jar ${ARGS}
