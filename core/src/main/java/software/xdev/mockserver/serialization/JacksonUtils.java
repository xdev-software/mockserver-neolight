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

import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;


/**
 * Emulation of
 * https://github.com/fge/jackson-coreutils/blob/master/src/main/java/com/github/fge/jackson/JacksonUtils.java
 */
public final class JacksonUtils
{
	private static final ObjectWriter WRITER = JsonMapper.builder(new JsonFactory())
		.nodeFactory(JsonNodeFactory.instance)
		.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
		.enable(SerializationFeature.INDENT_OUTPUT)
		.build()
		.writer()
		.with(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN);
	
	public static String prettyPrint(final JsonNode node)
	{
		return WRITER.writeValueAsString(node);
	}
	
	private JacksonUtils()
	{
	}
}
