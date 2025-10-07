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
package software.xdev.mockserver.formatting;

import static software.xdev.mockserver.character.Character.NEW_LINE;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@SuppressWarnings("PMD.AvoidStringBuilderOrBuffer") // Beyond saving
public final class StringFormatter
{
	private static final Map<Integer, String> INDENTS = IntStream.rangeClosed(0, 4)
		.boxed()
		.collect(Collectors.toMap(Function.identity(), i -> " ".repeat(i * 2)));
	
	private static final Pattern PATTERN_MULTI_LINE_START = Pattern.compile("(?m)^");
	private static final Pattern PATTERN_MESSAGE_PARTS = Pattern.compile("\\{}");
	
	public static StringBuilder[] indentAndToString(final Object... objects)
	{
		return indentAndToString(1, objects);
	}
	
	public static StringBuilder[] indentAndToString(final int indent, final Object... objects)
	{
		final StringBuilder[] indentedObjects = new StringBuilder[objects.length];
		for(int i = 0; i < objects.length; i++)
		{
			indentedObjects[i] =
				new StringBuilder(NEW_LINE)
					.append(NEW_LINE)
					.append(PATTERN_MULTI_LINE_START
						.matcher(String.valueOf(objects[i]))
						.replaceAll(INDENTS.get(indent)))
					.append(NEW_LINE);
		}
		return indentedObjects;
	}
	
	public static String formatLogMessage(final int indent, final String message, final Object... arguments)
	{
		final StringBuilder logMessage = new StringBuilder(16 + message.length() + arguments.length * 64);
		final StringBuilder[] formattedArguments = indentAndToString(indent + 1, arguments);
		final String[] messageParts = PATTERN_MESSAGE_PARTS.split(message);
		for(int messagePartIndex = 0; messagePartIndex < messageParts.length; messagePartIndex++)
		{
			logMessage.append(INDENTS.get(indent)).append(messageParts[messagePartIndex]);
			if(formattedArguments.length > 0
				&& formattedArguments.length > messagePartIndex)
			{
				logMessage.append(formattedArguments[messagePartIndex]);
			}
			if(messagePartIndex < messageParts.length - 1)
			{
				logMessage.append(NEW_LINE);
				if(!messageParts[messagePartIndex + 1].startsWith(" "))
				{
					logMessage.append(' ');
				}
			}
		}
		return logMessage.toString();
	}
	
	public static String formatLogMessage(final String[] messageParts, final Object... arguments)
	{
		final StringBuilder logMessage = new StringBuilder();
		final StringBuilder[] formattedArguments = indentAndToString(arguments);
		for(int messagePartIndex = 0; messagePartIndex < messageParts.length; messagePartIndex++)
		{
			logMessage.append(messageParts[messagePartIndex]);
			if(formattedArguments.length > 0
				&& formattedArguments.length > messagePartIndex)
			{
				logMessage.append(formattedArguments[messagePartIndex]);
			}
		}
		return logMessage.toString();
	}
	
	private StringFormatter()
	{
	}
}
