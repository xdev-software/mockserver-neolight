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
package software.xdev.mockserver.matchers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.xdev.mockserver.codec.ExpandedParameterDecoder;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.model.Parameters;

public class ParameterStringMatcher extends BodyMatcher<String> {
    private final MultiValueMapMatcher matcher;
    private final ExpandedParameterDecoder formParameterParser;
    private final Parameters matcherParameters;
    private final ExpandedParameterDecoder expandedParameterDecoder;

    ParameterStringMatcher(Configuration configuration, Parameters matcherParameters, boolean controlPlaneMatcher) {
        this.matcherParameters = matcherParameters;
        this.matcher = new MultiValueMapMatcher(matcherParameters, controlPlaneMatcher);
        this.formParameterParser = new ExpandedParameterDecoder(configuration);
        this.expandedParameterDecoder = new ExpandedParameterDecoder(configuration);
    }

    public boolean matches(final MatchDifference context, String matched) {
        boolean result = false;

        Parameters matchedParameters = formParameterParser.retrieveFormParameters(matched, matched != null && matched.contains("?"));
        expandedParameterDecoder.splitParameters(matcherParameters, matchedParameters);
        if (matcher.matches(context, matchedParameters)) {
            result = true;
        }

        return not != result;
    }

    public boolean isBlank() {
        return matcher.isBlank();
    }
}
