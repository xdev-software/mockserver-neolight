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

import java.util.Optional;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;


public class MockServerContainer extends GenericContainer<MockServerContainer>
{
	public static final String DEFAULT_IMAGE = "xdevsoftware/mockserver";
	public static final String DEFAULT_TAG = MockServerUtils.DEFAULT_VERSION;
	public static final int PORT = 1080;
	
	public MockServerContainer(final DockerImageName dockerImageName)
	{
		super(dockerImageName);
		
		this.waitingFor(Wait.forLogMessage(".*started on port: " + PORT + ".*", 1));
		this.addExposedPort(PORT);
	}
	
	public MockServerContainer(final String tag)
	{
		this(DockerImageName.parse(DEFAULT_IMAGE
			+ Optional.ofNullable(tag)
			.map(s -> ":" + s)
			.orElse("")));
	}
	
	public MockServerContainer()
	{
		this(MockServerUtils.getClasspathMockserverVersion());
	}
	
	public String getEndpoint()
	{
		return String.format("http://%s:%d", this.getHost(), this.getServerPort());
	}
	
	public String getSecureEndpoint()
	{
		return String.format("https://%s:%d", this.getHost(), this.getServerPort());
	}
	
	public int getServerPort()
	{
		// Can never return null
		return this.getMappedPort(PORT);
	}
}
