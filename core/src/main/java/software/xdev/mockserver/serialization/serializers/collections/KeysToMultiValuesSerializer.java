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
package software.xdev.mockserver.serialization.serializers.collections;

import static software.xdev.mockserver.model.NottableString.serialiseNottableString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import software.xdev.mockserver.model.KeyMatchStyle;
import software.xdev.mockserver.model.KeyToMultiValue;
import software.xdev.mockserver.model.KeysToMultiValues;
import software.xdev.mockserver.model.NottableString;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;


public abstract class KeysToMultiValuesSerializer<T extends KeysToMultiValues<? extends KeyToMultiValue, T>>
	extends StdSerializer<T>
{
	KeysToMultiValuesSerializer(final Class<T> valueClass)
	{
		super(valueClass);
	}
	
	@Override
	public void serialize(final T value, final JsonGenerator gen, final SerializationContext provider)
	{
		gen.writeStartObject();
		if(value.getKeyMatchStyle() != null && value.getKeyMatchStyle() != KeyMatchStyle.SUB_SET)
		{
			gen.writePOJOProperty("keyMatchStyle", value.getKeyMatchStyle());
		}
		final ArrayList<NottableString> keys = new ArrayList<>(value.keySet());
		Collections.sort(keys);
		for(final NottableString key : keys)
		{
			gen.writeName(serialiseNottableString(key));
			if(key.getParameterStyle() != null)
			{
				gen.writeStartObject();
				gen.writePOJOProperty("parameterStyle", key.getParameterStyle());
				gen.writeName("values");
				this.writeValuesArray(value, gen, key);
				gen.writeEndObject();
			}
			else
			{
				this.writeValuesArray(value, gen, key);
			}
		}
		gen.writeEndObject();
	}
	
	private void writeValuesArray(final T collection, final JsonGenerator gen, final NottableString key)
	{
		final Collection<NottableString> values = collection.getValues(key);
		gen.writeStartArray(values, values.size());
		for(final NottableString nottableString : values)
		{
			gen.writePOJO(nottableString);
		}
		gen.writeEndArray();
	}
}
