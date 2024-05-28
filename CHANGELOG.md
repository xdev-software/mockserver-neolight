# 1.0.0
_Initial release_
Minimalistic fork of [mock-server/mockserver](https://github.com/mock-server/mockserver), designed for maintainability and working with Testcontainers
* Copied over original code of the following modules with minimalistic dependencies:
  * client-java -> client
  * core -> core
  * netty -> standalone
* Bring code into a maintainable state
* Removed everything that is not core functionality
  * Metrics
  * MemoryTracing
  * Templating-Engines
  * all body matching that requires an external lib (XPATH, JSON, XML, OpenAPI, ...)
  * Version
  * Persistence of expectations
  * Dashboard
  * parts of Logging
  * TLS/SSL MitM (did never work with TLS 1.3+ and HTTP2+ anyway) 
  * JWT + mDNS auth
* Split configuration into corresponding modules
* Split Logs and Events (e.g. request received) into independent subsystems
* Improved performance
  * Removed reflective ``equals`` & ``hashCode``
* Slimed down dependencies

# 0.x.x
_Preview releases_
