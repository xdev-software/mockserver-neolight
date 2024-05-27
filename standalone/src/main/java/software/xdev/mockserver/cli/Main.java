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
package software.xdev.mockserver.cli;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.cli.Main.Arguments.logLevel;
import static software.xdev.mockserver.cli.Main.Arguments.proxyRemoteHost;
import static software.xdev.mockserver.cli.Main.Arguments.proxyRemotePort;
import static software.xdev.mockserver.cli.Main.Arguments.serverPort;
import static software.xdev.mockserver.mock.HttpState.setPort;
import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.util.StringUtils.substringAfter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.mockserver.configuration.ConfigurationProperties;
import software.xdev.mockserver.configuration.IntegerStringListParser;
import software.xdev.mockserver.configuration.ServerConfigurationProperties;
import software.xdev.mockserver.logging.MockServerLoggerConfiguration;
import software.xdev.mockserver.netty.MockServer;


public final class Main
{
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);
	
	static final String USAGE = String.join(NEW_LINE, List.of(
		"   java -jar <path to mockserver.jar> -serverPort <port> [-proxyRemotePort <port>] [-proxyRemoteHost "
			+ "<hostname>] [-logLevel <level>] ",
		"                                                                                                             "
			+ "              ",
		"     valid options are:                                                                                      "
			+ "              ",
		"        -serverPort <port>           The HTTP, HTTPS, SOCKS and HTTP CONNECT                                 "
			+ "              ",
		"                                     port(s) for both mocking and proxying                                   "
			+ "              ",
		"                                     requests.  Port unification is used to                                  "
			+ "              ",
		"                                     support all protocols for proxying and                                  "
			+ "              ",
		"                                     mocking on the same port(s). Supports                                   "
			+ "              ",
		"                                     comma separated list for binding to                                     "
			+ "              ",
		"                                     multiple ports.                                                         "
			+ "              ",
		"                                                                                                             "
			+ "              ",
		"        -proxyRemotePort <port>      Optionally enables port forwarding mode.                                "
			+ "              ",
		"                                     When specified all requests received will                               "
			+ "              ",
		"                                     be forwarded to the specified port, unless                              "
			+ "              ",
		"                                     they match an expectation.                                              "
			+ "              ",
		"                                                                                                             "
			+ "              ",
		"        -proxyRemoteHost <hostname>  Specified the host to forward all proxy                                 "
			+ "              ",
		"                                     requests to when port forwarding mode has                               "
			+ "              ",
		"                                     been enabled using the proxyRemotePort                                  "
			+ "              ",
		"                                     option.  This setting is ignored unless                                 "
			+ "              ",
		"                                     proxyRemotePort has been specified. If no                               "
			+ "              ",
		"                                     value is provided for proxyRemoteHost when                              "
			+ "              ",
		"                                     proxyRemotePort has been specified,                                     "
			+ "              ",
		"                                     proxyRemoteHost will default to \"localhost\".                          "
			+ "              ",
		"                                                                                                             "
			+ "              ",
		"        -logLevel <level>            Optionally specify log level using SLF4J levels:                        "
			+ "              ",
		"                                     TRACE, DEBUG, INFO, WARN, ERROR, OFF or Java                            "
			+ "              ",
		"                                     Logger levels: FINEST, FINE, INFO, WARNING,                             "
			+ "              ",
		"                                     SEVERE or OFF. If not specified default is INFO                         "
			+ "              ",
		"                                                                                                             "
			+ "              ",
		"   i.e. java -jar ./mockserver.jar -serverPort 1080 -proxyRemotePort 80 -proxyRemoteHost www.mock-server.com "
			+ "-logLevel WARN",
		"                                                                                                             "
			+ "              "
	));
	private static final IntegerStringListParser INTEGER_STRING_LIST_PARSER = new IntegerStringListParser();
	static PrintStream systemErr = System.err;
	static PrintStream systemOut = System.out;
	static boolean usageShown;
	
	/**
	 * Run the MockServer directly providing the arguments as specified below.
	 *
	 * @param arguments the entries are in pairs: - "-serverPort"       followed by the mandatory server local port, -
	 *                  "-proxyRemotePort"  followed by the optional proxyRemotePort port that enabled port forwarding
	 *                  mode, - "-proxyRemoteHost"  followed by the optional proxyRemoteHost port (ignored unless
	 *                  proxyRemotePort is specified) - "-logLevel"         followed by the log level
	 */
	public static void main(final String... arguments)
	{
		try
		{
			final Map<String, String> parsedArgs = parseArguments(arguments);
			final Map<String, String> cmdArgs = new HashMap<>(parsedArgs);
			final Map<String, String> envVarArgs = new HashMap<>();
			final Map<String, String> sysPropArgs = new HashMap<>();
			
			System.getenv().forEach((key, value) -> {
				if(key.startsWith("MOCKSERVER_") && isNotBlank(value))
				{
					envVarArgs.put(key, value);
				}
			});
			System.getProperties().forEach((key, value) -> {
				if(key instanceof final String strKey
					&& value instanceof final String strValue
					&& strKey.startsWith("mockserver")
					&& isNotBlank(strValue))
				{
					sysPropArgs.put((String)key, (String)value);
				}
			});
			
			for(final Arguments parsedArgument : Arrays.asList(serverPort, proxyRemoteHost, proxyRemotePort))
			{
				if(!parsedArgs.containsKey(parsedArgument.name()))
				{
					if(sysPropArgs.containsKey(parsedArgument.systemPropertyName()))
					{
						parsedArgs.put(parsedArgument.name(), sysPropArgs.get(parsedArgument.systemPropertyName()));
						envVarArgs.remove(parsedArgument.longEnvironmentVariableName());
						envVarArgs.remove(parsedArgument.shortEnvironmentVariableName());
					}
					else
					{
						if(envVarArgs.containsKey(parsedArgument.longEnvironmentVariableName()))
						{
							envVarArgs.remove(parsedArgument.shortEnvironmentVariableName());
							parsedArgs.put(
								parsedArgument.name(),
								envVarArgs.get(parsedArgument.longEnvironmentVariableName()));
						}
						else if(isNotBlank(System.getenv(parsedArgument.shortEnvironmentVariableName())))
						{
							if(!(parsedArgument == serverPort
								&& "1080".equals(System.getenv(serverPort.shortEnvironmentVariableName()))
								&& ConfigurationProperties.properties.containsKey(serverPort.systemPropertyName())))
							{
								envVarArgs.put(
									parsedArgument.shortEnvironmentVariableName(),
									System.getenv(parsedArgument.shortEnvironmentVariableName()));
								parsedArgs.put(
									parsedArgument.name(),
									envVarArgs.get(parsedArgument.shortEnvironmentVariableName()));
							}
						}
					}
				}
				else
				{
					sysPropArgs.remove(parsedArgument.systemPropertyName());
					envVarArgs.remove(parsedArgument.longEnvironmentVariableName());
					envVarArgs.remove(parsedArgument.shortEnvironmentVariableName());
				}
				if(!parsedArgs.containsKey(parsedArgument.name())
					&& ConfigurationProperties.properties.containsKey(parsedArgument.systemPropertyName()))
				{
					parsedArgs.put(
						parsedArgument.name(),
						String.valueOf(ConfigurationProperties.properties.get(parsedArgument.systemPropertyName())));
				}
			}
			
			if(LOG.isInfoEnabled())
			{
				LOG.info(
					"Using environment variables: {} and system properties: {} and command line options: {}",
					formatArgsForLog(envVarArgs),
					formatArgsForLog(sysPropArgs),
					formatArgsForLog(cmdArgs));
			}
			
			if(!parsedArgs.isEmpty() && parsedArgs.containsKey(serverPort.name()))
			{
				if(parsedArgs.containsKey(logLevel.name()))
				{
					ServerConfigurationProperties.logLevel(parsedArgs.get(logLevel.name()));
				}
				MockServerLoggerConfiguration.configureLogger();
				final Integer[] localPorts = INTEGER_STRING_LIST_PARSER.toArray(parsedArgs.get(serverPort.name()));
				launchMockServer(parsedArgs, localPorts);
				setPort(localPorts);
			}
			else
			{
				showUsage("\"" + serverPort.name() + "\" not specified");
			}
		}
		catch(final Exception ex)
		{
			LOG.error("Exception while starting", ex);
			showUsage(null);
			if(ServerConfigurationProperties.disableSystemOut())
			{
				new RuntimeException("exception while starting: " + ex.getMessage()).printStackTrace(System.err);
			}
		}
	}
	
	@SuppressWarnings("resource") // Launch
	private static void launchMockServer(final Map<String, String> parsedArgs, final Integer[] localPorts)
	{
		if(parsedArgs.containsKey(proxyRemotePort.name()))
		{
			String remoteHost = parsedArgs.get(proxyRemoteHost.name());
			if(isBlank(remoteHost))
			{
				remoteHost = "localhost";
			}
			new MockServer(Integer.parseInt(parsedArgs.get(proxyRemotePort.name())), remoteHost, localPorts);
		}
		else
		{
			new MockServer(localPorts);
		}
	}
	
	static String formatArgsForLog(final Map<String, String> args)
	{
		return "[\n\t"
			+ args.entrySet()
			.stream()
			.map(e -> e.getKey() + "=" + e.getValue())
			.collect(Collectors.joining(",\n\t"))
			+ "\n]";
	}
	
	private static Map<String, String> parseArguments(final String... arguments)
	{
		final Map<String, String> parsedArguments = new HashMap<>();
		final List<String> errorMessages = new ArrayList<>();
		
		final Iterator<String> argumentsIterator = Arrays.asList(arguments).iterator();
		while(argumentsIterator.hasNext())
		{
			final String next = argumentsIterator.next();
			final String argumentName = substringAfter(next, "-");
			if(argumentsIterator.hasNext())
			{
				final String argumentValue = argumentsIterator.next();
				if(!Arguments.names().containsIgnoreCase(argumentName))
				{
					showUsage("invalid argument \"" + argumentName + "\" found");
					break;
				}
				else
				{
					String errorMessage = "";
					switch(Arguments.valueOf(argumentName))
					{
						case serverPort:
							if(!argumentValue.matches("^\\d+(,\\d+)*$"))
							{
								errorMessage = argumentName + " value \"" + argumentValue
									+ "\" is invalid, please specify a comma separated list of ports i.e. \"1080,1081,"
									+ "1082\"";
							}
							break;
						case proxyRemotePort:
							if(!argumentValue.matches("^\\d+$"))
							{
								errorMessage = argumentName + " value \"" + argumentValue
									+ "\" is invalid, please specify a port i.e. \"1080\"";
							}
							break;
						case proxyRemoteHost:
							final String validIpAddressRegex =
								"^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}"
									+ "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
							final String validHostnameRegex =
								"^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*"
									+ "([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
							if(!(argumentValue.matches(validIpAddressRegex)
								|| argumentValue.matches(validHostnameRegex)))
							{
								errorMessage = argumentName + " value \"" + argumentValue
									+ "\" is invalid, please specify a host name i.e. \"localhost\" or \"127.0.0.1\"";
							}
							break;
						case logLevel:
							if(!Arrays.asList(
								"TRACE",
								"DEBUG",
								"INFO",
								"WARN",
								"ERROR",
								"OFF",
								"FINEST",
								"FINE",
								"INFO",
								"WARNING",
								"SEVERE").contains(argumentValue))
							{
								errorMessage = argumentName + " value \"" + argumentValue
									+ "\" is invalid, please specify one of SL4J levels: \"TRACE\", \"DEBUG\", "
									+ "\"INFO\", \"WARN\", \"ERROR\", \"OFF\" or the Java Logger levels: \"FINEST\", "
									+ "\"FINE\", \"INFO\", \"WARNING\", \"SEVERE\", \"OFF\"";
							}
							break;
						default:
							throw new UnsupportedOperationException();
					}
					if(errorMessage.isEmpty())
					{
						parsedArguments.put(argumentName, argumentValue);
					}
					else
					{
						errorMessages.add(errorMessage);
					}
				}
			}
			else
			{
				break;
			}
		}
		
		if(!errorMessages.isEmpty())
		{
			printValidationError(errorMessages);
			throw new IllegalArgumentException(errorMessages.toString());
		}
		return parsedArguments;
	}
	
	private static void printValidationError(final List<String> errorMessages)
	{
		int maxLengthMessage = 0;
		for(final String errorMessage : errorMessages)
		{
			if(errorMessage.length() > maxLengthMessage)
			{
				maxLengthMessage = errorMessage.length();
			}
		}
		systemOut.println(NEW_LINE + "   " + "=".repeat(maxLengthMessage));
		for(final String errorMessage : errorMessages)
		{
			systemOut.println("   " + errorMessage);
		}
		systemOut.println("   " + "=".repeat(maxLengthMessage) + NEW_LINE);
	}
	
	private static void showUsage(final String errorMessage)
	{
		if(!usageShown)
		{
			usageShown = true;
			systemOut.print(USAGE);
			systemOut.flush();
		}
		if(isNotBlank(errorMessage))
		{
			systemErr.print("\nERROR:  " + errorMessage + "\n\n");
			systemErr.flush();
		}
	}
	
	private Main()
	{
	}
	
	public enum Arguments
	{
		serverPort("SERVER_PORT"),
		proxyRemoteHost("PROXY_REMOTE_HOST"),
		proxyRemotePort("PROXY_REMOTE_PORT"),
		logLevel("LOG_LEVEL");
		
		static final CaseInsensitiveList NAMES = new CaseInsensitiveList();
		
		static
		{
			for(final Arguments arguments : values())
			{
				NAMES.add(arguments.name());
			}
		}
		
		private final String shortEnvironmentVariableName;
		
		Arguments(final String shortEnvironmentVariableName)
		{
			this.shortEnvironmentVariableName = shortEnvironmentVariableName;
		}
		
		public static CaseInsensitiveList names()
		{
			return NAMES;
		}
		
		public String shortEnvironmentVariableName()
		{
			return this.shortEnvironmentVariableName;
		}
		
		public String longEnvironmentVariableName()
		{
			return "MOCKSERVER_" + this.shortEnvironmentVariableName;
		}
		
		public String systemPropertyName()
		{
			return "mockserver." + this.name();
		}
	}
	
	
	public static class CaseInsensitiveList extends ArrayList<String>
	{
		CaseInsensitiveList()
		{
			super();
		}
		
		boolean containsIgnoreCase(final String matcher)
		{
			for(final String listItem : this)
			{
				if(listItem.equalsIgnoreCase(matcher))
				{
					return true;
				}
			}
			return false;
		}
	}
}
