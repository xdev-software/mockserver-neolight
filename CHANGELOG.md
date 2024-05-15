# 1.0.0
_Initial release_
Minimalistic fork of [mock-server/mockserver](https://github.com/mock-server/mockserver), designed for maintainability and working with Testcontainers
* Copied over original code of the following modules with minimalistic dependencies:
  * client-java -> client
  * core -> core
  * netty -> standalone
* Removed
  * Metrics, Templating, XML subsystem - to decrease amount of dependencies (core)
  * Version - as it's broken and that communication with original implementation is possible
