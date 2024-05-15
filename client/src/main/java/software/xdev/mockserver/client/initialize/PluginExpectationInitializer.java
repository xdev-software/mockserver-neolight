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
package software.xdev.mockserver.client.initialize;

import software.xdev.mockserver.client.MockServerClient;

/**
 * If the MockServer is started using the Maven Plugin a initializationClass property can be specified to initialize expectations, when the MockServer starts.
 *
 * Note: the plugin must be started during the process-test-classes to ensure that the initialization class has been compiled from either src/main/java or
 * src/test/java locations. In addition the initializer can only be used with start and run goals, it will not work with the runForked goal as a JVM is forked
 * with a separate classpath. (required: false, default: false)
 *
 * See: http://mock-server.com/mock_server/initializing_expectations.html#maven_plugin_expectation_initializer_class
 */
@SuppressWarnings("deprecation")
public interface PluginExpectationInitializer extends ExpectationInitializer {

    void initializeExpectations(MockServerClient mockServerClient);

}
