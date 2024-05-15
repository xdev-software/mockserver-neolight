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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.xdev.mockserver.model.NottableOptionalString.OPTIONAL_CHAR;
import static software.xdev.mockserver.model.NottableOptionalString.optional;
import static software.xdev.mockserver.model.ParameterStyle.DEEP_OBJECT;

public class NottableString extends ObjectWithJsonToString implements Comparable<NottableString> {

    public static final char NOT_CHAR = '!';
    private static final String EMPTY_STRING = "";
    private final String value;
    private final boolean isBlank;
    private final Boolean not;
    private final int hashCode;
    private final String json;
    private Pattern pattern;
    private ParameterStyle parameterStyle;

    NottableString(String value, Boolean not) {
        this.value = value;
        this.isBlank = StringUtils.isBlank(value);
        if (not != null) {
            this.not = not;
        } else {
            this.not = Boolean.FALSE;
        }
        this.hashCode = Objects.hash(this.value, this.not);
        this.json = serialise();
    }

    NottableString(String value) {
        this.isBlank = StringUtils.isBlank(value);
        if (!this.isBlank && value.charAt(0) == NOT_CHAR) {
            this.value = value.substring(1);
            this.not = Boolean.TRUE;
        } else {
            this.value = value;
            this.not = Boolean.FALSE;
        }
        this.hashCode = Objects.hash(this.value, this.not);
        this.json = serialise();
    }

    private String serialise() {
        if (this.isOptional() || this.not) {
            return (this.isOptional() ? "" + OPTIONAL_CHAR : "") + (this.not ? "" + NOT_CHAR : "") + (!this.isBlank ? this.value : EMPTY_STRING);
        } else if (this.isBlank) {
            return EMPTY_STRING;
        } else {
            return this.value;
        }
    }

    public static List<NottableString> deserializeNottableStrings(String... strings) {
        List<NottableString> nottableStrings = new LinkedList<>();
        for (String string : strings) {
            nottableStrings.add(string(string));
        }
        return nottableStrings;
    }

    public static List<NottableString> deserializeNottableStrings(List<String> strings) {
        List<NottableString> nottableStrings = new LinkedList<>();
        for (String string : strings) {
            nottableStrings.add(string(string));
        }
        return nottableStrings;
    }

    public static String serialiseNottableString(NottableString nottableString) {
        return nottableString.toString();
    }

    public static List<String> serialiseNottableStrings(Collection<NottableString> nottableStrings) {
        List<String> strings = new LinkedList<>();
        for (NottableString nottableString : nottableStrings) {
            strings.add(nottableString.toString());
        }
        return strings;
    }

    public static NottableString string(String value, Boolean not) {
        return new NottableString(value, not);
    }

    public static NottableString string(String value) {
        Boolean not = null;
        boolean optional = false;
        if (isNotBlank(value)) {
            if (value.charAt(0) == OPTIONAL_CHAR) {
                optional = true;
                value = value.substring(1);
            }
            if (value.charAt(0) == NOT_CHAR) {
                not = true;
                value = value.substring(1);
            }
            if (value.charAt(0) == OPTIONAL_CHAR) {
                optional = true;
                value = value.substring(1);
            }
        }
        if (optional) {
            return optional(value, not);
        } else {
            return new NottableString(value, not);
        }
    }

    public static NottableString not(String value) {
        return new NottableString(value, Boolean.TRUE);
    }

    public static List<NottableString> strings(String... values) {
        List<NottableString> nottableValues = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                nottableValues.add(string(value));
            }
        }
        return nottableValues;
    }

    public static List<NottableString> strings(Collection<String> values) {
        List<NottableString> nottableValues = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                nottableValues.add(string(value));
            }
        }
        return nottableValues;
    }

    public String getValue() {
        return value;
    }

    @JsonIgnore
    public boolean isNot() {
        return not;
    }

    public boolean isOptional() {
        return false;
    }

    public ParameterStyle getParameterStyle() {
        return parameterStyle;
    }

    public NottableString withStyle(ParameterStyle style) {
        if (style != null && style.equals(DEEP_OBJECT)) {
            throw new IllegalArgumentException("deep object style is not supported");
        }
        this.parameterStyle = style;
        return this;
    }

    NottableString capitalize() {
        final String[] split = (value + "_").split("-");
        for (int i = 0; i < split.length; i++) {
            split[i] = StringUtils.capitalize(split[i]);
        }
        return new NottableString(StringUtils.substringBeforeLast(Joiner.on("-").join(split), "_"), not);
    }

    public NottableString lowercase() {
        return new NottableString(value.toLowerCase(), not);
    }

    public boolean equalsIgnoreCase(Object other) {
        return equals(other, true);
    }

    private boolean equals(Object other, boolean ignoreCase) {
        if (other instanceof String) {
            if (ignoreCase) {
                return not != ((String) other).equalsIgnoreCase(value);
            } else {
                return not != other.equals(value);
            }
        } else if (other instanceof NottableString) {
            NottableString that = (NottableString) other;
            if (that.getValue() == null) {
                return value == null;
            }
            boolean reverse = (that.not != this.not) && (that.not || this.not);
            if (ignoreCase) {
                return reverse != that.getValue().equalsIgnoreCase(value);
            } else {
                return reverse != that.getValue().equals(value);
            }
        }
        return false;
    }

    public boolean isBlank() {
        return isBlank;
    }

    public boolean matches(String input) {
        if (pattern == null) {
            pattern = Pattern.compile(getValue(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        return pattern.matcher(input).matches();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof String) {
            return not != other.equals(value);
        } else if (other instanceof NottableString) {
            return equalsValue((NottableString) other);
        }
        return false;
    }

    private boolean equalsString(NottableString one, NottableString two) {
        if (one.value == null && two.value == null) {
            return true;
        } else if (one.value == null || two.value == null) {
            return false;
        } else {
            boolean reverse = (two.not != one.not) && (two.not || one.not);
            return reverse != two.value.equals(one.value);
        }
    }

    private boolean equalsValue(NottableString other) {
        return equalsString(this, other);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return json;
    }

    @Override
    public int compareTo(NottableString other) {
        return other.getValue().compareTo(this.getValue());
    }
}
