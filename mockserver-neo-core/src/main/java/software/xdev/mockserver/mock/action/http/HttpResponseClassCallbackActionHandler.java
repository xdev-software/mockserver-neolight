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
package software.xdev.mockserver.mock.action.http;

import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.mock.action.ExpectationResponseCallback;
import software.xdev.mockserver.model.HttpClassCallback;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import org.slf4j.event.Level;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static software.xdev.mockserver.model.HttpResponse.notFoundResponse;

public class HttpResponseClassCallbackActionHandler {

    private static ClassLoader contextClassLoader = ClassLoader.getSystemClassLoader();

    public static void setContextClassLoader(ClassLoader contextClassLoader) {
        HttpResponseClassCallbackActionHandler.contextClassLoader = contextClassLoader;
    }

    private final MockServerLogger mockServerLogger;

    public HttpResponseClassCallbackActionHandler(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
    }

    public HttpResponse handle(HttpClassCallback httpClassCallback, HttpRequest request) {
        return invokeCallbackMethod(httpClassCallback, request);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ExpectationResponseCallback instantiateCallback(HttpClassCallback httpClassCallback) {
        try {
            Class expectationResponseCallbackClass = contextClassLoader.loadClass(httpClassCallback.getCallbackClass());
            if (ExpectationResponseCallback.class.isAssignableFrom(expectationResponseCallbackClass)) {
                Constructor<? extends ExpectationResponseCallback> constructor = expectationResponseCallbackClass.getConstructor();
                return constructor.newInstance();
            } else {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setHttpRequest(null)
                        .setMessageFormat(httpClassCallback.getCallbackClass() + " does not implement " + ExpectationResponseCallback.class.getName() + " required for responses using class callback")
                );
            }
        } catch (ClassNotFoundException e) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("ClassNotFoundException - while trying to instantiate ExpectationResponseCallback class \"" + httpClassCallback.getCallbackClass() + "\"")
                    .setThrowable(e)
            );
        } catch (NoSuchMethodException e) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("NoSuchMethodException - while trying to create default constructor on ExpectationResponseCallback class \"" + httpClassCallback.getCallbackClass() + "\"")
                    .setThrowable(e)
            );
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("InvocationTargetException - while trying to execute default constructor on ExpectationResponseCallback class \"" + httpClassCallback.getCallbackClass() + "\"")
                    .setThrowable(e)
            );
        }
        return null;
    }

    private HttpResponse invokeCallbackMethod(HttpClassCallback httpClassCallback, HttpRequest httpRequest) {
        if (httpRequest != null) {
            ExpectationResponseCallback expectationResponseCallback = instantiateCallback(httpClassCallback);
            if (expectationResponseCallback != null) {
                try {
                    return expectationResponseCallback.handle(httpRequest);
                } catch (Throwable throwable) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.ERROR)
                            .setHttpRequest(httpRequest)
                            .setMessageFormat(httpClassCallback.getCallbackClass() + " throw exception while executing handle callback method - " + throwable.getMessage())
                            .setThrowable(throwable)
                    );
                    return notFoundResponse();
                }
            } else {
                return notFoundResponse();
            }
        } else {
            return notFoundResponse();
        }
    }
}
