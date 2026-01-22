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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import software.xdev.mockserver.serialization.deserializers.body.BodyDTODeserializer;
import software.xdev.mockserver.serialization.deserializers.body.BodyWithContentTypeDTODeserializer;
import software.xdev.mockserver.serialization.deserializers.collections.CookiesDeserializer;
import software.xdev.mockserver.serialization.deserializers.collections.HeadersDeserializer;
import software.xdev.mockserver.serialization.deserializers.collections.ParametersDeserializer;
import software.xdev.mockserver.serialization.deserializers.condition.TimeToLiveDTODeserializer;
import software.xdev.mockserver.serialization.deserializers.condition.VerificationTimesDTODeserializer;
import software.xdev.mockserver.serialization.deserializers.request.RequestDefinitionDTODeserializer;
import software.xdev.mockserver.serialization.deserializers.string.NottableStringDeserializer;
import software.xdev.mockserver.serialization.serializers.body.BinaryBodyDTOSerializer;
import software.xdev.mockserver.serialization.serializers.body.BinaryBodySerializer;
import software.xdev.mockserver.serialization.serializers.body.ParameterBodyDTOSerializer;
import software.xdev.mockserver.serialization.serializers.body.ParameterBodySerializer;
import software.xdev.mockserver.serialization.serializers.body.RegexBodyDTOSerializer;
import software.xdev.mockserver.serialization.serializers.body.RegexBodySerializer;
import software.xdev.mockserver.serialization.serializers.body.StringBodyDTOSerializer;
import software.xdev.mockserver.serialization.serializers.body.StringBodySerializer;
import software.xdev.mockserver.serialization.serializers.collections.CookiesSerializer;
import software.xdev.mockserver.serialization.serializers.collections.HeadersSerializer;
import software.xdev.mockserver.serialization.serializers.collections.ParametersSerializer;
import software.xdev.mockserver.serialization.serializers.condition.VerificationTimesDTOSerializer;
import software.xdev.mockserver.serialization.serializers.condition.VerificationTimesSerializer;
import software.xdev.mockserver.serialization.serializers.request.HttpRequestDTOSerializer;
import software.xdev.mockserver.serialization.serializers.response.HttpResponseDTOSerializer;
import software.xdev.mockserver.serialization.serializers.response.HttpResponseSerializer;
import software.xdev.mockserver.serialization.serializers.response.TimeToLiveDTOSerializer;
import software.xdev.mockserver.serialization.serializers.response.TimeToLiveSerializer;
import software.xdev.mockserver.serialization.serializers.response.TimesDTOSerializer;
import software.xdev.mockserver.serialization.serializers.response.TimesSerializer;
import software.xdev.mockserver.serialization.serializers.string.NottableStringSerializer;


@SuppressWarnings({"unchecked", "rawtypes"})
public final class ObjectMappers
{
	public static final ObjectWriter BASE_WRITER_PRETTY =
		buildObjectMapperWithoutRemovingEmptyValues().writerWithDefaultPrettyPrinter();
	
	public static final ObjectMapper DEFAULT_MAPPER =
		buildObjectMapperWithDeserializerAndSerializers(Collections.emptyList(), Collections.emptyList(), false);
	public static final ObjectWriter DEFAULT_WRITER_PRETTY = DEFAULT_MAPPER.writerWithDefaultPrettyPrinter();
	
	public static final ObjectWriter PRETTY_PRINT_WRITER =
		buildObjectMapperWithDeserializerAndSerializers(
			Collections.emptyList(),
			Collections.emptyList(),
			false).writerWithDefaultPrettyPrinter();
	public static final ObjectWriter PRETTY_PRINT_WRITER_THAT_SERIALISES_DEFAULT_FIELDS =
		buildObjectMapperWithDeserializerAndSerializers(
			Collections.emptyList(),
			Collections.emptyList(),
			true).writerWithDefaultPrettyPrinter();
	
	public static ObjectMapper createObjectMapper(final JsonDeserializer... replacementJsonDeserializers)
	{
		if(replacementJsonDeserializers == null || replacementJsonDeserializers.length == 0)
		{
			return DEFAULT_MAPPER;
		}
		
		return buildObjectMapperWithDeserializerAndSerializers(
			Arrays.asList(replacementJsonDeserializers),
			Collections.emptyList(),
			false);
	}
	
