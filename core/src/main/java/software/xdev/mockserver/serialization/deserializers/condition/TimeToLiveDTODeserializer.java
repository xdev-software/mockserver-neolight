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
package software.xdev.mockserver.serialization.deserializers.condition;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import software.xdev.mockserver.matchers.TimeToLive;
import software.xdev.mockserver.serialization.model.TimeToLiveDTO;


public class TimeToLiveDTODeserializer extends StdDeserializer<TimeToLiveDTO>
{
	private static final Logger LOG = LoggerFactory.getLogger(TimeToLiveDTODeserializer.class);
	
	public TimeToLiveDTODeserializer()
	{
		super(TimeToLiveDTO.class);
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	public TimeToLiveDTO deserialize(final JsonParser jsonParser, final DeserializationContext ctxt) throws IOException
	{
		TimeToLiveDTO timeToLiveDTO = null;
		TimeToLive timeToLive = null;
		final TimeUnit timeUnit;
		long ttl = 0L;
		final long endDate;
		boolean unlimited = false;
		
		final JsonNode timeToLiveDTONode = jsonParser.getCodec().readTree(jsonParser);
		final JsonNode unlimitedNode = timeToLiveDTONode.get("unlimited");
		if(unlimitedNode != null)
		{
			unlimited = unlimitedNode.asBoolean();
		}
		if(!unlimited)
		{
			final JsonNode timeToLiveNode = timeToLiveDTONode.get("timeToLive");
			if(timeToLiveNode != null)
			{
				ttl = timeToLiveNode.asLong();
			}
			final JsonNode timeUnitNode = timeToLiveDTONode.get("timeUnit");
			if(timeUnitNode != null)
			{
				try
				{
					timeUnit = Enum.valueOf(TimeUnit.class, timeUnitNode.asText());
					timeToLive = TimeToLive.exactly(timeUnit, ttl);
				}
				catch(final IllegalArgumentException iae)
				{
					if(LOG.isTraceEnabled())
					{
						LOG.trace("Exception parsing TimeToLiveDTO timeUnit", iae);
					}
				}
			}
			if(timeToLive != null)
			{
				final JsonNode endDateNode = timeToLiveDTONode.get("endDate");
				if(endDateNode != null)
				{
					endDate = endDateNode.asLong();
					timeToLive.setEndDate(endDate);
				}
				timeToLiveDTO = new TimeToLiveDTO(timeToLive);
			}
		}
		else
		{
			timeToLiveDTO = new TimeToLiveDTO(TimeToLive.unlimited());
		}
		
		return timeToLiveDTO;
	}
}
