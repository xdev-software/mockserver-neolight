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

import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;


public abstract class RequestDefinition extends Not
{
	private static final Logger LOG = LoggerFactory.getLogger(RequestDefinition.class);
	private String logCorrelationId;
	
	@JsonIgnore
	public String getLogCorrelationId()
	{
		return this.logCorrelationId;
	}
	
	public RequestDefinition withLogCorrelationId(final String logCorrelationId)
	{
		this.logCorrelationId = logCorrelationId;
		return this;
	}
	
	public abstract RequestDefinition shallowClone();
	
	public RequestDefinition cloneWithLogCorrelationId()
	{
		return LOG.isTraceEnabled() && isNotBlank(this.getLogCorrelationId())
			? this.shallowClone().withLogCorrelationId(this.getLogCorrelationId())
			: this;
	}
}
