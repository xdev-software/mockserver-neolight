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

import java.util.List;

import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.RequestDefinition;


public class ExpectationToJavaSerializer implements ToJavaSerializer<Expectation>
{
	public static final int INDENT_SIZE = 8;
	
	public String serialize(final List<Expectation> expectations)
	{
		final StringBuilder output = new StringBuilder();
		for(final Expectation expectation : expectations)
		{
			output.append(this.serialize(0, expectation))
				.append(NEW_LINE)
				.append(NEW_LINE);
		}
		return output.toString();
	}
	
	@SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity"})
	@Override
	public String serialize(final int numberOfSpacesToIndent, final Expectation expectation)
	{
		final StringBuilder output = new StringBuilder();
		if(expectation != null)
		{
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(
				"new MockServerClient(\"localhost\", 1080)");
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".when(");
			final RequestDefinition requestDefinition = expectation.getHttpRequest();
			if(requestDefinition instanceof HttpRequest)
			{
				output.append(new HttpRequestToJavaSerializer().serialize(
					numberOfSpacesToIndent + 1,
					(HttpRequest)requestDefinition));
			}
			output.append(',');
			if(expectation.getTimes() != null)
			{
				output.append(new TimesToJavaSerializer().serialize(
					numberOfSpacesToIndent + 1,
					expectation.getTimes()));
			}
			else
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append("null");
			}
			output.append(',');
			if(expectation.getTimeToLive() != null)
			{
				output.append(new TimeToLiveToJavaSerializer().serialize(
					numberOfSpacesToIndent + 1,
					expectation.getTimeToLive()));
			}
			else
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append("null");
			}
			output.append(',');
			this.appendNewLineAndIndent(
				(numberOfSpacesToIndent + 1) * INDENT_SIZE,
				output).append(expectation.getPriority());
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(')');
			if(expectation.getHttpResponse() != null)
			{
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".respond(");
				output.append(new HttpResponseToJavaSerializer().serialize(
					numberOfSpacesToIndent + 1,
					expectation.getHttpResponse()));
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(')');
			}
			if(expectation.getHttpResponseClassCallback() != null)
			{
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".respond(");
				output.append(new HttpClassCallbackToJavaSerializer().serialize(
					numberOfSpacesToIndent + 1,
					expectation.getHttpResponseClassCallback()));
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(')');
			}
			if(expectation.getHttpResponseObjectCallback() != null)
			{
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(
					"/*NOT POSSIBLE TO GENERATE CODE FOR OBJECT CALLBACK*/");
			}
			if(expectation.getHttpForward() != null)
			{
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".forward(");
				output.append(new HttpForwardToJavaSerializer().serialize(
					numberOfSpacesToIndent + 1,
					expectation.getHttpForward()));
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(')');
			}
			if(expectation.getHttpOverrideForwardedRequest() != null)
			{
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".forward(");
				output.append(new HttpOverrideForwardedRequestToJavaSerializer().serialize(
					numberOfSpacesToIndent + 1,
					expectation.getHttpOverrideForwardedRequest()));
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(')');
			}
			if(expectation.getHttpForwardClassCallback() != null)
			{
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".forward(");
				output.append(new HttpClassCallbackToJavaSerializer().serialize(
					numberOfSpacesToIndent + 1,
					expectation.getHttpForwardClassCallback()));
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(')');
			}
			if(expectation.getHttpForwardObjectCallback() != null)
			{
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(
					"/*NOT POSSIBLE TO GENERATE CODE FOR OBJECT CALLBACK*/");
			}
			if(expectation.getHttpError() != null)
			{
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".error(");
				output.append(new HttpErrorToJavaSerializer().serialize(
					numberOfSpacesToIndent + 1,
					expectation.getHttpError()));
				this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(')');
			}
			output.append(';');
		}
		return output.toString();
	}
	
	private StringBuilder appendNewLineAndIndent(final int numberOfSpacesToIndent, final StringBuilder output)
	{
		return output.append(NEW_LINE)
			.append(" ".repeat(numberOfSpacesToIndent));
	}
}
