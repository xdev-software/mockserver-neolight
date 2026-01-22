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

import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;


public abstract class AbstractSerializer<T> implements Serializer<T>
{
	protected final ObjectWriter objectWriter;
	protected final JsonMapper objectMapper;
	
	protected AbstractSerializer()
	{
		this(ObjectMappers.PRETTY_PRINT_WRITER);
	}
	
	protected AbstractSerializer(final ObjectWriter objectWriter)
	{
		this(objectWriter, ObjectMappers.DEFAULT_MAPPER);
	}
	
	protected AbstractSerializer(final ObjectWriter objectWriter, final JsonMapper objectMapper)
	{
		this.objectWriter = objectWriter;
		this.objectMapper = objectMapper;
	}
}
