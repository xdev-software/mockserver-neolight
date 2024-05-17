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

import static software.xdev.mockserver.model.NottableOptionalString.OPTIONAL_CHAR;
import static software.xdev.mockserver.model.NottableOptionalString.optional;
import static software.xdev.mockserver.model.ParameterStyle.DEEP_OBJECT;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import software.xdev.mockserver.util.StringUtils;


public class NottableString extends ObjectWithJsonToString implements Comparable<NottableString>
{
	public static final char NOT_CHAR = '!';
	private static final String EMPTY_STRING = "";
	private final String value;
	private final boolean isBlank;
	private final Boolean not;
	private final int hashCode;
	private final String json;
	private Pattern pattern;
	private ParameterStyle parameterStyle;
	
	NottableString(final String value, final Boolean not)
	{
		this.value = value;
		this.isBlank = StringUtils.isBlank(value);
		if(not != null)
		{
			this.not = not;
		}
		else
		{
			this.not = Boolean.FALSE;
		}
		this.hashCode = Objects.hash(this.value, this.not);
		this.json = this.serialise();
	}
	
	NottableString(final String value)
	{
		this.isBlank = StringUtils.isBlank(value);
		if(!this.isBlank && value.charAt(0) == NOT_CHAR)
		{
			this.value = value.substring(1);
			this.not = Boolean.TRUE;
		}
		else
		{
			this.value = value;
			this.not = Boolean.FALSE;
		}
		this.hashCode = Objects.hash(this.value, this.not);
		this.json = this.serialise();
	}
	
	private String serialise()
	{
		if(this.isOptional() || this.not)
		{
			return (this.isOptional() ? "" + OPTIONAL_CHAR : "")
				+ (this.not ? "" + NOT_CHAR : "")
				+ (!this.isBlank ? this.value : EMPTY_STRING);
		}
		else if(this.isBlank)
		{
			return EMPTY_STRING;
		}
		else
		{
			return this.value;
		}
	}
	
	public static List<NottableString> deserializeNottableStrings(final String... strings)
	{
		final List<NottableString> nottableStrings = new LinkedList<>();
		for(final String string : strings)
		{
			nottableStrings.add(string(string));
		}
		return nottableStrings;
	}
	
	public static List<NottableString> deserializeNottableStrings(final List<String> strings)
	{
		final List<NottableString> nottableStrings = new LinkedList<>();
		for(final String string : strings)
		{
			nottableStrings.add(string(string));
		}
		return nottableStrings;
	}
	
	public static String serialiseNottableString(final NottableString nottableString)
	{
		return nottableString.toString();
	}
	
	public static List<String> serialiseNottableStrings(final Collection<NottableString> nottableStrings)
	{
		final List<String> strings = new LinkedList<>();
		for(final NottableString nottableString : nottableStrings)
		{
			strings.add(nottableString.toString());
		}
		return strings;
	}
	
	public static NottableString string(final String value, final Boolean not)
	{
		return new NottableString(value, not);
	}
	
	@SuppressWarnings("checkstyle:FinalParameters")
	public static NottableString string(String value)
	{
		Boolean not = null;
		boolean optional = false;
		if(isNotBlank(value))
		{
			if(value.charAt(0) == OPTIONAL_CHAR)
			{
				optional = true;
				value = value.substring(1);
			}
			if(value.charAt(0) == NOT_CHAR)
			{
				not = true;
				value = value.substring(1);
			}
			if(value.charAt(0) == OPTIONAL_CHAR)
			{
				optional = true;
				value = value.substring(1);
			}
		}
		if(optional)
		{
			return optional(value, not);
		}
		else
		{
			return new NottableString(value, not);
		}
	}
	
	public static NottableString not(final String value)
	{
		return new NottableString(value, Boolean.TRUE);
	}
	
	public static List<NottableString> strings(final String... values)
	{
		final List<NottableString> nottableValues = new ArrayList<>();
		if(values != null)
		{
			for(final String value : values)
			{
				nottableValues.add(string(value));
			}
		}
		return nottableValues;
	}
	
	public static List<NottableString> strings(final Collection<String> values)
	{
		final List<NottableString> nottableValues = new ArrayList<>();
		if(values != null)
		{
			for(final String value : values)
			{
				nottableValues.add(string(value));
			}
		}
		return nottableValues;
	}
	
	public String getValue()
	{
		return this.value;
	}
	
	@JsonIgnore
	public boolean isNot()
	{
		return this.not;
	}
	
	public boolean isOptional()
	{
		return false;
	}
	
	public ParameterStyle getParameterStyle()
	{
		return this.parameterStyle;
	}
	
	public NottableString withStyle(final ParameterStyle style)
	{
		if(style != null && style.equals(DEEP_OBJECT))
		{
			throw new IllegalArgumentException("deep object style is not supported");
		}
		this.parameterStyle = style;
		return this;
	}
	
	public NottableString lowercase()
	{
		return new NottableString(this.value.toLowerCase(), this.not);
	}
	
	public boolean equalsIgnoreCase(final Object other)
	{
		return this.equals(other, true);
	}
	
	private boolean equals(final Object other, final boolean ignoreCase)
	{
		if(other instanceof final String s)
		{
			if(ignoreCase)
			{
				return this.not != s.equalsIgnoreCase(this.value);
			}
			else
			{
				return this.not != other.equals(this.value);
			}
		}
		else if(other instanceof final NottableString that)
		{
			if(that.getValue() == null)
			{
				return this.value == null;
			}
			final boolean reverse = (that.not != this.not) && (that.not || this.not);
			if(ignoreCase)
			{
				return reverse != that.getValue().equalsIgnoreCase(this.value);
			}
			else
			{
				return reverse != that.getValue().equals(this.value);
			}
		}
		return false;
	}
	
	public boolean isBlank()
	{
		return this.isBlank;
	}
	
	public boolean matches(final String input)
	{
		if(this.pattern == null)
		{
			this.pattern =
				Pattern.compile(this.getValue(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		}
		return this.pattern.matcher(input).matches();
	}
	
	@Override
	public boolean equals(final Object other)
	{
		if(other instanceof String)
		{
			return this.not != other.equals(this.value);
		}
		else if(other instanceof final NottableString nottableString)
		{
			return this.equalsValue(nottableString);
		}
		return false;
	}
	
	private boolean equalsString(final NottableString one, final NottableString two)
	{
		if(one.value == null && two.value == null)
		{
			return true;
		}
		else if(one.value == null || two.value == null)
		{
			return false;
		}
		else
		{
			final boolean reverse = (two.not != one.not) && (two.not || one.not);
			return reverse != two.value.equals(one.value);
		}
	}
	
	private boolean equalsValue(final NottableString other)
	{
		return this.equalsString(this, other);
	}
	
	@Override
	public int hashCode()
	{
		return this.hashCode;
	}
	
	@Override
	public String toString()
	{
		return this.json;
	}
	
	@Override
	public int compareTo(final NottableString other)
	{
		return other.getValue().compareTo(this.getValue());
	}
}
