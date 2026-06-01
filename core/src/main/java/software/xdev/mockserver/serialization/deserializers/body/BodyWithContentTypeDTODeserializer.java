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
package software.xdev.mockserver.serialization.deserializers.body;

import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.mockserver.model.BinaryBody;
import software.xdev.mockserver.model.Body;
import software.xdev.mockserver.model.MediaType;
import software.xdev.mockserver.model.StringBody;
import software.xdev.mockserver.serialization.model.BinaryBodyDTO;
import software.xdev.mockserver.serialization.model.BodyWithContentTypeDTO;
import software.xdev.mockserver.serialization.model.StringBodyDTO;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;


public class BodyWithContentTypeDTODeserializer extends StdDeserializer<BodyWithContentTypeDTO>
{
	private static final Logger LOG = LoggerFactory.getLogger(BodyWithContentTypeDTODeserializer.class);
	
	private static final Map<String, Body.Type> FIELD_NAME_TO_TYPE = new HashMap<>(Map.ofEntries(
		// NOTE: Key is always lowercase
		Map.entry("base64bytes", Body.Type.BINARY),
		Map.entry("string", Body.Type.STRING)
	));
	private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
	
	public BodyWithContentTypeDTODeserializer()
	{
		super(BodyWithContentTypeDTO.class);
	}
	
	@SuppressWarnings({
		"checkstyle:MethodLength",
		"PMD.CognitiveComplexity",
		"PMD.NPathComplexity",
		"PMD.CyclomaticComplexity",
		"PMD.AvoidDeeplyNestedIfStmts"})
	@Override
	public BodyWithContentTypeDTO deserialize(final JsonParser p, final DeserializationContext ctxt)
	{
		BodyWithContentTypeDTO result = null;
		final JsonToken currentToken = p.currentToken();
		String valueJsonValue = "";
		byte[] rawBytes = null;
		Body.Type type = null;
		Boolean not = null;
		Boolean optional = null;
		MediaType contentType = null;
		Charset charset = null;
		if(currentToken == JsonToken.START_OBJECT)
		{
			@SuppressWarnings("unchecked")
			final Map<Object, Object> body = (Map<Object, Object>)ctxt.readValue(p, Map.class);
			for(final Map.Entry<Object, Object> entry : body.entrySet())
			{
				if(entry.getKey() instanceof final String key)
				{
					if("type".equalsIgnoreCase(key))
					{
						try
						{
							type = Body.Type.valueOf(String.valueOf(entry.getValue()));
						}
						catch(final IllegalArgumentException iae)
						{
							if(LOG.isTraceEnabled())
							{
								LOG.trace("Ignoring invalid value for \"type\" field of \"{}\"",
									entry.getValue(), iae);
							}
						}
					}
					if(this.containsIgnoreCase(key, "string", "regex", "base64Bytes") && type != Body.Type.PARAMETERS)
					{
						final String fieldName = String.valueOf(entry.getKey()).toLowerCase();
						if(FIELD_NAME_TO_TYPE.containsKey(fieldName))
						{
							type = FIELD_NAME_TO_TYPE.get(fieldName);
						}
						valueJsonValue = String.valueOf(entry.getValue());
					}
					if(this.containsIgnoreCase(key, "rawBytes", "base64Bytes")
						&& entry.getValue() instanceof final String s)
					{
						try
						{
							rawBytes = BASE64_DECODER.decode(s);
						}
						catch(final Exception ex)
						{
							if(LOG.isDebugEnabled())
							{
								LOG.debug("Invalid base64 encoded rawBytes with value \"{}\"",
									entry.getValue(), ex);
							}
						}
					}
					
					if("not".equalsIgnoreCase(key))
					{
						not = Boolean.parseBoolean(String.valueOf(entry.getValue()));
					}
					if("optional".equalsIgnoreCase(key))
					{
						optional = Boolean.parseBoolean(String.valueOf(entry.getValue()));
					}
					if("contentType".equalsIgnoreCase(key))
					{
						try
						{
							final String mediaTypeHeader = String.valueOf(entry.getValue());
							if(isNotBlank(mediaTypeHeader))
							{
								final MediaType parsedMediaTypeHeader = MediaType.parse(mediaTypeHeader);
								if(isNotBlank(parsedMediaTypeHeader.toString()))
								{
									contentType = parsedMediaTypeHeader;
								}
							}
						}
						catch(final IllegalArgumentException uce)
						{
							if(LOG.isDebugEnabled())
							{
								LOG.debug("Ignoring unsupported MediaType with value \"{}\"",
									entry.getValue(), uce);
							}
						}
					}
					if("charset".equalsIgnoreCase(key))
					{
						try
						{
							charset = Charset.forName(String.valueOf(entry.getValue()));
						}
						catch(final UnsupportedCharsetException uce)
						{
							if(LOG.isDebugEnabled())
							{
								LOG.debug("Ignoring unsupported Charset with value \"{}\"",
									entry.getValue(), uce);
							}
						}
						catch(final IllegalCharsetNameException icne)
						{
							if(LOG.isDebugEnabled())
							{
								LOG.debug("Ignoring invalid Charset with value \"{}\"",
									entry.getValue(), icne);
							}
						}
					}
				}
			}
			if(type != null)
			{
				switch(type)
				{
					case BINARY:
						if(contentType != null && isNotBlank(contentType.toString()))
						{
							result = new BinaryBodyDTO(new BinaryBody(rawBytes, contentType), not);
							break;
						}
						else
						{
							result = new BinaryBodyDTO(new BinaryBody(rawBytes), not);
							break;
						}
					case STRING:
						if(contentType != null && isNotBlank(contentType.toString()))
						{
							result =
								new StringBodyDTO(new StringBody(valueJsonValue, rawBytes, false, contentType), not);
							break;
						}
						else if(charset != null)
						{
							result = new StringBodyDTO(new StringBody(
								valueJsonValue,
								rawBytes,
								false,
								StringBody.DEFAULT_CONTENT_TYPE.withCharset(charset)), not);
							break;
						}
						else
						{
							result = new StringBodyDTO(new StringBody(valueJsonValue, rawBytes, false, null), not);
							break;
						}
					default:
						throw new UnsupportedOperationException();
				}
			}
		}
		else if(currentToken == JsonToken.VALUE_STRING)
		{
			result = new StringBodyDTO(new StringBody(p.getString()));
		}
		if(result != null)
		{
			result.withOptional(optional);
		}
		return result;
	}
	
	private boolean containsIgnoreCase(final String valueToMatch, final String... listOfValues)
	{
		for(final String item : listOfValues)
		{
			if(item.equalsIgnoreCase(valueToMatch))
			{
				return true;
			}
		}
		return false;
	}
}
