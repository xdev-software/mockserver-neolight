[![Maven latest version](https://img.shields.io/maven-central/v/software.xdev.mockserver/client?logo=apache%20maven)](https://mvnrepository.com/artifact/software.xdev.mockserver/client)
[![DockerHub latest version](https://img.shields.io/docker/v/xdevsoftware/mockserver?sort=semver&logo=docker&label=DockerHub)](https://hub.docker.com/r/xdevsoftware/mockserver)
[![Build](https://img.shields.io/github/actions/workflow/status/xdev-software/mockserver-neolight/check-build.yml?branch=develop)](https://github.com/xdev-software/mockserver-neolight/actions/workflows/check-build.yml?query=branch%3Adevelop)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=xdev-software_mockserver-neolight&metric=alert_status)](https://sonarcloud.io/dashboard?id=xdev-software_mockserver-neolight)

# <img src="./assets/logo.avif" height=32 /> MockServer NeoLight

A lightweight rewrite of the abandoned [MockServer project](https://github.com/mock-server/mockserver) with focus on simplicity, maintainability and [Testcontainers](https://java.testcontainers.org/).

> [!NOTE]
> The full list of changes can be found in the [changelog](./CHANGELOG.md#100).<br/>
> You may also have a look at the [comparison with other frameworks](./COMPARISON.md).

## Usage

### In combination with Testcontainers
Besides a few differences the usage is mostly identical to the original project.

```java
try(MockServerContainer container = new MockServerContainer())
{
  container.start();
  
  try(MockServerClient client = new MockServerClient(
    container.getHost(),
    container.getServerPort()))
  {
    String expectedResponse = "Test";
    // Setup expectation
    client.when(request("/test").withMethod("GET"))
      .respond(response().withBody(expectedResponse));
    
    HttpClient httpClient = HttpClient.newHttpClient();
    
    // Execute request
    HttpResponse<String> resp = httpClient.send(
      HttpRequest.newBuilder()
        .uri(URI.create(container.getEndpoint() + "/test"))
        .GET()
        .build(),
      HttpResponse.BodyHandlers.ofString());
    
    assertEquals(expectedResponse, resp.body());
  }
}
```

<details><summary>Example using forwarding/recording</summary>

```java
try(MockServerContainer container = new MockServerContainer())
{
  container.start();
  
  try(MockServerClient client = new MockServerClient(
    container.getHost(),
    container.getServerPort()))
  {
    // Setup forwarding
    client.when(request("/"))
      .forward(HttpForward.forward().withHost("my-nginx.local"));
    
    HttpClient httpClient = HttpClient.newHttpClient();
    
    // Execute request
    HttpResponse<String> resp = httpClient.send(
      HttpRequest.newBuilder()
        .uri(URI.create(container.getEndpoint() + "/"))
        .GET()
        .build(),
      HttpResponse.BodyHandlers.ofString());
    
    assertTrue(resp.body().contains("Welcome to nginx!"));
    
    // You can also retrieve requests, expectations and responses
    String recorded =
      client.retrieveRecordedRequestsAndResponses(request("/"), Format.JSON);
    // or generate the code for writing them
    String codeToGenerateExpectation =
      client.retrieveRecordedExpectations(request("/"), Format.JAVA);
  }
}
```

The returned ``codeToGenerateExpectation`` will look like this:
```java
new MockServerClient("localhost", 1080)
.when(
        request()
                .withMethod("GET")
                .withPath("/")
                ...,
        Times.once(),
        TimeToLive.unlimited(),
        0
)
.respond(
        response()
                .withStatusCode(200)
                .withReasonPhrase("OK")
                .withHeaders(...)
                .withBody("<!DOCTYPE html>\n<html>\n<head>\n<title>Welcome to nginx!</title>...")
);
```

</details>

<details><summary>Required dependencies in <code>pom.xml</code></summary>

```xml
<dependency>
   <groupId>software.xdev.mockserver</groupId>
   <artifactId>client</artifactId>
   <version>...</version>
   <scope>test</scope>
</dependency>
<dependency>
   <groupId>software.xdev.mockserver</groupId>
   <artifactId>testcontainers</artifactId>
   <version>...</version>
   <scope>test</scope>
</dependency>
```

</details>

### Further documentation
* [Original project](https://www.mock-server.com/)
* [Testcontainers Mockserver module](https://java.testcontainers.org/modules/mockserver/)

MockServer also works really well together with a network failure simulation tools such as [ToxiProxy](https://java.testcontainers.org/modules/toxiproxy/).

## Installation
[Installation guide for the latest release](https://github.com/xdev-software/mockserver-neolight/releases/latest#Installation)

<table>
  <tr>
    <th>Module</th>
    <th>Distribution via</th>
  </tr>
  <tr>
    <td><a href="./client/">client</a></td>
    <td>
      <a href="https://mvnrepository.com/artifact/software.xdev.mockserver/client">
        <img src="https://img.shields.io/maven-central/v/software.xdev.mockserver/client?logo=apache%20maven"/>
      </a>
    </td>
  </tr>
  <tr>
    <td><a href="./server/">server</a></td>
    <td>
      <a href="https://hub.docker.com/r/xdevsoftware/mockserver">
        <img src="https://img.shields.io/docker/v/xdevsoftware/mockserver?sort=semver&logo=docker&label=DockerHub"/>
        <img src="https://img.shields.io/docker/pulls/xdevsoftware/mockserver?logo=docker&label=pulls"/>
      </a>
      <br/>
      <a href="https://github.com/xdev-software/mockserver-neolight/pkgs/container/mockserver-neolight">
        <img src="https://img.shields.io/badge/ghcr.io-available-blue?logo=docker"/>
      </a>
      <br/>
      <a href="https://github.com/xdev-software/mockserver-neolight/releases/latest">
        <img src="https://img.shields.io/github/v/release/xdev-software/mockserver-neolight?logo=apache%20maven&label=github"/>
      </a>
      <br/>
      <a href="https://mvnrepository.com/artifact/software.xdev.mockserver/server">
        <img src="https://img.shields.io/maven-central/v/software.xdev.mockserver/server?logo=apache%20maven"/>
    </td>
  </tr>
  <tr>
    <td><a href="./testcontainers/">testcontainers</a></td>
    <td>
      <a href="https://mvnrepository.com/artifact/software.xdev.mockserver/testcontainers">
        <img src="https://img.shields.io/maven-central/v/software.xdev.mockserver/testcontainers?logo=apache%20maven"/>
      </a>
    </td>
  </tr>
</table>

## Support
If you need support as soon as possible and you can't wait for any pull request, feel free to use [our support](https://xdev.software/en/services/support).

## Contributing
See the [contributing guide](./CONTRIBUTING.md) for detailed instructions on how to get started with our project.

## Dependencies and Licenses
View the [license of the current project](LICENSE) or the [docs that contain a dependency summary (per module)](https://xdev-software.github.io/mockserver-neolight/)
