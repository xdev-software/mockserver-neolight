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
package software.xdev.mockserver.xml;

import software.xdev.mockserver.model.ObjectWithReflectiveEqualsHashCodeToString;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

public class StringToXmlDocumentParser extends ObjectWithReflectiveEqualsHashCodeToString {

    public Document buildDocument(final String matched, final ErrorLogger errorLogger) throws ParserConfigurationException, IOException, SAXException {
        return buildDocument(matched, errorLogger, false);
    }

    public Document buildDocument(final String matched, final ErrorLogger errorLogger, boolean namespaceAware) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(namespaceAware);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        documentBuilder.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) {
                errorLogger.logError(matched, exception, ErrorLevel.WARNING);
            }

            @Override
            public void error(SAXParseException exception) {
                errorLogger.logError(matched, exception, ErrorLevel.ERROR);
            }

            @Override
            public void fatalError(SAXParseException exception) {
                errorLogger.logError(matched, exception, ErrorLevel.FATAL_ERROR);
            }
        });
        return documentBuilder.parse(new InputSource(new StringReader(matched)));
    }

    public interface ErrorLogger {
        void logError(final String xmlAsString, final Exception exception, ErrorLevel level);
    }

    public enum ErrorLevel {
        WARNING,
        ERROR,
        FATAL_ERROR;

        public static String prettyPrint(ErrorLevel errorLevel) {
            return errorLevel.name().toLowerCase().replaceAll("_", " ");
        }
    }
}
