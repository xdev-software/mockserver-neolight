# 1.0.3
* Updated dependencies

# 1.0.2
* Various minor code optimizations
* [Testcontainers] Added flag ``MOCKSERVER_VERSION_SNAPSHOT_ALLOWED`` to allow usage of Snapshot Container images
* Updated dependencies

# 1.0.1
* Fix ``proxyRemoteHost`` being ignored
* Improved some log messages

# 1.0.0
_Initial release_

## MockServer
Minimalistic fork of [mock-server/mockserver](https://github.com/mock-server/mockserver), designed for maintainability and working with Testcontainers
* Copied over original code of the following modules with minimalistic dependencies:
  * client-java → client
  * core → core
  * netty → server
* Brought code into a maintainable state
  * Removed about 75% of the original code
  * Removed everything that is not core functionality:
    * Metrics
    * MemoryTracing
    * Templating-Engines
    * all body matching that requires an external lib (XPATH, JSON, XML, OpenAPI, ...)
    * Versioncheck
    * Persistence of expectations to disk
    * Dashboard (contained self-built webfrontend)
    * parts of logging
    * TLS/SSL MitM (did never work with TLS 1.3+ and HTTP2+ anyway) 
    * JWT + mDNS auth
* Split configuration into corresponding modules
* Split Logs and Events (e.g. request received) into independent subsystems
  * Client no longer messes up logging by reconfiguring JUL dynamically
* Improved performance on various places
  * Removed reflective ``equals`` & ``hashCode``
* Slimed down dependencies
  * Standalone Server Jar is now 6x smaller (42MB → 6MB)
  * Reduced client dependencies by 60% (~42 → 17)
* Updated dependencies (fixes various CVEs)
* [Docker] Use ``eclipse-temurin`` instead of ``distroless``
  * The overall image is now roughly 2x smaller
* Compiles now with Java 17+
* No longer catches critical ``Errors`` such as ``OutOfMemoryError`` or ``NoClassDefFoundError``

### Migration guide
* Change all occurrences of ``org.mockserver`` to ``software.xdev.mockserver``

### Fixes the following issues from the [original project](https://github.com/mock-server/mockserver)
* Removed useless/Minimize dependencies of ``mockserver-client-java`` [#1494](https://github.com/mock-server/mockserver/issues/1494)
* ARM image doesn't include ARM binaries [#1568](https://github.com/mock-server/mockserver/issues/1568)
* Add any Shell to Docker Container [#1593](https://github.com/mock-server/mockserver/issues/1593)
* io.netty 4.1.89.Final is using unsupported JDK internal APIs [#1812](https://github.com/mock-server/mockserver/issues/1812)
*  MockServerClient.verify(RequestDefinition requestDefinition, VerificationTimes times) not works correctly after update to 5.14.0 [#1524](https://github.com/mock-server/mockserver/issues/1524) (Ported fix from @szada92)
* ... and probably many more

## Testcontainers
Created a fork of [testcontainers/mockserver](https://java.testcontainers.org/modules/mockserver) that
* fixes the problem that the docker/containerimage is not usable as
  * Testcontainers [requires that the used image is always provided by original project](https://github.com/testcontainers/testcontainers-java/blob/6658a2c0a880d01c6d402ea9a4cb5f72eb15083c/modules/mockserver/src/main/java/org/testcontainers/containers/MockServerContainer.java#L37)
  * The launch arguments are slightly different now (due to usage of ``eclipse-temurin`` instead of ``distroless`` as base image)
* If no version/[tag](https://docs.docker.com/glossary/#tag) is specified it tries to automatically detect the used Mockserver version from the classpath.<br/>This way it should be easier to tackle incompatibilities between client and server.

# 0.x.x
_Preview releases_
