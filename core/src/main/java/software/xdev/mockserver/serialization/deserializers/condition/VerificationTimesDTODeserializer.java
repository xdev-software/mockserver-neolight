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

import software.xdev.mockserver.serialization.model.VerificationTimesDTO;
import software.xdev.mockserver.verify.VerificationTimes;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;


public class VerificationTimesDTODeserializer extends StdDeserializer<VerificationTimesDTO>
{
	public VerificationTimesDTODeserializer()
	{
		super(VerificationTimesDTO.class);
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	public VerificationTimesDTO deserialize(final JsonParser p, final DeserializationContext ctxt)
	{
		VerificationTimesDTO verificationTimesDTO = null;
		
		Integer count = null;
		Boolean exact = null;
		Integer atLeast = null;
		Integer atMost = null;
		
		while(p.nextToken() != JsonToken.END_OBJECT)
		{
			final String fieldName = p.currentName();
			if("count".equals(fieldName))
			{
				p.nextToken();
				count = p.getIntValue();
			}
			else if("exact".equals(fieldName))
			{
				p.nextToken();
				exact = p.getBooleanValue();
			}
			else if("atLeast".equals(fieldName))
			{
				p.nextToken();
				atLeast = p.getIntValue();
			}
			else if("atMost".equals(fieldName))
			{
				p.nextToken();
				atMost = p.getIntValue();
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
