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
package software.xdev.mockserver.serialization;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;


/**
 * Emulation of
 * https://github.com/fge/jackson-coreutils/blob/master/src/main/java/com/github/fge/jackson/JacksonUtils.java
 */
public final class JacksonUtils
{
	private static final ObjectWriter WRITER = new ObjectMapper()
		.setNodeFactory(JsonNodeFactory.instance)
		.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
		.enable(SerializationFeature.INDENT_OUTPUT)
		.writer()
		.with(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
	
	public static String prettyPrint(final JsonNode node)
	{
		final StringWriter writer = new StringWriter();
		
		try {
			WRITER.writeValue(writer, node);
			writer.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		
		return writer.toString();
	}
	
	private JacksonUtils()
	{
	}
}
