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
package software.xdev.mockserver.mock.action;

import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;


public interface ExpectationResponseCallback extends ExpectationCallback<HttpResponse>
{
	/**
	 * Called for every request when expectation condition has been satisfied. The request that satisfied the
	 * expectation condition is passed as the parameter and the return value is the request that will be returned.
	 *
	 * @param httpRequest the request that satisfied the expectation condition
	 * @return the response that will be returned
	 */
	@Override
	HttpResponse handle(HttpRequest httpRequest) throws Exception;
}
