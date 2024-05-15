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

import software.xdev.mockserver.version.Version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PortBinding extends ObjectWithJsonToString {

    private static final String VERSION = Version.getVersion();
    private static final String ARTIFACT_ID = Version.getArtifactId();
    private static final String GROUP_ID = Version.getGroupId();

    private List<Integer> ports = new ArrayList<>();
    private final String version = VERSION;
    private final String artifactId = ARTIFACT_ID;
    private final String groupId = GROUP_ID;

    public static PortBinding portBinding(Integer... ports) {
        return portBinding(Arrays.asList(ports));
    }

    public static PortBinding portBinding(List<Integer> ports) {
        return new PortBinding().setPorts(ports);
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public PortBinding setPorts(List<Integer> ports) {
        this.ports = ports;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }
}
