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
package software.xdev.mockserver.codec;

import software.xdev.mockserver.util.StringUtils;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static software.xdev.mockserver.model.NottableString.string;

public class PathParametersDecoder {

    private static final Pattern PATH_VARIABLE_NAME_PATTERN = Pattern.compile("\\{[.;]?([^*]+)\\*?}");

    public NottableString normalisePathWithParametersForMatching(HttpRequest matcher) {
        NottableString result = null;
        if (matcher.getPath() != null) {
            if (matcher.getPathParameters() != null && !matcher.getPathParameters().isEmpty()) {
                String value = matcher.getPath().getValue();
                if (value.contains("{")) {
                    List<String> pathParts = new ArrayList<>();
                    for (String pathPart : matcher.getPath().getValue().split("/")) {
                        Matcher pathParameterName = PATH_VARIABLE_NAME_PATTERN.matcher(pathPart);
                        if (pathParameterName.matches()) {
                            pathParts.add(".*");
                        } else {
                            pathParts.add(pathPart);
                        }
                    }
                    result = string(String.join("/", pathParts) + (value.endsWith("/") ? "/" : ""));
                } else {
                    result = matcher.getPath();
                }
            } else {
                result = matcher.getPath();
            }
        }
        // Unable to load API spec attribute paths.'/pets/{petId}'. Declared path parameter petId needs to be defined as a path parameter in path or operation level
        return result;
    }

    public Parameters extractPathParameters(HttpRequest matcher, HttpRequest matched) {
        Parameters parsedParameters = matched.getPathParameters() != null ? matched.getPathParameters() : new Parameters();
        if (matcher.getPathParameters() != null && !matcher.getPathParameters().isEmpty()) {
            String[] matcherPathParts = getPathParts(matcher.getPath());
            String[] matchedPathParts = getPathParts(matched.getPath());
            if (matcherPathParts.length != matchedPathParts.length) {
                throw new IllegalArgumentException("expected path " + matcher.getPath().getValue() + " has " + matcherPathParts.length + " parts but path " + matched.getPath().getValue() + " has " + matchedPathParts.length + " part" + (matchedPathParts.length > 1 ? "s " : " "));
            }
            for (int i = 0; i < matcherPathParts.length; i++) {
                Matcher pathParameterName = PATH_VARIABLE_NAME_PATTERN.matcher(matcherPathParts[i]);
                if (pathParameterName.matches()) {
                    String parameterName = pathParameterName.group(1);
                    List<String> parameterValues = new ArrayList<>();
                    Matcher pathParameterValue = Pattern.compile("[.;]?(?:" + parameterName + "=)?([^,]++)[.,;]?").matcher(matchedPathParts[i]);
                    while (pathParameterValue.find()) {
                        parameterValues.add(pathParameterValue.group(1));
                    }
                    parsedParameters.withEntry(parameterName, parameterValues);
                }
            }
        }
        return parsedParameters;
    }

    private String[] getPathParts(NottableString path) {
        return path != null ? Arrays.stream(StringUtils.removeStart(path.getValue(), "/").split("/")).filter(StringUtils::isNotBlank).toArray(String[]::new) : new String[0];
    }
}
