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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import software.xdev.mockserver.serialization.model.VerificationTimesDTO;
import software.xdev.mockserver.verify.VerificationTimes;


public class VerificationTimesDTODeserializer extends StdDeserializer<VerificationTimesDTO>
{
	public VerificationTimesDTODeserializer()
	{
		super(VerificationTimesDTO.class);
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	public VerificationTimesDTO deserialize(final JsonParser jsonParser, final DeserializationContext ctxt)
		throws IOException
	{
		VerificationTimesDTO verificationTimesDTO = null;
		
		Integer count = null;
		Boolean exact = null;
		Integer atLeast = null;
		Integer atMost = null;
		
		while(jsonParser.nextToken() != JsonToken.END_OBJECT)
		{
			final String fieldName = jsonParser.currentName();
			if("count".equals(fieldName))
			{
				jsonParser.nextToken();
				count = jsonParser.getIntValue();
			}
			else if("exact".equals(fieldName))
			{
				jsonParser.nextToken();
				exact = jsonParser.getBooleanValue();
			}
			else if("atLeast".equals(fieldName))
			{
				jsonParser.nextToken();
				atLeast = jsonParser.getIntValue();
			}
			else if("atMost".equals(fieldName))
			{
				jsonParser.nextToken();
				atMost = jsonParser.getIntValue();
			}
			
			if(atLeast != null || atMost != null)
			{
				verificationTimesDTO = new VerificationTimesDTO(VerificationTimes.between(
					atLeast != null ? atLeast : -1,
					atMost != null ? atMost : -1));
			}
			else if(count != null)
			{
				if(exact != null && exact)
				{
					verificationTimesDTO = new VerificationTimesDTO(VerificationTimes.exactly(count));
				}
				else
				{
					verificationTimesDTO = new VerificationTimesDTO(VerificationTimes.atLeast(count));
				}
			}
		}
		
		return verificationTimesDTO;
	}
}
