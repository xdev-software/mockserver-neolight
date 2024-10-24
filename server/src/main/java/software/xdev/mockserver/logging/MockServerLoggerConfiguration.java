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
package software.xdev.mockserver.logging;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.logging.LogManager;

import software.xdev.mockserver.configuration.ServerConfigurationProperties;


public final class MockServerLoggerConfiguration
{
	@SuppressWarnings({"java:S106", "java:S4507"}) // If logging configuration fails use System.err
	public static void configureLogger()
	{
		try
		{
			if(System.getProperty("java.util.logging.config.file") == null
				&& System.getProperty("java.util.logging.config.class") == null)
			{
				LogManager.getLogManager().readConfiguration(new ByteArrayInputStream((
					"handlers=software.xdev.mockserver.logging.StandardOutConsoleHandler" + NEW_LINE
						+ "software.xdev.mockserver.logging.StandardOutConsoleHandler.level=ALL" + NEW_LINE
						+ "software.xdev.mockserver.logging.StandardOutConsoleHandler.formatter="
						+ "java.util.logging.SimpleFormatter" + NEW_LINE
						+ "java.util.logging.SimpleFormatter.format=%1$tF %1$tT %4$s %5$s %6$s%n" + NEW_LINE
						+ "software.xdev.mockserver.level=INFO" + NEW_LINE
						+ "io.netty.level=WARNING").getBytes(UTF_8)));
				if(isNotBlank(ServerConfigurationProperties.javaLoggerLogLevel()))
				{
					final String loggingConfiguration =
						(!ServerConfigurationProperties.disableSystemOut()
							? "handlers=software.xdev.mockserver.logging.StandardOutConsoleHandler" + NEW_LINE
							+ "software.xdev.mockserver.logging.StandardOutConsoleHandler.level=ALL" + NEW_LINE
							+ "software.xdev.mockserver.logging.StandardOutConsoleHandler.formatter="
							+ "java.util.logging.SimpleFormatter" + NEW_LINE
							: "")
							+ "java.util.logging.SimpleFormatter.format=%1$tF %1$tT %4$s %5$s %6$s%n" + NEW_LINE
							+ "software.xdev.mockserver.level=" + ServerConfigurationProperties.javaLoggerLogLevel()
							+ NEW_LINE
							+ "io.netty.level="
							+ (Arrays.asList("TRACE", "FINEST")
							.contains(ServerConfigurationProperties.javaLoggerLogLevel()) ? "FINE" : "WARNING");
					LogManager.getLogManager()
						.readConfiguration(new ByteArrayInputStream(loggingConfiguration.getBytes(UTF_8)));
				}
			}
		}
		catch(final Exception ex)
		{
			System.err.println("Failed to configure logger");
			ex.printStackTrace();
		}
	}
	
	private MockServerLoggerConfiguration()
	{
	}
}
