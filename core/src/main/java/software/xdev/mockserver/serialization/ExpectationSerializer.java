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

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.util.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.serialization.model.ExpectationDTO;


public class ExpectationSerializer extends AbstractSerializer<Expectation>
{
	private static final Logger LOG = LoggerFactory.getLogger(ExpectationSerializer.class);
	
	private final JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();
	
	public ExpectationSerializer()
	{
		super();
	}
	
	public ExpectationSerializer(final boolean serialiseDefaultValues)
	{
		super(serialiseDefaultValues
			? ObjectMappers.PRETTY_PRINT_WRITER_THAT_SERIALISES_DEFAULT_FIELDS
			: ObjectMappers.PRETTY_PRINT_WRITER);
	}
	
	@Override
	public String serialize(final Expectation expectation)
	{
		if(expectation == null)
		{
			return "";
		}
		try
		{
			return this.objectWriter.writeValueAsString(new ExpectationDTO(expectation));
		}
		catch(final Exception e)
		{
			throw new IllegalStateException(
				"Exception while serializing expectation to JSON with value " + expectation,
				e);
		}
	}
	
	public String serialize(final List<Expectation> expectations)
	{
		return this.serialize(expectations.toArray(new Expectation[0]));
	}
	
	public String serialize(final Expectation... expectations)
	{
		try
		{
			if(expectations == null || expectations.length == 0)
			{
				return "[]";
			}
			final ExpectationDTO[] expectationDTOs = new ExpectationDTO[expectations.length];
			for(int i = 0; i < expectations.length; i++)
			{
				expectationDTOs[i] = new ExpectationDTO(expectations[i]);
			}
			return this.objectWriter.writeValueAsString(expectationDTOs);
		}
		catch(final Exception e)
		{
			throw new IllegalStateException(
				"Exception while serializing expectation to JSON with value " + Arrays.asList(expectations),
				e);
		}
	}
	
	@Override
	public Expectation deserialize(final String jsonExpectation)
	{
		try
		{
			final ExpectationDTO expectationDTO = this.objectMapper.readValue(jsonExpectation, ExpectationDTO.class);
			if(expectationDTO != null)
			{
				return expectationDTO.buildObject();
			}
		}
		catch(final Exception ex)
		{
			throw new IllegalArgumentException(
				"exception while parsing [" + jsonExpectation + "] for Expectation",
				ex);
		}
		return null;
	}
	
	public Expectation[] deserializeArray(final String jsonExpectations, final boolean allowEmpty)
	{
		return this.deserializeArray(jsonExpectations, allowEmpty, (s, expectation) -> expectation);
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	public Expectation[] deserializeArray(
		final String strJsonExpectations,
		final boolean allowEmpty,
		final BiFunction<String, List<Expectation>, List<Expectation>> expectationModifier)
	{
		if(isBlank(strJsonExpectations))
		{
			throw new IllegalArgumentException(
				"1 error:" + NEW_LINE + " - an expectation or expectation array is required but value was \""
					+ strJsonExpectations + "\"");
		}
		final List<Expectation> expectations = new ArrayList<>();
		final List<String> validationErrors = new ArrayList<>();
		final List<JsonNode> jsonExpectations =
			this.jsonArraySerializer.splitJSONArrayToJSONNodes(strJsonExpectations);
		if(!jsonExpectations.isEmpty())
		{
			for(int i = 0; i < jsonExpectations.size(); i++)
			{
				final String jsonExpectation = JacksonUtils.prettyPrint(jsonExpectations.get(i));
				if(jsonExpectations.size() > 100)
				{
					if(LOG.isDebugEnabled())
					{
						LOG.debug(
							"Processing JSON expectation {} of {}: {}",
							i + 1,
							jsonExpectations.size(),
							jsonExpectation);
					}
					else if(LOG.isInfoEnabled())
					{
						LOG.info("Processing JSON expectation {} of {}", i + 1, jsonExpectations.size());
					}
				}
				try
				{
					expectations.addAll(expectationModifier.apply(
						jsonExpectation,
						Collections.singletonList(this.deserialize(jsonExpectation))));
				}
				catch(final IllegalArgumentException iae)
				{
					validationErrors.add(iae.getMessage());
				}
			}
			if(!validationErrors.isEmpty())
			{
				if(validationErrors.size() > 1)
				{
					throw new IllegalArgumentException(("[" + NEW_LINE
						+ String.join("," + NEW_LINE + NEW_LINE, validationErrors))
						.replaceAll(NEW_LINE, NEW_LINE + "  ")
						+ NEW_LINE + "]");
				}
				throw new IllegalArgumentException(validationErrors.get(0));
			}
		}
		else if(!allowEmpty)
		{
			throw new IllegalArgumentException(
				"1 error:" + NEW_LINE + " - an expectation or array of expectations is required");
		}
		return expectations.toArray(new Expectation[0]);
	}
}
