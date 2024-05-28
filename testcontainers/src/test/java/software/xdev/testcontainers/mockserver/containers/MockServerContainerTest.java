package software.xdev.testcontainers.mockserver.containers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.xdev.mockserver.model.HttpRequest.request;
import static software.xdev.mockserver.model.HttpResponse.response;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import software.xdev.mockserver.client.MockServerClient;
import software.xdev.testcontainers.imagebuilder.AdvancedImageFromDockerFile;


class MockServerContainerTest
{
	static DockerImageName image;
	
	@BeforeAll
	static void buildImage()
	{
		image = DockerImageName.parse(new AdvancedImageFromDockerFile("mockserver")
			.withLoggerForBuild(LoggerFactory.getLogger("container.build.mockserver"))
			.withAdditionalIgnoreLines(
				// Ignore files that aren't related to the built code
				".git/**",
				".config/**",
				".github/**",
				".idea/**",
				".run/**",
				"assets/**",
				"docs/**",
				".md",
				".cmd",
				"/renovate.json5",
				"/client/src/**",
				"/testcontainers/src/**")
			.withDockerFilePath(Paths.get("../testcontainers/Standalone.Dockerfile"))
			.withBaseDir(Paths.get("../"))
			.get());
	}
	
	@SuppressWarnings("resource") // HttpClient close does not exist on Java 17!
	@Test
	void smokeTest() throws Exception
	{
		try(final MockServerContainer container = new MockServerContainer(image)
			.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("container.mockserver"))))
		{
			container.start();
			
			try(final MockServerClient client = new MockServerClient(
				container.getHost(),
				container.getServerPort()))
			{
				final String expectedResponse = "Test";
				client.when(request()
						.withPath("/abc")
						.withMethod("GET"))
					.respond(response()
						.withBody(expectedResponse));
				
				final HttpClient httpClient = HttpClient.newBuilder()
					.connectTimeout(Duration.ofMinutes(1))
					.build();
				
				final HttpResponse<String> resp = httpClient.send(
					HttpRequest.newBuilder()
						.uri(URI.create(container.getEndpoint() + "/abc"))
						.GET()
						.timeout(Duration.ofMinutes(1))
						.build(),
					HttpResponse.BodyHandlers.ofString());
				
				Assertions.assertAll(
					() -> assertEquals(200, resp.statusCode()),
					() -> assertEquals(expectedResponse, resp.body()));
			}
		}
	}
}
