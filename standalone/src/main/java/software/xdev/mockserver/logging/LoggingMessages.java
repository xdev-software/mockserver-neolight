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
package software.xdev.mockserver.logging;

public final class LoggingMessages
{
	public static final String RECEIVED_REQUEST_MESSAGE_FORMAT = "received request:{}";
	public static final String UPDATED_EXPECTATION_MESSAGE_FORMAT = "updated expectation: {} with id: {}";
	public static final String CREATED_EXPECTATION_MESSAGE_FORMAT = "creating expectation: {} with id: {}";
	public static final String REMOVED_EXPECTATION_MESSAGE_FORMAT = "removed expectation: {} with id: {}";
	public static final String NO_MATCH_RESPONSE_NO_EXPECTATION_MESSAGE_FORMAT =
		"no expectation for: {} returning response: {}";
	public static final String NO_MATCH_RESPONSE_ERROR_MESSAGE_FORMAT =
		"error:{}handling request: {} returning response: {}";
	public static final String VERIFICATION_REQUESTS_MESSAGE_FORMAT = "verifying requests that match: {}";
	public static final String VERIFICATION_REQUEST_SEQUENCES_MESSAGE_FORMAT = "verifying sequence that match: {}";
	
	private LoggingMessages()
	{
	}
}
