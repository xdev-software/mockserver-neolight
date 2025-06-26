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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import software.xdev.mockserver.model.KeyMatchStyle;
import software.xdev.mockserver.model.KeyToMultiValue;
import software.xdev.mockserver.model.KeysToMultiValues;
import software.xdev.mockserver.model.NottableString;


public abstract class KeysToMultiValuesSerializer<T extends KeysToMultiValues<? extends KeyToMultiValue, T>>
	extends StdSerializer<T>
{
	KeysToMultiValuesSerializer(final Class<T> valueClass)
	{
		super(valueClass);
	}
	
	@Override
	public void serialize(final T collection, final JsonGenerator jgen, final SerializerProvider provider)
		throws IOException
	{
		jgen.writeStartObject();
		if(collection.getKeyMatchStyle() != null && collection.getKeyMatchStyle() != KeyMatchStyle.SUB_SET)
		{
			jgen.writeObjectField("keyMatchStyle", collection.getKeyMatchStyle());
		}
		final ArrayList<NottableString> keys = new ArrayList<>(collection.keySet());
		Collections.sort(keys);
		for(final NottableString key : keys)
		{
			jgen.writeFieldName(serialiseNottableString(key));
			if(key.getParameterStyle() != null)
			{
				jgen.writeStartObject();
				jgen.writeObjectField("parameterStyle", key.getParameterStyle());
				jgen.writeFieldName("values");
				this.writeValuesArray(collection, jgen, key);
				jgen.writeEndObject();
			}
			else
			{
				this.writeValuesArray(collection, jgen, key);
			}
		}
		jgen.writeEndObject();
	}
	
	private void writeValuesArray(final T collection, final JsonGenerator jgen, final NottableString key)
		throws IOException
	{
		final Collection<NottableString> values = collection.getValues(key);
		jgen.writeStartArray(values, values.size());
		for(final NottableString nottableString : values)
		{
			jgen.writeObject(nottableString);
		}
		jgen.writeEndArray();
	}
}
