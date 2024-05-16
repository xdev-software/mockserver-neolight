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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.*;
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
import software.xdev.mockserver.serialization.serializers.body.*;
import software.xdev.mockserver.serialization.serializers.collections.CookiesSerializer;
import software.xdev.mockserver.serialization.serializers.collections.HeadersSerializer;
import software.xdev.mockserver.serialization.serializers.collections.ParametersSerializer;
import software.xdev.mockserver.serialization.serializers.condition.VerificationTimesDTOSerializer;
import software.xdev.mockserver.serialization.serializers.condition.VerificationTimesSerializer;
import software.xdev.mockserver.serialization.serializers.request.HttpRequestDTOSerializer;
import software.xdev.mockserver.serialization.serializers.response.HttpResponseSerializer;
import software.xdev.mockserver.serialization.serializers.response.*;
import software.xdev.mockserver.serialization.serializers.string.NottableStringSerializer;

import java.util.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class ObjectMapperFactory {

    private static ObjectMapper objectMapper = buildObjectMapperWithDeserializerAndSerializers(Collections.emptyList(), Collections.emptyList(), false);
    private static final ObjectWriter prettyPrintWriter = buildObjectMapperWithDeserializerAndSerializers(Collections.emptyList(), Collections.emptyList(), false).writerWithDefaultPrettyPrinter();
    private static final ObjectWriter prettyPrintWriterThatSerialisesDefaultFields = buildObjectMapperWithDeserializerAndSerializers(Collections.emptyList(), Collections.emptyList(), true).writerWithDefaultPrettyPrinter();
    private static final ObjectWriter writer = buildObjectMapperWithDeserializerAndSerializers(Collections.emptyList(), Collections.emptyList(), false).writer();

    public static ObjectMapper createObjectMapper() {
        if (objectMapper == null) {
            objectMapper = buildObjectMapperWithDeserializerAndSerializers(Collections.emptyList(), Collections.emptyList(), false);
        }
        return objectMapper;
    }

    public static ObjectMapper createObjectMapper(JsonDeserializer... replacementJsonDeserializers) {
        if (replacementJsonDeserializers == null || replacementJsonDeserializers.length == 0) {
            if (objectMapper == null) {
                objectMapper = buildObjectMapperWithDeserializerAndSerializers(Collections.emptyList(), Collections.emptyList(), false);
            }
            return objectMapper;
        } else {
            return buildObjectMapperWithDeserializerAndSerializers(Arrays.asList(replacementJsonDeserializers), Collections.emptyList(), false);
        }
    }

    public static ObjectWriter createObjectMapper(boolean pretty, boolean serialiseDefaultValues, JsonSerializer... additionJsonSerializers) {
        if (additionJsonSerializers == null || additionJsonSerializers.length == 0) {
            if (pretty && serialiseDefaultValues) {
                return prettyPrintWriterThatSerialisesDefaultFields;
            } else if (pretty) {
                return prettyPrintWriter;
            } else {
                return writer;
            }
        } else {
            if (pretty) {
                return buildObjectMapperWithDeserializerAndSerializers(Collections.emptyList(), Arrays.asList(additionJsonSerializers), serialiseDefaultValues).writerWithDefaultPrettyPrinter();
            } else {
                return buildObjectMapperWithDeserializerAndSerializers(Collections.emptyList(), Arrays.asList(additionJsonSerializers), serialiseDefaultValues).writer();
            }
        }
    }

    public static ObjectMapper buildObjectMapperWithoutRemovingEmptyValues() {
        return new ObjectMapper(JsonFactory.builder()
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
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public static ObjectMapper buildObjectMapperWithOnlyConfigurationDefaults() {
        return buildObjectMapperWithoutRemovingEmptyValues()
            // remove empty values from JSON
            .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            // add support for java date time serialisation and de-serialisation
            .registerModule(new JavaTimeModule());
    }

    private static ObjectMapper buildObjectMapperWithDeserializerAndSerializers(List<JsonDeserializer> replacementJsonDeserializers, List<JsonSerializer> replacementJsonSerializers, boolean serialiseDefaultValues) {
        ObjectMapper objectMapper = buildObjectMapperWithOnlyConfigurationDefaults();

        // register our own module with our serializers and deserializers
        SimpleModule module = new SimpleModule();
        addDeserializers(module, replacementJsonDeserializers.toArray(new JsonDeserializer[0]));
        addSerializers(module, replacementJsonSerializers.toArray(new JsonSerializer[0]), serialiseDefaultValues);
        objectMapper.registerModule(module);
        return objectMapper;
    }

    private static void addDeserializers(SimpleModule module, JsonDeserializer[] replacementJsonDeserializers) {
        List<JsonDeserializer> jsonDeserializers = Arrays.asList(
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
        Map<Class, JsonDeserializer> jsonDeserializersByType = new HashMap<>();
        for (JsonDeserializer jsonDeserializer : jsonDeserializers) {
            jsonDeserializersByType.put(jsonDeserializer.handledType(), jsonDeserializer);
        }
        // override any existing deserializers
        for (JsonDeserializer additionJsonDeserializer : replacementJsonDeserializers) {
            jsonDeserializersByType.put(additionJsonDeserializer.handledType(), additionJsonDeserializer);
        }
        for (Map.Entry<Class, JsonDeserializer> additionJsonDeserializer : jsonDeserializersByType.entrySet()) {
            module.addDeserializer(additionJsonDeserializer.getKey(), additionJsonDeserializer.getValue());
        }
    }

    private static void addSerializers(SimpleModule module, JsonSerializer[] replacementJsonSerializers, boolean serialiseDefaultValues) {
        List<JsonSerializer> jsonSerializers = Arrays.asList(
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
        );
        customizers().stream()
            .flatMap(c -> c.additionalSerializers().stream())
            .forEach(jsonSerializers::add);
        Map<Class, JsonSerializer> jsonSerializersByType = new HashMap<>();
        for (JsonSerializer jsonSerializer : jsonSerializers) {
            jsonSerializersByType.put(jsonSerializer.handledType(), jsonSerializer);
        }
        // override any existing serializers
        for (JsonSerializer additionJsonSerializer : replacementJsonSerializers) {
            jsonSerializersByType.put(additionJsonSerializer.handledType(), additionJsonSerializer);
        }
        for (Map.Entry<Class, JsonSerializer> additionJsonSerializer : jsonSerializersByType.entrySet()) {
            module.addSerializer(additionJsonSerializer.getKey(), additionJsonSerializer.getValue());
        }
    }
    
    private static List<ObjectMapperFactoryCustomizer> _customizers;
    
    private static List<ObjectMapperFactoryCustomizer> customizers() {
        if(_customizers == null) {
            _customizers =  ServiceLoader.load(ObjectMapperFactoryCustomizer.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        }
        return _customizers;
    }
    
    private ObjectMapperFactory() {
    }

}
