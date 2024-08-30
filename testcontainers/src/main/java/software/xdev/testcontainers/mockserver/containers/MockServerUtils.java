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

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utility methods for MockServer
 */
public final class MockServerUtils
{
	private static final Logger LOG = LoggerFactory.getLogger(MockServerUtils.class);
	
	public static final String DEFAULT_VERSION = "latest";
	private static String cachedVersion;
	
	static final String MOCKSERVER_VERSION_SNAPSHOT_ALLOWED_KEY = "MOCKSERVER_VERSION_SNAPSHOT_ALLOWED";
	
	private MockServerUtils()
	{
	}
	
	/**
	 * Based on the JARs detected on the classpath, determine which version of mockserver is available.
	 *
	 * @return the detected version of Mockserver, or DEFAULT_VERSION if it could not be determined
	 */
	public static String getClasspathMockserverVersion()
	{
		if(cachedVersion != null)
		{
			return cachedVersion;
		}
		cachedVersion = determineClasspathMockserverVersion();
		return cachedVersion;
	}
	
	static synchronized String determineClasspathMockserverVersion()
	{
		if(cachedVersion != null)
		{
			return cachedVersion;
		}
		
		final Set<String> versions = new HashSet<>();
		try
		{
			final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			final Enumeration<URL> manifests = classLoader.getResources("META-INF/MANIFEST.MF");
			
			while(manifests.hasMoreElements())
			{
				final URL manifestURL = manifests.nextElement();
				try(final InputStream is = manifestURL.openStream())
				{
					final Manifest manifest = new Manifest();
					manifest.read(is);
					
					final String version = getMockserverVersionFromManifest(manifest);
					if(version != null)
					{
						versions.add(version);
						LOG.info("Mockserver API version {} detected on classpath", version);
					}
				}
			}
		}
		catch(final Exception e)
		{
			LOG.debug("Failed to determine Mockserver-Version from JAR Manifest", e);
		}
		
		if(versions.isEmpty())
		{
			LOG.warn(
				"Failed to determine Mockserver version from classpath - will use default version of {}",
				DEFAULT_VERSION
			);
			return DEFAULT_VERSION;
		}
		
		final String foundVersion = versions.iterator().next();
		if(versions.size() > 1)
		{
			LOG.warn(
				"Multiple versions of Mockserver API found on classpath - will select {}, but this may not be "
					+ "reliable",
				foundVersion
			);
		}
		
		if(foundVersion.endsWith("-SNAPSHOT") && !isSnapshotVersionAllowed())
		{
			LOG.warn("Found version is a SNAPSHOT - will use default {}", DEFAULT_VERSION);
			return DEFAULT_VERSION;
		}
		
		return foundVersion;
	}
	
	static boolean isSnapshotVersionAllowed()
	{
		return Stream.of(
				System.getenv(MOCKSERVER_VERSION_SNAPSHOT_ALLOWED_KEY),
				System.getProperty(MOCKSERVER_VERSION_SNAPSHOT_ALLOWED_KEY))
			.filter(Objects::nonNull)
			.anyMatch(s -> Boolean.parseBoolean(s) || "1".equals(s));
	}
	
	/**
	 * Read Manifest to get Mockserver version.
	 *
	 * @param manifest manifest
	 * @return Mockserver version detected
	 */
	static String getMockserverVersionFromManifest(final Manifest manifest)
	{
		String versuib = null;
		final Attributes buildInfo = manifest.getAttributes("Mockserver-Info");
		if(buildInfo != null)
		{
			versuib = buildInfo.getValue("Version");
		}
		
		return versuib;
	}
}
