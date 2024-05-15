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
package software.xdev.mockserver.openapi;

import com.google.common.base.Joiner;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.extensions.SwaggerParserExtension;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import software.xdev.mockserver.cache.LRUCache;
import software.xdev.mockserver.logging.MockServerLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.swagger.v3.parser.OpenAPIV3Parser.getExtensions;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class OpenAPIParser {

    private final static LRUCache<String, OpenAPI> openAPILRUCache = new LRUCache<>(new MockServerLogger(), 250, MINUTES.toMillis(30));

    public static final String OPEN_API_LOAD_ERROR = "Unable to load API spec";

    /**
     * Helper function that checks if the provided string is a reference to an API specification file
     * @param specUrlOrPayload - string that might contain an API specification file reference
     * @return <b>true</b> if the provided string ends with special file suffix
     */
    public static boolean isSpecUrl(String specUrlOrPayload) {
        return specUrlOrPayload != null && (
            specUrlOrPayload.endsWith(".json") || specUrlOrPayload.endsWith(".yaml") || specUrlOrPayload.endsWith(".yml")
        );
    }

    public static OpenAPI buildOpenAPI(String specUrlOrPayload, MockServerLogger mockServerLogger) {
        OpenAPI openAPI = openAPILRUCache.get(specUrlOrPayload);
        if (openAPI == null) {
            SwaggerParseResult swaggerParseResult = null;
            List<AuthorizationValue> auths = null;
            ParseOptions parseOptions = new ParseOptions();
            parseOptions.setResolve(true);
            parseOptions.setResolveFully(true);
            parseOptions.setResolveCombinators(true);
            parseOptions.setSkipMatches(true);
            parseOptions.setAllowEmptyString(true);
            parseOptions.setCamelCaseFlattenNaming(true);

            List<String> errorMessage = new ArrayList<>();
            try {
                if (OpenAPIParser.isSpecUrl(specUrlOrPayload)) {
                    specUrlOrPayload = specUrlOrPayload.replaceAll("\\\\", "/");
                    List<SwaggerParserExtension> parserExtensions = getExtensions();
                    for (SwaggerParserExtension extension : parserExtensions) {
                        swaggerParseResult = extension.readLocation(specUrlOrPayload, auths, parseOptions);
                        openAPI = swaggerParseResult.getOpenAPI();
                        if (openAPI != null) {
                            break;
                        } else {
                            errorMessage.addAll(swaggerParseResult.getMessages());
                        }
                    }
                } else {
                    swaggerParseResult = new OpenAPIV3Parser().readContents(specUrlOrPayload, auths, parseOptions);
                    openAPI = swaggerParseResult.getOpenAPI();
                    if (openAPI == null) {
                        errorMessage.addAll(swaggerParseResult.getMessages());
                    }
                }
            } catch (Throwable throwable) {
                throw new IllegalArgumentException(OPEN_API_LOAD_ERROR + (errorMessage.isEmpty() ? ", " + throwable.getMessage() : ", " + Joiner.on(", ").skipNulls().join(errorMessage)), throwable);
            }
            if (openAPI == null) {
                if (swaggerParseResult != null) {
                    String message = errorMessage.stream().filter(Objects::nonNull).collect(Collectors.joining(" and ")).trim();
                    throw new IllegalArgumentException((OPEN_API_LOAD_ERROR + (isNotBlank(message) ? ", " + message : "")));
                } else {
                    throw new IllegalArgumentException(OPEN_API_LOAD_ERROR);
                }
            }
            addMissingOperationIds(openAPI);
            openAPILRUCache.put(specUrlOrPayload, openAPI);
        }
        return openAPI;
    }

    private static void addMissingOperationIds(OpenAPI openAPI) {
        openAPI.getPaths().forEach(
            (path, pathItem) -> {
                mapOperations(pathItem).forEach(
                    stringOperationPair -> {
                        if (isBlank(stringOperationPair.getRight().getOperationId())) {
                            stringOperationPair.getRight().setOperationId(stringOperationPair.getLeft() + " " + path);
                        }
                    }
                );
            }
        );
    }

    public static List<Pair<String, Operation>> mapOperations(PathItem pathItem) {
        List<Pair<String, Operation>> allOperations = new ArrayList<>();
        if (pathItem.getGet() != null) {
            allOperations.add(new ImmutablePair<>("GET", pathItem.getGet()));
        }
        if (pathItem.getPut() != null) {
            allOperations.add(new ImmutablePair<>("PUT", pathItem.getPut()));
        }
        if (pathItem.getPost() != null) {
            allOperations.add(new ImmutablePair<>("POST", pathItem.getPost()));
        }
        if (pathItem.getPatch() != null) {
            allOperations.add(new ImmutablePair<>("PATCH", pathItem.getPatch()));
        }
        if (pathItem.getDelete() != null) {
            allOperations.add(new ImmutablePair<>("DELETE", pathItem.getDelete()));
        }
        if (pathItem.getHead() != null) {
            allOperations.add(new ImmutablePair<>("HEAD", pathItem.getHead()));
        }
        if (pathItem.getOptions() != null) {
            allOperations.add(new ImmutablePair<>("OPTIONS", pathItem.getOptions()));
        }
        if (pathItem.getTrace() != null) {
            allOperations.add(new ImmutablePair<>("TRACE", pathItem.getTrace()));
        }
        return allOperations;
    }
}
