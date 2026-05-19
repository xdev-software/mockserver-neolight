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

import static software.xdev.mockserver.model.NottableString.string;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class PathModifier extends ObjectWithJsonToString
{
	private static final Map<String, Pattern> REGEX_PATTERN_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
	
	private int hashCode;
	private String regex;
	@JsonIgnore
	private Pattern pattern;
	private String substitution;
	
	public String getRegex()
	{
		return this.regex;
	}
	
	/**
	 * <p>
	 * The regex value to use to modify matching substrings, if multiple matches are found they will all be modified
	 * with the substitution for full details of supported regex syntax see:
	 * http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html
	 * </p>
	 * <p>
	 * If a null or empty substitution string is provided the regex pattern will be used to remove any substring
	 * matching the regex
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * <pre>
	 * regex: ^/(.+)/(.+)$
	 * substitution: /prefix/$1/infix/$2/postfix
	 * then: /some/path &#61;&gt; /prefix/some/infix/path/postfix
	 * or: /some/longer/path &#61;&gt; /prefix/some/infix/longer/path/postfix
	 * </pre>
	 *
	 * @param regex regex value to match on
	 */
	public PathModifier withRegex(final String regex)
	{
		this.regex = regex;
		this.hashCode = 0;
		return this;
	}
	
	public String getSubstitution()
	{
		return this.substitution;
	}
	
	/**
	 * <p>
	 * The pattern to substitute for the matched regex, matching groups are supported using $ followed by the group
	 * number for example $1
	 * </p>
	 * <p>
	 * If a null or empty substitution string is provided the regex pattern will be used to remove any substring
	 * matching the regex
	 * </p>
	 *
	 * @param substitution the value to substitute for the regex
	 */
	public PathModifier withSubstitution(final String substitution)
	{
		this.substitution = substitution;
		this.hashCode = 0;
		return this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || this.getClass() != o.getClass())
		{
			return false;
		}
		if(this.hashCode() != o.hashCode())
		{
			return false;
		}
		final PathModifier that = (PathModifier)o;
		return Objects.equals(this.regex, that.regex)
			&& Objects.equals(this.substitution, that.substitution);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(this.regex, this.substitution);
		}
		return this.hashCode;
	}
	
	@JsonIgnore
	private Pattern getPattern()
	{
		if(this.pattern == null && this.regex != null)
		{
			this.pattern = REGEX_PATTERN_CACHE.computeIfAbsent(this.regex, Pattern::compile);
		}
		return this.pattern;
	}
	
	public NottableString update(final NottableString path)
	{
		return string(this.update(path.getValue()), path.isNot());
	}
	
	public String update(final String path)
	{
		final Pattern p = this.getPattern();
		if(p != null)
		{
			return p.matcher(path).replaceAll(Objects.requireNonNullElse(this.substitution, ""));
		}
		return path;
	}
}
