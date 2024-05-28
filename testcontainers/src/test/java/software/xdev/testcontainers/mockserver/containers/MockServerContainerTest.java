/*
 * Copyright © 2024 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
