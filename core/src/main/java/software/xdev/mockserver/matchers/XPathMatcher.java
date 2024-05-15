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
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.xml.StringToXmlDocumentParser;
import software.xdev.mockserver.xml.XPathEvaluator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.xdev.mockserver.xml.StringToXmlDocumentParser.ErrorLevel.prettyPrint;
import static org.slf4j.event.Level.DEBUG;

public class XPathMatcher extends BodyMatcher<String> {
    private static final String[] EXCLUDED_FIELDS = {"mockServerLogger", "xPathEvaluator"};
    private final MockServerLogger mockServerLogger;
    private final String matcher;
    private XPathEvaluator xPathEvaluator = null;
    XPathMatcher(MockServerLogger mockServerLogger, String matcher) {
        this(mockServerLogger, matcher, null);
    }

    XPathMatcher(MockServerLogger mockServerLogger, String matcher, Map<String, String> namespacePrefixes) {
        this.mockServerLogger = mockServerLogger;
        this.matcher = matcher;
        if (isNotBlank(matcher)) {
            try {
                xPathEvaluator = new XPathEvaluator(matcher, namespacePrefixes);
            } catch (Throwable throwable) {
                if (MockServerLogger.isEnabled(DEBUG) && mockServerLogger != null) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(DEBUG)
                            .setMessageFormat("error while creating xpath expression for{}assuming matcher not xpath{}")
                            .setArguments(matcher, throwable.getMessage())
                            .setThrowable(throwable)
                    );
                }
            }
        }
    }

    public boolean matches(final MatchDifference context, final String matched) {
        boolean result = false;
        boolean alreadyLoggedMatchFailure = false;

        if (xPathEvaluator == null) {
            if (context != null) {
                context.addDifference(mockServerLogger, "xpath match failed expected:{}found:{}failed because:{}", "null", matched, "xpath matcher was null");
                alreadyLoggedMatchFailure = true;
            }
        } else if (matcher.equals(matched)) {
            result = true;
        } else if (matched != null) {
            try {
                result = (Boolean) xPathEvaluator.evaluateXPathExpression(matched, (matchedInException, throwable, level) -> {
                    if (context != null) {
                        context.addDifference(mockServerLogger, throwable, "xpath match failed expected:{}found:{}failed because " + prettyPrint(level) + ":{}", matcher, matched, throwable.getMessage());
                    }
                }, XPathConstants.BOOLEAN);
            } catch (Throwable throwable) {
                if (context != null) {
                    context.addDifference(mockServerLogger, throwable, "xpath match failed expected:{}found:{}failed because:{}", matcher, matched, throwable.getMessage());
                    alreadyLoggedMatchFailure = true;
                }
            }
        }

        if (!result && !alreadyLoggedMatchFailure && context != null) {
            context.addDifference(mockServerLogger, "xpath match failed expected:{}found:{}failed because:{}", matcher, matched, "xpath did not evaluate to truthy");
        }

        return not != result;
    }

    public boolean isBlank() {
        return StringUtils.isBlank(matcher);
    }

    @Override
    @JsonIgnore
    protected String[] fieldsExcludedFromEqualsAndHashCode() {
        return EXCLUDED_FIELDS;
    }
}
