# 1.0.0
_Initial release_
Minimalistic fork of [mock-server/mockserver](https://github.com/mock-server/mockserver), designed for maintainability and working with Testcontainers
* Copied over original code of the following modules with minimalistic dependencies:
  * client-java -> client
  * core -> core
  * netty -> standalone
* Removed everything that is not core functionality
  * Metrics
  * MemoryTracing
  * Templating-Engines
  * all body matching that requires an external lib (XPATH, JSON, XML, OpenAPI, ...)
  * Version
  * Persistence of expectations
