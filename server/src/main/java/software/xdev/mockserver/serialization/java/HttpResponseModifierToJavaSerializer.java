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
import software.xdev.mockserver.model.HttpResponseModifier;
import software.xdev.mockserver.model.ObjectWithJsonToString;


public class HttpResponseModifierToJavaSerializer implements ToJavaSerializer<HttpResponseModifier>
{
	public String serialize(final List<HttpResponseModifier> httpResponseModifiers)
	{
		final StringBuilder output = new StringBuilder();
		for(final HttpResponseModifier httpResponseModifier : httpResponseModifiers)
		{
			output.append(this.serialize(0, httpResponseModifier))
				.append(';')
				.append(NEW_LINE);
		}
		return output.toString();
	}
	
	@Override
	public String serialize(final int numberOfSpacesToIndent, final HttpResponseModifier response)
	{
		final StringBuilder output = new StringBuilder(18);
		if(response != null)
		{
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output);
			output.append("responseModifier()");
			if(response.getHeaders() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withHeaders"
					+ "(");
				this.outputHeaders(numberOfSpacesToIndent, output, response.getHeaders().getAdd());
				this.outputHeaders(numberOfSpacesToIndent, output, response.getHeaders().getReplace());
				this.outputList(numberOfSpacesToIndent, output, response.getHeaders().getRemove());
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(')');
			}
			if(response.getCookies() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withCookies"
					+ "(");
				this.outputCookies(numberOfSpacesToIndent, output, response.getCookies().getAdd());
				this.outputCookies(numberOfSpacesToIndent, output, response.getCookies().getReplace());
				this.outputList(numberOfSpacesToIndent, output, response.getCookies().getRemove());
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(')');
			}
		}
		
		return output.toString();
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
