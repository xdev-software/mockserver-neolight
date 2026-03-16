# Testcontainers Integration

This a fork of [testcontainers/mockserver](https://java.testcontainers.org/modules/mockserver) that
* fixes the problem that the docker/containerimage is not usable as
  * Testcontainers [requires that the used image is always provided by original project](https://github.com/testcontainers/testcontainers-java/blob/6658a2c0a880d01c6d402ea9a4cb5f72eb15083c/modules/mockserver/src/main/java/org/testcontainers/containers/MockServerContainer.java#L37)
  * The launch arguments are slightly different now (due to usage of ``eclipse-temurin`` instead of ``distroless`` as base image)
* If no version/[tag](https://docs.docker.com/glossary/#tag) is specified it tries to automatically detect the used Mockserver version from the classpath.<br/>This way it should be easier to tackle incompatibilities between client and server.

> [!IMPORTANT]  
> Due to modularity this module doesn't ships with a dependency to ``software.xdev.mockserver:client``.<br/>
> You have to add it manually!
