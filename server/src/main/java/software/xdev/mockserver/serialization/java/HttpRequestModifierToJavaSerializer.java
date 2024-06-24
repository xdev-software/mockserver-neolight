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
package software.xdev.mockserver.serialization.java;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

import java.util.List;
import java.util.stream.Collectors;

import software.xdev.mockserver.model.Cookies;
import software.xdev.mockserver.model.Headers;
import software.xdev.mockserver.model.HttpRequestModifier;
import software.xdev.mockserver.model.ObjectWithJsonToString;
import software.xdev.mockserver.model.Parameters;


public class HttpRequestModifierToJavaSerializer implements ToJavaSerializer<HttpRequestModifier>
{
	public String serialize(final List<HttpRequestModifier> httpRequestModifiers)
	{
		final StringBuilder output = new StringBuilder(50);
		for(final HttpRequestModifier httpRequestModifier : httpRequestModifiers)
		{
			output.append(this.serialize(0, httpRequestModifier))
				.append(';')
				.append(NEW_LINE);
		}
		return output.toString();
	}
	
	@Override
	public String serialize(final int numberOfSpacesToIndent, final HttpRequestModifier request)
	{
		final StringBuilder output = new StringBuilder(20);
		if(request != null)
		{
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output);
			output.append("requestModifier()");
			if(request.getPath() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
				output.append(".withPath(\"")
					.append(request.getPath().getRegex())
					.append("\",\"")
					.append(request.getPath().getSubstitution())
					.append("\")");
			}
			if(request.getQueryStringParameters() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(
					".withQueryStringParameters(");
				this.outputQueryStringParameters(
					numberOfSpacesToIndent,
					output,
					request.getQueryStringParameters().getAdd());
				this.outputQueryStringParameters(
					numberOfSpacesToIndent,
					output,
					request.getQueryStringParameters().getReplace());
				this.outputList(numberOfSpacesToIndent, output, request.getQueryStringParameters().getRemove());
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(')');
			}
			if(request.getHeaders() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withHeaders"
					+ "(");
				this.outputHeaders(numberOfSpacesToIndent, output, request.getHeaders().getAdd());
				this.outputHeaders(numberOfSpacesToIndent, output, request.getHeaders().getReplace());
				this.outputList(numberOfSpacesToIndent, output, request.getHeaders().getRemove());
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(')');
			}
			if(request.getCookies() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withCookies"
					+ "(");
				this.outputCookies(numberOfSpacesToIndent, output, request.getCookies().getAdd());
				this.outputCookies(numberOfSpacesToIndent, output, request.getCookies().getReplace());
				this.outputList(numberOfSpacesToIndent, output, request.getCookies().getRemove());
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(')');
			}
		}
		
		return output.toString();
	}
	
	private void outputQueryStringParameters(
		final int numberOfSpacesToIndent,
		final StringBuilder output,
		final Parameters parameters)
	{
		if(parameters != null && !parameters.isEmpty())
		{
			this.appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("parameters(");
			this.appendObject(
				(numberOfSpacesToIndent + 2),
				output,
				new ParameterToJavaSerializer(),
				parameters.getEntries());
			this.appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("),");
		}
		else
		{
			this.appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("null,");
		}
	}
	
	private void outputHeaders(final int numberOfSpacesToIndent, final StringBuilder output, final Headers headers)
	{
		if(headers != null && !headers.isEmpty())
		{
			this.appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("headers(");
			this.appendObject((numberOfSpacesToIndent + 2), output, new HeaderToJavaSerializer(),
				headers.getEntries());
			this.appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("),");
		}
		else
		{
			this.appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("null,");
		}
	}
	
	private void outputCookies(final int numberOfSpacesToIndent, final StringBuilder output, final Cookies cookies)
	{
		if(cookies != null && !cookies.isEmpty())
		{
			this.appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("cookies(");
			this.appendObject((numberOfSpacesToIndent + 2), output, new CookieToJavaSerializer(),
				cookies.getEntries());
			this.appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("),");
		}
		else
		{
			this.appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("null,");
		}
	}
	
	private void outputList(final int numberOfSpacesToIndent, final StringBuilder output, final List<String> add)
	{
		if(add != null && !add.isEmpty())
		{
			this.appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output)
				.append("List.of(")
				.append(add.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")))
				.append(')');
		}
		else
		{
			this.appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("null");
		}
	}
	
	private <T extends ObjectWithJsonToString> void appendObject(
		final int numberOfSpacesToIndent,
		final StringBuilder output,
		final MultiValueToJavaSerializer<T> toJavaSerializer,
		final List<T> objects)
	{
		output.append(toJavaSerializer.serializeAsJava(numberOfSpacesToIndent + 1, objects));
	}
	
	private StringBuilder appendNewLineAndIndent(final int numberOfSpacesToIndent, final StringBuilder output)
	{
		return output.append(NEW_LINE).append(" ".repeat(numberOfSpacesToIndent));
	}
}
