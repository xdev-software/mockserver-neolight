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
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.ObjectWithJsonToString;
import software.xdev.mockserver.model.Parameter;
import software.xdev.mockserver.model.ParameterBody;
import software.xdev.mockserver.model.RegexBody;
import software.xdev.mockserver.model.StringBody;
import software.xdev.mockserver.serialization.Base64Converter;


public class HttpRequestToJavaSerializer implements ToJavaSerializer<HttpRequest>
{
	public String serialize(final List<HttpRequest> httpRequests)
	{
		final StringBuilder output = new StringBuilder(50);
		for(final HttpRequest httpRequest : httpRequests)
		{
			output.append(this.serialize(0, httpRequest))
				.append(';')
				.append(NEW_LINE);
		}
		return output.toString();
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	public String serialize(final int numberOfSpacesToIndent, final HttpRequest request)
	{
		final StringBuilder output = new StringBuilder(50);
		if(request != null)
		{
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output);
			output.append("request()");
			if(isNotBlank(request.getMethod().getValue()))
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
				output.append(".withMethod(\"").append(request.getMethod().getValue()).append("\")");
			}
			if(isNotBlank(request.getPath().getValue()))
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
				output.append(".withPath(\"").append(request.getPath().getValue()).append("\")");
			}
			this.outputHeaders(numberOfSpacesToIndent + 1, output, request.getHeaderList());
			this.outputCookies(numberOfSpacesToIndent + 1, output, request.getCookieList());
			this.outputQueryStringParameter(numberOfSpacesToIndent + 1, output, request.getQueryStringParameterList());
			if(request.isKeepAlive() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
				output.append(".withKeepAlive(").append(request.isKeepAlive().toString()).append(')');
			}
			if(request.getProtocol() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
				output.append(".withProtocol(Protocol.").append(request.getProtocol().toString()).append(')');
			}
			if(request.getSocketAddress() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
				output.append(".withSocketAddress(")
					.append(new SocketAddressToJavaSerializer().serialize(
						numberOfSpacesToIndent + 2,
						request.getSocketAddress()));
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
				output.append(')');
			}
			if(request.getBody() != null)
			{
				if(request.getBody() instanceof final RegexBody regexBody)
				{
					this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
					output.append(".withBody(new RegexBody(\"")
						.append(StringEscapeUtils.escapeJava(regexBody.getValue()))
						.append("\"))");
				}
				else if(request.getBody() instanceof final StringBody stringBody)
				{
					this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
					output.append(".withBody(new StringBody(\"")
						.append(StringEscapeUtils.escapeJava(stringBody.getValue()))
						.append("\"))");
				}
				else if(request.getBody() instanceof ParameterBody)
				{
					this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
					output.append(".withBody(");
					this.appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output);
					output.append("new ParameterBody(");
					final List<Parameter> bodyParameters = ((ParameterBody)request.getBody()).getValue().getEntries();
					output.append(new ParameterToJavaSerializer().serializeAsJava(
						numberOfSpacesToIndent + 3,
						bodyParameters));
					this.appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output);
					output.append(')');
					this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
					output.append(')');
				}
				else if(request.getBody() instanceof final BinaryBody body)
				{
					this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
					output.append(".withBody(Base64Converter.base64StringToBytes(\"")
						.append(Base64Converter.bytesToBase64String(body.getRawBytes()))
						.append("\"))");
				}
			}
		}
		
		return output.toString();
	}
	
	private void outputQueryStringParameter(
		final int numberOfSpacesToIndent,
		final StringBuilder output,
		final List<Parameter> parameters)
	{
		if(!parameters.isEmpty())
		{
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output)
				.append(".withQueryStringParameters(");
			this.appendObject(numberOfSpacesToIndent, output, new ParameterToJavaSerializer(), parameters);
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(')');
		}
	}
	
	private void outputCookies(
		final int numberOfSpacesToIndent, final StringBuilder output,
		final List<Cookie> cookies)
	{
		if(!cookies.isEmpty())
		{
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".withCookies(");
			this.appendObject(numberOfSpacesToIndent, output, new CookieToJavaSerializer(), cookies);
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
			this.appendObject(numberOfSpacesToIndent, output, new HeaderToJavaSerializer(), headers);
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(')');
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
