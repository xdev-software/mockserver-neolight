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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.xdev.mockserver.exception.ExceptionHandling;
import software.xdev.mockserver.serialization.deserializers.body.BodyDTODeserializer;
import software.xdev.mockserver.serialization.deserializers.body.BodyWithContentTypeDTODeserializer;
import software.xdev.mockserver.serialization.deserializers.collections.CookiesDeserializer;
import software.xdev.mockserver.serialization.deserializers.collections.HeadersDeserializer;
import software.xdev.mockserver.serialization.deserializers.collections.ParametersDeserializer;
import software.xdev.mockserver.serialization.deserializers.condition.TimeToLiveDTODeserializer;
import software.xdev.mockserver.serialization.deserializers.condition.VerificationTimesDTODeserializer;
import software.xdev.mockserver.serialization.deserializers.expectation.OpenAPIExpectationDTODeserializer;
import software.xdev.mockserver.serialization.deserializers.request.RequestDefinitionDTODeserializer;
import software.xdev.mockserver.serialization.deserializers.string.NottableStringDeserializer;
import software.xdev.mockserver.serialization.serializers.body.*;
import software.xdev.mockserver.serialization.serializers.certificate.CertificateSerializer;
import software.xdev.mockserver.serialization.serializers.collections.CookiesSerializer;
import software.xdev.mockserver.serialization.serializers.collections.HeadersSerializer;
import software.xdev.mockserver.serialization.serializers.collections.ParametersSerializer;
import software.xdev.mockserver.serialization.serializers.condition.VerificationTimesDTOSerializer;
import software.xdev.mockserver.serialization.serializers.condition.VerificationTimesSerializer;
import software.xdev.mockserver.serialization.serializers.expectation.OpenAPIExpectationDTOSerializer;
import software.xdev.mockserver.serialization.serializers.expectation.OpenAPIExpectationSerializer;
import software.xdev.mockserver.serialization.serializers.matcher.HttpRequestPropertiesMatcherSerializer;
import software.xdev.mockserver.serialization.serializers.matcher.HttpRequestsPropertiesMatcherSerializer;
import software.xdev.mockserver.serialization.serializers.request.HttpRequestDTOSerializer;
import software.xdev.mockserver.serialization.serializers.request.OpenAPIDefinitionDTOSerializer;
import software.xdev.mockserver.serialization.serializers.request.OpenAPIDefinitionSerializer;
import software.xdev.mockserver.serialization.serializers.certificate.X509CertificateSerializer;
import software.xdev.mockserver.serialization.serializers.response.HttpResponseSerializer;
import software.xdev.mockserver.serialization.serializers.response.*;
import software.xdev.mockserver.serialization.serializers.schema.*;
import software.xdev.mockserver.serialization.serializers.string.NottableStringSerializer;

import java.util.*;

import static software.xdev.mockserver.exception.ExceptionHandling.handleThrowable;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ObjectMapperFactory {

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

    public static ObjectMapper createObjectMapper(JsonSerializer... additionJsonSerializers) {
        if (additionJsonSerializers == null || additionJsonSerializers.length == 0) {
            if (objectMapper == null) {
                objectMapper = buildObjectMapperWithDeserializerAndSerializers(Collections.emptyList(), Collections.emptyList(), false);
            }
            return objectMapper;
        } else {
            return buildObjectMapperWithDeserializerAndSerializers(Collections.emptyList(), Arrays.asList(additionJsonSerializers), false);
        }
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

    @SuppressWarnings("deprecation")
    public static ObjectMapper buildObjectMapperWithoutRemovingEmptyValues() {
        ObjectMapper objectMapper = new ObjectMapper();

        // ignore failures
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(MapperFeature.ALLOW_COERCION_OF_SCALARS, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, false));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(MapperFeature.AUTO_DETECT_GETTERS, true));

        // relax parsing
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(JsonParser.Feature.ALLOW_MISSING_VALUES, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true));
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true));

        // use arrays
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true));

        // consistent json output
        ExceptionHandling.handleThrowable(() -> objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true));

        return objectMapper;
    }

    public static ObjectMapper buildObjectMapperWithOnlyConfigurationDefaults() {
        ObjectMapper objectMapper = buildObjectMapperWithoutRemovingEmptyValues();

        // remove empty values from JSON
        ExceptionHandling.handleThrowable(() -> objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT));
        ExceptionHandling.handleThrowable(() -> objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL));
        ExceptionHandling.handleThrowable(() -> objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY));

        // add support for java date time serialisation and de-serialisation
        objectMapper.registerModule(new JavaTimeModule());

        return objectMapper;
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
            // expectation
            new OpenAPIExpectationDTODeserializer(),
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
            // expectation
            new OpenAPIExpectationSerializer(),
            new OpenAPIExpectationDTOSerializer(),
            // times
            new TimesSerializer(),
            new TimesDTOSerializer(),
            new TimeToLiveSerializer(),
            new TimeToLiveDTOSerializer(),
            // request
            new software.xdev.mockserver.serialization.serializers.request.HttpRequestSerializer(),
            new HttpRequestDTOSerializer(),
            new OpenAPIDefinitionSerializer(),
            new OpenAPIDefinitionDTOSerializer(),
            // request body
            new BinaryBodySerializer(),
            new BinaryBodyDTOSerializer(),
            new JsonBodySerializer(serialiseDefaultValues),
            new JsonBodyDTOSerializer(serialiseDefaultValues),
            new JsonSchemaBodySerializer(),
            new JsonSchemaBodyDTOSerializer(),
            new JsonPathBodySerializer(),
            new JsonPathBodyDTOSerializer(),
            new ParameterBodySerializer(),
            new ParameterBodyDTOSerializer(),
            new RegexBodySerializer(),
            new RegexBodyDTOSerializer(),
            new StringBodySerializer(serialiseDefaultValues),
            new StringBodyDTOSerializer(serialiseDefaultValues),
            new XPathBodySerializer(),
            new XPathBodyDTOSerializer(),
            new LogEntryBodySerializer(),
            new LogEntryBodyDTOSerializer(),
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
            new CookiesSerializer(),
            // certificates
            new X509CertificateSerializer(),
            new CertificateSerializer(),
            // log
            new software.xdev.mockserver.serialization.serializers.log.LogEntrySerializer(),
            // matcher
            new HttpRequestsPropertiesMatcherSerializer(),
            new HttpRequestPropertiesMatcherSerializer(),
            // schema
            new SchemaSerializer(),
            new ArraySchemaSerializer(),
            new BinarySchemaSerializer(),
            new BooleanSchemaSerializer(),
            new ByteArraySchemaSerializer(),
            new ComposedSchemaSerializer(),
            new DateSchemaSerializer(),
            new DateTimeSchemaSerializer(),
            new EmailSchemaSerializer(),
            new FileSchemaSerializer(),
            new IntegerSchemaSerializer(),
            new MapSchemaSerializer(),
            new NumberSchemaSerializer(),
            new ObjectSchemaSerializer(),
            new PasswordSchemaSerializer(),
            new StringSchemaSerializer(),
            new UUIDSchemaSerializer()
        );
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

}
