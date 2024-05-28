# 1.0.0
_Initial release_

Minimalistic fork of [mock-server/mockserver](https://github.com/mock-server/mockserver), designed for maintainability and working with Testcontainers
* Copied over original code of the following modules with minimalistic dependencies:
  * client-java -> client
  * core -> core
  * netty -> standalone
* Brought code into a maintainable state
  * Removed about 75% of the original code
* Removed everything that is not core functionality:
  * Metrics
  * MemoryTracing
  * Templating-Engines
  * all body matching that requires an external lib (XPATH, JSON, XML, OpenAPI, ...)
  * Versioncheck
  * Persistence of expectations to disk
  * Dashboard (contains self-built webfrontend)
  * parts of Logging
  * TLS/SSL MitM (did never work with TLS 1.3+ and HTTP2+ anyway) 
  * JWT + mDNS auth
* Split configuration into corresponding modules
* Split Logs and Events (e.g. request received) into independent subsystems
* Improved performance on various places
  * Removed reflective ``equals`` & ``hashCode``
* Slimed down dependencies
* Updated dependencies (fixes various CVEs)

### Fixes the following issues from the [original project](https://github.com/mock-server/mockserver)

> * Removed useless/Minimize dependencies of ``mockserver-client-java`` [#1494](https://github.com/mock-server/mockserver/issues/1494)
> * ARM image doesn't include ARM binaries [#1568](https://github.com/mock-server/mockserver/issues/1568)
> * Add any Shell to Docker Container [#1593](https://github.com/mock-server/mockserver/issues/1593)
> * io.netty 4.1.89.Final is using unsupported JDK internal APIs [#1812](https://github.com/mock-server/mockserver/issues/1812)
> *  MockServerClient.verify(RequestDefinition requestDefinition, VerificationTimes times) not works correctly after update to 5.14.0 [#1524](https://github.com/mock-server/mockserver/issues/1524) 
> * ... and probably many more

# 0.x.x
_Preview releases_