	private static ObjectMapper buildObjectMapperWithoutRemovingEmptyValues()
	{
		return JsonMapper.builder(JsonFactory.builder()
			// relax parsing
			.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true)
			.configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
			.configure(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS, true)
			.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS, true)
			.configure(JsonReadFeature.ALLOW_MISSING_VALUES, true)
			.configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true)
			.build())
			// ignore failures
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
			.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false)
			.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false)
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
			// relax parsing
			.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
			.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
			.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
			.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true)
			.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
			.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
			.configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
			// use arrays
			.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true)
			// consistent json output
			.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
			.build();
	}
	
	private static ObjectMapper buildObjectMapperWithOnlyConfigurationDefaults()
	{
		return buildObjectMapperWithoutRemovingEmptyValues()
			// remove empty values from JSON
			.setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT)
			.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
			.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
			// add support for java date time serialisation and de-serialisation
			.registerModule(new JavaTimeModule());
	}
	
	private static ObjectMapper buildObjectMapperWithDeserializerAndSerializers(
		final List<JsonDeserializer> replacementJsonDeserializers,
		final List<JsonSerializer> replacementJsonSerializers,
		final boolean serialiseDefaultValues)
	{
		final ObjectMapper objectMapper = buildObjectMapperWithOnlyConfigurationDefaults();
		
		// register our own module with our serializers and deserializers
		final SimpleModule module = new SimpleModule();
		addDeserializers(module, replacementJsonDeserializers.toArray(new JsonDeserializer[0]));
		addSerializers(module, replacementJsonSerializers.toArray(new JsonSerializer[0]), serialiseDefaultValues);
		objectMapper.registerModule(module);
		return objectMapper;
	}
	
	private static void addDeserializers(
		final SimpleModule module,
		final JsonDeserializer[] replacementJsonDeserializers)
	{
		final List<JsonDeserializer> jsonDeserializers = Arrays.asList(
			// request
			new RequestDefinitionDTODeserializer(),
			// times
			new TimeToLiveDTODeserializer(),
			// request body
			new BodyDTODeserializer(),
			new BodyWithContentTypeDTODeserializer(),
			// condition
			new VerificationTimesDTODeserializer(),
			// nottable string
			new NottableStringDeserializer(),
			// key and multivalue
			new HeadersDeserializer(),
			new ParametersDeserializer(),
			new CookiesDeserializer()
		);
		final Map<Class, JsonDeserializer> jsonDeserializersByType = new HashMap<>();
		for(final JsonDeserializer jsonDeserializer : jsonDeserializers)
		{
			jsonDeserializersByType.put(jsonDeserializer.handledType(), jsonDeserializer);
		}
		// override any existing deserializers
		for(final JsonDeserializer additionJsonDeserializer : replacementJsonDeserializers)
		{
			jsonDeserializersByType.put(additionJsonDeserializer.handledType(), additionJsonDeserializer);
		}
		for(final Map.Entry<Class, JsonDeserializer> additionJsonDeserializer : jsonDeserializersByType.entrySet())
		{
			module.addDeserializer(additionJsonDeserializer.getKey(), additionJsonDeserializer.getValue());
		}
	}
	
	private static void addSerializers(
		final SimpleModule module,
		final JsonSerializer[] replacementJsonSerializers,
		final boolean serialiseDefaultValues)
	{
		final List<JsonSerializer> jsonSerializers = new ArrayList<>(List.of(
			// times
			new TimesSerializer(),
			new TimesDTOSerializer(),
			new TimeToLiveSerializer(),
			new TimeToLiveDTOSerializer(),
			// request
			new software.xdev.mockserver.serialization.serializers.request.HttpRequestSerializer(),
			new HttpRequestDTOSerializer(),
			// request body
			new BinaryBodySerializer(),
			new BinaryBodyDTOSerializer(),
			new ParameterBodySerializer(),
			new ParameterBodyDTOSerializer(),
			new RegexBodySerializer(),
			new RegexBodyDTOSerializer(),
			new StringBodySerializer(serialiseDefaultValues),
			new StringBodyDTOSerializer(serialiseDefaultValues),
			// condition
			new VerificationTimesDTOSerializer(),
			new VerificationTimesSerializer(),
			// nottable string
			new NottableStringSerializer(),
			// response
			new HttpResponseSerializer(),
			new HttpResponseDTOSerializer(),
			// key and multivalue
			new HeadersSerializer(),
			new ParametersSerializer(),
			new CookiesSerializer()
		));
		customizers().stream()
			.flatMap(c -> c.additionalSerializers().stream())
			.forEach(jsonSerializers::add);
		final Map<Class, JsonSerializer> jsonSerializersByType = new HashMap<>();
		for(final JsonSerializer jsonSerializer : jsonSerializers)
		{
			jsonSerializersByType.put(jsonSerializer.handledType(), jsonSerializer);
		}
		// override any existing serializers
		for(final JsonSerializer additionJsonSerializer : replacementJsonSerializers)
		{
			jsonSerializersByType.put(additionJsonSerializer.handledType(), additionJsonSerializer);
		}
		for(final Map.Entry<Class, JsonSerializer> additionJsonSerializer : jsonSerializersByType.entrySet())
		{
			module.addSerializer(additionJsonSerializer.getKey(), additionJsonSerializer.getValue());
		}
	}
	
	private static List<ObjectMapperFactoryCustomizer> customizers;
	
	private static List<ObjectMapperFactoryCustomizer> customizers()
	{
		if(customizers == null)
		{
			customizers = ServiceLoader.load(ObjectMapperFactoryCustomizer.class).stream()
				.map(ServiceLoader.Provider::get)
				.toList();
		}
		return customizers;
	}
	
	private ObjectMappers()
	{
	}
}
