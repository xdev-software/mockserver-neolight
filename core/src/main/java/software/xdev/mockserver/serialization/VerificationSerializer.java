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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import software.xdev.mockserver.serialization.model.VerificationDTO;
import software.xdev.mockserver.verify.Verification;


public class VerificationSerializer implements Serializer<Verification>
{
	private final ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
	private final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
	
	@Override
	public String serialize(final Verification verification)
	{
		try
		{
			return this.objectWriter.writeValueAsString(new VerificationDTO(verification));
		}
		catch(final Exception e)
		{
			throw new IllegalStateException(
				"Exception while serializing verification to JSON with value " + verification,
				e);
		}
	}
	
	@Override
	public Verification deserialize(final String jsonVerification)
	{
		try
		{
			final VerificationDTO verificationDTO =
				this.objectMapper.readValue(jsonVerification, VerificationDTO.class);
			if(verificationDTO != null)
			{
				return verificationDTO.buildObject();
			}
		}
		catch(final Exception ex)
		{
			throw new IllegalArgumentException(
				"exception while parsing [" + jsonVerification + "] for Verification",
				ex);
		}
		return null;
	}
	
	@Override
	public Class<Verification> supportsType()
	{
		return Verification.class;
	}
}
