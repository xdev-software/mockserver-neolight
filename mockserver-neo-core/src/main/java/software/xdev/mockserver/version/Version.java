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
package software.xdev.mockserver.version;

import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.StringUtils.isNotBlank;


/**
 * @deprecated Class was FILTERED before!
 * Needs refactor
 */
@Deprecated
public final class Version {

    private static final String VERSION = "${project.version}";
    private static final String GROUPID = "${project.groupId}";
    private static final String ARTIFACTID = "${project.artifactId}";
    private static String majorMinorVersion = null;

    private static String getValue(String value, String defaultValue) {
        if (!value.startsWith("$")) {
            return value;
        } else {
            return defaultValue;
        }
    }

    public static String getVersion() {
        return getValue(VERSION, System.getProperty("MOCKSERVER_VERSION", ""));
    }

    public static String getMajorMinorVersion() {
        if (majorMinorVersion == null) {
            majorMinorVersion = StringUtils.substringBeforeLast(getValue(VERSION, System.getProperty("MOCKSERVER_VERSION", "")), ".");
        }
        return majorMinorVersion;
    }

    public static boolean matchesMajorMinorVersion(String version) {
        boolean matches = true;
        if (isNotBlank(version) && isNotBlank(getMajorMinorVersion())) {
            matches = getMajorMinorVersion().equals(StringUtils.substringBeforeLast(version, "."));
        }
        return matches;
    }

    public static String getGroupId() {
        return getValue(GROUPID, "");
    }

    public static String getArtifactId() {
        return getValue(ARTIFACTID, "");
    }

}
