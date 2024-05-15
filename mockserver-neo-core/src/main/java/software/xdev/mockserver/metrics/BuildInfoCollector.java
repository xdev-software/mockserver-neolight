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
package software.xdev.mockserver.metrics;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import software.xdev.mockserver.version.Version;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;

public class BuildInfoCollector extends Collector {

    public List<Collector.MetricFamilySamples> collect() {
        String version = Version.getVersion();
        String majorMinorVersion = Version.getMajorMinorVersion();
        String groupId = Version.getGroupId();
        String artifactId = Version.getArtifactId();

        return Collections.singletonList(
            new GaugeMetricFamily(
                "mock_server_build_info",
                "Mock Server build information",
                asList(
                    "version",
                    "major_minor_version",
                    "group_id",
                    "artifact_id"
                )
            ).addMetric(asList(
                version != null ? version : "unknown",
                majorMinorVersion != null ? majorMinorVersion : "unknown",
                groupId != null ? groupId : "unknown",
                artifactId != null ? artifactId : "unknown"
            ), 1L)
        );
    }

}