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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public abstract class ObjectWithReflectiveEqualsHashCodeToString {

    private static final String[] IGNORE_KEY_FIELD = {};

    protected String[] fieldsExcludedFromEqualsAndHashCode() {
        return IGNORE_KEY_FIELD;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE, null, ObjectWithReflectiveEqualsHashCodeToString.class, false, false).setExcludeFieldNames(fieldsExcludedFromEqualsAndHashCode()).toString();
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        return new EqualsBuilder()
                    .setExcludeFields(fieldsExcludedFromEqualsAndHashCode())
                    .setReflectUpToClass(ObjectWithReflectiveEqualsHashCodeToString.class)
                    .setTestTransients(false)
                    .setTestRecursive(false)
                    .reflectionAppend(this, other)
                    .isEquals();
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, fieldsExcludedFromEqualsAndHashCode());
    }

}
