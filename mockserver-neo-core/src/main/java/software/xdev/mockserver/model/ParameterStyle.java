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
package software.xdev.mockserver.model;

public enum ParameterStyle {
    // PATH
    SIMPLE("\\,"),
    SIMPLE_EXPLODED("\\,"),
    LABEL("\\,"),
    LABEL_EXPLODED("\\."),
    MATRIX("\\,"),
    MATRIX_EXPLODED(";<name>="),
    // QUERY
    FORM_EXPLODED(""),
    FORM("\\,"),
    SPACE_DELIMITED_EXPLODED(""),
    SPACE_DELIMITED("(%20)|\\s|\\+"),
    PIPE_DELIMITED_EXPLODED(""),
    PIPE_DELIMITED("\\|"),
    DEEP_OBJECT("");

    private final String regex;
    private final boolean exploded;

    ParameterStyle(String regex) {
        this.regex = regex;
        this.exploded = !regex.isEmpty();
    }

    public String getRegex() {
        return regex;
    }

    public boolean isExploded() {
        return exploded;
    }

    @Override
    public String toString() {
        return name() + "(" + regex + "," + Boolean.toString(exploded) + ")";
    }
}