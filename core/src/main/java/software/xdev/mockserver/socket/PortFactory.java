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
package software.xdev.mockserver.socket;

import java.net.ServerSocket;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class PortFactory {

    private static final Random random = new Random();

    public static int findFreePort() {
        int[] freePorts = findFreePorts(1);
        return freePorts[random.nextInt(freePorts.length)];
    }

    @SuppressWarnings("SameParameterValue")
    private static int[] findFreePorts(int number) {
        int arraySize = number + random.nextInt(60);
        int[] port = new int[arraySize];
        ServerSocket[] serverSockets = new ServerSocket[arraySize];
        try {
            for (int i = port.length - 1; i >= 0; i--) {
                serverSockets[i] = new ServerSocket(0);
                port[i] = serverSockets[i].getLocalPort();
            }
            for (ServerSocket serverSocket : serverSockets) {
                serverSocket.close();
            }
            // allow time for the socket to be released
            TimeUnit.MILLISECONDS.sleep(250);
        } catch (Exception e) {
            throw new RuntimeException("Exception while trying to find a free port", e);
        }
        return port;
    }
}
