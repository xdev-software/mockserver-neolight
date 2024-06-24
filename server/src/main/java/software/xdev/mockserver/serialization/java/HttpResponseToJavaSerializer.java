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
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

import software.xdev.mockserver.model.BinaryBody;
import software.xdev.mockserver.model.Cookie;
import software.xdev.mockserver.model.Header;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.ObjectWithJsonToString;
import software.xdev.mockserver.serialization.Base64Converter;


public class HttpResponseToJavaSerializer implements ToJavaSerializer<HttpResponse>
{
	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	public String serialize(final int numberOfSpacesToIndent, final HttpResponse httpResponse)
	{
		final StringBuilder output = new StringBuilder(50);
		if(httpResponse != null)
		{
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output)
				.append("response()");
			if(httpResponse.getStatusCode() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
					.append(".withStatusCode(").append(httpResponse.getStatusCode()).append(')');
			}
			if(httpResponse.getReasonPhrase() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
					.append(".withReasonPhrase(\"")
					.append(StringEscapeUtils.escapeJava(httpResponse.getReasonPhrase()))
					.append("\")");
			}
			this.outputHeaders(numberOfSpacesToIndent + 1, output, httpResponse.getHeaderList());
			this.outputCookies(numberOfSpacesToIndent + 1, output, httpResponse.getCookieList());
			if(isNotBlank(httpResponse.getBodyAsString()))
			{
				if(httpResponse.getBody() instanceof final BinaryBody body)
				{
					this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
					output.append(".withBody(Base64Converter.base64StringToBytes(\"")
						.append(Base64Converter.bytesToBase64String(body.getRawBytes()))
						.append("\"))");
				}
				else
				{
					this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
						.append(".withBody(\"")
						.append(StringEscapeUtils.escapeJava(httpResponse.getBodyAsString()))
						.append("\")");
				}
			}
			if(httpResponse.getDelay() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
					.append(".withDelay(")
					.append(new DelayToJavaSerializer().serialize(0, httpResponse.getDelay())).append(')');
			}
			if(httpResponse.getConnectionOptions() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
					.append(".withConnectionOptions(");
				output.append(new ConnectionOptionsToJavaSerializer().serialize(
					numberOfSpacesToIndent + 2,
					httpResponse.getConnectionOptions()));
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
					.append(')');
			}
		}
		
		return output.toString();
	}
	
	private void outputCookies(
		final int numberOfSpacesToIndent, final StringBuilder output,
		final List<Cookie> cookies)
	{
		if(!cookies.isEmpty())
		{
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".withCookies(");
			this.appendObject(numberOfSpacesToIndent + 1, output, new CookieToJavaSerializer(), cookies);
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(')');
		}
	}
	
	private void outputHeaders(
		final int numberOfSpacesToIndent, final StringBuilder output,
		final List<Header> headers)
	{
		if(!headers.isEmpty())
		{
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".withHeaders(");
			this.appendObject(numberOfSpacesToIndent + 1, output, new HeaderToJavaSerializer(), headers);
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(')');
		}
	}
	
	private <T extends ObjectWithJsonToString> void appendObject(
		final int numberOfSpacesToIndent,
		final StringBuilder output,
		final MultiValueToJavaSerializer<T> toJavaSerializer,
		final List<T> objects)
	{
		output.append(toJavaSerializer.serializeAsJava(numberOfSpacesToIndent, objects));
	}
	
	private StringBuilder appendNewLineAndIndent(final int numberOfSpacesToIndent, final StringBuilder output)
	{
		return output.append(NEW_LINE).append(" ".repeat(numberOfSpacesToIndent));
	}
}
